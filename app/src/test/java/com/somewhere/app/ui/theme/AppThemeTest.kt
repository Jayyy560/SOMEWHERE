package com.somewhere.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class AppThemeTest {
    @Test
    fun missingPreferenceFallsBackToLiquid() {
        assertEquals(AppTheme.LIQUID, AppTheme.fromStoredValue(null))
    }

    @Test
    fun unknownPreferenceFallsBackToLiquid() {
        assertEquals(AppTheme.LIQUID, AppTheme.fromStoredValue("not-a-theme"))
    }

    @Test
    fun foldPreferenceRestoresFold() {
        assertEquals(AppTheme.FOLD, AppTheme.fromStoredValue("fold"))
    }
}
