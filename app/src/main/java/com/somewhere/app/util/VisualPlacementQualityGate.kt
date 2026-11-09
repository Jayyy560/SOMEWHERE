package com.somewhere.app.util

/** Do not host on a single transient good frame or while looking away from the spot. */
internal class VisualPlacementQualityGate(private val stableForMillis: Long = 750L) {
    private var goodSince: Long? = null

    fun update(tracking: Boolean, featureMapGood: Boolean, spotVisible: Boolean, nowMillis: Long): Boolean {
        if (!tracking || !featureMapGood || !spotVisible) {
            reset()
            return false
        }
        val start = goodSince ?: nowMillis.also { goodSince = it }
        return nowMillis - start >= stableForMillis
    }

    fun reset() { goodSince = null }
}
