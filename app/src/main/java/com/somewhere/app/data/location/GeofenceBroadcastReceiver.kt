package com.somewhere.app.data.location

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.somewhere.app.MainActivity
import com.somewhere.app.R
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null || geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(
                geofencingEvent?.errorCode ?: -1
            )
            println("Geofence error: $errorMessage")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

            val pendingResult = goAsync()
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val db = com.somewhere.app.data.local.AppDatabase.getInstance(context)
                    val dropDao = db.dropDao()

                    // Fire notification for each drop entered
                    for (geofence in triggeringGeofences) {
                        val dropId = geofence.requestId
                        if (dropDao.getDropById(dropId) != null) {
                            continue // Ignore, they already got this one!
                        }
                        sendNotification(context, dropId)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendNotification(context: Context, dropId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "trip_mode_channel",
                "Trip Mode Discoveries",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when you approach a discovery drop on your route"
                enableLights(true)
                lightColor = Color.YELLOW
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Deep link back into the app
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // We can pass the dropId to open the drop natively
            putExtra("open_drop_id", dropId) 
        }
        
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            dropId.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, "trip_mode_channel")
            .setSmallIcon(R.mipmap.ic_launcher) // Use app icon
            .setContentTitle("Discovery Ahead!")
            .setContentText("You are approaching a drop that matches your trip query.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(dropId.hashCode(), builder.build())
    }
}
