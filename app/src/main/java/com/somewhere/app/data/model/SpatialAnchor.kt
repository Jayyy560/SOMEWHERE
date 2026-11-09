package com.somewhere.app.data.model

import kotlinx.serialization.Serializable

/** WGS84 position and east-up-south orientation of the tapped surface, when VPS is available. */
@Serializable
data class GeospatialAnchorPose(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val qx: Float,
    val qy: Float,
    val qz: Float,
    val qw: Float
) {
    val isValid: Boolean
        get() = latitude.isFinite() && latitude in -89.9..89.9 &&
            longitude.isFinite() && longitude in -180.0..180.0 && altitudeMeters.isFinite() &&
            listOf(qx, qy, qz, qw).all { it.isFinite() } &&
            (qx * qx + qy * qy + qz * qz + qw * qw) in 0.9f..1.1f
}

@Serializable
data class SpatialAnchor(
    val cloudAnchorId: String,
    val expiresAtMillis: Long,
    val geospatialPose: GeospatialAnchorPose? = null
) {
    fun canResolveAt(nowMillis: Long): Boolean = cloudAnchorId.isNotBlank() && expiresAtMillis > nowMillis
}

/** A hosted visual anchor plus the geographic coordinate used to discover it. */
@Serializable
data class VisualPlacement(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val capturedAtMillis: Long,
    val provider: String,
    val anchor: SpatialAnchor
) {
    fun isUsableAt(nowMillis: Long): Boolean = latitude.isFinite() && latitude in -90.0..90.0 &&
        longitude.isFinite() && longitude in -180.0..180.0 &&
        accuracyMeters.isFinite() && accuracyMeters > 0f && anchor.canResolveAt(nowMillis)
}
