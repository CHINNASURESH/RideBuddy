import sys

filename = "android_project/app/src/main/java/com/example/ridebuddy/ui/MapScreen.kt"
with open(filename, "r") as f:
    content = f.read()

content = content.replace("import androidx.lifecycle.compose.LocalLifecycleOwner\n", "")

content = content.replace(
    """    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(viewModel.mapControlEvents, lifecycleOwner.lifecycle) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            viewModel.mapControlEvents.collect { event ->
                when (event) {
                    is MainViewModel.MapControlEvent.Pan -> mapManager?.pan(event.dx, event.dy)
                    is MainViewModel.MapControlEvent.Zoom -> {
                        if (event.delta > 0) mapManager?.zoomIn() else mapManager?.zoomOut()
                    }
                }
            }
        }
    }""",
    """    LaunchedEffect(viewModel.mapControlEvents) {
        viewModel.mapControlEvents.collect { event ->
            when (event) {
                is MainViewModel.MapControlEvent.Pan -> mapManager?.pan(event.dx, event.dy)
                is MainViewModel.MapControlEvent.Zoom -> {
                    if (event.delta > 0) mapManager?.zoomIn() else mapManager?.zoomOut()
                }
            }
        }
    }"""
)


with open(filename, "w") as f:
    f.write(content)
