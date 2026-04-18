import org.mapsforge.map.android.view.MapView
import org.mapsforge.core.model.LatLong
fun test(mapView: MapView) {
    val pos = mapView.model.mapViewPosition
    val center = pos.center
    pos.center = LatLong(center.latitude + 0.01, center.longitude)
}
