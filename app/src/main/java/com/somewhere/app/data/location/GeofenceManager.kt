package com.somewhere.app.data.location

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.somewhere.app.data.remote.NearbyDrop
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A rolling window leaves room under Android's per-app limit and replaces old registrations in order. */
class GeofenceManager(private val context: Context) {
    private val client = LocationServices.getGeofencingClient(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private val operations = Mutex()
    private val pendingIntent by lazy {
        PendingIntent.getBroadcast(context, 0, Intent(context, GeofenceBroadcastReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }
    @android.annotation.SuppressLint("MissingPermission")
    fun registerGeofences(drops: List<NearbyDrop>, mode: TravelMode, onError: (String) -> Unit = {}) {
        job?.cancel()
        job = scope.launch {
            try { operations.withLock {
                withContext(NonCancellable) { client.removeGeofences(pendingIntent).await() }
                ensureActive()
                if (drops.isEmpty()) return@launch
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return@launch
                if (Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // Foreground location tracking still supplies the same accuracy-checked alerts.
                    return@launch
                }
                val radius = when (mode) { TravelMode.WALKING -> 150f; TravelMode.CYCLING -> 350f; TravelMode.DRIVING -> 1000f }
                val fences = drops.distinctBy { it.id }.take(90).map {
                    Geofence.Builder().setRequestId(it.id).setCircularRegion(it.latitude,it.longitude,radius)
                        .setExpirationDuration(6 * 60 * 60 * 1000L).setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER).build()
                }
                withContext(NonCancellable) { client.addGeofences(GeofencingRequest.Builder().setInitialTrigger(0).addGeofences(fences).build(),pendingIntent).await() }
            } } catch (e: CancellationException) { throw e }
            catch (_: Exception) { onError("Background proximity alerts are unavailable. Live trip tracking is still active.") }
        }
    }
    fun clearGeofences() {
        job?.cancel()
        job = scope.launch { try { operations.withLock { withContext(NonCancellable) { client.removeGeofences(pendingIntent).await() } } } catch (e: CancellationException) { throw e } catch (_: Exception) {} }
    }
}
