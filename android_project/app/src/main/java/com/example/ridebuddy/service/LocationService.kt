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
import android.os.PowerManager
import com.example.ridebuddy.data.LocationRepository
import com.example.ridebuddy.network.NetworkMonitor
import com.example.ridebuddy.routing.RoutingStateManager
import com.example.ridebuddy.sms.SmsDispatcher
import com.example.ridebuddy.util.RemoteConfigManager
import com.example.ridebuddy.billing.BillingManager
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : Service() {

    @Inject
    lateinit var repository: LocationRepository

    @Inject
    lateinit var routingStateManager: RoutingStateManager

    @Inject
    lateinit var smsDispatcher: SmsDispatcher

    @Inject
    lateinit var remoteConfigManager: RemoteConfigManager

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @Inject
    lateinit var rideDao: com.example.ridebuddy.data.local.RideDao

    @Inject
    lateinit var billingManager: BillingManager

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var wakeLock: PowerManager.WakeLock? = null
    private var routingJob: kotlinx.coroutines.Job? = null
    private var smsJob: kotlinx.coroutines.Job? = null

    private var userId: String? = null
    private var groupId: String? = null
    private var sharingExpiry: Long = 0L
    private var lastUploadedLocation: Location? = null

    private var isRecording: Boolean = false
    private var currentSessionId: Long? = null
    private var lastRecordedLocation: Location? = null

    companion object {
        const val CHANNEL_ID = "location_service_channel"
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_UPDATE_INTERVAL = "ACTION_UPDATE_INTERVAL"
        const val ACTION_START_RECORDING = "ACTION_START_RECORDING"
        const val ACTION_STOP_RECORDING = "ACTION_STOP_RECORDING"

        const val EXTRA_INTERVAL = "EXTRA_INTERVAL"
        const val EXTRA_USER_ID = "EXTRA_USER_ID"
        const val EXTRA_GROUP_ID = "EXTRA_GROUP_ID"
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
                if (wakeLock == null) {
                    val powerManager = getSystemService(POWER_SERVICE) as PowerManager
                    wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RideBuddy::LocationServiceWakelock")
                    wakeLock?.acquire(10 * 60 * 60 * 1000L /*10 hours*/)
                }

                userId = intent.getStringExtra(EXTRA_USER_ID)
                groupId = intent.getStringExtra(EXTRA_GROUP_ID)
                sharingExpiry = intent.getLongExtra(EXTRA_EXPIRY, 0L)
                val interval = intent.getLongExtra(EXTRA_INTERVAL, 10000L) // Default 10s
                startForegroundService()
                startLocationUpdates(interval)

                if (routingJob == null) {
                    routingJob = serviceScope.launch {
                        routingStateManager.routingState.collect { state ->
                            updateNotification(state)
                        }
                    }
                }

                if (smsJob == null) {
                    smsJob = serviceScope.launch {
                        // 3-minute recurring SMS loop when offline
                        while (isActive) {
                            delay(3 * 60 * 1000L)

                            // Only dispatch if we are offline and have a valid location
                            if (remoteConfigManager.isSmsFallbackEnabled.value && !networkMonitor.isOnline.value && repository.isProActive.value) {
                                lastUploadedLocation?.let { loc ->
                                    userId?.let { uid ->
                                        val heading = repository.localUserHeading.value
                                        val status = repository.localUserStatus.value

                                        smsDispatcher.dispatch(
                                            userId = uid,
                                            lat = loc.latitude,
                                            lon = loc.longitude,
                                            heading = heading,
                                            status = status
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            ACTION_STOP -> {
                stopService()
            }
            ACTION_UPDATE_INTERVAL -> {
                val interval = intent.getLongExtra(EXTRA_INTERVAL, 10000L)
                startLocationUpdates(interval)
            }
            ACTION_START_RECORDING -> {
                isRecording = true
                serviceScope.launch {
                    val session = com.example.ridebuddy.data.local.RideSession(startTime = System.currentTimeMillis())
                    currentSessionId = rideDao.insertSession(session)
                    lastRecordedLocation = null
                }
                // Ensure foreground service is running and location updates are active
                startForegroundService()
                startLocationUpdates(10000L) // Ensure we have a reasonable interval
            }
            ACTION_STOP_RECORDING -> {
                isRecording = false
                currentSessionId?.let { sessionId ->
                    serviceScope.launch {
                        val session = rideDao.getSession(sessionId)
                        if (session != null) {
                            rideDao.updateSession(session.copy(endTime = System.currentTimeMillis()))
                        }
                    }
                }
                currentSessionId = null
                lastRecordedLocation = null
                // We might want to stop the service entirely if not sharing location
                if (userId == null || System.currentTimeMillis() > sharingExpiry) {
                    stopService()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun updateNotification(state: com.example.ridebuddy.routing.RoutingState) {
        val notificationManager = getSystemService(NotificationManager::class.java)

        val instruction = state.currentInstruction
        val contentText = if (state.isRoutingActive && instruction != null) {
            val dist = if (state.distanceToNextInstruction < 1000) {
                "${state.distanceToNextInstruction.toInt()}m"
            } else {
                String.format("%.1fkm", state.distanceToNextInstruction / 1000.0)
            }
            "Next: ${instruction.message} in $dist"
        } else {
            "Sharing location..."
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ride Buddy")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(1, notification)
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

                    routingStateManager.updateLocation(location)

                    if (isRecording) {
                        currentSessionId?.let { sessionId ->
                            serviceScope.launch {
                                rideDao.insertPoint(
                                    com.example.ridebuddy.data.local.RidePoint(
                                        sessionId = sessionId,
                                        timestamp = System.currentTimeMillis(),
                                        latitude = location.latitude,
                                        longitude = location.longitude,
                                        elevation = if (location.hasAltitude()) location.altitude else 0.0
                                    )
                                )

                                // Update distance
                                val lastLoc = lastRecordedLocation
                                if (lastLoc != null) {
                                    val distance = lastLoc.distanceTo(location).toDouble()
                                    val session = rideDao.getSession(sessionId)
                                    if (session != null && distance > 0) {
                                        rideDao.updateSession(session.copy(totalDistanceMeters = session.totalDistanceMeters + distance))
                                    }
                                }
                                lastRecordedLocation = location
                            }
                        }
                    }

                    val shouldUpdate = lastUploadedLocation?.let {
                        location.distanceTo(it) >= 10f
                    } ?: true

                    if (shouldUpdate) {
                        lastUploadedLocation = location
                        userId?.let { uid ->
                            groupId?.let { gid ->
                                serviceScope.launch {
                                    repository.updateUserLocation(
                                        gid,
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
            groupId?.let { gid ->
                serviceScope.launch {
                    repository.updateSharingStatus(gid, uid, false)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            } ?: run {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        } ?: run {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
