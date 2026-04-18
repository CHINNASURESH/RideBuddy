import org.mapsforge.map.android.view.MapView
import org.mapsforge.core.model.LatLong

fun test(mapView: MapView) {
    val pos = mapView.model.mapViewPosition
    val currentCenter = pos.center
    // To pan left, we decrease longitude. To pan right, we increase.
    pos.center = LatLong(currentCenter.latitude, currentCenter.longitude - 0.01)

    pos.zoomLevel = (pos.zoomLevel + 1).toByte()
}
