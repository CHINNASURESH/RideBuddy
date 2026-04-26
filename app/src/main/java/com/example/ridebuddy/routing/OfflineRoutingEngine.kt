package com.example.ridebuddy.routing

import org.mapsforge.core.model.LatLong

interface OfflineRoutingEngine {
    suspend fun calculateRoute(waypoints: List<LatLong>, profile: String = "motorcycle"): RoutingResult
}
