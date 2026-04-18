import sys

filename = "android_project/app/src/main/java/com/example/ridebuddy/routing/RoutingStateManager.kt"
with open(filename, "r") as f:
    content = f.read()

# Fix skipWaypoint to remove the second element (the first actual waypoint/destination after origin)
content = content.replace(
    """    fun skipWaypoint() {
        _routingState.update { state ->
            if (state.waypoints.size > 1) {
                state.copy(waypoints = state.waypoints.drop(1))
            } else {
                state
            }
        }
    }""",
    """    fun skipWaypoint() {
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
    }"""
)

with open(filename, "w") as f:
    f.write(content)
