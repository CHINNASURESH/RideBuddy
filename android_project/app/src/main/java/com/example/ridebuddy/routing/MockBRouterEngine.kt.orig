package com.example.ridebuddy.routing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.mapsforge.core.model.LatLong

class MockBRouterEngine : OfflineRoutingEngine {
    override suspend fun calculateRoute(start: LatLong, destination: LatLong): List<LatLong> {
        return withContext(Dispatchers.IO) {
            // Simulate offline calculation delay
            delay(1000)

            // Generate a simple fake route consisting of 5 points between start and destination
            val route = mutableListOf<LatLong>()
            route.add(start)

            val steps = 4
            val latDiff = (destination.latitude - start.latitude) / steps
            val lonDiff = (destination.longitude - start.longitude) / steps

            for (i in 1 until steps) {
                route.add(LatLong(start.latitude + (latDiff * i), start.longitude + (lonDiff * i)))
            }

            route.add(destination)
            route
        }
    }
}
