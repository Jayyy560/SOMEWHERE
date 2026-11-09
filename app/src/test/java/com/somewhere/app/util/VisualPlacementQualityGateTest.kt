package com.somewhere.app.util

import org.junit.Assert.*
import org.junit.Test

class VisualPlacementQualityGateTest {
    @Test fun goodVisualDetailMustBeStableBeforeHosting() {
        val gate = VisualPlacementQualityGate()
        assertFalse(gate.update(true, true, true, 0L))
        assertFalse(gate.update(true, true, true, 749L))
        assertTrue(gate.update(true, true, true, 750L))
    }

    @Test fun trackingLossResetsTheWholeQualityWindow() {
        val gate = VisualPlacementQualityGate()
        gate.update(true, true, true, 0L)
        assertFalse(gate.update(false, true, true, 740L))
        assertFalse(gate.update(true, true, true, 750L))
        assertTrue(gate.update(true, true, true, 1500L))
    }

    @Test fun lookingAwayOrInsufficientDetailCannotHost() {
        val gate = VisualPlacementQualityGate(0L)
        assertFalse(gate.update(true, true, false, 0L))
        assertFalse(gate.update(true, false, true, 1L))
        assertTrue(gate.update(true, true, true, 2L))
    }

    @Test fun choosingADifferentSurfaceRequiresNewQualityConfirmation() {
        val gate = VisualPlacementQualityGate()
        gate.update(true, true, true, 0L)
        assertTrue(gate.update(true, true, true, 800L))
        gate.reset()
        assertFalse(gate.update(true, true, true, 801L))
    }
}
