import java.io.File

fun main() {
    val file = File("android_project/app/src/main/java/com/example/ridebuddy/ui/MapsforgeMapManager.kt")
    val content = file.readText()
    val newContent = content.replace("}\n}", """
    }

    fun zoomIn() {
        mapView.model.mapViewPosition.zoomLevel = (mapView.model.mapViewPosition.zoomLevel + 1).toByte()
    }

    fun zoomOut() {
        mapView.model.mapViewPosition.zoomLevel = (mapView.model.mapViewPosition.zoomLevel - 1).toByte()
    }

    fun pan(dx: Double, dy: Double) {
        val center = mapView.model.mapViewPosition.center
        mapView.model.mapViewPosition.center = LatLong(center.latitude + dy, center.longitude + dx)
    }
}
""")
    file.writeText(newContent)
}
