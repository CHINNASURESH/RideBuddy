import sys

filename = "android_project/app/src/main/java/com/example/ridebuddy/ui/MainViewModel.kt"
with open(filename, "r") as f:
    content = f.read()

# Add imports
content = content.replace(
    "import kotlinx.coroutines.flow.StateFlow",
    "import kotlinx.coroutines.flow.StateFlow\nimport kotlinx.coroutines.flow.MutableSharedFlow\nimport kotlinx.coroutines.flow.asSharedFlow"
)

# Add properties
content = content.replace(
    "val currentUserId = \"current_user_id_123\"",
    """val currentUserId = "current_user_id_123"

    private val _mapControlEvents = MutableSharedFlow<MapControlEvent>(extraBufferCapacity = 10)
    val mapControlEvents = _mapControlEvents.asSharedFlow()

    sealed class MapControlEvent {
        data class Pan(val dx: Double, val dy: Double) : MapControlEvent()
        data class Zoom(val delta: Int) : MapControlEvent()
    }"""
)

# Add functions
content = content.replace(
    "    fun speakTurnInstruction(text: String) {\n        ttsHelper?.speak(text)\n    }",
    """    fun speakTurnInstruction(text: String) {
        ttsHelper?.speak(text)
    }

    fun panMap(dx: Double, dy: Double) {
        _mapControlEvents.tryEmit(MapControlEvent.Pan(dx, dy))
    }

    fun zoomMap(delta: Int) {
        _mapControlEvents.tryEmit(MapControlEvent.Zoom(delta))
    }

    fun skipWaypoint() {
        routingStateManager.skipWaypoint()
        val remainingWaypoints = routingStateManager.routingState.value.waypoints
        if (remainingWaypoints.size >= 2) {
            calculateRoute(remainingWaypoints)
        }
    }"""
)

with open(filename, "w") as f:
    f.write(content)
