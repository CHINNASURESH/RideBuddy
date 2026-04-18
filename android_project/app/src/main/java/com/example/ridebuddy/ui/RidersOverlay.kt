package com.example.ridebuddy.ui

import org.mapsforge.core.graphics.Canvas
import org.mapsforge.core.graphics.Paint
import org.mapsforge.core.graphics.Style
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Point
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.layer.Layer
import java.util.concurrent.ConcurrentHashMap

data class Rider(val id: String, var position: LatLong, val color: Int, val heading: Float? = null, val status: String? = null)

class RidersOverlay : Layer() {

    private val riders = ConcurrentHashMap<String, Rider>()

    // Use Mapsforge paint for rendering
    private val paint: Paint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
        setStyle(Style.FILL)
    }

    private val strokePaint: Paint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
        setStyle(Style.STROKE)
        setStrokeWidth(2f)
        setColor(android.graphics.Color.WHITE)
    }

    private val headingPaint: Paint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
        setStyle(Style.STROKE)
        setStrokeWidth(4f)
        setColor(android.graphics.Color.BLACK)
    }

    private val textPaint: Paint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
        setStyle(Style.FILL)
        setColor(android.graphics.Color.RED)
        setTextSize(30f)
    }

    private val textBackgroundPaint: Paint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
        setStyle(Style.FILL)
        setColor(android.graphics.Color.WHITE)
    }

    fun updateRider(rider: Rider) {
        riders[rider.id] = rider
        requestRedraw()
    }

    fun removeRider(id: String) {
        riders.remove(id)
        requestRedraw()
    }

    fun setRiders(newRiders: List<Rider>) {
        riders.clear()
        for (rider in newRiders) {
            riders[rider.id] = rider
        }
        requestRedraw()
    }

    override fun draw(boundingBox: BoundingBox, zoomLevel: Byte, canvas: Canvas, topLeftPoint: Point) {
        val tileSize = 256
        val mapSize = org.mapsforge.core.util.MercatorProjection.getMapSize(zoomLevel, tileSize)

        for (rider in riders.values) {
            val riderPos = rider.position
            // Check if rider is within current view bounding box
            if (boundingBox.contains(riderPos)) {
                // Convert LatLong to map pixel coordinates relative to the screen
                val pixelX = org.mapsforge.core.util.MercatorProjection.longitudeToPixelX(riderPos.longitude, mapSize) - topLeftPoint.x
                val pixelY = org.mapsforge.core.util.MercatorProjection.latitudeToPixelY(riderPos.latitude, mapSize) - topLeftPoint.y

                // Set paint color per rider
                paint.setColor(rider.color)

                // Draw a circle for the rider marker (radius 10 pixels for example)
                val radius = 10
                canvas.drawCircle(pixelX.toInt(), pixelY.toInt(), radius, paint)
                canvas.drawCircle(pixelX.toInt(), pixelY.toInt(), radius, strokePaint)

                // Draw a heading indicator (a line pointing in the direction of the heading)
                val headingToUse = rider.heading

                if (headingToUse != null) {
                    val angleRad = Math.toRadians((headingToUse - 90f).toDouble()) // Subtract 90 to make 0 point "up"
                    val lineLength = 20
                    val endX = pixelX + (Math.cos(angleRad) * lineLength).toInt()
                    val endY = pixelY + (Math.sin(angleRad) * lineLength).toInt()

                    canvas.drawLine(pixelX.toInt(), pixelY.toInt(), endX.toInt(), endY.toInt(), headingPaint)
                }

                if (rider.status != null) {
                    val statusText = rider.status
                    // simple drawing of text above the marker
                    val textWidth = textPaint.getTextWidth(statusText)
                    val textHeight = textPaint.getTextHeight(statusText)
                    val textX = pixelX - textWidth / 2
                    val textY = pixelY - radius - 10
                    // Draw a background rectangle for better visibility
                    canvas.drawText(statusText, textX.toInt(), textY.toInt(), textPaint)
                }
            }
        }
    }
}
