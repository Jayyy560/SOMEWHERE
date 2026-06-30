package com.somewhere.app.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.maps.model.LatLng
import com.somewhere.app.data.location.TripForegroundService
import com.somewhere.app.data.location.TripManager
import com.somewhere.app.data.remote.NearbyDrop
import kotlinx.coroutines.flow.StateFlow

data class TripUiState(
    val phase: TripPhase = TripPhase.INPUT,
    val promptText: String = "",
    val useCurrentLocation: Boolean = true,
    
    // Autocomplete state
    val destinationSuggestions: List<String> = emptyList(),
    val autocompleteTarget: String? = null, // "from" or "to" based on what the user is currently typing
    
    val isLoading: Boolean = false,
    val error: String? = null,

    // Route data
    val routePolyline: List<LatLng> = emptyList(),
    val routeDuration: String = "",
    val routeDistance: String = "",
    val routeSummary: String = "",
    val startAddress: String = "",
    val endAddress: String = "",

    // Drops along route
    val allDropsOnRoute: List<NearbyDrop> = emptyList(),
    val filteredDrops: List<NearbyDrop> = emptyList(),
    val selectedDrop: NearbyDrop? = null,

    // Navigation mode
    val isNavigating: Boolean = false,
    val currentUserLocation: LatLng? = null,
    val approachingDrop: NearbyDrop? = null,
    
    // Recalculation
    val isRecalculating: Boolean = false,
    val offRouteCounter: Int = 0
)

enum class TripPhase {
    INPUT,       // User entering origin/destination/query
    ROUTE_VIEW,  // Route calculated, showing map with drops
    NAVIGATING   // Active trip with geofence alerts
}

class TripViewModel(application: Application) : AndroidViewModel(application) {

    init {
        TripManager.initialize(application.applicationContext)
    }

    val uiState: StateFlow<TripUiState> = TripManager.uiState

    fun updatePrompt(text: String, cursorPosition: Int = text.length) = TripManager.updatePrompt(text, cursorPosition)
    fun selectSuggestion(suggestion: String) = TripManager.selectSuggestion(suggestion)
    fun toggleUseCurrentLocation() = TripManager.toggleUseCurrentLocation()
    fun clearError() = TripManager.clearError()
    fun resetTrip() = TripManager.resetTrip()

    fun routeToDrop(drop: NearbyDrop, currentLat: Double? = null, currentLng: Double? = null) {
        TripManager.routeToDrop(drop, currentLat, currentLng)
    }

    fun searchRoute(currentLat: Double? = null, currentLng: Double? = null) {
        TripManager.searchRoute(currentLat, currentLng)
    }

    fun selectDrop(drop: NearbyDrop?) = TripManager.selectDrop(drop)

    fun startNavigation() {
        TripManager.startNavigation(getApplication())
        // Start persistent foreground service
        val intent = Intent(getApplication(), TripForegroundService::class.java)
        getApplication<Application>().startForegroundService(intent)
    }

    fun stopNavigation() {
        TripManager.stopNavigation()
        // Stop persistent foreground service
        val intent = Intent(getApplication(), TripForegroundService::class.java)
        getApplication<Application>().stopService(intent)
    }

    fun dismissApproachingDrop() = TripManager.dismissApproachingDrop()
    
    // Fallback UI updates just in case (the Service handles live location, but we can keep this for Compose)
    fun updateUserLocation(latLng: LatLng) = TripManager.updateUserLocation(latLng)
}
