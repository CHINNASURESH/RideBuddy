import org.mapsforge.map.android.view.MapView

fun test(mapView: MapView) {
    mapView.model.mapViewPosition.zoomLevel = (mapView.model.mapViewPosition.zoomLevel + 1).toByte()
}
