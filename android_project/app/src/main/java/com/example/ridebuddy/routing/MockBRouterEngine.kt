package com.example.ridebuddy.routing

import com.google.android.gms.maps.model.LatLng

class MockBRouterEngine : OfflineRoutingEngine {
    override fun calculateRoute(start: LatLng, destination: LatLng): List<LatLng> {
        // Return a mock route
        return listOf(start, destination)
    }
}
