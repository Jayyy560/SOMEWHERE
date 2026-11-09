package com.somewhere.app.util

import kotlin.math.atan2
import kotlin.math.hypot

/** An immutable snapshot of the positions and camera matrices from one ARCore frame. */
internal data class SpatialFrame(
    val viewMatrix: FloatArray,
    val projectionMatrix: FloatArray,
    val cameraPosition: FloatArray,
    val positions: Map<String, FloatArray>,
    val widthPx: Int,
    val heightPx: Int
)

/** Projection math only; session-owned anchors live in GeospatialAnchors. */
object ARUtils {
    fun projectToScreen(
        worldPos: FloatArray,
        viewMatrix: FloatArray,
        projMatrix: FloatArray,
        screenWidthPx: Int,
        screenHeightPx: Int
    ): Pair<Float, Float>? {
        if (worldPos.size < 3 || viewMatrix.size != 16 || projMatrix.size != 16 ||
            screenWidthPx <= 0 || screenHeightPx <= 0 || worldPos.any { !it.isFinite() }
        ) return null
        val eye = multiply(viewMatrix, floatArrayOf(worldPos[0], worldPos[1], worldPos[2], 1f))
        val clip = multiply(projMatrix, eye)
        if (clip.any { !it.isFinite() } || clip[3] <= 0f) return null
        val ndcX = clip[0] / clip[3]
        val ndcY = clip[1] / clip[3]
        val ndcZ = clip[2] / clip[3]
        if (ndcZ !in -1f..1f) return null
        if (ndcX < -2f || ndcX > 2f || ndcY < -2f || ndcY > 2f) return null

        return Pair(
            (ndcX + 1f) / 2f * screenWidthPx,
            (1f - ndcY) / 2f * screenHeightPx
        )
    }

    /** Compass bearing of the camera's forward vector in east-up-south coordinates. */
    fun headingFromQuaternion(rotation: FloatArray): Float? {
        if (rotation.size != 4 || rotation.any { !it.isFinite() }) return null
        val (x, y, z, w) = rotation
        val east = -2.0 * (x * z + w * y)
        val south = -(1.0 - 2.0 * (x * x + y * y))
        if (hypot(east, south) < 0.05) return null
        return ((Math.toDegrees(atan2(east, -south)) + 360.0) % 360.0).toFloat()
    }

    private fun multiply(matrix: FloatArray, vector: FloatArray): FloatArray = FloatArray(4) { row ->
        matrix[row] * vector[0] + matrix[4 + row] * vector[1] +
            matrix[8 + row] * vector[2] + matrix[12 + row] * vector[3]
    }
}
