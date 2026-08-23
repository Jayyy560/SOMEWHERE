package com.somewhere.app.ui.theme

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppTheme(val storedValue: String, val displayName: String) {
    LIQUID("liquid", "Somewhere"),
    FOLD("fold", "Fold");

    companion object {
        fun fromStoredValue(value: String?): AppTheme =
            entries.firstOrNull { it.storedValue == value } ?: LIQUID
    }
}

/** The current Liquid theme is deliberately the fallback for every unknown state. */
val LocalAppTheme = staticCompositionLocalOf { AppTheme.LIQUID }

/**
 * Device-local theme preference. It is intentionally independent from account
 * and authentication state, so signing out or switching accounts cannot reset it.
 */
object AppThemePreferences {
    private const val PREFS_NAME = "somewhere_appearance"
    private const val KEY_THEME = "selected_theme"

    private val _selectedTheme = MutableStateFlow(AppTheme.LIQUID)
    val selectedTheme: StateFlow<AppTheme> = _selectedTheme.asStateFlow()

    private var appContext: Context? = null

    fun initialize(context: Context) {
        val applicationContext = context.applicationContext
        appContext = applicationContext
        val storedValue = applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, null)
        val restoredTheme = AppTheme.fromStoredValue(storedValue)
        SomewhereColors.activate(restoredTheme)
        _selectedTheme.value = restoredTheme
    }

    fun select(theme: AppTheme) {
        val context = checkNotNull(appContext) {
            "AppThemePreferences must be initialized before selecting a theme"
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, theme.storedValue)
            .commit()
        SomewhereColors.activate(theme)
        _selectedTheme.value = theme
    }
}
