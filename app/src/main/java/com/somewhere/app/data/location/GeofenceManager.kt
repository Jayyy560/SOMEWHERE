package com.somewhere.app.data.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.somewhere.app.data.remote.NearbyDrop

class GeofenceManager(private val context: Context) {
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        // FLAG_MUTABLE is required for Android 12+ if the intent is modified, but FLAG_UPDATE_CURRENT is usually sufficient
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * Registers a geofence around each drop.
     */
    @SuppressLint("MissingPermission")
    fun registerGeofences(drops: List<NearbyDrop>) {
        if (drops.isEmpty()) return

        val geofences = drops.map { drop ->
            Geofence.Builder()
                .setRequestId(drop.id) // unique ID for the geofence
                .setCircularRegion(
                    drop.latitude,
                    drop.longitude,
                    2000f // 2km radius to trigger slightly early on the highway
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        geofencingClient.addGeofences(request, geofencePendingIntent).run {
            addOnSuccessListener {
                // Geofences added
            }
            addOnFailureListener {
                it.printStackTrace()
            }
        }
    }

    /**
     * Removes all active trip geofences.
     */
    fun clearGeofences() {
        geofencingClient.removeGeofences(geofencePendingIntent)
    }
}
