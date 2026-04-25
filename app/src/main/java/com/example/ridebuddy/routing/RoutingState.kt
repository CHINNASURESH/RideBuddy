package com.example.ridebuddy.routing

import org.mapsforge.core.model.LatLong

data class RoutingState(
    val waypoints: List<LatLong> = emptyList(),
    val routePath: List<LatLong> = emptyList(),
    val turnInstructions: List<TurnInstruction> = emptyList(),
    val currentInstructionIndex: Int = 0,
    val distanceToNextInstruction: Double = 0.0,
    val distanceToDestination: Double = 0.0,
    val isRoutingActive: Boolean = false,
    val expectedArrivalTime: Long? = null,
    val destinationSunsetTime: Long? = null,
    val totalDistance: Int = 0,
    val totalSeconds: Int = 0,
    val announced500mInstructionIndex: Int = -1
) {
    val currentInstruction: TurnInstruction?
        get() = turnInstructions.getOrNull(currentInstructionIndex)

    val isNightRidingAnticipated: Boolean
        get() = if (expectedArrivalTime != null && destinationSunsetTime != null) {
            expectedArrivalTime > destinationSunsetTime
        } else {
            false
        }
}
