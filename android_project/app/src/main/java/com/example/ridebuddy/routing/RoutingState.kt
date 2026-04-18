package com.example.ridebuddy.routing

import org.mapsforge.core.model.LatLong

data class RoutingState(
    val waypoints: List<LatLong> = emptyList(),
    val routePath: List<LatLong> = emptyList(),
    val turnInstructions: List<TurnInstruction> = emptyList(),
    val currentInstructionIndex: Int = 0,
    val distanceToNextInstruction: Double = 0.0,
    val isRoutingActive: Boolean = false
) {
    val currentInstruction: TurnInstruction?
        get() = turnInstructions.getOrNull(currentInstructionIndex)
}
