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
    private val repository: LocationRepository,
    private val authRepository: AuthRepository,
    private val application: Application,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    // Ideally, get current user ID from Auth. For now hardcoded or passed.
    val currentUserId = "current_user_id_123"

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


    val activeFriends: StateFlow<List<UserUiModel>> = repository.getActiveGroupRiders("default_group")
        .map { users ->
            users.map { user ->
                UserUiModel(
                    userId = user.userId,
                    position = LatLong(user.latitude, user.longitude),
                    lastSeenText = "Last seen: ${user.lastUpdated?.toDate()}",
                    status = user.status
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
}
