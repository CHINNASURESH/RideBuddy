import org.mapsforge.map.android.view.MapView
import org.mapsforge.core.model.LatLong

fun test(mapView: MapView) {
    val pos = mapView.model.mapViewPosition
    val center = pos.center
    pos.zoomIn()
    pos.zoomOut()
    pos.moveCenter(10.0, 10.0) // trying double
}
