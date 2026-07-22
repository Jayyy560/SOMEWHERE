    package com.somewhere.app.data.location

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.somewhere.app.data.remote.NearbyDrop
import com.somewhere.app.data.repository.TripRepository
import com.somewhere.app.viewmodel.TripPhase
import com.somewhere.app.viewmodel.TripUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Global Singleton to hold the trip state even when the UI is destroyed.
 */
object TripManager {

    private var repository: TripRepository? = null
    private var geofenceManager: GeofenceManager? = null
    private var searchJob: kotlinx.coroutines.Job? = null
    private var routeSearchJob: kotlinx.coroutines.Job? = null
    private var locationClient: com.google.android.gms.location.FusedLocationProviderClient? = null
    private var locationCallback: com.google.android.gms.location.LocationCallback? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _uiState = MutableStateFlow(TripUiState())
    val uiState: StateFlow<TripUiState> = _uiState.asStateFlow()

    fun initialize(context: Context) {
        if (repository == null) {
            repository = TripRepository(context.applicationContext)
            geofenceManager = GeofenceManager(context.applicationContext)
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun startLocationTracking(context: Context) {
        if (locationClient == null) {
            locationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context.applicationContext)
        }
        
        if (locationCallback == null) {
            locationCallback = object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    result.lastLocation?.let { loc ->
                        val latLng = LatLng(loc.latitude, loc.longitude)
                        updateUserLocation(latLng)
                    }
                }
            }
            val request = com.google.android.gms.location.LocationRequest.Builder(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 3000
            ).build()
            locationClient?.requestLocationUpdates(request, locationCallback!!, android.os.Looper.getMainLooper())
            
            locationClient?.lastLocation?.addOnSuccessListener { loc ->
                if (loc != null && _uiState.value.currentUserLocation == null) {
                    updateUserLocation(LatLng(loc.latitude, loc.longitude))
                }
            }
        }
    }

    fun stopLocationTracking() {
        locationCallback?.let {
            locationClient?.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    fun updatePrompt(text: String, cursorPosition: Int) {
        _uiState.value = _uiState.value.copy(promptText = text)
        
        searchJob?.cancel()
        
        val textBeforeCursor = text.substring(0, minOf(cursorPosition, text.length))
        
        // Find the closest " from " or " to " before the cursor
        val fromIndex = textBeforeCursor.lastIndexOf("from ", ignoreCase = true)
        val toIndex = textBeforeCursor.lastIndexOf("to ", ignoreCase = true)
        
        var target: String? = null
        var queryForAutocomplete = ""
        
        if (fromIndex > toIndex && fromIndex != -1) {
            target = "from"
            queryForAutocomplete = textBeforeCursor.substring(fromIndex + 5).trim()
        } else if (toIndex > fromIndex && toIndex != -1) {
            target = "to"
            queryForAutocomplete = textBeforeCursor.substring(toIndex + 3).trim()
        } else if (textBeforeCursor.lowercase().startsWith("from ")) {
            target = "from"
            queryForAutocomplete = textBeforeCursor.substring(5).trim()
        } else if (textBeforeCursor.lowercase().startsWith("to ")) {
            target = "to"
            queryForAutocomplete = textBeforeCursor.substring(3).trim()
        }
        
        if (target != null && queryForAutocomplete.length >= 3) {
            // Ignore if they just typed "here" or "current location"
            if (queryForAutocomplete.lowercase() in listOf("here", "current location")) {
                _uiState.value = _uiState.value.copy(destinationSuggestions = emptyList(), autocompleteTarget = null)
                return
            }
            
            _uiState.value = _uiState.value.copy(autocompleteTarget = target)
            
            searchJob = scope.launch {
                kotlinx.coroutines.delay(400) // debounce
                try {
                    val repo = repository ?: return@launch
                    val suggestions = repo.getPlaceSuggestions(queryForAutocomplete)
                    _uiState.value = _uiState.value.copy(destinationSuggestions = suggestions)
                } catch (e: Exception) {
                    // ignore
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(destinationSuggestions = emptyList(), autocompleteTarget = null)
        }
    }

    fun selectSuggestion(suggestion: String) {
        searchJob?.cancel()
        
        val target = _uiState.value.autocompleteTarget ?: return
        val currentText = _uiState.value.promptText
        
        var newText = currentText
        if (target == "from") {
            newText = currentText.replace(Regex("(?i)(from\\s+)(.*?)(?=\\s+to\\s+|$)"), "$1$suggestion")
        } else if (target == "to") {
            newText = currentText.replace(Regex("(?i)(to\\s+)(.*?)(?=\\s+from\\s+|$)"), "$1$suggestion")
        }
        
        _uiState.value = _uiState.value.copy(
            promptText = newText.trim(),
            destinationSuggestions = emptyList(),
            autocompleteTarget = null
        )
    }

    fun toggleUseCurrentLocation() {
        _uiState.value = _uiState.value.copy(
            useCurrentLocation = !_uiState.value.useCurrentLocation
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetTrip() {
        _uiState.value = TripUiState()
        geofenceManager?.clearGeofences()
    }

    fun searchRoute(currentLat: Double? = null, currentLng: Double? = null, overridePrompt: String? = null) {
        val state = _uiState.value
        val prompt = overridePrompt ?: state.promptText

        val parsed = TripPromptParser.parse(prompt)

        // 0. Intercept Multi-Step "Then" Journey
        if (parsed.multiStepNextPrompt != null && overridePrompt == null) {
            _uiState.value = state.copy(isLoading = true, error = null)
            routeSearchJob?.cancel()
            routeSearchJob = scope.launch {
                try {
                    val repo = repository ?: return@launch
                    if (currentLat == null || currentLng == null) {
                        _uiState.value = state.copy(error = "Waiting for GPS signal... please wait a moment.", isLoading = false)
                        return@launch
                    }
                    val cLat = currentLat
                    val cLng = currentLng
                    
                    val step1Drops = repo.getDropsAroundLocation(cLat, cLng, parsed.radiusMeters ?: 5000.0)
                    val step1Filtered = repo.filterDropsByQuery(step1Drops, parsed.query, parsed.exclusions)
                    
                    val bestDrop = step1Filtered.firstOrNull() // Pick the closest/best matching drop based on backend sort
                    if (bestDrop != null) {
                        // Construct the new prompt to force the multi-stop route
                        val newPrompt = "from $cLat,$cLng to ${parsed.multiStepNextPrompt} via ${bestDrop.latitude},${bestDrop.longitude}"
                        searchRoute(currentLat, currentLng, overridePrompt = newPrompt)
                    } else {
                        _uiState.value = state.copy(error = "Could not find any drops for the first part of your journey.", isLoading = false)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _uiState.value = state.copy(error = e.message, isLoading = false)
                }
            }
            return
        }

        var origin = parsed.origin
        var destination = parsed.destination ?: ""
        val waypoint = parsed.waypoint
        val query = parsed.query
        val exclusions = parsed.exclusions
        var radiusMeters = parsed.radiusMeters
        val isSurpriseMe = parsed.action == TripAction.SURPRISE_ME

        if (origin.isNullOrBlank()) {
            if (currentLat != null && currentLng != null) {
                origin = "$currentLat,$currentLng"
            } else {
                _uiState.value = state.copy(error = "Waiting for GPS signal... please wait a moment.")
                return
            }
        }

        // If neither destination nor surprise me nor radius is present, default to a local 5km search
        if (destination.isBlank() && !isSurpriseMe && radiusMeters == null) {
            radiusMeters = 5000.0
        }

        _uiState.value = state.copy(isLoading = true, error = null)
        
        routeSearchJob?.cancel()
        routeSearchJob = scope.launch {
            try {
                val startTime = System.currentTimeMillis()
                val repo = repository ?: return@launch

                if (isSurpriseMe) {
                    if (currentLat == null || currentLng == null) {
                        _uiState.value = state.copy(error = "Waiting for GPS signal... please wait a moment.", isLoading = false)
                        return@launch
                    }
                    val centerLat = currentLat
                    val centerLng = currentLng
                    val allLocalDrops = repo.getDropsAroundLocation(centerLat, centerLng, 30000.0) // 30km radius
                    if (allLocalDrops.isNotEmpty()) {
                        val chosenDrop = allLocalDrops.random()
                        destination = "${chosenDrop.latitude},${chosenDrop.longitude}"
                    } else {
                        _uiState.value = state.copy(error = "No nearby drops found to surprise you with!", isLoading = false)
                        return@launch
                    }
                }

                if (destination.isNotBlank()) {
                    // 1. Wayfinder Philosophy: ALWAYS prioritize routing to a Drop over a generic Google Maps location.
                    // If the destination is a text phrase (not coordinates), we search the Drops database first.
                    if (!destination.matches(Regex("^[-\\d.]+,[-\\d.]+$"))) {
                        if (currentLat == null || currentLng == null) {
                            _uiState.value = state.copy(error = "Waiting for GPS signal... please wait a moment.", isLoading = false)
                            return@launch
                        }
                        val cLat = currentLat
                        val cLng = currentLng
                        // Massive 50km search to find a drop matching the destination intent
                        val possibleDrops = repo.getDropsAroundLocation(cLat, cLng, 50000.0)
                        val dropMatches = repo.filterDropsByQuery(possibleDrops, destination)
                        if (dropMatches.isNotEmpty()) {
                            val bestDestDrop = dropMatches.first()
                            destination = "${bestDestDrop.latitude},${bestDestDrop.longitude}"
                        }
                    }

                    // 2. Driving Route Logic
                    var route: TripRepository.RouteResult? = null
                    try {
                        route = repo.fetchRoute(origin, destination, waypoint)
                    } catch (e: Exception) {
                        val msg = e.message ?: ""
                        if (msg.contains("NOT_FOUND", ignoreCase = true) || msg.contains("ZERO_RESULTS", ignoreCase = true)) {
                            // If it still fails, bubble up the error since we already tried finding a drop
                            throw Exception("Could not find a place or drop matching your destination.")
                        } else {
                            throw e
                        }
                    }

                    if (route == null) return@launch
                    
                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed < 2000L) kotlinx.coroutines.delay(2000L - elapsed)

                    _uiState.value = _uiState.value.copy(
                        routePolyline = route.polylinePoints,
                        routeDuration = route.durationText,
                        routeDistance = route.distanceText,
                        routeSummary = route.summary,
                        startAddress = route.startAddress,
                        endAddress = route.endAddress,
                        phase = TripPhase.ROUTE_VIEW,
                        isLoading = false
                    )

                    val dropsOnRoute = repo.getDropsAlongRoute(route.polylinePoints)
                    val filtered = if (query.isNotBlank() || exclusions.isNotEmpty()) repo.filterDropsByQuery(dropsOnRoute, query, exclusions) else dropsOnRoute
                    
                    _uiState.value = _uiState.value.copy(
                        allDropsOnRoute = filtered,
                        filteredDrops = filtered
                    )
                } else if (radiusMeters != null) {
                    // Radius Search Logic (No Route)
                    val centerParts = origin.split(",")
                    val parsedLat = centerParts.getOrNull(0)?.toDoubleOrNull() ?: currentLat
                    val parsedLng = centerParts.getOrNull(1)?.toDoubleOrNull() ?: currentLng
                    if (parsedLat == null || parsedLng == null) {
                        _uiState.value = state.copy(error = "Waiting for GPS signal... please wait a moment.", isLoading = false)
                        return@launch
                    }
                    val centerLat = parsedLat
                    val centerLng = parsedLng

                    val dropsInRadius = repo.getDropsAroundLocation(centerLat, centerLng, radiusMeters)
                    val filtered = if (query.isNotBlank() || exclusions.isNotEmpty()) repo.filterDropsByQuery(dropsInRadius, query, exclusions) else dropsInRadius

                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed < 2000L) kotlinx.coroutines.delay(2000L - elapsed)

                    val radiusText = if (radiusMeters >= 1000) "${(radiusMeters / 1000).toInt()} km" else "${radiusMeters.toInt()} m"

                    _uiState.value = _uiState.value.copy(
                        routePolyline = emptyList(),
                        routeDuration = "",
                        routeDistance = "Radius: $radiusText",
                        routeSummary = "Showing drops around you",
                        startAddress = "Current Location",
                        endAddress = "Search Radius",
                        phase = TripPhase.ROUTE_VIEW,
                        isLoading = false,
                        allDropsOnRoute = filtered,
                        filteredDrops = filtered
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    /**
     * Instantly sets the destination to a specific drop's coordinates and searches.
     */
    fun routeToDrop(drop: NearbyDrop, currentLat: Double? = null, currentLng: Double? = null) {
        // Reset the prompt to point directly to the drop so the user sees what's happening
        val destStr = "${drop.latitude},${drop.longitude}"
        _uiState.value = _uiState.value.copy(
            promptText = "to $destStr",
            selectedDrop = null // close the sheet
        )
        searchRoute(currentLat, currentLng)
    }

    fun selectDrop(drop: NearbyDrop?) {
        _uiState.value = _uiState.value.copy(selectedDrop = drop)
    }

    fun startNavigation(context: Context) {
        val state = _uiState.value
        
        // If there is no route generated yet (e.g. they just did a radius search)
        // and they pressed START TRIP, we should generate a route to the best drop!
        if (state.routePolyline.isEmpty()) {
            val targetDrop = state.selectedDrop ?: state.filteredDrops.firstOrNull()
            if (targetDrop != null) {
                val loc = state.currentUserLocation
                if (loc != null) {
                    routeToDrop(targetDrop, loc.latitude, loc.longitude)
                } else {
                    routeToDrop(targetDrop)
                }
                return
            }
        }

        _uiState.value = state.copy(
            phase = TripPhase.NAVIGATING,
            isNavigating = true
        )
        geofenceManager?.registerGeofences(state.filteredDrops)
    }

    fun stopNavigation() {
        _uiState.value = _uiState.value.copy(
            phase = TripPhase.ROUTE_VIEW,
            isNavigating = false,
            approachingDrop = null
        )
        geofenceManager?.clearGeofences()
    }

    fun updateUserLocation(latLng: LatLng) {
        val currentState = _uiState.value
        
        var newOffRouteCounter = currentState.offRouteCounter
        var newIsRecalculating = currentState.isRecalculating

        if (currentState.isNavigating && currentState.routePolyline.isNotEmpty() && !currentState.isRecalculating) {
            val isOnRoute = PolyUtil.isLocationOnPath(latLng, currentState.routePolyline, false, 50.0)
            
            if (!isOnRoute) {
                newOffRouteCounter += 1
                if (newOffRouteCounter >= 3) {
                    newIsRecalculating = true
                    newOffRouteCounter = 0
                    recalculateRoute(latLng)
                }
            } else {
                newOffRouteCounter = 0
            }
        }

        _uiState.value = currentState.copy(
            currentUserLocation = latLng,
            offRouteCounter = newOffRouteCounter,
            isRecalculating = newIsRecalculating
        )

        // Find approaching drops natively (UI trigger)
        val closest = _uiState.value.filteredDrops.minByOrNull { drop ->
            haversineDistance(latLng.latitude, latLng.longitude, drop.latitude, drop.longitude)
        }

        if (closest != null) {
            val dist = haversineDistance(
                latLng.latitude, latLng.longitude,
                closest.latitude, closest.longitude
            )
            if (dist <= 500.0) {
                _uiState.value = _uiState.value.copy(approachingDrop = closest)
            } else if (_uiState.value.approachingDrop?.id == closest.id) {
                _uiState.value = _uiState.value.copy(approachingDrop = null)
            }
        }
    }

    fun dismissApproachingDrop() {
        _uiState.value = _uiState.value.copy(approachingDrop = null)
    }

    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    private fun recalculateRoute(currentLocation: LatLng) {
        val currentState = _uiState.value
        val origin = "${currentLocation.latitude},${currentLocation.longitude}"
        
        val prompt = currentState.promptText
        val toRegex = Regex("(?i)to\\s+(.*?)(?=\\s+from\\s+|$)")
        var dest = toRegex.find(prompt)?.groupValues?.getOrNull(1)?.trim() ?: currentState.endAddress
        
        val fromRegex = Regex("(?i)from\\s+(.*?)(?=\\s+to\\s+|$)")
        var query = prompt
        fromRegex.find(prompt)?.let { query = query.replace(it.value, "") }
        toRegex.find(prompt)?.let { query = query.replace(it.value, "") }
        query = query.trim()
        
        if (dest.isBlank()) {
            dest = prompt
            query = ""
        }

        scope.launch {
            try {
                val repo = repository ?: return@launch
                
                val route = repo.fetchRoute(origin, dest)
                val polyline = route.polylinePoints
                
                val routeDrops = repo.getDropsAlongRoute(polyline)
                val filteredDrops = repo.filterDropsByQuery(routeDrops, query)

                geofenceManager?.clearGeofences()
                geofenceManager?.registerGeofences(filteredDrops)

                _uiState.value = _uiState.value.copy(
                    routePolyline = polyline,
                    allDropsOnRoute = routeDrops,
                    filteredDrops = filteredDrops,
                    isRecalculating = false
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isRecalculating = false)
            }
        }
    }
}
