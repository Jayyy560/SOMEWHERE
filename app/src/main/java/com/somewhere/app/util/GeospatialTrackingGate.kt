package com.somewhere.app.util

/** Enter after a stable fix; wider exit thresholds avoid flickering at the boundary. */
internal class GeospatialTrackingGate {
    private var stableSince: Long? = null
    var isLocalized: Boolean = false
        private set

    fun update(
        tracking: Boolean,
        horizontalAccuracyMeters: Double,
        yawAccuracyDegrees: Double,
        nowMillis: Long
    ): Boolean {
        val horizontalLimit = if (isLocalized) 15.0 else 10.0
        val yawLimit = if (isLocalized) 20.0 else 15.0
        if (!tracking || !horizontalAccuracyMeters.isFinite() || !yawAccuracyDegrees.isFinite() ||
            horizontalAccuracyMeters <= 0.0 || yawAccuracyDegrees < 0.0 ||
            horizontalAccuracyMeters > horizontalLimit || yawAccuracyDegrees > yawLimit
        ) {
            reset()
            return false
        }
        if (stableSince == null) stableSince = nowMillis
        if (nowMillis - stableSince!! >= 1_000L) isLocalized = true
        return isLocalized
    }

    fun reset() {
        stableSince = null
        isLocalized = false
    }
}
