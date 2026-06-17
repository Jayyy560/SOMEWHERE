package com.somewhere.app.viewmodel

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.*
import com.somewhere.app.data.model.Drop
import com.somewhere.app.data.repository.AiRepository
import com.somewhere.app.data.repository.DropRepository
import com.somewhere.app.util.LocationUtils
import com.somewhere.app.util.LocationUtils.HYSTERESIS_MARGIN
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Represents a discovered drop with its spatial metadata.
 */
data class DiscoveredDrop(
    val drop: Drop,
    val distanceMeters: Float,
    val isUnlocked: Boolean,        // true if within UNLOCK_RADIUS
    val angleDegrees: Float,        // angular offset from device heading
    val isPrimary: Boolean = false, // closest item gets primary treatment
    val isNewlyDiscovered: Boolean = false // triggers ping pulse
)

/**
 * ViewModel for the Discovery screen.
 * Combines location updates + compass heading to produce spatially positioned overlays.
 */
@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val repository: DropRepository,
    private val aiRepository: AiRepository,
    private val sensorManager: SensorManager,
    private val fusedLocationClient: FusedLocationProviderClient
) : ViewModel() {

    data class DiscoveryUiState(
        val nearbyDrops: List<DiscoveredDrop> = emptyList(),
        val heading: Float = 0f,
        val userLat: Double = 0.0,
        val userLon: Double = 0.0,
        val hasLocation: Boolean = false,
        val locationAccuracyMeters: Float? = null,
        val selectedDrop: DiscoveredDrop? = null,
        val message: String? = null,
        val isSummarizing: Boolean = false,
        val summaryText: String? = null,
        val isCurating: Boolean = false,
        val curatedDrop: Pair<String, String>? = null, // (Intro, Text)
        val selectedCategory: String? = null
    )

    private val _uiState = MutableStateFlow(DiscoveryUiState())
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    // Track which drops we've already "discovered" to trigger ping only once
    private val discoveredIds = mutableSetOf<String>()

    // Current device heading (compass)
    private var currentHeading = 0f
    private var realtimeStarted = false

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                // Remap coordinate system for portrait mode (holding phone upright)
                val remappedMatrix = FloatArray(9)
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_X,
                    SensorManager.AXIS_Z,
                    remappedMatrix
                )

                val orientation = FloatArray(3)
                SensorManager.getOrientation(remappedMatrix, orientation)
                
                // Low-pass filter the heading slightly for visual stability
                val newHeading = Math.toDegrees(orientation[0].toDouble()).toFloat()
                val normalizedNew = if (newHeading < 0) newHeading + 360f else newHeading
                
                // Handle 359 -> 0 wrap around smoothly
                val diff = LocationUtils.angleDifference(currentHeading, normalizedNew)
                currentHeading = (currentHeading + diff * 0.15f)
                while (currentHeading < 0) currentHeading += 360f
                currentHeading %= 360f

                updateOverlayPositions()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            _uiState.value = _uiState.value.copy(
                userLat = location.latitude,
                userLon = location.longitude,
                hasLocation = true,
                locationAccuracyMeters = location.accuracy
            )
            // Fetch nearby drops whenever location updates
            fetchNearbyDrops(location.latitude, location.longitude)
        }
    }

    fun startTracking() {
        // Register fused rotation vector sensor for stable AR heading
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
        }

        // Request location updates
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setMinUpdateIntervalMillis(1000)
            .build()

        try {
            // Fetch immediately so stationary emulators/devices don't hang waiting for an update
            // Use getCurrentLocation instead of lastLocation to ensure high accuracy matching the DropScreen
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, com.google.android.gms.tasks.CancellationTokenSource().token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        _uiState.value = _uiState.value.copy(
                            userLat = location.latitude,
                            userLon = location.longitude,
                            hasLocation = true,
                            locationAccuracyMeters = location.accuracy
                        )
                        fetchNearbyDrops(location.latitude, location.longitude)
                    }
                }
            
            fusedLocationClient.requestLocationUpdates(
                request, locationCallback, Looper.getMainLooper()
            )
        } catch (_: SecurityException) {
            // Permission not granted — handled in UI layer
        }

        if (!realtimeStarted) {
            realtimeStarted = true
            viewModelScope.launch {
                repository.startRealtimeDrops()
            }

            viewModelScope.launch {
                repository.dropChanges.collectLatest {
                    val state = _uiState.value
                    if (state.hasLocation) {
                        fetchNearbyDrops(state.userLat, state.userLon)
                    }
                }
            }
        }
    }

    fun stopTracking() {
        sensorManager.unregisterListener(sensorListener)
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    fun selectDrop(drop: DiscoveredDrop?) {
        _uiState.update { it.copy(selectedDrop = drop) }
        
        // Record unlock in backend
        if (drop != null) {
            viewModelScope.launch {
                repository.unlockDrop(drop.drop.id)
            }
        }
    }

    fun setCategoryFilter(category: String?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        val state = _uiState.value
        if (state.hasLocation) {
            fetchNearbyDrops(state.userLat, state.userLon)
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun deleteDrop(item: DiscoveredDrop) {
        viewModelScope.launch {
            repository.deleteDrop(item.drop)
            _uiState.value = _uiState.value.copy(
                selectedDrop = null,
                message = "Drop deleted"
            )
            fetchNearbyDrops(_uiState.value.userLat, _uiState.value.userLon)
        }
    }

    fun reportDrop(item: DiscoveredDrop) {
        viewModelScope.launch {
            repository.reportDrop(item.drop.id)
            _uiState.value = _uiState.value.copy(
                selectedDrop = null,
                message = "Drop reported. It will no longer be visible to you."
            )
            fetchNearbyDrops(_uiState.value.userLat, _uiState.value.userLon)
        }
    }

    fun blockUser(authorName: String) {
        viewModelScope.launch {
            repository.blockUser(authorName)
            _uiState.value = _uiState.value.copy(
                selectedDrop = null,
                message = "User blocked. Their drops will no longer be visible."
            )
            fetchNearbyDrops(_uiState.value.userLat, _uiState.value.userLon)
        }
    }

    fun summarizePlace() {
        val drops = _uiState.value.nearbyDrops.map { it.drop }
        if (drops.isEmpty()) {
            _uiState.value = _uiState.value.copy(summaryText = "No drops nearby to summarize.")
            return
        }

        _uiState.value = _uiState.value.copy(isSummarizing = true, summaryText = null)
        viewModelScope.launch {
            val summary = aiRepository.getPlaceSummary(drops)
            _uiState.value = _uiState.value.copy(
                isSummarizing = false,
                summaryText = summary
            )
        }
    }

    fun clearSummary() {
        _uiState.value = _uiState.value.copy(summaryText = null, isSummarizing = false)
    }

    fun curateDrop() {
        val drops = _uiState.value.nearbyDrops.map { it.drop }
        if (drops.isEmpty()) {
            _uiState.value = _uiState.value.copy(message = "No drops nearby for the curator.")
            return
        }

        _uiState.value = _uiState.value.copy(isCurating = true, curatedDrop = null)
        viewModelScope.launch {
            val curated = aiRepository.getCuratorDrop(drops)
            if (curated != null) {
                _uiState.value = _uiState.value.copy(
                    isCurating = false,
                    curatedDrop = curated
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isCurating = false,
                    message = "Curator is resting right now."
                )
            }
        }
    }

    fun clearCuratedDrop() {
        _uiState.value = _uiState.value.copy(curatedDrop = null, isCurating = false)
    }

    private fun fetchNearbyDrops(lat: Double, lon: Double) {
        viewModelScope.launch {
            val nearbyWithDistance = repository.getDropsNear(lat, lon)
            val currentCategory = _uiState.value.selectedCategory
            
            val filtered = if (currentCategory != null) {
                nearbyWithDistance.filter { it.first.category == currentCategory }
            } else {
                nearbyWithDistance
            }

            val previousIds = _uiState.value.nearbyDrops.map { it.drop.id }.toSet()

            val discovered = filtered.mapIndexed { index, (drop, distance) ->
                val bearing = LocationUtils.bearing(lat, lon, drop.latitude, drop.longitude)
                val angle = LocationUtils.angleDifference(currentHeading, bearing)
                val isNew = drop.id !in discoveredIds

                if (isNew) discoveredIds.add(drop.id)

                DiscoveredDrop(
                    drop = drop,
                    distanceMeters = distance,
                    isUnlocked = distance <= LocationUtils.UNLOCK_RADIUS,
                    angleDegrees = angle,
                    isPrimary = index == 0, // closest is primary
                    isNewlyDiscovered = isNew
                )
            }

            val merged = discovered
                .distinctBy { it.drop.id }
                .sortedBy { it.distanceMeters }
                .take(LocationUtils.MAX_VISIBLE)

            _uiState.value = _uiState.value.copy(nearbyDrops = merged)
        }
    }

    private fun updateOverlayPositions() {
        val state = _uiState.value
        if (state.nearbyDrops.isEmpty()) return
        if (!state.hasLocation) return

        val updated = state.nearbyDrops.map { item ->
            val bearing = LocationUtils.bearing(
                state.userLat, state.userLon,
                item.drop.latitude, item.drop.longitude
            )
            val angle = LocationUtils.angleDifference(currentHeading, bearing)
            val dist = LocationUtils.haversineDistance(
                state.userLat, state.userLon,
                item.drop.latitude, item.drop.longitude
            )

            item.copy(
                distanceMeters = dist,
                isUnlocked = dist <= LocationUtils.UNLOCK_RADIUS,
                angleDegrees = angle,
                isNewlyDiscovered = false
            )
        }
            .sortedBy { it.distanceMeters }
            .mapIndexed { index, item -> item.copy(isPrimary = index == 0) }

        _uiState.value = state.copy(nearbyDrops = updated, heading = currentHeading)
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }
}
