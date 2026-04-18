package com.example.ridebuddy.routing

import org.mapsforge.core.model.LatLong

data class RoutingResult(
    val path: List<LatLong>,
    val instructions: List<TurnInstruction> = emptyList(),
    val totalSeconds: Int = 0
)
