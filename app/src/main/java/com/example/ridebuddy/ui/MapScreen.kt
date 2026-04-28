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
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Menu
import com.example.ridebuddy.data.User
import org.mapsforge.map.android.view.MapView
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ridebuddy.data.offline.OfflineStorageManager
import com.example.ridebuddy.routing.RoutingState
import org.mapsforge.core.graphics.Color
import org.mapsforge.core.model.LatLong
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri

import org.mapsforge.map.android.graphics.AndroidGraphicFactory

enum class MapMode {
    MY_RIDE,
    MY_GROUP
}


fun calculateBoundingBox(
    currentLocation: android.location.Location,
    riders: List<com.example.ridebuddy.ui.UserUiModel>
): org.mapsforge.core.model.BoundingBox {
    var minLat = currentLocation.latitude
    var maxLat = currentLocation.latitude
    var minLon = currentLocation.longitude
    var maxLon = currentLocation.longitude

    for (rider in riders) {
        if (rider.position.latitude < minLat) minLat = rider.position.latitude
        if (rider.position.latitude > maxLat) maxLat = rider.position.latitude
        if (rider.position.longitude < minLon) minLon = rider.position.longitude
        if (rider.position.longitude > maxLon) maxLon = rider.position.longitude
    }

    val latMargin = (maxLat - minLat) * 0.1
    val lonMargin = (maxLon - minLon) * 0.1
    val margin = maxOf(latMargin, lonMargin, 0.001)

    return org.mapsforge.core.model.BoundingBox(
        minLat - margin, minLon - margin, maxLat + margin, maxLon + margin
    )
}

@Composable
fun MapsforgeMap(

    modifier: Modifier = Modifier,
    currentLocation: android.location.Location? = null,
    mapManager: MapsforgeMapManager? = null,
    mapMode: MapMode = MapMode.MY_RIDE,
    friends: List<com.example.ridebuddy.ui.UserUiModel> = emptyList(),
    extractedMapFile: java.io.File? = null,
    isCameraLockedToGps: Boolean = true,
    onMapInteraction: () -> Unit = {},
    onMapReady: (MapView) -> Unit
) {
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var isFirstLocation by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose {
            mapViewInstance?.destroyAll()
            AndroidGraphicFactory.clearResourceMemoryCache()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            MapView(ctx).apply {
                mapViewInstance = this
                setOnTouchListener { _, event ->
                    if (event.action == android.view.MotionEvent.ACTION_DOWN || event.action == android.view.MotionEvent.ACTION_MOVE) {
                        onMapInteraction()
                    }
                    false
                }
                onMapReady(this)
            }
        },
        update = { mapView ->
            if (extractedMapFile != null && isFirstLocation) {
                try {
                    val mapDataStore = org.mapsforge.map.reader.MapFile(extractedMapFile)
                    val tileCache = org.mapsforge.map.android.util.AndroidUtil.createTileCache(
                        mapView.context, "mapcache",
                        mapView.model.displayModel.tileSize, 1f,
                        mapView.model.frameBufferModel.overdrawFactor
                    )

                    val tileRendererLayer = org.mapsforge.map.layer.renderer.TileRendererLayer(
                        tileCache,
                        mapDataStore,
                        mapView.model.mapViewPosition,
                        AndroidGraphicFactory.INSTANCE
                    )

                    tileRendererLayer.setXmlRenderTheme(org.mapsforge.map.rendertheme.InternalRenderTheme.OSMARENDER)
                    mapView.layerManager.layers.add(tileRendererLayer)

                    // Center on India
                    mapView.model.mapViewPosition.center = LatLong(20.5937, 78.9629)
                    mapView.model.mapViewPosition.zoomLevel = 6.toByte()

                    isFirstLocation = false
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (currentLocation != null && mapManager != null) {
                val latLong = LatLong(currentLocation.latitude, currentLocation.longitude)
                mapManager.updateUserLocation(latLong)

                if (!isFirstLocation && isCameraLockedToGps) {
                    when (mapMode) {
                        MapMode.MY_RIDE -> {
                            mapManager.setCenter(latLong)
                            mapManager.setZoomLevel(16.toByte())
                        }
                        MapMode.MY_GROUP -> {
                            val activeRiders = friends // friends are already filtered correctly by the activeFriends flow (they are active group riders)
                            val boundingBox = calculateBoundingBox(currentLocation, activeRiders)

                            val latDiff = boundingBox.maxLatitude - boundingBox.minLatitude
                            val lonDiff = boundingBox.maxLongitude - boundingBox.minLongitude
                            val maxDiff = maxOf(latDiff, lonDiff)

                            var zoom: Byte = 16
                            if (maxDiff > 0) {
                                // Simple heuristic for zoom level based on bounding box size
                                val scale = 360.0 / maxDiff
                                zoom = (Math.log(scale) / Math.log(2.0)).toInt().toByte()
                            }

                            // Clamp zoom to reasonable values
                            if (zoom < 5) zoom = 5
                            if (zoom > 20) zoom = 20

                            val newPosition = org.mapsforge.core.model.MapPosition(boundingBox.centerPoint, zoom)
                            mapManager.setMapPosition(newPosition)
                        }
                    }
                }
            }
        }
    )
}

suspend fun extractMapAsset(context: Context, fileName: String): java.io.File = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    val destFile = java.io.File(context.filesDir, fileName)
    if (!destFile.exists()) {
        context.assets.open(fileName).use { input ->
            java.io.FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
    }
    destFile
}

fun findActivity(context: android.content.Context): android.app.Activity? {
    var currentContext = context
    while (currentContext is ContextWrapper) {
        if (currentContext is android.app.Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
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

    val locationTracker = remember { LocationTracker(context) }
    val userLocation by locationTracker.location.collectAsState()

    var isCameraLockedToGps by remember { mutableStateOf(true) }


    // Announce route start
    LaunchedEffect(routingState.isRoutingActive) {
        if (routingState.isRoutingActive) {
            viewModel.speakTurnInstruction("Navigation started, proceed to the route")
        }
    }

    // Announce new turns
    LaunchedEffect(routingState.currentInstructionIndex) {
        val instruction = routingState.currentInstruction
        if (instruction != null) {
            viewModel.speakTurnInstruction(instruction.message)
        }
    }

    // Announce 500m warning
    LaunchedEffect(routingState.announced500mInstructionIndex) {
        if (routingState.announced500mInstructionIndex == routingState.currentInstructionIndex) {
            val instruction = routingState.currentInstruction
            if (instruction != null) {
                viewModel.speakTurnInstruction("In 500 meters, ${instruction.message}")
            }
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

    var allPermissionsGranted by remember { mutableStateOf(false) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        allPermissionsGranted = permissionsMap.values.all { it }
    }

    DisposableEffect(allPermissionsGranted) {
        if (allPermissionsGranted) {
            compassManager.start()
            locationTracker.fastFetch()
            locationTracker.start()
        }
        onDispose {
            compassManager.stop()
            locationTracker.stop()
        }
    }

    // Map Mode State
    var mapMode by remember { mutableStateOf(MapMode.MY_RIDE) }

    // UI State for selections
    var selectedDuration by remember { mutableStateOf(4) } // Hours
    var selectedFrequency by remember { mutableStateOf(10) } // Minutes (0 = Live)
    var isSharing by remember { mutableStateOf(false) }
    var showProDialog = remember { mutableStateOf(false) }

    var showGroupSetup by remember { mutableStateOf(false) }

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
        permissionsLauncher.launch(permissions.toTypedArray())
    }

    LaunchedEffect(userLocation) {
        userLocation?.let {
            viewModel.routingStateManager.updateLocation(it)
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

    val recordedPath by viewModel.rideRecorder.recordedPath.collectAsState()

    LaunchedEffect(recordedPath) {
        mapManager?.drawBreadcrumbs(recordedPath)
    }

    LaunchedEffect(routingState.waypoints) {
        mapManager?.drawWaypoints(routingState.waypoints)
        if (routingState.waypoints.size >= 2) {
            viewModel.calculateRoute(routingState.waypoints)
        }
    }


    LaunchedEffect(routingState.routePath) {
        if (routingState.routePath.isNotEmpty()) {
            mapManager?.drawRoute(routingState.routePath, isDashed = routingState.isOffRoadFallback)
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

    val currentGroupId by viewModel.currentGroupId.collectAsState()

    LaunchedEffect(mapMode) {
        if (mapMode == MapMode.MY_GROUP && currentGroupId == null) {
            showGroupSetup = true
        }
    }

    if (showGroupSetup) {
        GroupSetupScreen(
            onGroupSelected = { code ->
                viewModel.joinGroup(code)
                showGroupSetup = false
            }
        )
        return
    }

    var searchResultLocation by remember { mutableStateOf<LatLong?>(null) }
    var searchResultName by remember { mutableStateOf<String?>(null) }
    var autoStartNavigation by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Text(
                    text = "Ride Buddy Controls",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
                )

                Column(modifier = Modifier.padding(horizontal = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (mapMode == MapMode.MY_GROUP) {
                        if (!allPermissionsGranted) {
                            Text(
                                "Permissions required to share location.",
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(
                                onClick = { permissionsLauncher.launch(permissions.toTypedArray()) },
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
                    } else if (mapMode == MapMode.MY_RIDE) {
                        Button(
                            onClick = { gpxPickerLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Import GPX Route")
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
                                    viewModel.toggleRecording(context, false, locationTracker.location)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Stop Recording")
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.toggleRecording(context, true, locationTracker.location)
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
    ) {
    Box(modifier = Modifier.fillMaxSize()) {
        var isMapLoading by remember { mutableStateOf(true) }
        var extractedMapFile by remember { mutableStateOf<java.io.File?>(null) }

        LaunchedEffect(Unit) {
            val file = extractMapAsset(context, "India-southern-zone.map")
            extractedMapFile = file
            isMapLoading = false
        }

        val preRideResult by viewModel.preRideRouteResult.collectAsState()
        val currentProfile by viewModel.currentVehicleProfile.collectAsState()

        val mapExtractionReady by offlineStorageManager?.mapExtractionReady?.collectAsState(initial = false) ?: remember { mutableStateOf(false) }

        LaunchedEffect(mapExtractionReady, mapManager) {
            if (mapExtractionReady && mapManager != null && offlineStorageManager != null) {
                val mapFile = offlineStorageManager.getOfflineFile("germany.map")
                // mapManager?.loadMapFile(mapFile) // Temporarily override with custom logic below
            }
        }

        if (isMapLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        MapsforgeMap(
            modifier = Modifier.fillMaxSize(),
            currentLocation = userLocation,
            mapManager = mapManager,
            mapMode = mapMode,
            friends = friends,
            extractedMapFile = extractedMapFile,
            isCameraLockedToGps = isCameraLockedToGps,
            onMapInteraction = { isCameraLockedToGps = false },
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

        if (!isCameraLockedToGps) {
            val bottomPadding = if (routingState.isRoutingActive || DEBUG_ASO_MODE) {
                120.dp
            } else if (preRideResult != null) {
                300.dp // Offset above PreRideSetupBottomSheet
            } else {
                16.dp
            }

            FloatingActionButton(
                onClick = {
                    isCameraLockedToGps = true
                    userLocation?.let { loc ->
                        mapManager?.setCenter(LatLong(loc.latitude, loc.longitude))
                        mapManager?.setZoomLevel(16.toByte())
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = bottomPadding)
            ) {
                Icon(Icons.Filled.Place, contentDescription = "Recenter Map")
            }
        }

        // Top Container for Tabs & Search
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Map Mode TabRow
            Surface(
                modifier = Modifier.fillMaxWidth(0.6f),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shadowElevation = 4.dp
            ) {
                TabRow(
                    selectedTabIndex = mapMode.ordinal,
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    divider = {}
                ) {
                    Tab(
                        selected = mapMode == MapMode.MY_RIDE,
                        onClick = { mapMode = MapMode.MY_RIDE },
                        modifier = Modifier.weight(1f),
                        text = { Text("My Ride", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                    Tab(
                        selected = mapMode == MapMode.MY_GROUP,
                        onClick = { mapMode = MapMode.MY_GROUP },
                        modifier = Modifier.weight(1f),
                        text = { Text("My Group", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }

            if (!routingState.isRoutingActive) {
                var searchQuery by remember { mutableStateOf("") }
                var isSearching by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 8.dp
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search Destination") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        singleLine = true,
                        trailingIcon = {
                            if (isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(onClick = {
                                    if (searchQuery.isNotBlank() && userLocation != null) {
                                        isSearching = true
                                        coroutineScope.launch {
                                            val destLatLong = com.example.ridebuddy.util.LocationUtil.geocode(context, searchQuery)
                                            if (destLatLong != null) {
                                                searchResultLocation = destLatLong
                                                searchResultName = searchQuery

                                                // Center map on searched location
                                                isCameraLockedToGps = false
                                                mapManager?.setCenter(destLatLong)
                                                mapManager?.setZoomLevel(15.toByte())
                                            }
                                            isSearching = false
                                        }
                                    }
                                }) {
                                    Icon(Icons.Filled.Place, contentDescription = "Search")
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                }
            }
        }

        LaunchedEffect(preRideResult) {
            if (preRideResult != null) {
                isCameraLockedToGps = false
                mapManager?.drawRoute(preRideResult!!.path, isDashed = preRideResult!!.isOffRoadFallback)

                // Adjust view to fit the route
                if (preRideResult!!.path.size >= 2) {
                    var minLat = preRideResult!!.path[0].latitude
                    var maxLat = preRideResult!!.path[0].latitude
                    var minLon = preRideResult!!.path[0].longitude
                    var maxLon = preRideResult!!.path[0].longitude

                    for (point in preRideResult!!.path) {
                        if (point.latitude < minLat) minLat = point.latitude
                        if (point.latitude > maxLat) maxLat = point.latitude
                        if (point.longitude < minLon) minLon = point.longitude
                        if (point.longitude > maxLon) maxLon = point.longitude
                    }

                    val margin = maxOf((maxLat - minLat) * 0.1, (maxLon - minLon) * 0.1, 0.001)
                    val boundingBox = org.mapsforge.core.model.BoundingBox(
                        minLat - margin, minLon - margin, maxLat + margin, maxLon + margin
                    )

                    val maxDiff = maxOf(maxLat - minLat, maxLon - minLon)
                    var zoom: Byte = 16
                    if (maxDiff > 0) {
                        val scale = 360.0 / maxDiff
                        zoom = (Math.log(scale) / Math.log(2.0)).toInt().toByte()
                    }
                    if (zoom < 5) zoom = 5
                    if (zoom > 20) zoom = 20

                    val newPosition = org.mapsforge.core.model.MapPosition(boundingBox.centerPoint, zoom)
                    mapManager?.setMapPosition(newPosition)
                }
            } else {
                mapManager?.clearRouteAndWaypoints()
            }
        }

        if (preRideResult != null) {
            PreRideSetupBottomSheet(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                distanceMeters = preRideResult!!.totalDistance,
                etaSeconds = preRideResult!!.totalSeconds,
                selectedVehicle = currentProfile,
                onVehicleSelected = { profile ->
                    viewModel.setVehicleProfile(profile)
                    // Recalculate route when vehicle changes
                    if (userLocation != null && preRideResult!!.path.isNotEmpty()) {
                        val originLatLong = LatLong(userLocation!!.latitude, userLocation!!.longitude)
                        val destLatLong = preRideResult!!.path.last()
                        viewModel.calculatePreRideRouteStraightLine(originLatLong, destLatLong)
                    }
                },
                onStartRide = {
                    isCameraLockedToGps = true
                    viewModel.startPreRideNavigation()
                },
                onDismiss = {
                    isCameraLockedToGps = true
                    viewModel.clearPreRideRoute()
                }
            )
        }

        LaunchedEffect(preRideResult, autoStartNavigation) {
            if (preRideResult != null && autoStartNavigation) {
                isCameraLockedToGps = true
                viewModel.startPreRideNavigation()
                autoStartNavigation = false
            }
        }

        if (searchResultLocation != null && preRideResult == null && !routingState.isRoutingActive) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = searchResultName ?: "Selected Location", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "${String.format("%.4f", searchResultLocation!!.latitude)}, ${String.format("%.4f", searchResultLocation!!.longitude)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Button(onClick = {
                            if (userLocation != null) {
                                val originLatLong = LatLong(userLocation!!.latitude, userLocation!!.longitude)
                                viewModel.calculatePreRideRouteStraightLine(originLatLong, searchResultLocation!!)
                            }
                            searchResultLocation = null
                        }) {
                            Text("Route")
                        }
                        Button(onClick = {
                            if (userLocation != null) {
                                autoStartNavigation = true
                                val originLatLong = LatLong(userLocation!!.latitude, userLocation!!.longitude)
                                viewModel.calculatePreRideRouteStraightLine(originLatLong, searchResultLocation!!)
                            }
                            searchResultLocation = null
                        }) {
                            Text("Start")
                        }
                        Button(onClick = {
                            android.widget.Toast.makeText(context, "Location saved as favourite", android.widget.Toast.LENGTH_SHORT).show()
                            searchResultLocation = null
                        }) {
                            Text("Save")
                        }
                    }
                }
            }
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
        if (mapMode == MapMode.MY_RIDE && ((routingState.isRoutingActive && routingState.currentInstruction != null) || DEBUG_ASO_MODE)) {
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
