package com.example.ridebuddy.routing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.mapsforge.core.model.LatLong

class MockBRouterEngine : OfflineRoutingEngine {
    override suspend fun calculateRoute(waypoints: List<LatLong>): RoutingResult {
        if (waypoints.size < 2) return RoutingResult(emptyList(), emptyList())
        return withContext(Dispatchers.IO) {
            // Simulate offline calculation delay
            delay(1000)

            // Generate a simple fake route consisting of 5 points between start and destination
            val route = mutableListOf<LatLong>()
            for (i in 0 until waypoints.size - 1) {
                val start = waypoints[i]
                val destination = waypoints[i+1]
                if (i == 0) route.add(start)

                val steps = 4
                val latDiff = (destination.latitude - start.latitude) / steps
                val lonDiff = (destination.longitude - start.longitude) / steps

                for (j in 1 until steps) {
                    route.add(LatLong(start.latitude + (latDiff * j), start.longitude + (lonDiff * j)))
                }
                route.add(destination)
            }
            RoutingResult(route, emptyList(), totalSeconds = 600, totalDistance = 5000)
        }
    }
}
