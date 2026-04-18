import sys

filename = "android_project/app/src/main/java/com/example/ridebuddy/ui/MapScreen.kt"
with open(filename, "r") as f:
    content = f.read()

# Make sure imports are present
if "import androidx.lifecycle.compose.collectAsStateWithLifecycle" not in content:
    content = content.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\nimport androidx.lifecycle.compose.collectAsStateWithLifecycle\nimport androidx.lifecycle.compose.LocalLifecycleOwner\nimport androidx.lifecycle.Lifecycle")

content = content.replace(
    """    LaunchedEffect(Unit) {
        viewModel.mapControlEvents.collect { event ->
            when (event) {
                is MainViewModel.MapControlEvent.Pan -> mapManager?.pan(event.dx, event.dy)
                is MainViewModel.MapControlEvent.Zoom -> {
                    if (event.delta > 0) mapManager?.zoomIn() else mapManager?.zoomOut()
                }
            }
        }
    }""",
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
    }"""
)

if "import androidx.lifecycle.repeatOnLifecycle" not in content:
    content = content.replace("import androidx.lifecycle.compose.LocalLifecycleOwner", "import androidx.lifecycle.compose.LocalLifecycleOwner\nimport androidx.lifecycle.repeatOnLifecycle")


with open(filename, "w") as f:
    f.write(content)
