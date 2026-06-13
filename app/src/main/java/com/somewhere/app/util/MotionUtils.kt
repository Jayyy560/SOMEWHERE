package com.somewhere.app.util

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

object MotionUtils {
    fun isReduceMotionEnabled(context: Context): Boolean {
        return runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE
            ) == 0f
        }.getOrDefault(false)
    }
}

@Composable
fun rememberReduceMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) { MotionUtils.isReduceMotionEnabled(context) }
}
