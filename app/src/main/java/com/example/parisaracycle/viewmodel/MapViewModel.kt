package com.example.parisaracycle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.parisaracycle.data.model.BuddyLocation
import com.example.parisaracycle.data.model.DangerZone
import com.example.parisaracycle.data.model.DangerZoneType
import com.example.parisaracycle.data.model.MapLayer
import com.example.parisaracycle.data.model.PitStop
import com.example.parisaracycle.data.repository.BuddyLocationRepository
import com.example.parisaracycle.data.repository.DangerZoneRepository
import com.example.parisaracycle.data.repository.DirectionsRepository
import com.example.parisaracycle.data.repository.EcoStatsRepository
import com.example.parisaracycle.data.repository.LocationRepository
import com.example.parisaracycle.data.repository.PitStopRepository
import com.example.parisaracycle.utils.distanceToKm
import com.example.parisaracycle.utils.formatCo2
import com.example.parisaracycle.utils.formatDistance
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MapUiState(
    val hasLocationPermission: Boolean = false,
    val currentLocation: LatLng? = null,
    val destination: LatLng? = null,
    val routePoints: List<LatLng> = emptyList(),
    val routeDistanceKm: Double? = null,
    val dangerZones: List<DangerZone> = emptyList(),
    val pitStops: List<PitStop> = emptyList(),
    val buddyLocations: List<BuddyLocation> = emptyList(),
    val enabledLayers: Set<MapLayer> = MapLayer.entries.toSet(),
    val isPickingDestination: Boolean = false,
    val isRouting: Boolean = false,
    val isSharingLocation: Boolean = false,
    val pendingDangerPosition: LatLng? = null,
    val message: String? = null,
    val errorMessage: String? = null
)

class MapViewModel(
    private val locationRepository: LocationRepository,
    private val directionsRepository: DirectionsRepository,
    private val dangerZoneRepository: DangerZoneRepository,
    private val pitStopRepository: PitStopRepository,
    private val buddyLocationRepository: BuddyLocationRepository,
    private val ecoStatsRepository: EcoStatsRepository
) : ViewModel() {
    private val defaultCenter = LatLng(12.9716, 77.5946)
    private val _uiState = MutableStateFlow(
        MapUiState(
            currentLocation = defaultCenter,
            pitStops = pitStopRepository.getNearbyPitStops(defaultCenter),
            message = "Running in keyless local mode."
        )
    )
    private var activeUserId: String = ""
    private var allBuddyLocations: List<BuddyLocation> = emptyList()
    private var sharingJob: Job? = null

    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dangerZoneRepository.observeDangerZones().collect { zones ->
                _uiState.update { it.copy(dangerZones = zones) }
            }
        }

        viewModelScope.launch {
            buddyLocationRepository.observeBuddyLocations().collect { locations ->
                allBuddyLocations = locations
                updateVisibleBuddyLocations()
            }
        }
    }

    fun setActiveUser(userId: String) {
        activeUserId = userId
        updateVisibleBuddyLocations()
    }

    fun setLocationPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(hasLocationPermission = granted) }
        if (granted) refreshCurrentLocation() else stopSharing()
    }

    fun refreshCurrentLocation() {
        viewModelScope.launch {
            val location = locationRepository.getCurrentLocation()
            if (location == null) {
                _uiState.update {
                    it.copy(errorMessage = "Location unavailable. Check permission and device location.")
                }
                return@launch
            }

            setCurrentLocation(location)
        }
    }

    fun startDestinationPick() {
        _uiState.update {
            it.copy(
                isPickingDestination = true,
                message = "Tap the map to set destination.",
                errorMessage = null
            )
        }
    }

    fun onMapClick(position: LatLng) {
        if (!_uiState.value.isPickingDestination) return
        _uiState.update {
            it.copy(
                destination = position,
                routePoints = emptyList(),
                routeDistanceKm = null,
                isPickingDestination = false,
                message = "Destination selected.",
                errorMessage = null
            )
        }
    }

    fun onMapLongClick(position: LatLng) {
        _uiState.update { it.copy(pendingDangerPosition = position) }
    }

    fun dismissDangerReport() {
        _uiState.update { it.copy(pendingDangerPosition = null) }
    }

    fun reportDangerZone(type: DangerZoneType) {
        val position = _uiState.value.pendingDangerPosition ?: return
        val userId = activeUserId

        viewModelScope.launch {
            val result = dangerZoneRepository.reportDangerZone(position, type, userId)
            _uiState.update {
                it.copy(
                    pendingDangerPosition = null,
                    message = if (result.isSuccess) "${type.label} reported." else null,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun findRoute() {
        val origin = _uiState.value.currentLocation
        val destination = _uiState.value.destination

        if (origin == null) {
            _uiState.update { it.copy(errorMessage = "Set current location before routing.") }
            return
        }
        if (destination == null) {
            _uiState.update { it.copy(errorMessage = "Tap Pick destination, then tap the map.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isRouting = true, errorMessage = null, message = null) }
            val result = directionsRepository.getBicycleRoute(origin, destination)
            result.onSuccess { route ->
                ecoStatsRepository.addTrip(route.distanceKm)
                _uiState.update {
                    it.copy(
                        routePoints = route.points,
                        routeDistanceKm = route.distanceKm,
                        isRouting = false,
                        message = "Route saved: ${formatDistance(route.distanceKm)} saved ${formatCo2(route.distanceKm * 120.0)} CO2."
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(isRouting = false, errorMessage = throwable.message)
                }
            }
        }
    }

    fun toggleLayer(layer: MapLayer) {
        _uiState.update { state ->
            val updated = state.enabledLayers.toMutableSet()
            if (!updated.add(layer)) updated.remove(layer)
            state.copy(enabledLayers = updated)
        }
    }

    fun setSharingLocation(enabled: Boolean) {
        if (enabled) startSharing() else stopSharing()
    }

    fun clearTransientMessages() {
        _uiState.update { it.copy(message = null, errorMessage = null) }
    }

    private fun startSharing() {
        if (_uiState.value.isSharingLocation) return
        if (activeUserId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Sign in before sharing live location.") }
            return
        }
        if (!_uiState.value.hasLocationPermission) {
            val fallbackLocation = _uiState.value.currentLocation
            if (fallbackLocation != null) {
                viewModelScope.launch {
                    buddyLocationRepository.publishLocation(activeUserId, fallbackLocation)
                    _uiState.update {
                        it.copy(
                            isSharingLocation = true,
                            message = "Sharing demo location. Allow location for live updates.",
                            errorMessage = null
                        )
                    }
                }
                return
            }
            _uiState.update { it.copy(errorMessage = "Location permission is required for live sharing.") }
            return
        }

        sharingJob?.cancel()
        _uiState.update { it.copy(isSharingLocation = true, errorMessage = null) }
        sharingJob = viewModelScope.launch {
            locationRepository.locationUpdates()
                .onCompletion {
                    if (_uiState.value.isSharingLocation) {
                        _uiState.update { state ->
                            state.copy(isSharingLocation = false)
                        }
                    }
                }
                .collect { position ->
                    setCurrentLocation(position)
                    val result = buddyLocationRepository.publishLocation(activeUserId, position)
                    result.onFailure { throwable ->
                        _uiState.update {
                            it.copy(isSharingLocation = false, errorMessage = throwable.message)
                        }
                        sharingJob?.cancel()
                    }
                }
        }
    }

    private fun stopSharing() {
        val userId = activeUserId
        sharingJob?.cancel()
        sharingJob = null
        _uiState.update { it.copy(isSharingLocation = false) }
        if (userId.isNotBlank()) {
            viewModelScope.launch { buddyLocationRepository.removeLocation(userId) }
        }
    }

    private fun setCurrentLocation(position: LatLng) {
        _uiState.update {
            it.copy(
                currentLocation = position,
                pitStops = pitStopRepository.getNearbyPitStops(position),
                errorMessage = null
            )
        }
        updateVisibleBuddyLocations()
    }

    private fun updateVisibleBuddyLocations() {
        val now = System.currentTimeMillis()
        val center = _uiState.value.currentLocation
        val visible = allBuddyLocations.filter { buddy ->
            buddy.userId != activeUserId &&
                now - buddy.timestamp <= 5 * 60 * 1000 &&
                (center == null || center.distanceToKm(buddy.position) <= 5.0)
        }
        _uiState.update { it.copy(buddyLocations = visible) }
    }

    companion object {
        fun factory(
            locationRepository: LocationRepository,
            directionsRepository: DirectionsRepository,
            dangerZoneRepository: DangerZoneRepository,
            pitStopRepository: PitStopRepository,
            buddyLocationRepository: BuddyLocationRepository,
            ecoStatsRepository: EcoStatsRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    MapViewModel(
                        locationRepository = locationRepository,
                        directionsRepository = directionsRepository,
                        dangerZoneRepository = dangerZoneRepository,
                        pitStopRepository = pitStopRepository,
                        buddyLocationRepository = buddyLocationRepository,
                        ecoStatsRepository = ecoStatsRepository
                    ) as T
            }
    }
}
