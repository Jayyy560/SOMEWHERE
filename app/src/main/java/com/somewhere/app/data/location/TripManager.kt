package com.somewhere.app.data.location

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.SystemClock
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.somewhere.app.data.local.TripStore
import com.somewhere.app.data.remote.NearbyDrop
import com.somewhere.app.data.repository.TripRepository
import com.somewhere.app.util.LocationSample
import com.somewhere.app.viewmodel.TripPhase
import com.somewhere.app.viewmodel.TripUiState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import kotlin.math.ceil

/** One owner for location updates, trip jobs, resolved itinerary, and account-scoped history. */
object TripManager {
    private lateinit var appContext: Context
    private lateinit var repository: TripRepository
    private lateinit var geofences: GeofenceManager
    private lateinit var store: TripStore
    private var storeUserId: String? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var searchJob: Job? = null
    private var autocompleteJob: Job? = null
    private var libraryJob: Job? = null
    private var accountJob: Job = SupervisorJob()
    private var generation = 0L
    private var locationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var tracker = TripNavigationTracker(TravelMode.WALKING)
    private var resolvedPlan: TripPlan? = null
    private var activeTripId: String? = null
    private var lastGeofenceProgress = -1000.0
    private var geofenceIds = emptySet<String>()
    private var screenTracking = false
    private var remainingMandatoryWaypoints = emptyList<TripPoint>()
    private var pendingNavigationStart = false
    private var navigationStartedElapsed = 0L
    private var lastLocationElapsed = -60_000L
    private val _uiState = MutableStateFlow(TripUiState())
    val uiState: StateFlow<TripUiState> = _uiState.asStateFlow()

    fun initialize(context: Context) {
        if (!::repository.isInitialized) {
            appContext = context.applicationContext
            repository = TripRepository(appContext)
            geofences = GeofenceManager(appContext)
            storeUserId = repository.currentUserId
            store = TripStore(appContext, storeUserId.orEmpty())
            refreshLibrary()
        }
        refreshAccount()
    }
    fun refreshAccount() {
        if (!::repository.isInitialized) return
        val user = repository.currentUserId
        if (user == storeUserId) return
        cancelSearch(); autocompleteJob?.cancel(); libraryJob?.cancel(); accountJob.cancel(); accountJob = SupervisorJob()
        geofences.clearGeofences(); stopLocationUpdates()
        appContext.stopService(Intent(appContext, TripForegroundService::class.java))
        storeUserId = user; store = TripStore(appContext, user.orEmpty())
        activeTripId = null; resolvedPlan = null; pendingNavigationStart = false
        _uiState.value = TripUiState()
        refreshLibrary()
    }
    private fun refreshLibrary() {
        _uiState.update { it.copy(savedTrips = store.trips(), visitedDropIds = store.visitedIds(), resumableTripId = store.activeId(), currentUserId = storeUserId) }
    }

    fun startLocationTracking(context: Context) { initialize(context); screenTracking = true; startLocationUpdates() }
    fun ensureNavigationTracking(context: Context) { initialize(context); if (_uiState.value.isNavigating) startLocationUpdates() }
    fun stopLocationTracking() { screenTracking = false; if (!_uiState.value.isNavigating) stopLocationUpdates() }
    @android.annotation.SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (locationCallback != null) return
        val client = LocationServices.getFusedLocationProviderClient(appContext)
        locationClient = client
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) { result.locations.forEach(::updateLocation) }
        }
        locationCallback = callback
        val interval = if (_uiState.value.options.travelMode == TravelMode.DRIVING) 2000L else 4000L
        try {
            client.requestLocationUpdates(LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
                .setMinUpdateDistanceMeters(0f).build(), callback, android.os.Looper.getMainLooper())
                .addOnFailureListener { error -> scope.launch { _uiState.update { it.copy(error = "Location updates are unavailable. Check your location permission.") } } }
            client.lastLocation.addOnSuccessListener { if (it != null) updateLocation(it) }
        } catch (_: SecurityException) {
            locationCallback = null
            _uiState.update { it.copy(error = "Allow location access to use Wayfinder.") }
        }
    }
    private fun stopLocationUpdates() { locationCallback?.let { locationClient?.removeLocationUpdates(it) }; locationCallback = null }
    fun updateLocation(location: Location) {
        if (!::repository.isInitialized) return
        scope.launch {
            val sample = LocationSample(location.latitude, location.longitude, location.accuracy, location.elapsedRealtimeNanos / 1_000_000)
            val state = _uiState.value
            val update = tracker.update(sample, SystemClock.elapsedRealtime(), if (state.isNavigating) resolvedPlan?.route?.points.orEmpty() else emptyList()) ?: return@launch
            lastLocationElapsed = sample.elapsedMillis
            _uiState.update { it.copy(currentUserLocation = LatLng(update.point.latitude, update.point.longitude), locationAccuracyMeters = update.accuracyMeters) }
            if (!state.isNavigating) return@launch
            _uiState.update { it.copy(routeProgressMeters = update.progressMeters) }
            val plan = resolvedPlan ?: return@launch
            val upcoming = upcomingDrops(plan, update.progressMeters)
            remainingMandatoryWaypoints = remainingMandatoryWaypoints.filter { waypoint ->
                TripGeometry.project(waypoint, plan.route?.points.orEmpty()).progressMeters >= update.progressMeters - 35
            }
            for (drop in plan.stops) {
                if (drop.id !in store.visitedIds() && tracker.isVisit(drop.id, TripGeometry.distance(update.point, drop.point()), update.accuracyMeters, sample.elapsedMillis)) recordVisit(drop.id)
            }
            val alertRadius = when (state.options.travelMode) { TravelMode.WALKING -> 120.0; TravelMode.CYCLING -> 350.0; TravelMode.DRIVING -> 1000.0 }
            val approaching = upcoming.firstOrNull { TripGeometry.distance(update.point, it.point()) <= alertRadius && it.id !in store.dismissedIds() && it.id !in store.visitedIds() }
            val oldAlert = _uiState.value.approachingDrop
            if (oldAlert == null && approaching != null && store.claimAlert(approaching.id, System.currentTimeMillis())) {
                _uiState.update { it.copy(approachingDrop = approaching) }
                if (!screenTracking) TripNotifications.approaching(appContext, approaching.id, state.options.haptics)
            } else if (oldAlert != null && (oldAlert.id in store.visitedIds() || upcoming.none { it.id == oldAlert.id } || TripGeometry.distance(update.point, oldAlert.point()) > alertRadius * 1.5)) {
                _uiState.update { it.copy(approachingDrop = null) }
            }
            updateGeofences(upcoming, update.progressMeters)
            if (update.shouldReroute && !state.isRecalculating && !state.isLoading) recalculateRoute(update.point)
        }
    }
    private fun upcomingDrops(plan: TripPlan, progress: Double): List<NearbyDrop> {
        val line = plan.route?.points.orEmpty()
        val now = System.currentTimeMillis()
        val scheduled = plan.stops.firstOrNull { it.id !in store.visitedIds() && it.id !in store.dismissedIds() && (it.expiresAt == null || (TripSearch.timestamp(it.expiresAt) ?: 0) > now) }
        val scheduledIds = plan.stops.map { it.id }.toSet()
        val discoveries = plan.discoveries.filter { ranked ->
            ranked.drop.id !in scheduledIds && (ranked.drop.expiresAt == null || (TripSearch.timestamp(ranked.drop.expiresAt) ?: 0) > now) &&
            ranked.drop.id !in store.visitedIds() && ranked.drop.id !in store.dismissedIds() &&
                (ranked.routeProgressMeters ?: TripGeometry.project(ranked.drop.point(), line).progressMeters) >= progress - 35
        }.sortedBy { it.routeProgressMeters ?: TripGeometry.project(it.drop.point(), line, (progress - 35).coerceAtLeast(0.0)).progressMeters }.map { it.drop }
        return listOfNotNull(scheduled) + discoveries
    }
    private fun updateGeofences(upcoming: List<NearbyDrop>, progress: Double, force: Boolean = false) {
        val next = upcoming.take(90)
        val ids = next.map { it.id }.toSet()
        if (!force && ids == geofenceIds && progress - lastGeofenceProgress < 500) return
        geofenceIds = ids; lastGeofenceProgress = progress
        geofences.registerGeofences(next, _uiState.value.options.travelMode) { message -> _uiState.update { it.copy(notice = message) } }
    }

    fun updatePrompt(text: String, cursorPosition: Int) {
        if (_uiState.value.isLoading) { cancelSearch(); pendingNavigationStart = false; _uiState.update { it.copy(isLoading = false, planningMessage = "") } }
        _uiState.update { it.copy(promptText = text) }
        autocompleteJob?.cancel()
        val before = text.take(cursorPosition.coerceIn(0, text.length))
        val marker = Regex("(?i)\\b(from|to|via|near)\\s+([^;]+)$").find(before)
        val target = marker?.groupValues?.get(1)?.lowercase()
        val query = marker?.groupValues?.get(2)?.trim().orEmpty()
        _uiState.update { it.copy(destinationSuggestions = emptyList(), autocompleteTarget = target) }
        if (query.length < 3 || query.lowercase() in setOf("here", "current location")) return
        val expected = text
        autocompleteJob = scope.launch {
            delay(350)
            try {
                val suggestions = repository.getPlaceSuggestions(query)
                ensureActive()
                if (_uiState.value.promptText == expected) _uiState.update { it.copy(destinationSuggestions = suggestions) }
            } catch (e: CancellationException) { throw e } catch (_: Exception) { /* Search can still resolve complete addresses. */ }
        }
    }
    fun selectSuggestion(suggestion: String) {
        autocompleteJob?.cancel()
        val state = _uiState.value; val target = state.autocompleteTarget ?: return
        val pattern = Regex("(?i)\\b${Regex.escape(target)}\\s+.+?(?=\\s+\\b(?:from|to|via|near)\\b\\s+|$)")
        val replaced = pattern.replace(state.promptText) { "$target \"$suggestion\"" }
        _uiState.update { it.copy(promptText = replaced, destinationSuggestions = emptyList(), autocompleteTarget = null) }
    }
    fun updateOptions(options: TripOptions) {
        if (_uiState.value.isNavigating || _uiState.value.isLoading) return
        val valid = options.copy(radiusMeters = options.radiusMeters.coerceIn(100.0, 100000.0), stopCount = options.stopCount.coerceIn(1, 8), dwellMinutes = options.dwellMinutes.coerceIn(0, 30))
        val modeChanged = valid.travelMode != _uiState.value.options.travelMode
        _uiState.update { it.copy(options = valid) }
        if (modeChanged) {
            tracker = TripNavigationTracker(valid.travelMode)
            if (locationCallback != null) { stopLocationUpdates(); startLocationUpdates() }
        }
    }
    fun setListNavigation(enabled: Boolean) {
        _uiState.update { it.copy(options = it.options.copy(cameraFree = enabled)) }
        resolvedPlan = resolvedPlan?.let { it.copy(request = it.request.copy(options = it.request.options.copy(cameraFree = enabled))) }
    }
    fun toggleUseCurrentLocation() { _uiState.update { it.copy(useCurrentLocation = !it.useCurrentLocation) } }
    fun clearError() { _uiState.update { it.copy(error = null, notice = null) } }
    fun resetTrip() {
        cancelSearch(); autocompleteJob?.cancel(); pendingNavigationStart = false
        if (_uiState.value.isNavigating) stopNavigation()
        resolvedPlan = null; activeTripId = null; geofences.clearGeofences(); tracker = TripNavigationTracker(_uiState.value.options.travelMode)
        _uiState.update { TripUiState(options = it.options, currentUserLocation = it.currentUserLocation, locationAccuracyMeters = it.locationAccuracyMeters,
            savedTrips = store.trips(), visitedDropIds = store.visitedIds(), currentUserId = storeUserId) }
    }
    private fun cancelSearch() { generation++; searchJob?.cancel(); searchJob = null }

    fun searchRoute(currentLat: Double? = null, currentLng: Double? = null, overridePrompt: String? = null) {
        val state = _uiState.value
        val current = if (currentLat != null && currentLng != null) TripPoint(currentLat, currentLng)
            else state.currentUserLocation?.let { TripPoint(it.latitude, it.longitude) }
        val prompt = overridePrompt ?: state.promptText
        launchSearch {
            require(current != null) { "Waiting for a fresh location. Check GPS and try again." }
            val parsed = TripPromptParser.parse(prompt)
            val options = state.options.copy(travelMode = parsed.travelMode ?: state.options.travelMode,
                radiusMeters = parsed.radiusMeters ?: state.options.radiusMeters,
                timeBudgetMinutes = parsed.timeBudgetMinutes ?: state.options.timeBudgetMinutes,
                maxDetourMinutes = parsed.maxDetourMinutes ?: state.options.maxDetourMinutes,
                stopCount = parsed.stopCount ?: state.options.stopCount,
                returnToStart = parsed.returnToStart || state.options.returnToStart,
                unvisitedOnly = parsed.unvisitedOnly || state.options.unvisitedOnly,
                preferRecent = parsed.preferRecent || state.options.preferRecent)
            val origin = parsed.origin?.let { repository.resolvePlace(it, current, options.travelMode) } ?: current
            val center = parsed.searchCenter?.let { repository.resolvePlace(it, current, options.travelMode) } ?: origin
            var destination = parsed.destination?.let { repository.resolvePlace(it, origin, options.travelMode) }
            val via = parsed.waypoints.map { repository.resolvePlace(it, origin, options.travelMode) }.toMutableList()
            var action = parsed.action
            var next = parsed.multiStepNextPrompt
            var steps = 0
            while (!next.isNullOrBlank()) {
                require(++steps <= 8) { "Use at most eight journey stages." }
                destination?.let { via += it }
                val stage = TripPromptParser.parse(next)
                val target = stage.destination ?: stage.query.takeIf { it.isNotBlank() }
                require(target != null) { "Add a destination after ‘then’." }
                destination = repository.resolvePlace(target, destination ?: origin, options.travelMode)
                next = stage.multiStepNextPrompt
                if (parsed.destination == null) action = TripAction.SURPRISE_ME // discover the requested first stop, then continue
            }
            if (options.returnToStart && destination != null) { via += destination; destination = origin }
            TripRequest(origin, destination, via, parsed.query, parsed.exclusions, options, action,
                destinationLabel = if (options.returnToStart) "Back to start" else parsed.destination.orEmpty(),
                searchCenter = center, radiusIsConstraint = parsed.radiusMeters != null, returnPoint = if (options.returnToStart) origin else null, buildItinerary = parsed.stopCount != null)
        }
    }
    private fun launchSearch(fixedDrops: List<NearbyDrop>? = null, restoringTripId: String? = null, build: suspend () -> TripRequest) {
        cancelSearch(); autocompleteJob?.cancel()
        val requestGeneration = generation
        if (_uiState.value.isNavigating) stopNavigation()
        _uiState.update { it.copy(isLoading = true, isRecalculating = false, error = null, notice = null, planningMessage = "Finding your route…", destinationSuggestions = emptyList()) }
        searchJob = scope.launch {
            try {
                val request = build()
                val baseEnd = if (request.options.returnToStart) request.returnPoint ?: request.origin else request.destination
                val baseline = baseEnd?.takeUnless { it == request.origin && request.waypoints.isEmpty() }?.let {
                    repository.route(request.origin, it, request.waypoints, request.options.travelMode)
                }
                ensureCurrent(requestGeneration)
                _uiState.update { it.copy(planningMessage = "Finding matching Drops…") }
                val candidates = fixedDrops ?: if (request.fixedStopIds.isNotEmpty()) repository.dropsByIds(request.fixedStopIds)
                    else repository.candidates(request, store.visitedIds(), baseline?.points.orEmpty())
                ensureCurrent(requestGeneration)
                _uiState.update { it.copy(planningMessage = "Checking travel time…") }
                val planner = TripPlanner(object : TripRouting {
                    override suspend fun route(origin: TripPoint, destination: TripPoint, waypoints: List<TripPoint>, mode: TravelMode): TripRoute =
                        if (baseline != null && origin == request.origin && destination == baseEnd && waypoints == request.waypoints) baseline
                        else repository.route(origin, destination, waypoints, mode)
                })
                val plan = planner.plan(request, candidates, store.visitedIds(), repository.currentUserId)
                ensureCurrent(requestGeneration)
                resolvedPlan = plan.copy(request = plan.request.copy(fixedStopIds = plan.stops.map { it.id }))
                remainingMandatoryWaypoints = plan.request.waypoints
                tracker = TripNavigationTracker(request.options.travelMode)
                activeTripId = restoringTripId
                applyPlan(resolvedPlan!!)
                if (pendingNavigationStart) { pendingNavigationStart = false; beginNavigation() }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (generation == requestGeneration) {
                    pendingNavigationStart = false
                    _uiState.update { it.copy(isLoading = false, isRecalculating = false, planningMessage = "", error = friendlyError(e)) }
                }
            }
        }
    }
    private suspend fun ensureCurrent(expected: Long) { currentCoroutineContext().ensureActive(); if (generation != expected) throw CancellationException() }
    private fun applyPlan(plan: TripPlan) {
        val route = plan.route
        _uiState.update { it.copy(phase = TripPhase.ROUTE_VIEW, isLoading = false, isRecalculating = false, planningMessage = "", options = plan.request.options,
            routePolyline = route?.points.orEmpty().map { p -> LatLng(p.latitude, p.longitude) },
            routeDuration = route?.let { formatDuration(it.durationSeconds + plan.stops.size * plan.request.options.dwellMinutes * 60L) }.orEmpty(),
            routeDistance = route?.let { formatDistance(it.distanceMeters) } ?: "Within ${formatDistance(plan.request.options.radiusMeters.toLong())}",
            routeSummary = route?.summary.orEmpty(), startAddress = route?.startAddress.orEmpty(), endAddress = plan.request.destinationLabel.ifBlank { route?.endAddress.orEmpty() },
            allDropsOnRoute = plan.discoveries.map { d -> d.drop }, filteredDrops = plan.discoveries.map { d -> d.drop },
            recommendations = plan.discoveries.associateBy { d -> d.drop.id }, itineraryStops = plan.stops,
            selectedDrop = null, approachingDrop = null, routeProgressMeters = 0.0,
            notice = listOfNotNull(plan.message, route?.warnings?.firstOrNull()).joinToString(" ").ifBlank { null }) }
    }
    fun routeToDrop(drop: NearbyDrop, currentLat: Double? = null, currentLng: Double? = null) {
        val state = _uiState.value
        val origin = if (currentLat != null && currentLng != null) TripPoint(currentLat, currentLng) else state.currentUserLocation?.let { TripPoint(it.latitude, it.longitude) }
        _uiState.update { it.copy(promptText = "to ${drop.latitude},${drop.longitude}") }
        launchSearch(build = {
            require(origin != null) { "Waiting for a fresh location. Check GPS and try again." }
            TripRequest(origin, drop.point(), options = state.options.copy(returnToStart = false, unvisitedOnly = false), action = TripAction.ROUTE,
                destinationLabel = drop.text.take(40), fixedStopIds = listOf(drop.id))
        }, fixedDrops = listOf(drop))
    }
    fun selectDrop(drop: NearbyDrop?) { _uiState.update { it.copy(selectedDrop = drop) } }
    fun addStop(drop: NearbyDrop) {
        val plan = resolvedPlan ?: return
        if (_uiState.value.isNavigating) return
        val ids = (plan.stops.map { it.id } + drop.id).distinct()
        if (ids.size > 8) { _uiState.update { it.copy(error = "Use at most eight stops.") }; return }
        launchSearch(build = { plan.request.copy(fixedStopIds = ids, useFixedStops = true) })
    }
    fun removeStop(dropId: String) {
        val plan = resolvedPlan ?: return
        if (_uiState.value.isNavigating) return
        val remaining = plan.stops.filterNot { it.id == dropId }
        val endpoint = if (plan.request.action != TripAction.ROUTE && !plan.request.options.returnToStart && plan.stops.lastOrNull()?.id == dropId) remaining.lastOrNull()?.point() else plan.request.destination
        launchSearch(build = { plan.request.copy(destination = endpoint, fixedStopIds = remaining.map { it.id }, useFixedStops = true) })
    }
    fun startNavigation() {
        if (_uiState.value.isNavigating || _uiState.value.isLoading) return
        val plan = resolvedPlan
        if (plan?.route == null) {
            val drop = _uiState.value.selectedDrop ?: _uiState.value.filteredDrops.firstOrNull()
            if (drop == null) { _uiState.update { it.copy(error = "No Drops were found to start a trip.") }; return }
            pendingNavigationStart = true; routeToDrop(drop); return
        }
        beginNavigation()
    }
    private fun beginNavigation() {
        val plan = resolvedPlan ?: return
        if (plan.route == null) return
        if (SystemClock.elapsedRealtime() - lastLocationElapsed > 15_000 || (_uiState.value.locationAccuracyMeters ?: 999f) > 75f) {
            _uiState.update { it.copy(error = "Waiting for an accurate location before starting your trip.") }; return
        }
        navigationStartedElapsed = SystemClock.elapsedRealtime()
        store.clearDismissed(); tracker = TripNavigationTracker(plan.request.options.travelMode)
        val trip = snapshot(false)
        store.save(trip); activeTripId = trip.id; store.setActive(trip.id)
        _uiState.update { it.copy(phase = TripPhase.NAVIGATING, isNavigating = true, routeProgressMeters = 0.0, notice = null) }
        startLocationUpdates()
        try { androidx.core.content.ContextCompat.startForegroundService(appContext, Intent(appContext, TripForegroundService::class.java)) }
        catch (e: Exception) { stopNavigation(); _uiState.update { it.copy(error = "Navigation could not start. Check location access and try again.") }; return }
        updateGeofences(upcomingDrops(plan, 0.0), 0.0, true)
        refreshLibrary()
    }
    fun stopNavigation() {
        pendingNavigationStart = false
        if (_uiState.value.isRecalculating) { cancelSearch() }
        activeTripId?.let { id -> store.trips().firstOrNull { it.id == id }?.let { store.save(it.copy(completedAtMillis = System.currentTimeMillis())) } }
        store.setActive(null); geofences.clearGeofences(); geofenceIds = emptySet()
        _uiState.update { it.copy(isNavigating = false, phase = TripPhase.ROUTE_VIEW, approachingDrop = null, isRecalculating = false) }
        appContext.stopService(Intent(appContext, TripForegroundService::class.java))
        if (!screenTracking) stopLocationUpdates()
        refreshLibrary()
    }
    fun dismissApproachingDrop() {
        _uiState.value.approachingDrop?.id?.let(store::dismiss)
        _uiState.update { it.copy(approachingDrop = null) }
    }
    fun recordVisit(dropId: String) {
        store.recordVisit(dropId, activeTripId)
        _uiState.update { it.copy(approachingDrop = it.approachingDrop?.takeUnless { d -> d.id == dropId }) }
        refreshLibrary()
    }
    private fun recalculateRoute(current: TripPoint) {
        val plan = resolvedPlan ?: return; val request = plan.request
        val endpoint = if (request.options.returnToStart) request.returnPoint ?: request.origin else request.destination ?: return
        val expected = generation
        _uiState.update { it.copy(isRecalculating = true) }
        searchJob = scope.launch {
            try {
                val remaining = plan.stops.filterNot { it.id in store.visitedIds() }
                val via = remainingMandatoryWaypoints + remaining.map { it.point() }.filterNot { it == endpoint }
                val route = repository.route(current, endpoint, via, request.options.travelMode)
                ensureCurrent(expected)
                val newRequest = request.copy(origin = current, waypoints = remainingMandatoryWaypoints)
                val candidates = repository.candidates(newRequest, store.visitedIds(), route.points)
                val verificationRequest = newRequest.copy(destination = endpoint, waypoints = via, action = TripAction.ROUTE,
                    fixedStopIds = emptyList(), buildItinerary = false, useFixedStops = false,
                    options = request.options.copy(timeBudgetMinutes = null, returnToStart = false))
                val verified = TripPlanner(object : TripRouting {
                    override suspend fun route(origin: TripPoint, destination: TripPoint, waypoints: List<TripPoint>, mode: TravelMode): TripRoute =
                        if (origin == current && destination == endpoint && waypoints == via) route else repository.route(origin,destination,waypoints,mode)
                }).plan(verificationRequest,candidates,store.visitedIds(),repository.currentUserId)
                val recommendations = verified.discoveries
                ensureCurrent(expected)
                resolvedPlan = plan.copy(request = newRequest, route = route, discoveries = recommendations)
                val elapsedSeconds = (SystemClock.elapsedRealtime() - navigationStartedElapsed) / 1000
                val overBudget = request.options.timeBudgetMinutes?.let { elapsedSeconds + route.durationSeconds + remaining.size * request.options.dwellMinutes * 60L > it * 60L } == true
                tracker.resetRouteProgress()
                _uiState.update { it.copy(routePolyline = route.points.map { p -> LatLng(p.latitude, p.longitude) },
                    routeDuration = formatDuration(route.durationSeconds + remaining.size * request.options.dwellMinutes * 60L), routeDistance = formatDistance(route.distanceMeters),
                    routeSummary = route.summary, filteredDrops = recommendations.map { r -> r.drop }, allDropsOnRoute = candidates,
                    recommendations = recommendations.associateBy { r -> r.drop.id }, isRecalculating = false, routeProgressMeters = 0.0, approachingDrop = null,
                    notice = if (overBudget) "The updated route exceeds your original time budget." else it.notice) }
                updateGeofences(upcomingDrops(resolvedPlan!!, 0.0), 0.0, true)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (generation == expected) _uiState.update { it.copy(isRecalculating = false, error = "Could not refresh your route. ${friendlyError(e)}") } }
        }
    }
    private fun snapshot(bookmark: Boolean, title: String? = null): SavedTrip {
        val plan = checkNotNull(resolvedPlan)
        val existing = activeTripId?.let { id -> store.trips().firstOrNull { it.id == id } }
        return SavedTrip(existing?.id ?: UUID.randomUUID().toString(), title?.trim()?.take(80)?.ifBlank { null } ?: existing?.title ?: defaultTitle(plan),
            plan.request, plan.stops.map { it.id }, existing?.createdAtMillis ?: System.currentTimeMillis(),
            visitedStopIds = existing?.visitedStopIds.orEmpty(), note = existing?.note.orEmpty(), isBookmark = bookmark || existing?.isBookmark == true,
            stopNotes = existing?.stopNotes.orEmpty(), photoPaths = existing?.photoPaths.orEmpty())
    }
    private fun defaultTitle(plan: TripPlan): String = plan.request.destinationLabel.ifBlank {
        if (plan.request.options.returnToStart) "${plan.request.options.travelMode.label} back to start" else plan.request.query.ifBlank { "${plan.request.options.travelMode.label} discovery" }
    }
    fun saveTrip(title: String) {
        if (resolvedPlan == null) return
        val trip = snapshot(true, title); store.save(trip); activeTripId = trip.id
        refreshLibrary(); _uiState.update { it.copy(notice = "Trip saved.") }
    }
    fun deleteSavedTrip(id: String) { if (id == activeTripId && _uiState.value.isNavigating) stopNavigation(); store.delete(id); refreshLibrary() }
    fun updateJournal(id: String, note: String, stopId: String? = null) { store.updateNote(id, note, stopId); refreshLibrary() }
    fun addJournalPhotos(id: String, uris: List<android.net.Uri>) {
        if (_uiState.value.isSavingPhotos) return
        val targetStore = store
        val trip = targetStore.trips().firstOrNull { it.id == id } ?: return
        val selected = uris.take((12 - trip.photoPaths.size).coerceAtLeast(0))
        if (selected.isEmpty()) { _uiState.update { it.copy(error = "A trip can hold up to 12 photos.") }; return }
        _uiState.update { it.copy(isSavingPhotos = true, error = null) }
        scope.launch(accountJob) {
            val created = mutableListOf<java.io.File>()
            var attached = false
            try {
                withContext(Dispatchers.IO) {
                    targetStore.photoDirectory.mkdirs()
                    for (uri in selected) {
                        ensureActive()
                        require(appContext.contentResolver.getType(uri)?.startsWith("image/") == true) { "Choose a photo to add to this trip." }
                        val target = java.io.File(targetStore.photoDirectory, "${UUID.randomUUID()}.photo")
                        created += target
                        appContext.contentResolver.openInputStream(uri).use { input ->
                            requireNotNull(input) { "This photo could not be opened." }
                            target.outputStream().use { output ->
                                val buffer = ByteArray(8192); var total = 0L
                                while (true) {
                                    ensureActive()
                                    val bytes = input.read(buffer); if (bytes < 0) break
                                    total += bytes
                                    require(total <= 25 * 1024 * 1024) { "Choose photos smaller than 25 MB." }
                                    output.write(buffer, 0, bytes)
                                }
                            }
                        }
                    }
                }
                ensureActive()
                check(targetStore.addPhotos(id,created.map { it.absolutePath })) { "This journal is no longer available." }; attached = true
                refreshLibrary(); _uiState.update { it.copy(isSavingPhotos = false) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(isSavingPhotos = false, error = friendlyError(e)) } }
            finally { if (!attached) withContext(NonCancellable + Dispatchers.IO) { created.forEach { it.delete() } } }
        }
    }
    fun removeJournalPhoto(id: String, path: String) { store.removePhoto(id,path); refreshLibrary() }
    fun openSavedTrip(saved: SavedTrip) {
        val current = _uiState.value.currentUserLocation?.let { TripPoint(it.latitude, it.longitude) }
        launchSearch(restoringTripId = saved.id.takeIf { saved.completedAtMillis == null }, build = { saved.request.copy(origin = current ?: saved.request.origin,
            fixedStopIds = saved.stopIds.filterNot { saved.completedAtMillis == null && it in saved.visitedStopIds }) })
    }
    fun loadTrails() {
        libraryJob?.cancel()
        val state = _uiState.value; val current = state.currentUserLocation?.let { TripPoint(it.latitude, it.longitude) }
        if (current == null) { _uiState.update { it.copy(error = "Waiting for your location to find trails.") }; return }
        _uiState.update { it.copy(isLoadingTrails = true) }
        libraryJob = scope.launch {
            try { val trails = repository.trails(current, state.options.radiusMeters); ensureActive(); _uiState.update { it.copy(communityTrails = trails, isLoadingTrails = false) } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(isLoadingTrails = false, error = friendlyError(e)) } }
        }
    }
    fun openTrail(trail: CuratedTrail) {
        val current = _uiState.value.currentUserLocation?.let { TripPoint(it.latitude, it.longitude) }
        val mode = TravelMode.entries.firstOrNull { it.apiValue == trail.travel_mode } ?: TravelMode.WALKING
        launchSearch(build = {
            require(current != null) { "Waiting for your location to start this trail." }
            TripRequest(current, options = _uiState.value.options.copy(travelMode = mode, returnToStart = trail.return_to_start, unvisitedOnly = false, category = null, media = TripMedia.ANY),
                action = TripAction.ROUTE, destinationLabel = trail.title, trailId = trail.id, fixedStopIds = trail.stop_ids,
                returnPoint = if (trail.return_to_start) current else null)
        })
    }
    fun publishTrail(title: String, description: String) {
        if (_uiState.value.isPublishingTrail) return
        if (resolvedPlan?.stops.isNullOrEmpty()) { _uiState.update { it.copy(error = "Add a stop to this trip before publishing a trail.") }; return }
        if (storeUserId == null) { _uiState.update { it.copy(error = "Sign in to publish a trail.") }; return }
        val snapshot = snapshot(true, title)
        _uiState.update { it.copy(isPublishingTrail = true) }
        scope.launch(accountJob) {
            try { repository.publishTrail(title, description, snapshot); store.save(snapshot); refreshLibrary(); _uiState.update { it.copy(isPublishingTrail = false, notice = "Trail published.") } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(isPublishingTrail = false, error = friendlyError(e)) } }
        }
    }
    fun setStepFreeAccess(drop: NearbyDrop, accessible: Boolean?) {
        if (drop.authorId != storeUserId) return
        scope.launch(accountJob) {
            try {
                repository.setStepFreeAccess(drop.id, accessible)
                fun update(d: NearbyDrop) = if (d.id == drop.id) d.copy(stepFreeAccess = accessible) else d
                resolvedPlan = resolvedPlan?.let { plan -> plan.copy(stops = plan.stops.map(::update), discoveries = plan.discoveries.map { it.copy(drop = update(it.drop)) }) }
                _uiState.update { it.copy(notice = "Stop access updated.", itineraryStops = it.itineraryStops.map(::update), filteredDrops = it.filteredDrops.map(::update), allDropsOnRoute = it.allDropsOnRoute.map(::update)) }
            }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(error = friendlyError(e)) } }
        }
    }
    fun activePlan(): TripPlan? = resolvedPlan
    private fun friendlyError(e: Exception): String = when (e) {
        is NoTripRouteException -> e.message ?: "No route is available for this travel mode."
        is java.io.IOException -> "Check your connection and try again."
        is IllegalArgumentException, is IllegalStateException -> e.message?.take(250) ?: "Could not complete this trip. Try again."
        else -> "Could not load Wayfinder data. Please try again."
    }
    fun formatDuration(seconds: Long): String { val minutes = ceil(seconds / 60.0).toLong().coerceAtLeast(1); return if (minutes >= 60) "${minutes / 60} hr ${minutes % 60} min" else "$minutes min" }
    fun formatDistance(meters: Long): String = if (meters < 1000) "$meters m" else String.format(java.util.Locale.getDefault(), "%.1f km", meters / 1000.0)
}
