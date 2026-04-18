import sys

filename = "android_project/app/src/main/java/com/example/ridebuddy/routing/RoutingStateManager.kt"
with open(filename, "r") as f:
    content = f.read()

content = content.replace(
    "    fun clearWaypoints() {\n        _routingState.update { it.copy(waypoints = emptyList(), routePath = emptyList(), turnInstructions = emptyList(), isRoutingActive = false) }\n    }",
    """    fun clearWaypoints() {
        _routingState.update { it.copy(waypoints = emptyList(), routePath = emptyList(), turnInstructions = emptyList(), isRoutingActive = false) }
    }

    fun skipWaypoint() {
        _routingState.update { state ->
            if (state.waypoints.size > 1) {
                state.copy(waypoints = state.waypoints.drop(1))
            } else {
                state
            }
        }
    }"""
)

with open(filename, "w") as f:
    f.write(content)
