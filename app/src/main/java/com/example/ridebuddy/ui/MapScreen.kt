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


import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ridebuddy.util.GpxParser
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Menu
import com.example.ridebuddy.data.User
import org.mapsforge.map.android.view.MapView
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.example.ridebuddy.data.offline.OfflineStorageManager
import com.example.ridebuddy.routing.RoutingState
import org.mapsforge.core.graphics.Color
import org.mapsforge.core.model.LatLong
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri

import org.mapsforge.map.android.graphics.AndroidGraphicFactory

@Composable
fun MapsforgeMap(
    modifier: Modifier = Modifier,
    onMapReady: (MapView) -> Unit
) {
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mapViewInstance?.destroyAll()
            AndroidGraphicFactory.clearResourceMemoryCache()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                mapViewInstance = this
                onMapReady(this)
            }
        },
        update = { mapView ->
            // Update logic here if needed
        }
    )
}

fun findActivity(context: android.content.Context): android.app.Activity? {
    var currentContext = context
    while (currentContext is ContextWrapper) {
        if (currentContext is android.app.Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

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
    val speedMps by compassManager.speedMps.collectAsState()
    val altitude by compassManager.altitude.collectAsState()

    val routingState by viewModel.routingStateManager.routingState.collectAsState()
    var showFeedbackDialog by remember { mutableStateOf(false) }
    val isOnline by viewModel.isOnline.collectAsState()

    val isPro by viewModel.isPro.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

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
    var showProDialog = remember { mutableStateOf(false) }

    val isSolarTelemetryEnabled by viewModel.isSolarTelemetryEnabled.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()

    val ridersOverlay = remember { RidersOverlay() }

    val coroutineScope = rememberCoroutineScope()
    var mapManager by remember { mutableStateOf<MapsforgeMapManager?>(null) }
    var importedRoute by remember { mutableStateOf<List<LatLong>>(emptyList()) }

    // Auto-Dark Mode based on sunset with continuous updates
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000L) // update every minute
            currentTime = System.currentTimeMillis()
        }
    }

    LaunchedEffect(routingState.destinationSunsetTime, currentTime) {
        val sunsetTime = if (isSolarTelemetryEnabled) routingState.destinationSunsetTime else null
        if (sunsetTime != null) {
            val isNightMode = currentTime > sunsetTime
            mapManager?.setNightMode(isNightMode)
        } else {
            mapManager?.setNightMode(false) // Default to day mode
        }
    }

    val gpxPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                context.contentResolver.openInputStream(it)?.let { stream ->
                    val parsedPoints = GpxParser.parse(stream)
                    importedRoute = parsedPoints
                }
            }
        }
    }


    // Request permissions on launch
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    // Combine friends list and heading to correctly update the overlay without overwriting state
    LaunchedEffect(friends, heading) {
        val currentUserId = viewModel.currentUserId
        // Pass local heading down so it can be picked up for SMS dispatches when offline
        viewModel.updateHeading(heading)

        val newRiders = friends.map { user ->
            val userHeading = if (user.userId == currentUserId) heading else user.heading
            Rider(user.userId, user.position, android.graphics.Color.RED, userHeading, user.status)
        }
        ridersOverlay.setRiders(newRiders)
    }


    LaunchedEffect(importedRoute) {
        if (importedRoute.isNotEmpty()) {
            mapManager?.drawImportedRoute(importedRoute)
        }
    }


    LaunchedEffect(routingState.waypoints) {
        mapManager?.drawWaypoints(routingState.waypoints)
        if (routingState.waypoints.size >= 2) {
            viewModel.calculateRoute(routingState.waypoints)
        }
    }


    LaunchedEffect(routingState.routePath) {
        if (routingState.routePath.isNotEmpty()) {
            mapManager?.drawRoute(routingState.routePath)
        }
    }

    LaunchedEffect(viewModel.mapControlEvents) {
        viewModel.mapControlEvents.collect { event ->
            when (event) {
                is MainViewModel.MapControlEvent.Pan -> mapManager?.pan(event.dx, event.dy)
                is MainViewModel.MapControlEvent.Zoom -> {
                    if (event.delta > 0) mapManager?.zoomIn() else mapManager?.zoomOut()
                }
            }
        }
    }

    var showStatusMenu by remember { mutableStateOf(false) }

    val DEBUG_ASO_MODE = false

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                NavigationDrawerItem(
                    label = { Text("Community Board") },
                    selected = false,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.com/invite/ridebuddy"))
                        context.startActivity(intent)
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
    Box(modifier = Modifier.fillMaxSize()) {
        val mapExtractionReady by offlineStorageManager?.mapExtractionReady?.collectAsState(initial = false) ?: remember { mutableStateOf(false) }

        LaunchedEffect(mapExtractionReady, mapManager) {
            if (mapExtractionReady && mapManager != null && offlineStorageManager != null) {
                val mapFile = offlineStorageManager.getOfflineFile("germany.map")
                mapManager?.loadMapFile(mapFile)
            }
        }

        MapsforgeMap(
            modifier = Modifier.fillMaxSize(),
            onMapReady = { mapView ->
                val newMapManager = MapsforgeMapManager(mapView.context, mapView)
                mapManager = newMapManager
                newMapManager.onMapLongPressListener = object : MapsforgeMapManager.OnMapLongPressListener {
                    override fun onLongPress(latLong: LatLong) {
                        viewModel.routingStateManager.addWaypoint(latLong)
                    }
                }
                mapView.layerManager.layers.add(ridersOverlay)

                if (DEBUG_ASO_MODE) {
                    newMapManager.setNightMode(true)
                    val mockRoute = listOf(
                        LatLong(46.5580, 12.0125), // Dolomite pass fake data
                        LatLong(46.5600, 12.0150),
                        LatLong(46.5650, 12.0160),
                        LatLong(46.5700, 12.0180),
                        LatLong(46.5750, 12.0140),
                        LatLong(46.5800, 12.0100)
                    )
                    newMapManager.drawRoute(mockRoute, strokeWidth = 12f, color = android.graphics.Color.CYAN)
                    newMapManager.setCenter(LatLong(46.5650, 12.0160))
                    newMapManager.setZoomLevel(15.toByte())
                }
            }
        )

        IconButton(
            onClick = { coroutineScope.launch { drawerState.open() } },
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        ) {
            Icon(Icons.Filled.Menu, contentDescription = "Menu")
        }

        // Network Status Overlay
        if (!isOnline && !DEBUG_ASO_MODE) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ) {
                Text(
                    text = "No Connection - Offline Mode",
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // Turn By Turn Overlay
        if ((routingState.isRoutingActive && routingState.currentInstruction != null) || DEBUG_ASO_MODE) {
            val instruction = if (DEBUG_ASO_MODE) {
                com.example.ridebuddy.routing.TurnInstruction(
                    coordinate = LatLong(0.0, 0.0),
                    command = 5, // TR
                    message = "Turn right onto Passo di Giau",
                    distanceToNext = 150.0,
                    indexInTrack = 0
                )
            } else routingState.currentInstruction

            val dist = if (DEBUG_ASO_MODE) 150.0 else routingState.distanceToNextInstruction

            TurnByTurnOverlay(
                currentInstruction = instruction,
                distanceToNext = dist,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = if (!isOnline && !DEBUG_ASO_MODE) 32.dp else 0.dp)
            )
        }

        // Night Riding Warning Overlay
        if (isSolarTelemetryEnabled && routingState.isRoutingActive && routingState.isNightRidingAnticipated && !DEBUG_ASO_MODE) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (!isOnline) 100.dp else 68.dp)
                    .fillMaxWidth(0.9f),
                color = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = "Warning", modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = "Night Riding Anticipated: Arrival after sunset.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Rider Dashboard
        if (routingState.isRoutingActive || DEBUG_ASO_MODE) {
            if (showFeedbackDialog) {
                FeedbackDialog(
                    onDismiss = { showFeedbackDialog = false },
                    onSubmit = { category, details ->
                        viewModel.submitFeedback(category, details)
                        showFeedbackDialog = false
                    }
                )
            }
            RiderDashboard(
                speedMps = if (DEBUG_ASO_MODE) 12.5f else speedMps, // 45 km/h
                altitudeMeters = if (DEBUG_ASO_MODE) 2236.0 else altitude,
                distanceToDestinationMeters = if (DEBUG_ASO_MODE) 12400.0 else routingState.distanceToDestination,
                etaMillis = if (DEBUG_ASO_MODE) System.currentTimeMillis() + 1800000 else routingState.expectedArrivalTime,
                onFeedbackClick = { showFeedbackDialog = true },
                onStopClick = {
                    viewModel.routingStateManager.clearWaypoints()
                    viewModel.routingStateManager.stopRouting()
                    mapManager?.clearRouteAndWaypoints()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                isSolarTelemetryEnabled = isSolarTelemetryEnabled
            )
        } else if (!DEBUG_ASO_MODE) {
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

                        Button(
                            onClick = { gpxPickerLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Import GPX Route")
                        }
                    }

                    if (!isPro) {
                        Button(
                            onClick = {
                                viewModel.logProUpgradeView()
                                showProDialog.value = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Unlock Ridebuddy Pro")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Record Ride:", style = MaterialTheme.typography.labelLarge)
                    if (isRecording) {
                        Button(
                            onClick = {
                                viewModel.toggleRecording(context, false)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Stop Recording")
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.toggleRecording(context, true)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Start Recording")
                        }
                        Button(
                            onClick = {
                                viewModel.exportLatestRide(context) { success ->
                                    // Normally we would show a toast here, but simple implementation is fine
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Export Last Ride GPX")
                        }

                        Button(
                            onClick = {
                                viewModel.shareLatestRide(context)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Share Last Ride GPX")
                        }
                    }
                }
            }
            }
        }

        // Quick-Action UI
        if (isSharing) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp, bottom = 100.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (showStatusMenu) {
                    FloatingActionButton(
                        onClick = {
                            viewModel.setStatus("Need Gas")
                            showStatusMenu = false
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Filled.Place, contentDescription = "Need Gas")
                    }
                    FloatingActionButton(
                        onClick = {
                            viewModel.setStatus("Pulled Over")
                            showStatusMenu = false
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = "Pulled Over")
                    }
                    FloatingActionButton(
                        onClick = {
                            viewModel.setStatus("Hazard Ahead")
                            showStatusMenu = false
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = "Hazard Ahead")
                    }
                    FloatingActionButton(
                        onClick = {
                            viewModel.setStatus(null) // Clear Status
                            showStatusMenu = false
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear Status")
                    }
                }
                FloatingActionButton(
                    onClick = { showStatusMenu = !showStatusMenu },
                ) {
                    Icon(if (showStatusMenu) Icons.Filled.Clear else Icons.Filled.Add, contentDescription = "Status Menu")
                }
            }
        }

        if (showProDialog.value) {
            ModalBottomSheet(
                onDismissRequest = { showProDialog.value = false }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ridebuddy Pro", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Unlock premium features like SMS Fallback Sync and Solar Telemetry.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.logProSubscribeTap()
                            findActivity(context)?.let { activity ->
                                viewModel.launchBillingFlow(activity) { _ -> }
                                showProDialog.value = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Purchase Now")
                    }
                    Spacer(modifier = Modifier.height(32.dp))
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
