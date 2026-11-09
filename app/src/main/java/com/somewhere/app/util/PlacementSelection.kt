package com.somewhere.app.util

internal data class PlacementPin(val latitude: Double, val longitude: Double, val selectedAtMillis: Long)

internal data class PlacementLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val capturedAtMillis: Long,
    val provider: String?
)

/** Keep a person's pin separate from sensor updates and their measured uncertainty. */
internal data class PlacementSelection(
    val gpsFix: StableLocationFix? = null,
    val pin: PlacementPin? = null
) {
    val location: PlacementLocation?
        get() = pin?.let {
            // GPS accuracy describes the GPS point, not a different point chosen on a map.
            PlacementLocation(it.latitude, it.longitude, null, it.selectedAtMillis, "manual-map")
        } ?: gpsFix?.let {
            PlacementLocation(it.latitude, it.longitude, it.accuracyMeters, it.capturedAtMillis, it.provider)
        }

    fun withPin(latitude: Double, longitude: Double, nowMillis: Long): PlacementSelection {
        if (gpsFix == null || !latitude.isFinite() || latitude !in -90.0..90.0 ||
            !longitude.isFinite() || longitude !in -180.0..180.0
        ) return this
        return copy(pin = PlacementPin(latitude, longitude, nowMillis))
    }
}
