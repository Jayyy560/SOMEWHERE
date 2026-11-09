package com.somewhere.app.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.maps.model.LatLng
import com.somewhere.app.data.location.TripForegroundService
import com.somewhere.app.data.location.TripManager
import com.somewhere.app.data.remote.NearbyDrop
import com.somewhere.app.data.location.*
import com.somewhere.app.data.remote.SupabaseManager
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

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
    val offRouteCounter: Int = 0,
    val options: TripOptions = TripOptions(),
    val recommendations: Map<String, RankedTripDrop> = emptyMap(),
    val itineraryStops: List<NearbyDrop> = emptyList(),
    val routeProgressMeters: Double = 0.0,
    val locationAccuracyMeters: Float? = null,
    val visitedDropIds: Set<String> = emptySet(),
    val savedTrips: List<SavedTrip> = emptyList(),
    val communityTrails: List<CuratedTrail> = emptyList(),
    val resumableTripId: String? = null,
    val currentUserId: String? = null,
    val isLoadingTrails: Boolean = false,
    val isPublishingTrail: Boolean = false,
    val isSavingPhotos: Boolean = false,
    val planningMessage: String = "",
    val notice: String? = null
)

enum class TripPhase {
    INPUT,       // User entering origin/destination/query
    ROUTE_VIEW,  // Route calculated, showing map with drops
    NAVIGATING   // Active trip with geofence alerts
}

class TripViewModel(application: Application) : AndroidViewModel(application) {

    init {
        TripManager.initialize(application.applicationContext)
        viewModelScope.launch {
            SupabaseManager.client.auth.sessionStatus.collect { TripManager.refreshAccount() }
        }
    }

    val uiState: StateFlow<TripUiState> = TripManager.uiState

    fun updatePrompt(text: String, cursorPosition: Int = text.length) = TripManager.updatePrompt(text, cursorPosition)
    fun selectSuggestion(suggestion: String) = TripManager.selectSuggestion(suggestion)
    fun toggleUseCurrentLocation() = TripManager.toggleUseCurrentLocation()
    fun clearError() = TripManager.clearError()
    fun resetTrip() = TripManager.resetTrip()
    
    fun startLocationTracking() = TripManager.startLocationTracking(getApplication())
    fun stopLocationTracking() = TripManager.stopLocationTracking()

    fun routeToDrop(drop: NearbyDrop, currentLat: Double? = null, currentLng: Double? = null) {
        TripManager.routeToDrop(drop, currentLat, currentLng)
    }

    fun searchRoute(currentLat: Double? = null, currentLng: Double? = null) {
        TripManager.searchRoute(currentLat, currentLng)
    }

    fun selectDrop(drop: NearbyDrop?) = TripManager.selectDrop(drop)

    fun startNavigation() {
        TripManager.startNavigation()
    }

    fun stopNavigation() {
        TripManager.stopNavigation()
    }

    fun dismissApproachingDrop() = TripManager.dismissApproachingDrop()
    
    fun setListNavigation(enabled: Boolean) = TripManager.setListNavigation(enabled)
    fun updateOptions(options: TripOptions) = TripManager.updateOptions(options)
    fun addStop(drop: NearbyDrop) = TripManager.addStop(drop)
    fun removeStop(dropId: String) = TripManager.removeStop(dropId)
    fun recordVisit(dropId: String) = TripManager.recordVisit(dropId)
    fun saveTrip(title: String) = TripManager.saveTrip(title)
    fun deleteSavedTrip(id: String) = TripManager.deleteSavedTrip(id)
    fun addJournalPhotos(id: String, uris: List<android.net.Uri>) = TripManager.addJournalPhotos(id, uris)
    fun removeJournalPhoto(id: String, path: String) = TripManager.removeJournalPhoto(id, path)
    fun updateJournal(id: String, note: String, stopId: String? = null) = TripManager.updateJournal(id, note, stopId)
    fun openSavedTrip(trip: SavedTrip) = TripManager.openSavedTrip(trip)
    fun loadTrails() = TripManager.loadTrails()
    fun openTrail(trail: CuratedTrail) = TripManager.openTrail(trail)
    fun publishTrail(title: String, description: String) = TripManager.publishTrail(title, description)
    fun setStepFreeAccess(drop: NearbyDrop, value: Boolean?) = TripManager.setStepFreeAccess(drop, value)
}
