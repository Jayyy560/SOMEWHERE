package com.somewhere.app.util

import android.opengl.Matrix
import com.google.ar.core.Camera
import com.google.ar.core.Pose
import kotlin.math.*

/**
 * Projects GPS drops onto the screen using ARCore camera matrices.
 *
 * Phase 1 Architecture:
 * 1. CALIBRATE: Determine where geographic North is in ARCore's world
 * 2. PLACE:     Compute each drop's 3D world position and attach a real ARCore Anchor.
 *               Co-located drops use deterministic fanning based on their ID.
 * 3. PROJECT:   Every frame, project the live Anchor translation through View*Projection -> pixels.
 */
object ARUtils {

    // --- Calibration state ---
    private var cachedNorthAngle: Double? = null
    private var calibrationFrameCount = 0
    private const val WARMUP_FRAMES = 60  // ~1 second at 60fps before we trust the calibration

    // --- Drop AR Anchors ---
    // Keep up to 50 anchors cached to prevent churn when drops briefly leave and re-enter view
    private val dropAnchors = object : LinkedHashMap<String, com.google.ar.core.Anchor>(50, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, com.google.ar.core.Anchor>?): Boolean {
            if (size > 50) {
                eldest?.value?.detach()
                return true
            }
            return false
        }
    }

    fun resetCalibration() {
        cachedNorthAngle = null
        calibrationFrameCount = 0
        dropAnchors.values.forEach { it.detach() }
        dropAnchors.clear()
    }

    private fun wrapAngle(rad: Double): Double {
        var r = rad
        while (r > Math.PI) r -= 2.0 * Math.PI
        while (r < -Math.PI) r += 2.0 * Math.PI
        return r
    }

    fun isCalibrated(): Boolean = calibrationFrameCount >= WARMUP_FRAMES

    fun calibrateNorth(cameraPose: Pose, headingDegrees: Float) {
        val localForward = floatArrayOf(0f, 0f, -1f)
        val worldForward = FloatArray(3)
        cameraPose.rotateVector(localForward, 0, worldForward, 0)

        val cameraAngleInWorld = atan2(
            worldForward[0].toDouble(),
            -worldForward[2].toDouble()
        )

        val headingRad = Math.toRadians(headingDegrees.toDouble())
        val rawNorthAngle = cameraAngleInWorld - headingRad

        calibrationFrameCount++

        val cached = cachedNorthAngle
        if (cached == null) {
            cachedNorthAngle = rawNorthAngle
        } else if (calibrationFrameCount < WARMUP_FRAMES) {
            val diff = wrapAngle(rawNorthAngle - cached)
            cachedNorthAngle = cached + diff * 0.10
        }
    }

    fun recomputeAllPositions(
        session: com.google.ar.core.Session,
        cameraPose: Pose,
        userLat: Double, userLon: Double,
        drops: List<Pair<String, Pair<Double, Double>>>
    ) {
        val northAngle = cachedNorthAngle ?: return
        if (!isCalibrated()) return

        val currentIds = drops.map { it.first }.toSet()
        val newDropIds = currentIds.filter { it !in dropAnchors }
        
        // We purposely do NOT detach anchors that leave currentIds here. 
        // This allows them to stay in the LRU cache so they don't jump if they re-enter.
        // They will be evicted naturally by the LRU if they stay out too long.

        if (newDropIds.isEmpty()) {
            return
        }

        val cameraPos = cameraPose.translation

        drops.filter { it.first in newDropIds }.forEach { (id, coords) ->
            val distance = LocationUtils.haversineDistance(userLat, userLon, coords.first, coords.second)
            
            val bearingRad: Double
            val heightOffset: Float
            val placementDistance: Float
            
            // Deterministic placement for co-located drops
            if (distance <= LocationUtils.CO_LOCATED_METERS) {
                // Use hash of drop ID to create a stable fan-out pattern
                val hash = abs(id.hashCode())
                val angleOffset = (hash % 360).toDouble()
                bearingRad = Math.toRadians(angleOffset)

                // Deterministic height offset between -0.75m and +0.75m
                // (kept small: at 2m away, ±1.2m looked like ceiling/floor)
                val heightIndex = (hash % 7) - 3 // -3 to +3
                heightOffset = heightIndex * 0.25f

                // CRITICAL: anchor co-located drops INSIDE the room (1.5–3.5m),
                // not at their noisy GPS distance — parallax becomes visible
                // and cards stop being anchored beyond the walls.
                placementDistance = 1.5f + (((hash / 360) % 100) / 100f) * 2f
            } else {
                val bearing = LocationUtils.bearing(userLat, userLon, coords.first, coords.second)
                bearingRad = Math.toRadians(bearing.toDouble())
                heightOffset = -0.2f
                placementDistance = distance.coerceIn(2f, 25f)
            }

            val dropAngleInWorld = northAngle + bearingRad

            val dropX = cameraPos[0] + (placementDistance * sin(dropAngleInWorld)).toFloat()
            val dropY = cameraPos[1] + heightOffset
            val dropZ = cameraPos[2] + (-placementDistance * cos(dropAngleInWorld)).toFloat()

            val pose = Pose.makeTranslation(dropX, dropY, dropZ)
            
            // Create anchor to let ARCore track it stably
            try {
                val anchor = session.createAnchor(pose)
                dropAnchors[id] = anchor
            } catch (e: Exception) {
                // Ignore tracking failures gracefully
            }
        }
    }

    fun getWorldPosition(dropId: String): FloatArray? {
        val anchor = dropAnchors[dropId] ?: return null
        // Allow PAUSED state so drops don't blink out when ARCore is readjusting
        if (anchor.trackingState == com.google.ar.core.TrackingState.STOPPED) {
            dropAnchors.remove(dropId)
            return null
        }
        return anchor.pose.translation
    }
    
    fun hasWorldPositions(): Boolean = dropAnchors.isNotEmpty()

    fun projectToScreen(
        worldPos: FloatArray,
        viewMatrix: FloatArray,
        projMatrix: FloatArray,
        screenWidthPx: Int,
        screenHeightPx: Int
    ): Pair<Float, Float>? {
        val modelViewProj = FloatArray(16)
        Matrix.multiplyMM(modelViewProj, 0, projMatrix, 0, viewMatrix, 0)
        
        val clipCoords = FloatArray(4)
        val worldCoords = floatArrayOf(worldPos[0], worldPos[1], worldPos[2], 1f)
        Matrix.multiplyMV(clipCoords, 0, modelViewProj, 0, worldCoords, 0)
        
        // If w <= 0, the point is behind the camera
        if (clipCoords[3] <= 0f) return null
        
        val ndcX = clipCoords[0] / clipCoords[3]
        val ndcY = clipCoords[1] / clipCoords[3]
        
        // If outside frustum (with some margin)
        if (ndcX < -2f || ndcX > 2f || ndcY < -2f || ndcY > 2f) return null
        
        val screenX = (ndcX + 1f) / 2f * screenWidthPx
        val screenY = (1f - ndcY) / 2f * screenHeightPx
        
        return Pair(screenX, screenY)
    }
}
