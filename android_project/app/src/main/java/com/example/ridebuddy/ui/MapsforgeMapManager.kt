package com.example.ridebuddy.ui

import android.content.Context
import org.mapsforge.core.graphics.Color
import org.mapsforge.core.graphics.Paint
import org.mapsforge.core.graphics.Style
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.overlay.Polyline
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.InternalRenderTheme
import java.io.File

class MapsforgeMapManager(private val context: Context, private val mapView: MapView) {

    private var tileRendererLayer: TileRendererLayer? = null
    private var routePolyline: Polyline? = null
    private var importedRoutePolyline: Polyline? = null

    init {
        // Initialize Mapsforge graphic factory if not already initialized
        AndroidGraphicFactory.createInstance(context.applicationContext)

        // Setup MapView basic properties
        mapView.isClickable = true
        mapView.mapScaleBar.isVisible = true
        mapView.setBuiltInZoomControls(true)
    }

    fun loadMapFile(mapFile: File) {
        if (!mapFile.exists()) return

        val tileCache = AndroidUtil.createTileCache(
            context, "mapcache",
            mapView.model.displayModel.tileSize, 1f,
            mapView.model.frameBufferModel.overdrawFactor
        )

        val mapDataStore = MapFile(mapFile)

        tileRendererLayer = TileRendererLayer(
            tileCache,
            mapDataStore,
            mapView.model.mapViewPosition,
            AndroidGraphicFactory.INSTANCE
        )

        tileRendererLayer?.setXmlRenderTheme(InternalRenderTheme.OSMARENDER)

        mapView.layerManager.layers.add(tileRendererLayer)

        mapView.model.mapViewPosition.center = LatLong(0.0, 0.0)

        mapView.model.mapViewPosition.zoomLevel = 12.toByte()
    }

    fun drawRoute(routePoints: List<LatLong>, color: Int = android.graphics.Color.BLUE, strokeWidth: Float = 10f) {
        // Remove existing route if any
        routePolyline?.let {
            mapView.layerManager.layers.remove(it)
        }

        val paint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
            setColor(color)
            setStrokeWidth(strokeWidth)
            setStyle(Style.STROKE)
        }

        val polyline = Polyline(paint, AndroidGraphicFactory.INSTANCE)
        val latLongs = polyline.latLongs
        for (point in routePoints) {
            latLongs.add(point)
        }

        mapView.layerManager.layers.add(polyline)
        routePolyline = polyline
    }

    fun drawImportedRoute(routePoints: List<LatLong>, color: Int = android.graphics.Color.MAGENTA, strokeWidth: Float = 10f) {
        // Remove existing imported route if any
        importedRoutePolyline?.let {
            mapView.layerManager.layers.remove(it)
        }

        val paint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
            setColor(color)
            setStrokeWidth(strokeWidth)
            setStyle(Style.STROKE)
            setDashPathEffect(floatArrayOf(20f, 20f))
        }

        val polyline = Polyline(paint, AndroidGraphicFactory.INSTANCE)
        val latLongs = polyline.latLongs
        for (point in routePoints) {
            latLongs.add(point)
        }

        mapView.layerManager.layers.add(polyline)
        importedRoutePolyline = polyline

        // Optionally center map on the first point of the route
        if (routePoints.isNotEmpty()) {
            mapView.model.mapViewPosition.center = routePoints.first()
        }
    }

    fun cleanUp() {
        mapView.destroyAll()
        AndroidGraphicFactory.clearResourceMemoryCache()
    }
}
