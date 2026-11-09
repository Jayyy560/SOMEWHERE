package com.somewhere.app.data.location

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.somewhere.app.MainActivity
import com.somewhere.app.R

class TripForegroundService : Service() {

    private val CHANNEL_ID = "TripModeActiveChannel"
    private val NOTIFICATION_ID = 1001

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        TripManager.initialize(this)
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_TRIP") {
            TripManager.stopNavigation()
            stopSelf()
            return START_NOT_STICKY
        }

        if (!TripManager.uiState.value.isNavigating) { stopSelf(); return START_NOT_STICKY }
        // Start Foreground Notification
        startForeground(NOTIFICATION_ID, createNotification())

        TripManager.ensureNavigationTracking(this)
        // A killed process exposes a resumable itinerary; it must not restart an empty trip.
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, TripForegroundService::class.java).apply { action = "STOP_TRIP" },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val customView = android.widget.RemoteViews(packageName, R.layout.notification_trip_mode)
        customView.setOnClickPendingIntent(R.id.notification_stop_btn, stopIntent)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(customView)
            .setPriority(NotificationCompat.PRIORITY_LOW) // LOW so it doesn't pop up and annoy
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active Trip",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Trip Mode running in the background"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
