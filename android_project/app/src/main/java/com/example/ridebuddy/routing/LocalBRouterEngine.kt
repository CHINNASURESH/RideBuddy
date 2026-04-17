package com.example.ridebuddy.routing

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import btools.router.RoutingContext
import btools.router.RoutingEngine
import btools.router.OsmNodeNamed
import btools.mapaccess.OsmNode
import java.io.File
import java.util.ArrayList

class LocalBRouterEngine(private val context: Context, private val brouterDir: File) : OfflineRoutingEngine {

    override fun calculateRoute(start: LatLng, destination: LatLng): List<LatLng> {
        val rc = RoutingContext()

        val segmentsDir = File(brouterDir, "segments4")
        if (!segmentsDir.exists()) {
            segmentsDir.mkdirs()
        }

        val profileFile = File(brouterDir, "motorcycle.brf")
        rc.localFunction = profileFile.absolutePath

        // BRouter expects coordinates in 1E6 format with +180 and +90 shift
        val startLon = ((start.longitude + 180.0) * 1000000.0).toInt()
        val startLat = ((start.latitude + 90.0) * 1000000.0).toInt()
        val startNode = OsmNodeNamed(OsmNode(startLon, startLat))
        startNode.name = "start"

        val destLon = ((destination.longitude + 180.0) * 1000000.0).toInt()
        val destLat = ((destination.latitude + 90.0) * 1000000.0).toInt()
        val destNode = OsmNodeNamed(OsmNode(destLon, destLat))
        destNode.name = "destination"

        val waypoints = ArrayList<OsmNodeNamed>()
        waypoints.add(startNode)
        waypoints.add(destNode)

        val engine = RoutingEngine(null, null, segmentsDir, waypoints, rc)

        engine.doRun(10000)

        if (engine.errorMessage != null) {
            println("BRouter error: " + engine.errorMessage)
            return emptyList()
        }

        val track = engine.foundTrack
        if (track != null && track.nodes != null) {
            val result = mutableListOf<LatLng>()
            for (node in track.nodes) {
                // Revert shift and 1E6 multiplication
                val lat = (node.getILat() / 1000000.0) - 90.0
                val lng = (node.getILon() / 1000000.0) - 180.0
                result.add(LatLng(lat, lng))
            }
            return result
        }

        return emptyList()
    }
}
