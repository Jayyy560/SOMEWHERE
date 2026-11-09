package com.somewhere.app.data.location

import kotlin.math.*

/** Metre-based geometry, independent of Android and Google Maps for replay tests. */
object TripGeometry {
    private const val EARTH = 6371000.0
    fun distance(a: TripPoint, b: TripPoint): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(wrappedLongitude(b.longitude - a.longitude))
        val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.latitude)) *
            cos(Math.toRadians(b.latitude)) * sin(dLon / 2).pow(2)
        return 2 * EARTH * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }
    data class Projection(val distanceMeters: Double, val progressMeters: Double, val segment: Int)

    fun project(point: TripPoint, line: List<TripPoint>, minProgressMeters: Double = 0.0, maxProgressMeters: Double = Double.POSITIVE_INFINITY): Projection {
        if (line.isEmpty()) return Projection(Double.POSITIVE_INFINITY, 0.0, 0)
        if (line.size == 1) return Projection(distance(point, line.first()), 0.0, 0)
        var best = Projection(Double.POSITIVE_INFINITY, 0.0, 0)
        var travelled = 0.0
        for (i in 0 until line.lastIndex) {
            val a = line[i]
            val b = line[i + 1]
            val length = distance(a, b)
            if (travelled > maxProgressMeters) break
            if (travelled + length < minProgressMeters) { travelled += length; continue }
            val scale = cos(Math.toRadians(point.latitude)).coerceAtLeast(0.00001)
            fun x(p: TripPoint) = Math.toRadians(wrappedLongitude(p.longitude - point.longitude)) * EARTH * scale
            fun y(p: TripPoint) = Math.toRadians(p.latitude - point.latitude) * EARTH
            val ax = x(a); val ay = y(a); val dx = x(b) - ax; val dy = y(b) - ay
            val denominator = dx * dx + dy * dy
            val tMin = if (length > 0) ((minProgressMeters - travelled) / length).coerceIn(0.0, 1.0) else 0.0
            val tMax = if (length > 0) ((maxProgressMeters - travelled) / length).coerceIn(tMin, 1.0) else 1.0
            val t = if (denominator == 0.0) 0.0 else (-(ax * dx + ay * dy) / denominator).coerceIn(tMin, tMax)
            val perpendicular = hypot(ax + t * dx, ay + t * dy)
            val progress = travelled + t * length
            // At crossings prefer the earlier occurrence; do not jump to the return leg of a loop.
            if (perpendicular < best.distanceMeters - 0.5) best = Projection(perpendicular, progress, i)
            travelled += length
        }
        return best
    }
    fun length(line: List<TripPoint>): Double = line.zipWithNext().sumOf { (a, b) -> distance(a, b) }
    private fun wrappedLongitude(degrees: Double) = (degrees + 540.0) % 360.0 - 180.0
}
