package com.somewhere.app.ui.screen

import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.ar.core.*
import com.google.ar.core.Anchor
import com.somewhere.app.BuildConfig
import com.somewhere.app.data.model.GeospatialAnchorPose
import com.somewhere.app.data.model.SpatialAnchor
import com.somewhere.app.data.model.VisualPlacement
import com.somewhere.app.ui.component.SpatialDataConsent
import com.somewhere.app.util.ARUtils
import com.somewhere.app.util.StableLocationFix
import com.somewhere.app.util.VisualPlacementQualityGate
import io.github.sceneview.ar.ARSceneView
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException

/** A separate, session-owned camera: cancelling leaves the photo/message untouched. */
@Composable
fun VisualPlacementScreen(
    gpsFix: StableLocationFix,
    onPlaced: (VisualPlacement) -> Unit,
    onClose: () -> Unit
) {
    val placedCallback by rememberUpdatedState(onPlaced)
    val closeCallback by rememberUpdatedState(onClose)
    val context = LocalContext.current
    val controller = remember(gpsFix) { VisualPlacementController(gpsFix) { placedCallback(it) } }
    var arAvailable by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(context) {
        arAvailable = try {
            val apk = ArCoreApk.getInstance()
            var availability = apk.checkAvailability(context)
            var attempts = 0
            while (availability.isTransient && attempts++ < 15) {
                delay(200L)
                availability = apk.checkAvailability(context)
            }
            availability == ArCoreApk.Availability.SUPPORTED_INSTALLED
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
        if (arAvailable == false) controller.reset("Visual placement needs a supported phone with Google Play Services for AR installed and updated. Your draft is still safe; close this scan and retry after setup.")
    }
    val owner = LocalLifecycleOwner.current
    val markerRadius = with(LocalDensity.current) { 14.dp.toPx().toInt() }
    DisposableEffect(owner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                controller.reset()
                closeCallback()
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            controller.reset()
        }
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        SpatialDataConsent(onDecline = onClose, content = {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                if (arAvailable == true) AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    onRelease = { scene ->
                        scene.onSessionUpdated = null
                        controller.reset()
                        runCatching { scene.session?.pause() }
                        scene.destroy()
                    },
                    factory = { context ->
                        ARSceneView(context).apply {
                            planeRenderer.isVisible = true
                            configureSession { session, config ->
                                config.cloudAnchorMode = Config.CloudAnchorMode.ENABLED
                                config.geospatialMode = if (session.isGeospatialModeSupported(Config.GeospatialMode.ENABLED)) {
                                    Config.GeospatialMode.ENABLED
                                } else Config.GeospatialMode.DISABLED
                                if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                                    config.depthMode = Config.DepthMode.AUTOMATIC
                                }
                            }
                            onSessionFailed = {
                                controller.reset("AR is unavailable. Install/update Google Play Services for AR, then retry. ${it.javaClass.simpleName}")
                            }
                            onSessionUpdated = { session, frame -> controller.update(session, frame, width, height) }
                            setOnTouchListener { _, event ->
                                if (event.action == MotionEvent.ACTION_UP) {
                                    frame?.let { controller.tap(it, event.x, event.y) }
                                    performClick()
                                }
                                true
                            }
                        }
                    }
                )
                if (arAvailable == null) CircularProgressIndicator(Modifier.align(Alignment.Center))
                controller.marker?.let { (x, y) ->
                    Box(Modifier.offset { IntOffset(x.toInt() - markerRadius, y.toInt() - markerRadius) }
                        .size(28.dp).background(Color(0xFF8FFFC1), CircleShape))
                }
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .background(Color.Black.copy(alpha = .8f)).navigationBarsPadding().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Place the Drop in the real world", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Text(controller.error ?: controller.message, color = Color.White)
                    Text("Move slowly around the spot. Scan textured, stationary surfaces from several angles; avoid blank walls and moving objects.", color = Color.LightGray)
                    if (controller.hosting) LinearProgressIndicator(Modifier.fillMaxWidth())
                    Button(onClick = controller::host, enabled = controller.canHost && !controller.hosting,
                        modifier = Modifier.fillMaxWidth()) {
                        Text(if (controller.hosting) "Saving visual anchor…" else "Use this spot")
                    }
                    TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Cancel scan") }
                }
            }
        })
    }
}

private class VisualPlacementController(
    private val gps: StableLocationFix,
    private val onPlaced: (VisualPlacement) -> Unit
) {
    var marker by mutableStateOf<Pair<Float, Float>?>(null)
        private set
    var message by mutableStateOf("Scan the surroundings, then tap a detected surface within 8 metres.")
        private set
    var canHost by mutableStateOf(false)
        private set
    var hosting by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var session: Session? = null
    private var anchor: Anchor? = null
    private var future: HostCloudAnchorFuture? = null
    private var startedAt = 0L
    private var hostedAt = 0L
    private var lastUiAt = 0L
    private var completed = false
    private var origin: FloatArray? = null
    private val qualityGate = VisualPlacementQualityGate()

    fun tap(frame: Frame, x: Float, y: Float) {
        if (hosting || completed || frame.camera.trackingState != TrackingState.TRACKING) return
        error = null
        val hit = frame.hitTest(x, y).firstOrNull { hit ->
            hit.distance in .2f..8f && when (val surface = hit.trackable) {
                is Plane -> surface.trackingState == TrackingState.TRACKING && surface.isPoseInPolygon(hit.hitPose)
                is Point -> surface.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
                is DepthPoint -> surface.trackingState == TrackingState.TRACKING
                else -> false
            }
        }
        if (hit == null) {
            message = "No stable surface there yet. Move slowly and tap a textured surface nearby."
            return
        }
        runCatching { hit.createAnchor() }.onSuccess { selected ->
            anchor?.detach()
            anchor = selected
            qualityGate.reset()
            canHost = false
            message = "Spot selected. Keep scanning around it to capture visual detail."
        }.onFailure { message = "Tracking changed. Scan and tap the spot again." }
    }

    fun update(session: Session, frame: Frame, width: Int, height: Int) {
        this.session = session
        if (completed) return
        if (origin == null && frame.camera.trackingState == TrackingState.TRACKING) {
            origin = frame.camera.pose.translation
        }
        val selected = anchor
        val tracking = frame.camera.trackingState == TrackingState.TRACKING && selected?.trackingState == TrackingState.TRACKING
        val now = SystemClock.elapsedRealtime()
        if (hosting) {
            val request = future ?: return
            if (request.state == FutureState.DONE) {
                future = null
                hosting = false
                if (request.resultCloudAnchorState == Anchor.CloudAnchorState.SUCCESS && selected != null) {
                    val id = request.resultCloudAnchorId
                    if (!id.isNullOrBlank()) {
                        val placement = placement(session, selected, frame, id)
                        if (placement.isUsableAt(System.currentTimeMillis())) {
                            completed = true
                            onPlaced(placement)
                        } else error = "The geographic search location is invalid. Close the scan and refresh location before retrying."
                    } else error = "The service returned no anchor ID. Please retry."
                } else {
                    error = cloudAnchorErrorMessage(request.resultCloudAnchorState)
                }
            } else if (now - startedAt > 60_000L || request.state == FutureState.CANCELLED) {
                request.cancel()
                future = null
                hosting = false
                error = "Hosting timed out. Check the internet connection, scan again, and retry."
            }
        }
        if (now - lastUiAt < 100L) return
        lastUiAt = now
        if (!tracking) {
            qualityGate.reset()
            marker = null
            canHost = false
            if (!hosting) message = "Move slowly to regain tracking, then tap a detected surface."
            return
        }
        val view = FloatArray(16)
        val projection = FloatArray(16)
        frame.camera.getViewMatrix(view, 0)
        frame.camera.getProjectionMatrix(projection, 0, .1f, 100f)
        marker = ARUtils.projectToScreen(selected.pose.translation, view, projection, width, height)
        if (!hosting) {
            val quality = runCatching { session.estimateFeatureMapQualityForHosting(frame.camera.pose) }.getOrNull()
            val visible = marker?.let { it.first in 0f..width.toFloat() && it.second in 0f..height.toFloat() } == true
            canHost = qualityGate.update(tracking, quality == Session.FeatureMapQuality.GOOD, visible, now)
            if (canHost) {
                message = "Visual detail captured. Save this spot, or tap to move it."
            } else if (!canHost) message = "Keep scanning the selected spot from different angles. More visual detail is needed."
        }
    }

    fun host() {
        val current = session ?: return
        val selected = anchor ?: return
        if (!canHost || hosting || completed || selected.trackingState != TrackingState.TRACKING) return
        error = null
        try {
            hostedAt = System.currentTimeMillis()
            future = current.hostCloudAnchorAsync(selected, BuildConfig.CLOUD_ANCHOR_TTL_DAYS, null)
            startedAt = SystemClock.elapsedRealtime()
            hosting = true
            message = "Saving the visual map. Keep the camera pointed around the spot."
        } catch (exception: Exception) {
            error = "Hosting failed (${exception.javaClass.simpleName}). Check ARCore authorization and the connection."
        }
    }

    private fun placement(session: Session, anchor: Anchor, frame: Frame, id: String): VisualPlacement {
        val earth = session.earth
        val geo = runCatching {
            if (earth?.trackingState != TrackingState.TRACKING) return@runCatching null
            val pose = earth.getGeospatialPose(anchor.pose)
            if (!pose.horizontalAccuracy.isFinite() || !pose.verticalAccuracy.isFinite() ||
                !pose.orientationYawAccuracy.isFinite() || pose.horizontalAccuracy > 10.0 ||
                pose.verticalAccuracy > 5.0 || pose.orientationYawAccuracy > 15.0) return@runCatching null
            val q = pose.eastUpSouthQuaternion
            GeospatialAnchorPose(pose.latitude, pose.longitude, pose.altitude, q[0], q[1], q[2], q[3])
                .takeIf { it.isValid }?.let { it to pose.horizontalAccuracy.toFloat().coerceAtLeast(.1f) }
        }.getOrNull()
        val camera = origin ?: frame.camera.pose.translation
        val point = anchor.pose.translation
        val distance = sqrt(point.indices.sumOf { i -> ((point[i] - camera[i]) * (point[i] - camera[i])).toDouble() }).toFloat()
        return VisualPlacement(
            latitude = geo?.first?.latitude ?: gps.latitude,
            longitude = geo?.first?.longitude ?: gps.longitude,
            accuracyMeters = geo?.second ?: (gps.accuracyMeters + distance),
            capturedAtMillis = System.currentTimeMillis(),
            provider = if (geo != null) "arcore-geospatial-cloud" else "arcore-cloud-gps",
            anchor = SpatialAnchor(id, hostedAt + BuildConfig.CLOUD_ANCHOR_TTL_DAYS * 86_400_000L, geo?.first)
        )
    }

    fun reset(reason: String? = null) {
        future?.cancel()
        future = null
        anchor?.let { runCatching { it.detach() } }
        anchor = null
        session = null
        origin = null
        qualityGate.reset()
        marker = null
        hosting = false
        canHost = false
        error = reason
    }
}

internal fun cloudAnchorErrorMessage(state: Anchor.CloudAnchorState): String = when (state) {
    Anchor.CloudAnchorState.ERROR_NOT_AUTHORIZED -> "Cloud Anchor authorization failed. Register this app's package and signing certificate in Google Cloud."
    Anchor.CloudAnchorState.ERROR_RESOURCE_EXHAUSTED -> "Cloud Anchor quota reached. Try again later."
    Anchor.CloudAnchorState.ERROR_HOSTING_DATASET_PROCESSING_FAILED -> "Hosting failed: not enough stable visual detail. Tap again and scan textured surfaces."
    else -> "Hosting failed ($state). Check the connection, scan the spot again, and retry."
}
