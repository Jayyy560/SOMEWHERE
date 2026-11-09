package com.somewhere.app.data.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

/** Geofences wake the same accuracy-checked tracker; they never bypass progress or cooldown rules. */
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError() || event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return
        TripManager.initialize(context)
        if (!TripManager.uiState.value.isNavigating) return
        event.triggeringLocation?.let(TripManager::updateLocation)
    }
}
