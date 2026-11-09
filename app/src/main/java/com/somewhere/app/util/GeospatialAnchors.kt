package com.somewhere.app.util

import android.os.SystemClock
import com.google.ar.core.*

/** Session-owned visual and geographic anchors. Cloud matching does not require Earth/VPS. */
internal class GeospatialAnchors {
    private fun store(timeout: Long) = AnchorRequestStore<Anchor>(
        nowMillis = SystemClock::elapsedRealtime,
        detach = { it.detach() },
        isStopped = { it.trackingState == TrackingState.STOPPED },
        requestTimeoutMillis = timeout
    )
    private val cloud = store(45_000L)
    private val earthAnchors = store(20_000L)
    var cloudError: Anchor.CloudAnchorState? = null
        private set

    fun update(session: Session, earth: Earth?, localized: Boolean, targets: List<AnchorTarget>) {
        val now = System.currentTimeMillis()
        cloud.update(targets.filter { it.spatialAnchor?.canResolveAt(now) == true }) { target ->
            val future = session.resolveCloudAnchorAsync(target.spatialAnchor!!.cloudAnchorId, null)
            object : AnchorRequest<Anchor> {
                private var result: AnchorRequestResult<Anchor>? = null
                override fun poll(): AnchorRequestResult<Anchor> {
                    result?.let { return it }
                    val terminal = when (future.state) {
                        FutureState.DONE -> {
                            val state = future.resultCloudAnchorState
                            val anchor = future.resultAnchor
                            if (state == Anchor.CloudAnchorState.SUCCESS && anchor != null) {
                                AnchorRequestResult.Resolved(anchor)
                            } else {
                                anchor?.detach()
                                cloudError = state
                                AnchorRequestResult.Failed(cloudFailureIsRetryable(state))
                            }
                        }
                        FutureState.CANCELLED -> AnchorRequestResult.Failed(true)
                        else -> AnchorRequestResult.Pending
                    }
                    if (terminal !is AnchorRequestResult.Pending) result = terminal
                    return terminal
                }
                override fun cancel() { future.cancel() }
            }
        }
        // Never invent an outdoor terrain position for a visual-only indoor anchor:
        // that would put it on the wrong wall/floor while recognition is pending.
        if (localized && earth != null) {
            earthAnchors.update(targets.filter { it.hasEarthPlacement }) { target ->
                val pose = target.spatialAnchor?.geospatialPose
                if (pose != null) {
                    val anchor = earth.createAnchor(pose.latitude, pose.longitude, pose.altitudeMeters,
                        pose.qx, pose.qy, pose.qz, pose.qw)
                    object : AnchorRequest<Anchor> {
                        override fun poll() = AnchorRequestResult.Resolved(anchor)
                        override fun cancel() { }
                    }
                } else terrainRequest(earth, target)
            }
        } else {
            earthAnchors.clear()
        }
    }

    private fun terrainRequest(earth: Earth, target: AnchorTarget): AnchorRequest<Anchor> {
        val future = earth.resolveAnchorOnTerrainAsync(target.latitude, target.longitude,
            LABEL_HEIGHT_METERS, 0f, 0f, 0f, 1f, null)
        return object : AnchorRequest<Anchor> {
            private var terminalResult: AnchorRequestResult<Anchor>? = null
            override fun poll(): AnchorRequestResult<Anchor> {
                terminalResult?.let { return it }
                val result = when (future.state) {
                    FutureState.DONE -> {
                        val state = future.resultTerrainAnchorState
                        val anchor = future.resultAnchor
                        if (state == Anchor.TerrainAnchorState.SUCCESS && anchor != null) {
                            AnchorRequestResult.Resolved(anchor)
                        } else {
                            anchor?.detach()
                            AnchorRequestResult.Failed(state != Anchor.TerrainAnchorState.ERROR_UNSUPPORTED_LOCATION &&
                                state != Anchor.TerrainAnchorState.ERROR_NOT_AUTHORIZED)
                        }
                    }
                    FutureState.CANCELLED -> AnchorRequestResult.Failed(true)
                    else -> AnchorRequestResult.Pending
                }
                if (result !is AnchorRequestResult.Pending) terminalResult = result
                return result
            }
            override fun cancel() { future.cancel() }
        }
    }

    private fun tracked(store: AnchorRequestStore<Anchor>): Map<String, FloatArray> = store.anchors().mapNotNull { (id, anchor) ->
        if (anchor.trackingState == TrackingState.TRACKING) id to anchor.pose.translation else null
    }.toMap()

    fun positions(): Map<String, FloatArray> = tracked(earthAnchors) + tracked(cloud)
    fun visualPositions(): Map<String, FloatArray> = tracked(cloud)

    fun clear() {
        cloud.clear()
        earthAnchors.clear()
        cloudError = null
    }

    companion object {
        const val LABEL_HEIGHT_METERS = 1.35
    }
}

internal fun cloudFailureIsRetryable(state: Anchor.CloudAnchorState): Boolean = when (state) {
    Anchor.CloudAnchorState.ERROR_NOT_AUTHORIZED,
    Anchor.CloudAnchorState.ERROR_CLOUD_ID_NOT_FOUND,
    Anchor.CloudAnchorState.ERROR_RESOLVING_SDK_VERSION_TOO_NEW,
    Anchor.CloudAnchorState.ERROR_RESOLVING_SDK_VERSION_TOO_OLD -> false
    else -> true
}
