package com.example.ridebuddy.routing

import android.location.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.mapsforge.core.model.LatLong
import com.example.ridebuddy.util.SolarMath
import com.example.ridebuddy.data.LocationRepository

class RoutingStateManager(
    private val repository: LocationRepository
) {
    private val _routingState = MutableStateFlow(RoutingState())
    val routingState: StateFlow<RoutingState> = _routingState.asStateFlow()

    fun addWaypoint(waypoint: LatLong) {
        _routingState.update { it.copy(waypoints = it.waypoints + waypoint) }
    }

    fun clearWaypoints() {
        _routingState.update { it.copy(waypoints = emptyList(), routePath = emptyList(), turnInstructions = emptyList(), isRoutingActive = false) }
    }

    fun skipWaypoint() {
        _routingState.update { state ->
            if (state.waypoints.size > 2) {
                val newWaypoints = state.waypoints.toMutableList()
                newWaypoints.removeAt(1) // Remove the next destination, keep origin
                state.copy(waypoints = newWaypoints)
            } else if (state.waypoints.size == 2) {
                // If there's only origin and one destination, and we skip it, we clear the route
                state.copy(waypoints = listOf(state.waypoints[0]), routePath = emptyList(), turnInstructions = emptyList(), isRoutingActive = false)
            } else {
                state
            }
        }
    }

    fun startRouting(result: RoutingResult) {
        val expectedArrival = System.currentTimeMillis() + (result.totalSeconds * 1000L)
        val sunsetTime = if (result.path.isNotEmpty() && repository.isProActive.value) {
            val destination = result.path.last()
            SolarMath.calculateSunset(destination.latitude, destination.longitude)
        } else {
            null
        }

        _routingState.value = _routingState.value.copy(
            routePath = result.path,
            turnInstructions = result.instructions,
            currentInstructionIndex = 0,
            distanceToNextInstruction = if (result.instructions.isNotEmpty()) {
                // Initialize with some default, will be updated on location change
                0.0
            } else {
                0.0
            },
            distanceToDestination = 0.0,
            isRoutingActive = true,
            expectedArrivalTime = expectedArrival,
            destinationSunsetTime = sunsetTime
        )
    }

    fun stopRouting() {
        _routingState.value = RoutingState()
    }

    fun updateLocation(location: Location) {
        val state = _routingState.value
        if (!state.isRoutingActive || state.turnInstructions.isEmpty()) return

        val currentInstructionIndex = state.currentInstructionIndex
        if (currentInstructionIndex >= state.turnInstructions.size) return

        val nextInstruction = state.turnInstructions[currentInstructionIndex]
        val distanceToNext = calculateDistance(
            location.latitude, location.longitude,
            nextInstruction.coordinate.latitude, nextInstruction.coordinate.longitude
        )

        val distanceToDest = if (state.routePath.isNotEmpty()) {
            val dest = state.routePath.last()
            calculateDistance(location.latitude, location.longitude, dest.latitude, dest.longitude)
        } else {
            0.0
        }

        // Check if we passed the instruction (e.g., distance < 20 meters)
        if (distanceToNext < 20.0) {
            val newIndex = currentInstructionIndex + 1
            if (newIndex < state.turnInstructions.size) {
                val newNextInstruction = state.turnInstructions[newIndex]
                val newDistance = calculateDistance(
                    location.latitude, location.longitude,
                    newNextInstruction.coordinate.latitude, newNextInstruction.coordinate.longitude
                )
                _routingState.update {
                    it.copy(
                        currentInstructionIndex = newIndex,
                        distanceToNextInstruction = newDistance,
                        distanceToDestination = distanceToDest
                    )
                }
            } else {
                // Reached the destination/end of instructions
                _routingState.update {
                    it.copy(
                        currentInstructionIndex = newIndex,
                        distanceToNextInstruction = 0.0,
                        distanceToDestination = 0.0,
                        isRoutingActive = false
                    )
                }
            }
        } else {
            // Just update the distance
            _routingState.update {
                it.copy(
                    distanceToNextInstruction = distanceToNext,
                    distanceToDestination = distanceToDest
                )
            }
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }
}
