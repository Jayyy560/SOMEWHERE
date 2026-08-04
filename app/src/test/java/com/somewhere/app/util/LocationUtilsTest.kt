package com.somewhere.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationUtilsTest {

    @Test
    fun exactFixUsesPhysicalUnlockRadius() {
        assertEquals(
            LocationUtils.UNLOCK_RADIUS,
            LocationUtils.effectiveUnlockRadius(0f),
            0f
        )
        assertTrue(LocationUtils.isWithinUnlockRange(30f, 0f))
        assertFalse(LocationUtils.isWithinUnlockRange(30.1f, 0f))
    }

    @Test
    fun reportedAccuracyPreventsSameSpotFalseLock() {
        assertEquals(46f, LocationUtils.effectiveUnlockRadius(16f), 0f)
        assertTrue(LocationUtils.isWithinUnlockRange(42f, 16f))
    }

    @Test
    fun poorAccuracyCannotCreateUnboundedUnlockRadius() {
        assertEquals(
            LocationUtils.UNLOCK_RADIUS + LocationUtils.MAX_UNLOCK_ACCURACY_BUFFER,
            LocationUtils.effectiveUnlockRadius(250f),
            0f
        )
        assertFalse(LocationUtils.isWithinUnlockRange(60f, 250f))
    }

    @Test
    fun placementAndViewerAccuracyAreCombined() {
        val expected = LocationUtils.UNLOCK_RADIUS + kotlin.math.sqrt(16f * 16f + 12f * 12f)
        assertEquals(expected, LocationUtils.effectiveUnlockRadius(16f, 12f), 0.001f)
        assertTrue(LocationUtils.isWithinUnlockRange(expected, 16f, 12f))
        assertFalse(LocationUtils.isWithinUnlockRange(expected + 0.1f, 16f, 12f))
    }

    @Test
    fun combinedAccuracyRemainsCapped() {
        assertEquals(
            LocationUtils.UNLOCK_RADIUS + LocationUtils.MAX_UNLOCK_ACCURACY_BUFFER,
            LocationUtils.effectiveUnlockRadius(40f, 40f),
            0f
        )
    }
}
