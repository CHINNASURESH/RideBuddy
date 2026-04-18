import sys

filename = "android_project/app/src/main/java/com/example/ridebuddy/MainActivity.kt"
with open(filename, "r") as f:
    content = f.read()

content = content.replace(
    "import com.example.ridebuddy.ui.MapScreen",
    "import com.example.ridebuddy.ui.MapScreen\nimport com.example.ridebuddy.ui.MainViewModel\nimport androidx.activity.viewModels\nimport android.view.KeyEvent"
)

content = content.replace(
    "    lateinit var offlineStorageManager: OfflineStorageManager",
    "    lateinit var offlineStorageManager: OfflineStorageManager\n\n    private val viewModel: MainViewModel by viewModels()"
)

content = content.replace(
    "            MapScreen(offlineStorageManager = offlineStorageManager)",
    "            MapScreen(viewModel = viewModel, offlineStorageManager = offlineStorageManager)"
)

content = content.replace(
    "    private fun checkBatteryOptimizations() {",
    """    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    viewModel.panMap(0.0, 0.001)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    viewModel.panMap(0.0, -0.001)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    viewModel.panMap(-0.001, 0.0)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    viewModel.panMap(0.001, 0.0)
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    viewModel.zoomMap(1)
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    viewModel.zoomMap(-1)
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    viewModel.skipWaypoint()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun checkBatteryOptimizations() {"""
)

with open(filename, "w") as f:
    f.write(content)
