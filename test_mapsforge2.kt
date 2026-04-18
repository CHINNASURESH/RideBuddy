import org.mapsforge.map.android.view.MapView
import org.mapsforge.core.model.LatLong
fun test(mapView: MapView) {
    val pos = mapView.model.mapViewPosition
    pos.zoomIn()
    pos.zoomOut()
    pos.moveCenter(10f, 10f)
}
