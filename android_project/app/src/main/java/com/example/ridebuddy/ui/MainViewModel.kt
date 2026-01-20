package com.example.ridebuddy.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ridebuddy.data.AuthRepository
import com.example.ridebuddy.data.LocationRepository
import com.example.ridebuddy.data.User
import com.example.ridebuddy.service.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: LocationRepository,
    private val authRepository: AuthRepository,
    private val application: Application
) : ViewModel() {

    val activeFriends: StateFlow<List<User>> = repository.getActiveFriends()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startSharing(durationHours: Int, intervalMinutes: Int) {
        viewModelScope.launch {
            try {
                val currentUserId = authRepository.getUserId()
                val expiry = System.currentTimeMillis() + (durationHours * 60 * 60 * 1000)

                val intervalMillis = if (intervalMinutes == 0) 10000L else intervalMinutes * 60 * 1000L

                val intent = Intent(application, LocationService::class.java).apply {
                    action = LocationService.ACTION_START
                    putExtra(LocationService.EXTRA_USER_ID, currentUserId)
                    putExtra(LocationService.EXTRA_EXPIRY, expiry)
                    putExtra(LocationService.EXTRA_INTERVAL, intervalMillis)
                }
                application.startForegroundService(intent)
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
