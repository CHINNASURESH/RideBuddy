import sys

filename = "android_project/app/src/main/java/com/example/ridebuddy/MainActivity.kt"
with open(filename, "r") as f:
    content = f.read()

content = content.replace(
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
    }""",
    """    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handledCodes = listOf(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_MEDIA_NEXT
        )

        if (event.keyCode in handledCodes) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> viewModel.panMap(0.0, 0.001)
                    KeyEvent.KEYCODE_DPAD_DOWN -> viewModel.panMap(0.0, -0.001)
                    KeyEvent.KEYCODE_DPAD_LEFT -> viewModel.panMap(-0.001, 0.0)
                    KeyEvent.KEYCODE_DPAD_RIGHT -> viewModel.panMap(0.001, 0.0)
                    KeyEvent.KEYCODE_VOLUME_UP -> viewModel.zoomMap(1)
                    KeyEvent.KEYCODE_VOLUME_DOWN -> viewModel.zoomMap(-1)
                    KeyEvent.KEYCODE_MEDIA_NEXT -> viewModel.skipWaypoint()
                }
            }
            return true // Consume both ACTION_DOWN and ACTION_UP
        }
        return super.dispatchKeyEvent(event)
    }"""
)

with open(filename, "w") as f:
    f.write(content)
