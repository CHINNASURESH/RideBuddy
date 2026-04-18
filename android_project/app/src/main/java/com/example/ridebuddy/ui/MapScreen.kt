package com.example.ridebuddy.ui

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ridebuddy.data.User
import org.mapsforge.map.android.view.MapView
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.example.ridebuddy.data.offline.OfflineStorageManager
import com.example.ridebuddy.routing.RoutingState
import org.mapsforge.core.graphics.Color
import org.mapsforge.core.model.LatLong

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    viewModel: MainViewModel = hiltViewModel(),
    offlineStorageManager: OfflineStorageManager? = null
) {
    val friends by viewModel.activeFriends.collectAsState()
    val context = LocalContext.current

    val compassManager = remember { CompassManager(context) }
    val heading by compassManager.heading.collectAsState()

    val routingState by viewModel.routingStateManager.routingState.collectAsState()

    // Announce new turns
    LaunchedEffect(routingState.currentInstructionIndex) {
        val instruction = routingState.currentInstruction
        if (instruction != null) {
            viewModel.speakTurnInstruction(instruction.message)
        }
    }


    // Permissions
    val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    val permissionsState = rememberMultiplePermissionsState(permissions = permissions)

    DisposableEffect(permissionsState.allPermissionsGranted) {
        compassManager.start()
        onDispose {
            compassManager.stop()
        }
    }

    // UI State for selections
    var selectedDuration by remember { mutableStateOf(4) } // Hours
    var selectedFrequency by remember { mutableStateOf(10) } // Minutes (0 = Live)
    var isSharing by remember { mutableStateOf(false) }

    val ridersOverlay = remember { RidersOverlay() }

    // Request permissions on launch
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    // Combine friends list and heading to correctly update the overlay without overwriting state
    LaunchedEffect(friends, heading) {
        val currentUserId = viewModel.currentUserId
        val newRiders = friends.map { user ->
            val userHeading = if (user.userId == currentUserId) heading else null
            Rider(user.userId, user.position, android.graphics.Color.RED, userHeading)
        }
        ridersOverlay.setRiders(newRiders)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    val mapManager = MapsforgeMapManager(ctx, this)
                    if (offlineStorageManager != null) {
                        val mapFile = offlineStorageManager.getOfflineFile("germany.map")
                        mapManager.loadMapFile(mapFile)
                    }
                    this.layerManager.layers.add(ridersOverlay)
                }
            },
            update = { mapView ->
                // Mapsforge v0.20 MapViewPosition does not natively support setBearing/bearing API.
                // We fallback to Plan B: map remains North-Up, and we rotate the puck (rider overlay).
            }
        )

        // Turn By Turn Overlay
        if (routingState.isRoutingActive && routingState.currentInstruction != null) {
            TurnByTurnOverlay(
                currentInstruction = routingState.currentInstruction,
                distanceToNext = routingState.distanceToNextInstruction,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // Control Panel
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "Ride Buddy Controls", style = MaterialTheme.typography.titleLarge)

                if (!permissionsState.allPermissionsGranted) {
                    Text(
                        "Permissions required to share location.",
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = { permissionsState.launchMultiplePermissionRequest() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Permissions")
                    }
                } else {
                    if (isSharing) {
                        Button(
                            onClick = {
                                viewModel.stopSharing()
                                isSharing = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Stop Sharing")
                        }

                        Text("Update Frequency:", style = MaterialTheme.typography.labelLarge)
                        FrequencySelector(
                            selected = selectedFrequency,
                            onSelect = {
                                selectedFrequency = it
                                viewModel.updateFrequency(it)
                            }
                        )
                    } else {
                        Text("Start Sharing Location:", style = MaterialTheme.typography.labelLarge)

                        Text("Duration:", style = MaterialTheme.typography.bodyMedium)
                        DurationSelector(
                            selected = selectedDuration,
                            onSelect = { selectedDuration = it }
                        )

                        Text("Update Frequency:", style = MaterialTheme.typography.bodyMedium)
                        FrequencySelector(
                            selected = selectedFrequency,
                            onSelect = { selectedFrequency = it }
                        )

                        Button(
                            onClick = {
                                viewModel.startSharing(selectedDuration, selectedFrequency)
                                isSharing = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Start Sharing")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrequencySelector(selected: Int, onSelect: (Int) -> Unit) {
    val options = listOf(
        0 to "Live",
        3 to "3m",
        5 to "5m",
        10 to "10m"
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationSelector(selected: Int, onSelect: (Int) -> Unit) {
    val options = listOf(4, 8, 12, 24)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { value ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text("${value}h") }
            )
        }
    }
}
