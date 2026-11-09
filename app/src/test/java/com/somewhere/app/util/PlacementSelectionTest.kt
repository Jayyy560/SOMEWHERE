package com.somewhere.app.util

import org.junit.Assert.*
import org.junit.Test

class PlacementSelectionTest {
    private val gps = StableLocationFix(28.6, 77.2, 12f, 1_000L, "fused")

    @Test fun sensorUpdateDoesNotOverwriteTheChosenPin() {
        val selected = PlacementSelection(gps).withPin(28.6001, 77.2001, 2_000L)
        val updated = selected.copy(gpsFix = gps.copy(latitude = 28.61, accuracyMeters = 5f))
        assertEquals(28.6001, updated.location!!.latitude, 0.0)
        assertEquals(77.2001, updated.location!!.longitude, 0.0)
        assertEquals(2_000L, updated.location!!.capturedAtMillis)
    }

    @Test fun manualPinDoesNotInheritGpsConfidence() {
        val selected = PlacementSelection(gps).withPin(28.6001, 77.2001, 2_000L)
        assertNull(selected.location!!.accuracyMeters)
        assertEquals("manual-map", selected.location!!.provider)
        val restored = selected.copy(pin = null).location!!
        assertEquals(12f, restored.accuracyMeters!!, 0f)
        assertEquals("fused", restored.provider)
        assertEquals(1_000L, restored.capturedAtMillis)
    }

    @Test fun invalidCoordinateCannotReplaceSelection() {
        val selected = PlacementSelection(gps)
        assertSame(selected, selected.withPin(Double.NaN, 77.2, 2_000L))
        assertSame(selected, selected.withPin(91.0, 77.2, 2_000L))
        assertSame(selected, selected.withPin(28.6, 181.0, 2_000L))
    }
}
