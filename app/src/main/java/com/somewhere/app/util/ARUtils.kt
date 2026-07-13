package com.somewhere.app.util

import android.opengl.Matrix
import com.google.ar.core.Camera
import com.google.ar.core.Pose
import kotlin.math.*

/**
 * Projects GPS drops onto the screen using ARCore camera matrices.
 *
 * Architecture:
 * 1. CALIBRATE: Determine where geographic North is in ARCore's world (first ~2 sec: fast convergence)
 * 2. PLACE:     Compute each drop's FIXED 3D world position (on GPS update or new drop)
 * 3. PROJECT:   Every frame, project fixed positions through View*Projection → screen pixels
 *
 * Key insight: Once a drop's world position is stored, ARCore's VIO tracking keeps it
 * locked to that spot automatically via the view matrix. No recomputation needed.
 */
object ARUtils {

    // --- Calibration state ---
    private var cachedNorthAngle: Double? = null
    private var calibrationFrameCount = 0
    private const val WARMUP_FRAMES = 60  // ~1 second at 60fps before we trust the calibration

    // --- Drop world positions ---
    private val dropWorldPositions = mutableMapOf<String, FloatArray>()
    private var lastCalibrationLat: Double = 0.0
    private var lastCalibrationLon: Double = 0.0
    private var lastDropIds: Set<String> = emptySet()

    fun resetCalibration() {
        cachedNorthAngle = null
        calibrationFrameCount = 0
        dropWorldPositions.clear()
        lastCalibrationLat = 0.0
        lastCalibrationLon = 0.0
        lastDropIds = emptySet()
    }

    private fun wrapAngle(rad: Double): Double {
        var r = rad
        while (r > Math.PI) r -= 2.0 * Math.PI
        while (r < -Math.PI) r += 2.0 * Math.PI
        return r
    }

    /**
     * Returns true once the calibration has warmed up enough to trust.
     */
    fun isCalibrated(): Boolean = calibrationFrameCount >= WARMUP_FRAMES

    /**
     * Calibrate where geographic North is in ARCore's world coordinate system.
     *
     * Uses a two-phase convergence strategy:
     * - First WARMUP_FRAMES frames: 10% blend (fast convergence, reaches accuracy in ~1 sec)
     * - After warmup: 0.5% blend (very slow drift correction, keeps stability)
     */
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
            // Fast convergence during warmup only.
            // After warmup, we STOP listening to the compass entirely to prevent wobble.
            // ARCore SLAM will keep the world locked.
            val diff = wrapAngle(rawNorthAngle - cached)
            cachedNorthAngle = cached + diff * 0.10
        }
    }

    /**
     * Recompute drop world positions when needed:
     * - GPS moved >2 meters
     * - New drops appeared in the list
     * - First time computing
     *
     * This is the ONLY place world positions are calculated. They are then
     * stored and reused for every subsequent frame.
     */
    fun recomputeAllPositions(
        cameraPose: Pose,
        userLat: Double, userLon: Double,
        drops: List<Pair<String, Pair<Double, Double>>>,
        heightOffsets: Map<String, Float>
    ) {
        val northAngle = cachedNorthAngle ?: return
        if (!isCalibrated()) return  // Don't place drops until calibration is stable

        val currentIds = drops.map { it.first }.toSet()
        val newDropIds = currentIds.filter { it !in dropWorldPositions }
        
        // Only compute positions for brand NEW drops.
        // Once a drop is placed, its world position is permanently locked to ARCore's SLAM tracking.
        // We completely ignore future GPS updates for existing drops to prevent jumping.
        if (newDropIds.isEmpty()) {
            // Clean up old drops
            dropWorldPositions.keys.removeAll { it !in currentIds }
            return
        }

        lastCalibrationLat = userLat
        lastCalibrationLon = userLon
        lastDropIds = currentIds

        val cameraPos = cameraPose.translation

        // Only compute and place the new drops
        drops.filter { it.first in newDropIds }.forEach { (id, coords) ->
            val bearing = LocationUtils.bearing(userLat, userLon, coords.first, coords.second)
            val distance = LocationUtils.haversineDistance(userLat, userLon, coords.first, coords.second)

            val bearingRad = Math.toRadians(bearing.toDouble())
            val dropAngleInWorld = northAngle + bearingRad
            val clampedDistance = distance.coerceIn(3f, 40f)
            val heightOffset = heightOffsets[id] ?: -0.2f

            val dropX = cameraPos[0] + (clampedDistance * sin(dropAngleInWorld)).toFloat()
            val dropY = cameraPos[1] + heightOffset
            val dropZ = cameraPos[2] + (-clampedDistance * cos(dropAngleInWorld)).toFloat()

            dropWorldPositions[id] = floatArrayOf(dropX, dropY, dropZ)
        }

        // Clean up positions for drops no longer in the list
        dropWorldPositions.keys.removeAll { it !in currentIds }
    }

    fun getWorldPosition(dropId: String): FloatArray? {
        return dropWorldPositions[dropId]
    }
    fun hasWorldPositions(): Boolean = dropWorldPositions.isNotEmpty()

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
