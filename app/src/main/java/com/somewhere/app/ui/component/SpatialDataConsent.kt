package com.somewhere.app.ui.component

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/** Disclose Google's sensor processing before any Cloud/Geospatial session starts. */
@Composable
fun SpatialDataConsent(content: @Composable () -> Unit, onDecline: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("spatial_privacy", 0) }
    var accepted by remember { mutableStateOf(prefs.getBoolean("google_sensor_notice_v1", false)) }
    if (accepted) {
        content()
    } else {
        AlertDialog(
            onDismissRequest = onDecline,
            title = { Text("Recognize real-world places") },
            text = {
                Text("To power this session, Google will process sensor data (e.g., camera and location).\n\nVisual anchors use your surroundings to place and rediscover Drops. Avoid scanning private or sensitive spaces.")
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.edit().putBoolean("google_sensor_notice_v1", true).apply()
                    accepted = true
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = onDecline) { Text("Cancel") }
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(
                        "https://support.google.com/ar?p=how-google-play-services-for-ar-handles-your-data"
                    )))
                }) { Text("Learn more") }
            }
        )
    }
}
