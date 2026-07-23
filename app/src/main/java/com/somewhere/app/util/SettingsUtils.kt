package com.somewhere.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object SettingsUtils {
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

object PermissionRequestHistory {
    private const val PREFS_NAME = "permission_request_history"

    fun wasRequested(context: Context, permission: String): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(permission, false)

    fun markRequested(context: Context, permissions: Collection<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .apply {
                permissions.forEach { putBoolean(it, true) }
            }
            .apply()
    }
}
