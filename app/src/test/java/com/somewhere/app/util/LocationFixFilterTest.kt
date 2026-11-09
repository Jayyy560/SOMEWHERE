package com.somewhere.app.util

import org.junit.Assert.*
import org.junit.Test

class LocationFixFilterTest {
    @Test fun staleOrOutOfOrderReadingsCannotRefreshTheLocation() {
        val filter = LocationFixFilter()
        val sample = LocationSample(28.6, 77.2, 5f, 40_000L)
        filter.update(sample, 40_000L)
        assertNull(filter.update(sample.copy(elapsedMillis = 39_000L), 40_000L))
        assertNull(filter.update(sample, 71_000L))
        assertNull(filter.update(sample.copy(latitude = Double.NaN), 40_000L))
    }

    @Test fun worseningGpsDoesNotKeepThePreviousOptimisticAccuracy() {
        val filter = LocationFixFilter()
        filter.update(LocationSample(28.6, 77.2, 5f, 1_000L), 1_000L)
        val fix = filter.update(LocationSample(28.6001, 77.2, 50f, 2_000L), 2_000L)!!
        assertTrue(fix.accuracyMeters >= 50f)
        assertTrue(fix.latitude > 28.6 && fix.latitude < 28.6001)
    }

    @Test fun isolatedJumpIsHeldUntilACorroboratingReading() {
        val filter = LocationFixFilter()
        filter.update(LocationSample(28.6, 77.2, 5f, 1_000L), 1_000L)
        val held = filter.update(LocationSample(28.61, 77.2, 5f, 2_000L), 2_000L)!!
        assertEquals(28.6, held.latitude, 0.0)
        val confirmed = filter.update(LocationSample(28.61001, 77.2, 5f, 3_000L), 3_000L)!!
        assertEquals(28.61001, confirmed.latitude, 0.0)
    }
}
