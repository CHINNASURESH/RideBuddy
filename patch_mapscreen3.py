import sys

filename = "android_project/app/src/main/java/com/example/ridebuddy/ui/MapScreen.kt"
with open(filename, "r") as f:
    content = f.read()

# Fix Unresolved reference issues
content = content.replace("import androidx.lifecycle.compose.collectAsStateWithLifecycle", "")
content = content.replace("import androidx.lifecycle.compose.LocalLifecycleOwner", "")

content = content.replace(
    "import androidx.lifecycle.repeatOnLifecycle",
    "import androidx.lifecycle.repeatOnLifecycle\nimport androidx.lifecycle.compose.LocalLifecycleOwner"
)

# Replace LaunchedEffect
content = content.replace(
    """    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel.mapControlEvents, lifecycleOwner.lifecycle) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
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
    }"""
)

with open(filename, "w") as f:
    f.write(content)
