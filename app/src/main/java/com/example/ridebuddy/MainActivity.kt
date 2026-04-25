package com.example.ridebuddy

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.annotation.SuppressLint
import com.example.ridebuddy.ui.MapScreen
import com.example.ridebuddy.ui.MainViewModel
import androidx.activity.viewModels
import android.view.KeyEvent
import androidx.lifecycle.lifecycleScope
import com.example.ridebuddy.data.offline.OfflineStorageManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var offlineStorageManager: OfflineStorageManager

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkBatteryOptimizations()

        // Launch coroutine to extract asset if needed
        lifecycleScope.launch {
            offlineStorageManager.extractMapAsset("germany.map")
        }

        setContent {
            MapScreen(viewModel = viewModel, offlineStorageManager = offlineStorageManager)
        }
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handledCodes = listOf(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_MEDIA_NEXT
        )

        if (event.keyCode in handledCodes) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> viewModel.panMap(0.0, 0.001)
                    KeyEvent.KEYCODE_DPAD_DOWN -> viewModel.panMap(0.0, -0.001)
                    KeyEvent.KEYCODE_DPAD_LEFT -> viewModel.panMap(-0.001, 0.0)
                    KeyEvent.KEYCODE_DPAD_RIGHT -> viewModel.panMap(0.001, 0.0)
                    KeyEvent.KEYCODE_VOLUME_UP -> viewModel.zoomMap(1)
                    KeyEvent.KEYCODE_VOLUME_DOWN -> viewModel.zoomMap(-1)
                    KeyEvent.KEYCODE_MEDIA_NEXT -> viewModel.skipWaypoint()
                }
            }
            return true // Consume both ACTION_DOWN and ACTION_UP
        }
        return super.dispatchKeyEvent(event)
    }

    private fun checkBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
}
