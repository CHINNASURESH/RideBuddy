package com.example.ridebuddy.ui

import android.location.Location
import com.example.ridebuddy.data.local.RideDao
import com.example.ridebuddy.data.local.RidePoint
import com.example.ridebuddy.data.local.RideSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.mapsforge.core.model.LatLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RideRecorder @Inject constructor(
    private val rideDao: RideDao
) {
    private val recorderScope = CoroutineScope(Dispatchers.IO)
    private var recordingJob: Job? = null

    private var currentSessionId: Long? = null
    private var lastRecordedLocation: Location? = null

    private val _recordedPath = MutableStateFlow<List<LatLong>>(emptyList())
    val recordedPath: StateFlow<List<LatLong>> = _recordedPath.asStateFlow()

    fun startRecording(locationFlow: StateFlow<Location?>) {
        if (recordingJob?.isActive == true) return

        recorderScope.launch {
            val session = RideSession(startTime = System.currentTimeMillis())
            currentSessionId = rideDao.insertSession(session)
            lastRecordedLocation = null
            _recordedPath.value = emptyList()

            recordingJob = launch {
                locationFlow.filterNotNull().collect { location ->
                    val sessionId = currentSessionId ?: return@collect

                    rideDao.insertPoint(
                        RidePoint(
                            sessionId = sessionId,
                            timestamp = System.currentTimeMillis(),
                            latitude = location.latitude,
                            longitude = location.longitude,
                            elevation = if (location.hasAltitude()) location.altitude else 0.0
                        )
                    )

                    // Update local path flow for UI
                    val newPoint = LatLong(location.latitude, location.longitude)
                    _recordedPath.value = _recordedPath.value + newPoint

                    // Update total distance
                    val lastLoc = lastRecordedLocation
                    if (lastLoc != null) {
                        val distance = lastLoc.distanceTo(location).toDouble()
                        val currentSession = rideDao.getSession(sessionId)
                        if (currentSession != null && distance > 0) {
                            rideDao.updateSession(currentSession.copy(totalDistanceMeters = currentSession.totalDistanceMeters + distance))
                        }
                    }
                    lastRecordedLocation = location
                }
            }
        }
    }

    fun stopRecording() {
        recordingJob?.cancel()
        recordingJob = null

        recorderScope.launch {
            currentSessionId?.let { sessionId ->
                val session = rideDao.getSession(sessionId)
                if (session != null) {
                    rideDao.updateSession(session.copy(endTime = System.currentTimeMillis()))
                }
            }
            currentSessionId = null
            lastRecordedLocation = null
            // We keep _recordedPath visible until next recording or user action if we want, or clear it.
            // Keeping it helps user see their route after finishing.
        }
    }
}
