package com.example.ridebuddy.ui

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ridebuddy.data.AuthRepository
import com.example.ridebuddy.data.LocationRepository
import com.example.ridebuddy.data.User
import com.example.ridebuddy.service.LocationService
import org.mapsforge.core.model.LatLong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.ridebuddy.routing.RoutingStateManager
import com.example.ridebuddy.routing.RoutingState
import com.example.ridebuddy.routing.VoiceAnnouncer
import com.example.ridebuddy.network.NetworkMonitor
import com.example.ridebuddy.billing.BillingManager
import com.example.ridebuddy.util.RemoteConfigManager
import com.example.ridebuddy.util.SmartReviewManager
import com.example.ridebuddy.util.getActivity

@HiltViewModel
class MainViewModel @Inject constructor(
    val routingStateManager: RoutingStateManager,
    private val routingEngine: com.example.ridebuddy.routing.OfflineRoutingEngine,
    private val repository: LocationRepository,
    private val authRepository: AuthRepository,
    private val application: Application,
    networkMonitor: NetworkMonitor,
    private val rideDao: com.example.ridebuddy.data.local.RideDao,
    private val billingManager: BillingManager,
    private val analyticsManager: com.example.ridebuddy.util.AnalyticsManager,
    private val smartReviewManager: SmartReviewManager,
    private val remoteConfigManager: RemoteConfigManager,
    val rideRecorder: RideRecorder
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    val isSmsFallbackEnabled: StateFlow<Boolean> = remoteConfigManager.isSmsFallbackEnabled
    val isSolarTelemetryEnabled: StateFlow<Boolean> = remoteConfigManager.isSolarTelemetryEnabled

    val isPro: StateFlow<Boolean> = repository.isProActive

    private val _isRecording = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    // Ideally, get current user ID from Auth. For now hardcoded or passed.
    val currentUserId = "current_user_id_123"

    private val _currentGroupId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val currentGroupId: StateFlow<String?> = _currentGroupId

    private val _mapControlEvents = MutableSharedFlow<MapControlEvent>(extraBufferCapacity = 10)
    val mapControlEvents = _mapControlEvents.asSharedFlow()

    sealed class MapControlEvent {
        data class Pan(val dx: Double, val dy: Double) : MapControlEvent()
        data class Zoom(val delta: Int) : MapControlEvent()
    }

    private var voiceAnnouncer: VoiceAnnouncer? = null

    init {
        voiceAnnouncer = VoiceAnnouncer(application)

        // Start listening to the permanent user document in Firestore to enable premium features
        viewModelScope.launch {
            try {
                val currentUserId = authRepository.getUserId()
                repository.listenToUserEntitlements(currentUserId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun shareLatestRide(context: android.content.Context) {
        viewModelScope.launch {
            val session = rideDao.getLastSession()
            if (session != null) {
                val points = rideDao.getPointsForSession(session.id)
                val intent = com.example.ridebuddy.util.GpxExporter.createShareIntent(context, session, points)
                if (intent != null) {
                    val chooser = Intent.createChooser(intent, "Share GPX Ride")
                    context.startActivity(chooser)
                }
            }
        }
    }


    fun submitFeedback(category: String, details: String) {
        viewModelScope.launch {
            try {
                // Get battery state
                val batteryIntent = application.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
                val level = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
                val batteryPct = if (scale > 0) level * 100 / scale else -1

                // Get network state
                val isOnlineState = isOnline.value

                // Get routing waypoints
                val waypoints = routingStateManager.routingState.value.waypoints.map { mapOf("lat" to it.latitude, "lon" to it.longitude) }

                // Get location (for now we check the activeFriends list to find our own location if shared, or we can just pass null if not immediately available. However, a better way is to pass current location from MapScreen if available. Let's add it to the MainViewModel state if we can, or just grab the last known location from the service)
                // Actually, the app tracks location via LocationService. We don't have a direct hook here. Let's see if we can get it from activeFriends.
                val currentUserLocation = activeFriends.value.find { it.userId == currentUserId }?.position

                repository.submitBetaFeedback(
                    userId = currentUserId,
                    category = category,
                    details = details,
                    batteryPct = batteryPct,
                    isOnline = isOnlineState,
                    waypoints = waypoints,
                    lat = currentUserLocation?.latitude,
                    lon = currentUserLocation?.longitude
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceAnnouncer?.shutdown()
    }

    fun speakTurnInstruction(text: String) {
        voiceAnnouncer?.speak(text)
    }

    fun panMap(dx: Double, dy: Double) {
        _mapControlEvents.tryEmit(MapControlEvent.Pan(dx, dy))
    }

    fun zoomMap(delta: Int) {
        _mapControlEvents.tryEmit(MapControlEvent.Zoom(delta))
    }

    fun skipWaypoint() {
        routingStateManager.skipWaypoint()
        val remainingWaypoints = routingStateManager.routingState.value.waypoints
        if (remainingWaypoints.size >= 2) {
            calculateRoute(remainingWaypoints)
        }
    }


    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeFriends: StateFlow<List<UserUiModel>> = _currentGroupId
        .flatMapLatest { groupId ->
            if (groupId == null) kotlinx.coroutines.flow.flowOf(emptyList())
            else repository.getActiveGroupRiders(groupId).map { users ->
                users.map { user ->
                    UserUiModel(
                        userId = user.userId,
                        position = LatLong(user.latitude, user.longitude),
                        heading = user.heading,
                        lastSeenText = "Last seen: ${user.lastUpdated?.toDate()}",
                        status = user.status
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun joinGroup(groupId: String) {
        _currentGroupId.value = groupId
    }

    fun updateHeading(heading: Float) {
        repository.localUserHeading.value = heading
    }

    fun setStatus(status: String?) {
        val groupId = _currentGroupId.value ?: return
        viewModelScope.launch {
            try {
                val currentUserId = authRepository.getUserId()
                repository.updateUserStatus(groupId, currentUserId, status)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startSharing(durationHours: Int, intervalMinutes: Int) {
        val groupId = _currentGroupId.value ?: return
        viewModelScope.launch {
            try {
                val currentUserId = authRepository.getUserId()
                val expiry = System.currentTimeMillis() + (durationHours * 60 * 60 * 1000)

                val intervalMillis = if (intervalMinutes == 0) 10000L else intervalMinutes * 60 * 1000L

                val intent = Intent(application, LocationService::class.java).apply {
                    action = LocationService.ACTION_START
                    putExtra(LocationService.EXTRA_USER_ID, currentUserId)
                    putExtra(LocationService.EXTRA_GROUP_ID, groupId)
                    putExtra(LocationService.EXTRA_EXPIRY, expiry)
                    putExtra(LocationService.EXTRA_INTERVAL, intervalMillis)
                }
                androidx.core.content.ContextCompat.startForegroundService(application, intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun logProUpgradeView() {
        analyticsManager.logProUpgradeView()
    }

    fun logProSubscribeTap() {
        analyticsManager.logProSubscribeTap()
    }

    fun launchBillingFlow(activity: Activity, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            billingManager.launchBillingFlow(activity, onComplete)
        }
    }

    fun toggleRecording(context: android.content.Context, start: Boolean, locationFlow: StateFlow<android.location.Location?>) {
        _isRecording.value = start

        if (start) {
            rideRecorder.startRecording(locationFlow)
        } else {
            rideRecorder.stopRecording()
        }

        // Keep the old intents in case we want to clean up LocationService
        // But since we removed the logic from there, we just start/stop location updates
        val intent = Intent(context, LocationService::class.java).apply {
            action = if (start) LocationService.ACTION_START_RECORDING else LocationService.ACTION_STOP_RECORDING
        }
        if (start && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        // If stopping recording, we consider it a ride completed for the review prompt
        if (!start) {
            context.getActivity()?.let { activity ->
                smartReviewManager.onRideCompleted(activity)
            }
        }
    }

    fun exportLatestRide(context: android.content.Context, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val session = rideDao.getLastSession()
            if (session != null) {
                val points = rideDao.getPointsForSession(session.id)
                val success = com.example.ridebuddy.util.GpxExporter.exportRideSession(context, session, points)
                onResult(success)
            } else {
                onResult(false)
            }
        }
    }

    fun stopSharing() {
        val intent = Intent(application, LocationService::class.java).apply {
            action = LocationService.ACTION_STOP
        }
        application.startService(intent)
    }

    fun updateFrequency(intervalMinutes: Int) {
        val intervalMillis = if (intervalMinutes == 0) 10000L else intervalMinutes * 60 * 1000L
        val intent = Intent(application, LocationService::class.java).apply {
            action = LocationService.ACTION_UPDATE_INTERVAL
            putExtra(LocationService.EXTRA_INTERVAL, intervalMillis)
        }
        application.startService(intent)
    }

    private val _preRideRouteResult = kotlinx.coroutines.flow.MutableStateFlow<com.example.ridebuddy.routing.RoutingResult?>(null)
    val preRideRouteResult: StateFlow<com.example.ridebuddy.routing.RoutingResult?> = _preRideRouteResult

    private val _currentVehicleProfile = kotlinx.coroutines.flow.MutableStateFlow("Bike")
    val currentVehicleProfile: StateFlow<String> = _currentVehicleProfile

    fun setVehicleProfile(profile: String) {
        _currentVehicleProfile.value = profile
    }

    fun calculatePreRideRoute(waypoints: List<org.mapsforge.core.model.LatLong>) {
        viewModelScope.launch {
            val result = routingEngine.calculateRoute(waypoints, _currentVehicleProfile.value)
            _preRideRouteResult.value = result
        }
    }

    fun clearPreRideRoute() {
        _preRideRouteResult.value = null
    }

    fun startPreRideNavigation() {
        val result = _preRideRouteResult.value
        if (result != null) {
            routingStateManager.startRouting(result)
            _preRideRouteResult.value = null
        }
    }

    fun calculateRoute(waypoints: List<org.mapsforge.core.model.LatLong>) {
        viewModelScope.launch {
            val result = routingEngine.calculateRoute(waypoints, _currentVehicleProfile.value)
            routingStateManager.startRouting(result)
        }
    }

}