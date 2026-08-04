package com.somewhere.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class StableLocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val capturedAtMillis: Long,
    val provider: String?
)

/**
 * Collects fresh fused-location samples for up to ten seconds and returns an
 * accuracy-weighted centre. The reported accuracy never claims to be better
 * than Android's best source reading, because nearby GPS errors are correlated.
 */
@SuppressLint("MissingPermission")
suspend fun collectStableLocation(
    context: Context,
    onProgress: (StableLocationFix) -> Unit = {}
): StableLocationFix? = suspendCancellableCoroutine { continuation ->
    val handler = Handler(Looper.getMainLooper())
    val client = LocationServices.getFusedLocationProviderClient(context)
    val samples = mutableListOf<Location>()
    var bestEstimate: StableLocationFix? = null
    var completed = false
    var callback: LocationCallback? = null
    var timeoutRunnable: Runnable? = null

    fun cleanup() {
        timeoutRunnable?.let(handler::removeCallbacks)
        callback?.let(client::removeLocationUpdates)
    }

    fun finish(result: StableLocationFix?) {
        if (completed) return
        completed = true
        cleanup()
        if (continuation.isActive) continuation.resume(result)
    }

    callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { location ->
                val ageMillis = (
                    SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
                ) / 1_000_000L
                val isFresh = ageMillis in 0..10_000
                val hasUsableAccuracy = location.hasAccuracy() &&
                    location.accuracy.isFinite() &&
                    location.accuracy in 1f..100f
                if (!isFresh || !hasUsableAccuracy) return@forEach

                samples += location
                while (samples.size > 12) samples.removeAt(0)

                val estimate = estimateStableLocation(samples)
                bestEstimate = estimate
                onProgress(estimate)

                val isStable = samples.size >= 4 &&
                    estimate.accuracyMeters <= 20f &&
                    sampleSpreadMeters(samples, estimate) <= 10f
                if (isStable) finish(estimate)
            }
        }
    }
    timeoutRunnable = Runnable { finish(bestEstimate) }

    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 800L)
        .setMinUpdateIntervalMillis(400L)
        .setWaitForAccurateLocation(true)
        .setMaxUpdates(20)
        .build()

    try {
        client.requestLocationUpdates(request, callback!!, Looper.getMainLooper())
            .addOnFailureListener { finish(bestEstimate) }
        handler.postDelayed(timeoutRunnable!!, 10_000L)
    } catch (_: SecurityException) {
        finish(null)
    } catch (_: Exception) {
        finish(bestEstimate)
    }

    continuation.invokeOnCancellation {
        if (!completed) {
            completed = true
            cleanup()
        }
    }
}

private fun estimateStableLocation(samples: List<Location>): StableLocationFix {
    val weights = samples.map { sample ->
        1.0 / (sample.accuracy.toDouble() * sample.accuracy.toDouble()).coerceAtLeast(1.0)
    }
    val totalWeight = weights.sum().coerceAtLeast(1e-9)
    val latitude = samples.indices.sumOf { index -> samples[index].latitude * weights[index] } /
        totalWeight
    val longitude = samples.indices.sumOf { index -> samples[index].longitude * weights[index] } /
        totalWeight
    val bestSample = samples.minBy { it.accuracy }
    val provisional = StableLocationFix(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = bestSample.accuracy,
        capturedAtMillis = samples.maxOf { it.time },
        provider = bestSample.provider
    )
    return provisional.copy(
        accuracyMeters = maxOf(bestSample.accuracy, sampleSpreadMeters(samples, provisional))
    )
}

private fun sampleSpreadMeters(
    samples: List<Location>,
    centre: StableLocationFix
): Float {
    if (samples.isEmpty()) return Float.MAX_VALUE
    val distances = samples.map { sample ->
        LocationUtils.haversineDistance(
            centre.latitude,
            centre.longitude,
            sample.latitude,
            sample.longitude
        )
    }.sorted()
    val percentileIndex = (distances.lastIndex * 0.75f).toInt()
    return distances[percentileIndex]
}
