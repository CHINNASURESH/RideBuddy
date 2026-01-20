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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val friends by viewModel.activeFriends.collectAsState()
    val context = LocalContext.current

    // Default camera position (e.g., London)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(51.505, -0.09), 10f)
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

    // UI State for selections
    var selectedDuration by remember { mutableStateOf(4) } // Hours
    var selectedFrequency by remember { mutableStateOf(10) } // Minutes (0 = Live)
    var isSharing by remember { mutableStateOf(false) }

    // Request permissions on launch
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            friends.forEach { user ->
                Marker(
                    state = MarkerState(position = user.position),
                    title = user.userId,
                    snippet = user.lastSeenText
                )
            }
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
