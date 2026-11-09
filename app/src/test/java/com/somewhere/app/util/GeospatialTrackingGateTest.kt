package com.somewhere.app.util

import org.junit.Assert.*
import org.junit.Test

class GeospatialTrackingGateTest {
    @Test fun localizationRequiresAStableInterval() {
        val gate = GeospatialTrackingGate()
        assertFalse(gate.update(true, 5.0, 5.0, 100L))
        assertFalse(gate.update(true, 5.0, 5.0, 1_099L))
        assertTrue(gate.update(true, 5.0, 5.0, 1_100L))
    }

    @Test fun slightlyWorseFixDoesNotFlickerButTrackingLossResetsImmediately() {
        val gate = GeospatialTrackingGate()
        gate.update(true, 5.0, 5.0, 0L)
        assertTrue(gate.update(true, 5.0, 5.0, 1_000L))
        assertTrue(gate.update(true, 12.0, 18.0, 1_100L))
        assertFalse(gate.update(false, 5.0, 5.0, 1_200L))
        assertFalse(gate.update(true, 5.0, 5.0, 1_300L))
        assertTrue(gate.update(true, 5.0, 5.0, 2_300L))
    }

    @Test fun invalidOrInaccuratePoseCannotLocalize() {
        val gate = GeospatialTrackingGate()
        gate.update(true, 5.0, 5.0, 0L)
        assertFalse(gate.update(true, Double.NaN, 5.0, 1_000L))
        assertFalse(gate.update(true, 12.0, 5.0, 2_000L))
        assertFalse(gate.update(true, 5.0, Double.POSITIVE_INFINITY, 3_000L))
        assertFalse(gate.update(true, 0.0, 5.0, 4_000L))
    }
}
