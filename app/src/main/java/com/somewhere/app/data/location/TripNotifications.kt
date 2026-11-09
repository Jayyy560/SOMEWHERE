package com.somewhere.app.data.location

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.somewhere.app.MainActivity
import com.somewhere.app.R

object TripNotifications {
    @android.annotation.SuppressLint("MissingPermission")
    fun approaching(context: Context, dropId: String, haptics: Boolean) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = if (haptics) "wayfinder_discoveries" else "wayfinder_discoveries_quiet"
        manager.createNotificationChannel(NotificationChannel(channelId, if (haptics) "Wayfinder discoveries" else "Wayfinder discoveries without vibration", NotificationManager.IMPORTANCE_DEFAULT).apply {
            enableVibration(haptics); description = "Upcoming Drops during an active trip"
        })
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_drop_id", dropId)
        }
        val pending = PendingIntent.getActivity(context, dropId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(dropId.hashCode(), NotificationCompat.Builder(context, channelId).setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Drop ahead").setContentText("A matching Drop is coming up on your trip.")
            .setContentIntent(pending).setAutoCancel(true).build())
    }
}
