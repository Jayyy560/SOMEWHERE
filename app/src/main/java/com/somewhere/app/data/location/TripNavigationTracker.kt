package com.somewhere.app.data.location

import com.somewhere.app.util.LocationFixFilter
import com.somewhere.app.util.LocationSample
import kotlin.math.max

/** Pure navigation state, driven by monotonic sample times so GPS replay tests are deterministic. */
class TripNavigationTracker(mode: TravelMode) {
    private val filter = when (mode) {
        TravelMode.WALKING -> LocationFixFilter(5.0, 2.5)
        TravelMode.CYCLING -> LocationFixFilter(25.0, 6.0)
        TravelMode.DRIVING -> LocationFixFilter(65.0, 12.0)
    }
    private val baseTolerance = when (mode) { TravelMode.WALKING -> 25.0; TravelMode.CYCLING -> 35.0; TravelMode.DRIVING -> 50.0 }
    private var progress = 0.0
    private var previousPoint: TripPoint? = null
    private var offRouteSince: Long? = null
    private var lastReroute = -30_000L
    private var offCount = 0
    private val dwellStarted = mutableMapOf<String, Long>()
    data class Update(val point: TripPoint, val accuracyMeters: Float, val progressMeters: Double, val shouldReroute: Boolean)

    fun update(sample: LocationSample, nowElapsedMillis: Long, route: List<TripPoint>): Update? {
        if (nowElapsedMillis - sample.elapsedMillis !in 0..10_000L) return null
        val fix = filter.update(sample, nowElapsedMillis) ?: return null
        if (fix.elapsedMillis != sample.elapsedMillis) return null // isolated jump held by the filter
        val point = TripPoint(fix.latitude, fix.longitude)
        val movement = previousPoint?.let { TripGeometry.distance(it, point) } ?: 0.0
        previousPoint = point
        var reroute = false
        if (route.size >= 2 && fix.accuracyMeters <= 75f) {
            val projection = TripGeometry.project(point, route, (progress - 30).coerceAtLeast(0.0), progress + max(120.0, movement * 2 + fix.accuracyMeters))
            val tolerance = max(baseTolerance, fix.accuracyMeters * 1.5)
            if (projection.distanceMeters > tolerance) {
                if (offRouteSince == null) offRouteSince = nowElapsedMillis
                offCount++
                if (offCount >= 3 && nowElapsedMillis - offRouteSince!! >= 8000 && nowElapsedMillis - lastReroute >= 30_000) {
                    reroute = true; lastReroute = nowElapsedMillis; offRouteSince = null; offCount = 0
                }
            } else {
                progress = max(progress, projection.progressMeters)
                offRouteSince = null; offCount = 0
            }
        } else { offRouteSince = null; offCount = 0 }
        return Update(point, fix.accuracyMeters, progress, reroute)
    }
    fun resetRouteProgress() { progress = 0.0; previousPoint = null; offRouteSince = null; offCount = 0 }
    fun isVisit(id: String, distanceMeters: Double, accuracyMeters: Float, elapsedMillis: Long): Boolean {
        if (accuracyMeters > 25 || distanceMeters > 30) { dwellStarted.remove(id); return false }
        val start = dwellStarted.getOrPut(id) { elapsedMillis }
        return elapsedMillis - start >= 8000
    }
}
