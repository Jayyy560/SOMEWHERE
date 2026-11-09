package com.somewhere.app.util

import kotlin.math.*

/** Monotonic timestamps prevent cached/late fixes from moving the user backwards. */
data class LocationSample(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val elapsedMillis: Long
)

/** Rejects isolated jumps, then smooths noise without claiming improved GPS precision. */
class LocationFixFilter(
    private val maxSpeedMetersPerSecond: Double = 10.0,
    private val motionNoiseMetersPerSecond: Double = 2.5
) {
    private var estimate: LocationSample? = null
    private var pendingJump: LocationSample? = null
    private var lastSeenMillis = -1L

    fun reset() {
        estimate = null
        pendingJump = null
        lastSeenMillis = -1L
    }

    fun update(sample: LocationSample, nowElapsedMillis: Long): LocationSample? {
        if (!sample.latitude.isFinite() || sample.latitude !in -90.0..90.0 ||
            !sample.longitude.isFinite() || sample.longitude !in -180.0..180.0 ||
            !sample.accuracyMeters.isFinite() || sample.accuracyMeters <= 0f || sample.accuracyMeters > 200f
        ) return null

        // Allow fixes up to 30s old to support rapid cold start from cached GPS
        val ageMillis = nowElapsedMillis - sample.elapsedMillis
        if (ageMillis !in -1_000L..30_000L) return null

        val previous = estimate
        if (previous == null) {
            estimate = sample
            pendingJump = null
            lastSeenMillis = sample.elapsedMillis
            return sample
        }

        // Late or repeated samples must never move the estimate backwards in time.
        if (sample.elapsedMillis <= lastSeenMillis) return null
        lastSeenMillis = sample.elapsedMillis

        val seconds = ((sample.elapsedMillis - previous.elapsedMillis).coerceAtLeast(0L)) / 1000.0
        if (seconds > 10.0) {
            // Long gap between fixes; take new sample directly
            estimate = sample
            pendingJump = null
            return sample
        }

        val displacement = distanceMeters(previous, sample)
        val jumpThreshold = max(35.0, previous.accuracyMeters + sample.accuracyMeters + maxSpeedMetersPerSecond * seconds)
        if (displacement > jumpThreshold) {
            val pending = pendingJump
            if (pending != null) {
                val pendingDist = distanceMeters(pending, sample)
                val timeSinceJump = sample.elapsedMillis - pending.elapsedMillis
                if (pendingDist <= max(25f, sample.accuracyMeters + pending.accuracyMeters) || timeSinceJump > 4_000L) {
                    // Two consecutive fixes confirm relocation, or timeout reached
                    estimate = sample
                    pendingJump = null
                    return sample
                }
            }
            pendingJump = sample
            return estimate // Maintain current smooth estimate while waiting for jump confirmation
        }

        pendingJump = null
        val previousVariance = previous.accuracyMeters.toDouble().pow(2) + (motionNoiseMetersPerSecond * seconds).pow(2)
        val sampleVariance = sample.accuracyMeters.toDouble().pow(2)
        val gain = (previousVariance / (previousVariance + sampleVariance)).coerceIn(0.15, 1.0)

        val longitudeDelta = (sample.longitude - previous.longitude + 540) % 360 - 180
        val smoothedPosition = sample.copy(
            latitude = previous.latitude + gain * (sample.latitude - previous.latitude),
            longitude = (previous.longitude + gain * longitudeDelta + 540) % 360 - 180
        )
        // Smoothing can introduce lag, not proof of extra GPS precision. Include the
        // displacement from the measured point instead of preserving an old small radius.
        val smoothed = smoothedPosition.copy(
            accuracyMeters = sample.accuracyMeters + distanceMeters(sample, smoothedPosition)
        )
        estimate = smoothed
        return estimate
    }

    private fun distanceMeters(a: LocationSample, b: LocationSample): Float {
        val lat = Math.toRadians(b.latitude - a.latitude)
        val lon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(lat / 2).pow(2) + cos(Math.toRadians(a.latitude)) *
            cos(Math.toRadians(b.latitude)) * sin(lon / 2).pow(2)
        return (12_742_000 * asin(sqrt(h.coerceIn(0.0, 1.0)))).toFloat()
    }
}
