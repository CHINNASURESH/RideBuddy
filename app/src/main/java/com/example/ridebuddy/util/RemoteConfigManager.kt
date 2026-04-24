package com.example.ridebuddy.util

import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigManager @Inject constructor() {
    private val remoteConfig = Firebase.remoteConfig

    private val _isSmsFallbackEnabled = MutableStateFlow(true)
    val isSmsFallbackEnabled: StateFlow<Boolean> = _isSmsFallbackEnabled.asStateFlow()

    private val _isSolarTelemetryEnabled = MutableStateFlow(true)
    val isSolarTelemetryEnabled: StateFlow<Boolean> = _isSolarTelemetryEnabled.asStateFlow()

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600 // 1 hour
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(com.example.ridebuddy.R.xml.remote_config_defaults)

        // Read synchronously what's already in the cache so we have it immediately available on cold-start
        _isSmsFallbackEnabled.value = remoteConfig.getBoolean("is_sms_fallback_enabled")
        _isSolarTelemetryEnabled.value = remoteConfig.getBoolean("is_solar_telemetry_enabled")

        fetchAndActivate()
    }

    private fun fetchAndActivate() {
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("RemoteConfig", "Config params updated: \${task.result}")
                } else {
                    Log.e("RemoteConfig", "Fetch failed")
                }
                updateFlags()
            }
    }

    private fun updateFlags() {
        _isSmsFallbackEnabled.value = remoteConfig.getBoolean("is_sms_fallback_enabled")
        _isSolarTelemetryEnabled.value = remoteConfig.getBoolean("is_solar_telemetry_enabled")
    }
}
