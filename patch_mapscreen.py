import sys

filename = "android_project/app/src/main/java/com/example/ridebuddy/ui/MapScreen.kt"
with open(filename, "r") as f:
    content = f.read()

content = content.replace(
    "    var showStatusMenu by remember { mutableStateOf(false) }",
    """    LaunchedEffect(Unit) {
        viewModel.mapControlEvents.collect { event ->
            when (event) {
                is MainViewModel.MapControlEvent.Pan -> mapManager?.pan(event.dx, event.dy)
                is MainViewModel.MapControlEvent.Zoom -> {
                    if (event.delta > 0) mapManager?.zoomIn() else mapManager?.zoomOut()
                }
            }
        }
    }

    var showStatusMenu by remember { mutableStateOf(false) }"""
)

with open(filename, "w") as f:
    f.write(content)
