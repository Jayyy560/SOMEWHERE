package com.somewhere.app.util

import kotlin.math.*

/**
 * Distance and bearing calculations for GPS coordinates.
 */
object LocationUtils {

    const val DISCOVERY_RADIUS = 500f   // meters — max radius to show drops
    const val UNLOCK_RADIUS = 30f       // meters — drops become tappable within this (generous to handle GPS drift)
    const val MAX_VISIBLE = 15          // max overlays shown at once
    const val ACCURACY_WARNING_METERS = 35f
    const val HYSTERESIS_MARGIN = 20f   // extra meters before removing a visible drop

    /**
     * Haversine distance between two GPS points in meters.
     */
    fun haversineDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    /**
     * Bearing from point 1 to point 2 in degrees (0 = north, clockwise).
     * Used to position overlay cards relative to device heading.
     */
    fun bearing(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val dLon = Math.toRadians(lon2 - lon1)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val y = sin(dLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) -
                sin(lat1Rad) * cos(lat2Rad) * cos(dLon)
        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 360) % 360).toFloat()
    }

    /**
     * Angular difference between device heading and drop bearing,
     * normalized to [-180, 180]. Used for horizontal overlay positioning.
     */
    fun angleDifference(heading: Float, bearing: Float): Float {
        var diff = bearing - heading
        while (diff > 180) diff -= 360
        while (diff < -180) diff += 360
        return diff
    }

    /**
     * Format distance for display: "4m away", "12m away"
     */
    fun formatDistance(meters: Float): String {
        return if (meters <= UNLOCK_RADIUS) {
            "here"
        } else {
            "${meters.roundToInt()}m"
        }
    }
}
