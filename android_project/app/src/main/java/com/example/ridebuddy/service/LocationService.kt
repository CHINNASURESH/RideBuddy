package com.example.ridebuddy.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import android.location.Location
import com.example.ridebuddy.data.LocationRepository
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : Service() {

    @Inject
    lateinit var repository: LocationRepository

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var userId: String? = null
    private var sharingExpiry: Long = 0L
    private var lastUploadedLocation: Location? = null

    companion object {
        const val CHANNEL_ID = "location_service_channel"
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_UPDATE_INTERVAL = "ACTION_UPDATE_INTERVAL"

        const val EXTRA_INTERVAL = "EXTRA_INTERVAL"
        const val EXTRA_USER_ID = "EXTRA_USER_ID"
        const val EXTRA_EXPIRY = "EXTRA_EXPIRY"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                userId = intent.getStringExtra(EXTRA_USER_ID)
                sharingExpiry = intent.getLongExtra(EXTRA_EXPIRY, 0L)
                val interval = intent.getLongExtra(EXTRA_INTERVAL, 10000L) // Default 10s
                startForegroundService()
                startLocationUpdates(interval)
            }
            ACTION_STOP -> {
                stopService()
            }
            ACTION_UPDATE_INTERVAL -> {
                val interval = intent.getLongExtra(EXTRA_INTERVAL, 10000L)
                startLocationUpdates(interval)
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ride Buddy")
            .setContentText("Sharing location...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notification)
        }
    }

    private fun startLocationUpdates(intervalMillis: Long) {
        // Remove existing updates if any to restart with new interval
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }

        // Priority depends on interval. If it's very frequent, High Accuracy. Else Balanced.
        val priority = if (intervalMillis <= 10000) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY

        val locationRequest = LocationRequest.Builder(priority, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    // Auto-stop logic
                    if (System.currentTimeMillis() > sharingExpiry) {
                        stopService()
                        return
                    }

                    val shouldUpdate = lastUploadedLocation?.let {
                        location.distanceTo(it) >= 10f
                    } ?: true

                    if (shouldUpdate) {
                        lastUploadedLocation = location
                        userId?.let { uid ->
                            serviceScope.launch {
                                repository.updateUserLocation(
                                    uid,
                                    location.latitude,
                                    location.longitude,
                                    true,
                                    sharingExpiry
                                )
                            }
                        }
                    }
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Assuming permissions are handled by the activity before starting service
        }
    }

    private fun stopService() {
        userId?.let { uid ->
            serviceScope.launch {
                repository.updateSharingStatus(uid, false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Location Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
