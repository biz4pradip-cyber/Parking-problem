package com.parkspot.app.ui

import android.hardware.GeomagneticField
import android.location.Location
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.parkspot.app.ParkSpotApplication
import com.parkspot.app.R
import com.parkspot.app.bluetooth.PairedDevice
import com.parkspot.app.bluetooth.PairedDevices
import com.parkspot.app.data.AutoParkConfig
import com.parkspot.app.data.AutoParkSettings
import com.parkspot.app.data.ParkingRepository
import com.parkspot.app.data.ParkingSpot
import com.parkspot.app.data.PhotoStore
import com.parkspot.app.location.CompassClient
import com.parkspot.app.location.CompassReading
import com.parkspot.app.location.LocationClient
import com.parkspot.app.util.GeoUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.max

/** A one-shot message for the snackbar. */
data class UiMessage(@StringRes val textRes: Int, val id: Long = System.currentTimeMillis())

data class HomeUiState(
    val activeSpot: ParkingSpot? = null,
    val history: List<ParkingSpot> = emptyList(),
    val isSaving: Boolean = false,
    val locationPermissionGranted: Boolean = false,
    val now: Long = System.currentTimeMillis(),
) {
    val parkedFor: Long?
        get() = activeSpot?.let { max(0L, now - it.savedAt) }

    val reminderIn: Long?
        get() = activeSpot?.reminderAt?.let { it - now }
}

data class FindUiState(
    val spot: ParkingSpot? = null,
    val currentLocation: Location? = null,
    val compass: CompassReading? = null,
    val hasCompass: Boolean = false,
    val locationPermissionGranted: Boolean = false,
    /** Local difference between magnetic and true north, in degrees. */
    val magneticDeclination: Float = 0f,
) {
    /** Metres from here to the car, or `null` while we have no fix. */
    val distanceMeters: Double?
        get() {
            val spot = spot ?: return null
            val here = currentLocation ?: return null
            return GeoUtils.distanceMeters(here.latitude, here.longitude, spot.latitude, spot.longitude)
        }

    /** Bearing to the car, degrees clockwise from true north. */
    val bearingToCar: Double?
        get() {
            val spot = spot ?: return null
            val here = currentLocation ?: return null
            return GeoUtils.bearingDegrees(here.latitude, here.longitude, spot.latitude, spot.longitude)
        }

    /**
     * Heading of the phone relative to **true** north. The compass reports magnetic north, so the
     * local declination is added; without it the arrow can be off by more than 20° in some parts
     * of the world.
     */
    val trueHeading: Float?
        get() {
            val azimuth = compass?.azimuthDegrees ?: return null
            return GeoUtils.normalizeDegrees(azimuth + magneticDeclination)
        }

    /** How far to rotate the arrow on screen so it points at the car. */
    val arrowRotation: Float?
        get() {
            val bearing = bearingToCar ?: return null
            val heading = trueHeading ?: return null
            return GeoUtils.normalizeDegrees(bearing.toFloat() - heading)
        }

    /** Close enough that walking by bearing stops being meaningful. */
    val hasArrived: Boolean
        get() {
            val distance = distanceMeters ?: return false
            val noise = max(currentLocation?.accuracy ?: 0f, spot?.accuracyMeters ?: 0f)
            return distance <= max(ARRIVAL_RADIUS_METERS, noise.toDouble())
        }

    private companion object {
        const val ARRIVAL_RADIUS_METERS = 8.0
    }
}

class ParkingViewModel(
    private val repository: ParkingRepository,
    private val locationClient: LocationClient,
    private val compassClient: CompassClient,
    private val photoStore: PhotoStore,
    private val autoParkSettings: AutoParkSettings,
    private val pairedDevices: PairedDevices,
) : ViewModel() {

    private val locationPermission = MutableStateFlow(locationClient.hasLocationPermission())
    private val saving = MutableStateFlow(false)

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message

    private val _devices = MutableStateFlow<List<PairedDevice>>(emptyList())

    /** Paired Bluetooth devices, for choosing which one is the car. */
    val devices: StateFlow<List<PairedDevice>> = _devices

    val autoPark: StateFlow<AutoParkConfig> = autoParkSettings.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AutoParkConfig())

    /** Keeps "parked 2 h 05 min ago" and the reminder countdown ticking over. */
    private val ticker: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(TICK_INTERVAL_MILLIS)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val locationUpdates: Flow<Location?> = locationPermission
        .flatMapLatest { granted ->
            if (granted) locationClient.locationUpdates() else emptyFlow()
        }
        .map<Location, Location?> { it }
        .onStart { emit(null) }

    private val compassUpdates: Flow<CompassReading?> = compassClient.headings()
        .map<CompassReading, CompassReading?> { it }
        .onStart { emit(null) }

    val homeState: StateFlow<HomeUiState> = combine(
        repository.activeSpot,
        repository.history,
        saving,
        locationPermission,
        ticker,
    ) { spot, history, isSaving, granted, now ->
        HomeUiState(
            activeSpot = spot,
            history = history,
            isSaving = isSaving,
            locationPermissionGranted = granted,
            now = now,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState())

    val findState: StateFlow<FindUiState> = combine(
        repository.activeSpot,
        locationUpdates,
        compassUpdates,
        locationPermission,
    ) { spot, location, compass, granted ->
        FindUiState(
            spot = spot,
            currentLocation = location,
            compass = compass,
            hasCompass = compassClient.isAvailable,
            locationPermissionGranted = granted,
            magneticDeclination = location?.let { declinationAt(it) } ?: 0f,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), FindUiState())

    /** Declination changes only with position, so it is computed per fix, not per frame. */
    private fun declinationAt(location: Location): Float = GeomagneticField(
        location.latitude.toFloat(),
        location.longitude.toFloat(),
        location.altitude.toFloat(),
        System.currentTimeMillis(),
    ).declination

    // --- Actions -------------------------------------------------------------------------------

    fun onLocationPermissionChanged(granted: Boolean) {
        locationPermission.value = granted
    }

    /** Re-reads the real permission state, e.g. after returning from system settings. */
    fun refreshPermissionState() {
        locationPermission.value = locationClient.hasLocationPermission()
    }

    /** Takes a GPS fix and remembers it as the current parking spot. */
    fun saveCurrentSpot(onSaved: () -> Unit = {}) {
        if (saving.value) return
        viewModelScope.launch {
            if (!locationClient.hasLocationPermission()) {
                show(R.string.permission_body)
                return@launch
            }
            if (!locationClient.isLocationEnabled()) {
                show(R.string.error_no_location)
                return@launch
            }
            saving.value = true
            try {
                val location = locationClient.awaitCurrentLocation()
                if (location == null) {
                    show(R.string.error_no_location)
                } else {
                    repository.saveSpot(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracyMeters = location.accuracy,
                    )
                    onSaved()
                }
            } finally {
                saving.value = false
            }
        }
    }

    fun markFound() {
        viewModelScope.launch { repository.markFound() }
    }

    fun saveDetails(spot: ParkingSpot, level: String, spotLabel: String, note: String) {
        viewModelScope.launch { repository.updateDetails(spot, level, spotLabel, note) }
    }

    fun newPhotoUri(): Uri = photoStore.createPhotoUri()

    fun onPhotoCaptured(spot: ParkingSpot, uri: Uri) {
        viewModelScope.launch { repository.setPhoto(spot, uri.toString()) }
    }

    fun onPhotoCaptureCancelled(uri: Uri?) {
        photoStore.deleteIfEmpty(uri)
    }

    fun removePhoto(spot: ParkingSpot) {
        viewModelScope.launch { repository.setPhoto(spot, null) }
    }

    fun setReminderInMinutes(spot: ParkingSpot, minutes: Long) {
        val at = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes)
        viewModelScope.launch { repository.setReminder(spot, at) }
    }

    fun clearReminder(spot: ParkingSpot) {
        viewModelScope.launch { repository.setReminder(spot, null) }
    }

    fun deleteSpot(spot: ParkingSpot) {
        viewModelScope.launch { repository.delete(spot) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    // --- Automatic parking ---------------------------------------------------------------------

    fun hasBluetoothPermission(): Boolean = pairedDevices.hasPermission()

    fun isBluetoothAvailable(): Boolean = pairedDevices.isBluetoothAvailable()

    /** Re-reads the paired list, e.g. after the permission is granted or Bluetooth is switched on. */
    fun refreshPairedDevices() {
        _devices.value = pairedDevices.list()
    }

    fun setAutoParkEnabled(enabled: Boolean) {
        autoParkSettings.setEnabled(enabled)
    }

    fun setCar(device: PairedDevice?) {
        autoParkSettings.setCar(device?.address, device?.name)
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun show(@StringRes textRes: Int) {
        _message.value = UiMessage(textRes)
    }

    companion object {
        private const val TICK_INTERVAL_MILLIS = 5_000L
        private const val STOP_TIMEOUT_MILLIS = 3_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as ParkSpotApplication
                val container = application.container
                ParkingViewModel(
                    repository = container.repository,
                    locationClient = container.locationClient,
                    compassClient = container.compassClient,
                    photoStore = container.photoStore,
                    autoParkSettings = container.autoParkSettings,
                    pairedDevices = container.pairedDevices,
                )
            }
        }
    }
}
