package com.example.ridebuddy.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

class CompassManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _heading = MutableStateFlow(0f)
    val heading: StateFlow<Float> = _heading.asStateFlow()

    private var lastHeading = 0f
    private val alpha = 0.1f // Low-pass filter constant

    // GPS location bearing can be used if moving
    private var isMoving = false

    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null

    fun start() {
        rotationVectorSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        startLocationUpdates()
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        stopLocationUpdates()
    }

    private fun startLocationUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L).build()
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        updateLocation(location)
                    }
                }
            }
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Permission not granted yet
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    private fun updateLocation(location: Location) {
        // If speed is greater than 1.5 m/s, prefer GPS bearing
        if (location.hasSpeed() && location.speed > 1.5f && location.hasBearing()) {
            isMoving = true
            updateHeading(location.bearing)
        } else {
            isMoving = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            // Remap coordinate system to handle vertical mounting
            val remappedMatrix = FloatArray(9)
            SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, remappedMatrix)

            val orientation = FloatArray(3)
            SensorManager.getOrientation(remappedMatrix, orientation)

            // azimuth in radians to degrees
            var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
            if (azimuth < 0) {
                azimuth += 360f
            }

            // Only use sensor if not moving fast
            if (!isMoving) {
                updateHeading(azimuth)
            }
        }
    }

    private fun updateHeading(newHeading: Float) {
        // Handle wraparound for smoothing (e.g. 359 to 1)
        var diff = newHeading - lastHeading
        if (diff > 180) diff -= 360
        if (diff < -180) diff += 360

        val smoothedHeading = (lastHeading + alpha * diff) % 360f

        var finalHeading = smoothedHeading
        if (finalHeading < 0) finalHeading += 360f

        // Only emit if change is significant enough to prevent tiny jitters
        if (abs(diff) > 0.5f) {
            lastHeading = finalHeading
            _heading.value = finalHeading
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
