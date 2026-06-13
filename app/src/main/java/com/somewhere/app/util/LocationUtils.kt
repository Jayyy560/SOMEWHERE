package com.somewhere.app.util

import kotlin.math.*

/**
 * Distance and bearing calculations for GPS coordinates.
 */
object LocationUtils {

    const val DISCOVERY_RADIUS = 15f   // meters — max radius to show drops
    const val UNLOCK_RADIUS = 8f       // meters — drops become tappable within this
    const val MAX_VISIBLE = 3          // max overlays shown at once
    const val ACCURACY_WARNING_METERS = 35f

    /**
     * Haversine distance between two GPS points in meters.
     */
    fun haversineDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val r = 6_371_000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c).toFloat()
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
        return if (meters < 5f) {
            "here"
        } else {
            "${meters.roundToInt()}m"
        }
    }
}
