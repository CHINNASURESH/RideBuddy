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
    interface OnMapLongPressListener {
        fun onLongPress(latLong: LatLong)
    }
    var onMapLongPressListener: OnMapLongPressListener? = null


    private var tileRendererLayer: TileRendererLayer? = null
    private var routePolyline: Polyline? = null
    private var importedRoutePolyline: Polyline? = null

    private var currentNightMode: Boolean = false

    private val longPressLayer = object : org.mapsforge.map.layer.Layer() {
        override fun draw(boundingBox: org.mapsforge.core.model.BoundingBox?, zoomLevel: Byte, canvas: org.mapsforge.core.graphics.Canvas?, topLeftPoint: org.mapsforge.core.model.Point?) {}
        override fun onLongPress(tapLatLong: LatLong?, layerXY: org.mapsforge.core.model.Point?, tapXY: org.mapsforge.core.model.Point?): Boolean {
            tapLatLong?.let {
                onMapLongPressListener?.onLongPress(it)
                return true
            }
            return false
        }
    }


    init {
        // Initialize Mapsforge graphic factory if not already initialized
        AndroidGraphicFactory.createInstance(context.applicationContext)

        // Setup MapView basic properties
        mapView.isClickable = true
        mapView.mapScaleBar.isVisible = true
        mapView.setBuiltInZoomControls(true)
        mapView.layerManager.layers.add(longPressLayer)
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

    fun setNightMode(isNightMode: Boolean) {
        if (this.currentNightMode == isNightMode) return
        this.currentNightMode = isNightMode

        if (isNightMode) {
            // Try applying a different theme from InternalRenderTheme.
            try {
                tileRendererLayer?.setXmlRenderTheme(InternalRenderTheme.DEFAULT)
            } catch (e: Exception) {
                // Ignore if not available
            }
        } else {
            tileRendererLayer?.setXmlRenderTheme(InternalRenderTheme.OSMARENDER)
        }
        mapView.layerManager.redrawLayers()
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

    private val waypointMarkers = mutableListOf<org.mapsforge.map.layer.overlay.Marker>()

    fun clearRouteAndWaypoints() {
        routePolyline?.let {
            mapView.layerManager.layers.remove(it)
            routePolyline = null
        }
        for (marker in waypointMarkers) {
            mapView.layerManager.layers.remove(marker)
        }
        waypointMarkers.clear()
        mapView.layerManager.redrawLayers()
    }

    fun drawWaypoints(waypoints: List<LatLong>) {
        for (marker in waypointMarkers) {
            mapView.layerManager.layers.remove(marker)
        }
        waypointMarkers.clear()

        for ((index, waypoint) in waypoints.withIndex()) {
            // But Mapforge Circle doesn't have text. So let's use a standard Marker and we provide a Bitmap.
            // To create a generic bitmap, we can draw on android.graphics.Canvas and convert.

            val androidBitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(androidBitmap)
            val p = android.graphics.Paint()
            p.color = android.graphics.Color.RED
            canvas.drawCircle(50f, 50f, 40f, p)
            p.color = android.graphics.Color.WHITE
            p.textSize = 40f
            p.textAlign = android.graphics.Paint.Align.CENTER
            canvas.drawText("${index + 1}", 50f, 65f, p)

            val drawable = android.graphics.drawable.BitmapDrawable(context.resources, androidBitmap)
            val marker = org.mapsforge.map.layer.overlay.Marker(waypoint, org.mapsforge.map.android.graphics.AndroidGraphicFactory.convertToBitmap(drawable), 0, 0)
            mapView.layerManager.layers.add(marker)
            waypointMarkers.add(marker)
        }
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

    fun setCenter(latLong: LatLong) {
        mapView.model.mapViewPosition.center = latLong
    }

    fun setZoomLevel(zoom: Byte) {
        mapView.model.mapViewPosition.zoomLevel = zoom
    }
}
