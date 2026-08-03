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
}
