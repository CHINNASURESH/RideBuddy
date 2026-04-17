package com.example.ridebuddy.routing

import com.google.android.gms.maps.model.LatLng

interface OfflineRoutingEngine {
    fun calculateRoute(start: LatLng, destination: LatLng): List<LatLng>
}
