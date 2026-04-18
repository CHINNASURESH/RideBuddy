package com.example.ridebuddy.ui

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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.ridebuddy.routing.RoutingStateManager
import com.example.ridebuddy.routing.RoutingState
import com.example.ridebuddy.routing.TtsHelper
import com.example.ridebuddy.network.NetworkMonitor

@HiltViewModel
class MainViewModel @Inject constructor(
    val routingStateManager: RoutingStateManager,
    private val routingEngine: com.example.ridebuddy.routing.OfflineRoutingEngine,
    private val repository: LocationRepository,
    private val authRepository: AuthRepository,
    private val application: Application,
    networkMonitor: NetworkMonitor,
    private val rideDao: com.example.ridebuddy.data.local.RideDao
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    private val _isRecording = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    // Ideally, get current user ID from Auth. For now hardcoded or passed.
    val currentUserId = "current_user_id_123"

    private val _mapControlEvents = MutableSharedFlow<MapControlEvent>(extraBufferCapacity = 10)
    val mapControlEvents = _mapControlEvents.asSharedFlow()

    sealed class MapControlEvent {
        data class Pan(val dx: Double, val dy: Double) : MapControlEvent()
        data class Zoom(val delta: Int) : MapControlEvent()
    }

    private var ttsHelper: TtsHelper? = null

    init {
        ttsHelper = TtsHelper(application)
    }

    override fun onCleared() {
        super.onCleared()
        ttsHelper?.shutdown()
    }

    fun speakTurnInstruction(text: String) {
        ttsHelper?.speak(text)
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


    val activeFriends: StateFlow<List<UserUiModel>> = repository.getActiveGroupRiders("default_group")
        .map { users ->
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateHeading(heading: Float) {
        repository.localUserHeading.value = heading
    }

    fun setStatus(status: String?) {
        viewModelScope.launch {
            try {
                val currentUserId = authRepository.getUserId()
                repository.updateUserStatus("default_group", currentUserId, status)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startSharing(durationHours: Int, intervalMinutes: Int) {
        viewModelScope.launch {
            try {
                val currentUserId = authRepository.getUserId()
                val expiry = System.currentTimeMillis() + (durationHours * 60 * 60 * 1000)

                val intervalMillis = if (intervalMinutes == 0) 10000L else intervalMinutes * 60 * 1000L

                val intent = Intent(application, LocationService::class.java).apply {
                    action = LocationService.ACTION_START
                    putExtra(LocationService.EXTRA_USER_ID, currentUserId)
                    putExtra(LocationService.EXTRA_GROUP_ID, "default_group")
                    putExtra(LocationService.EXTRA_EXPIRY, expiry)
                    putExtra(LocationService.EXTRA_INTERVAL, intervalMillis)
                }
                androidx.core.content.ContextCompat.startForegroundService(application, intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleRecording(context: android.content.Context, start: Boolean) {
        _isRecording.value = start
        val intent = Intent(context, LocationService::class.java).apply {
            action = if (start) LocationService.ACTION_START_RECORDING else LocationService.ACTION_STOP_RECORDING
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
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

    fun calculateRoute(waypoints: List<org.mapsforge.core.model.LatLong>) {
        viewModelScope.launch {
            val result = routingEngine.calculateRoute(waypoints)
            routingStateManager.startRouting(result)
        }
    }

}