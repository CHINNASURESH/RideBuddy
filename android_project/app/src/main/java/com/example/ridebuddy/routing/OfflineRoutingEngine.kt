package com.example.ridebuddy.routing

import org.mapsforge.core.model.LatLong

interface OfflineRoutingEngine {
    suspend fun calculateRoute(start: LatLong, destination: LatLong): RoutingResult
}
