package com.example.ridebuddy.routing

import android.content.Context
import org.mapsforge.core.model.LatLong
import btools.router.RoutingContext
import btools.router.RoutingEngine
import btools.router.OsmNodeNamed
import btools.mapaccess.OsmNode
import java.io.File
import java.util.ArrayList
import kotlinx.coroutines.withContext

class LocalBRouterEngine(private val context: Context, private val brouterDir: File) : OfflineRoutingEngine {

    override suspend fun calculateRoute(waypoints: List<LatLong>, profile: String): RoutingResult {
        if (waypoints.size < 2) return RoutingResult(emptyList(), emptyList())
        val rc = RoutingContext()
        rc.turnInstructionMode = 3 // osmand style

        val segmentsDir = File(brouterDir, "segments4")
        if (!segmentsDir.exists()) {
            segmentsDir.mkdirs()
        }

        // Map general vehicle profiles to brouter profiles
        val brouterProfileName = when (profile.lowercase()) {
            "car" -> "car-eco.brf"
            "bus" -> "car-eco.brf" // Fallback to car
            "bike", "motorcycle" -> "motorcycle.brf"
            else -> "motorcycle.brf"
        }

        val profileFile = File(brouterDir, brouterProfileName)
        rc.localFunction = profileFile.absolutePath

        // Enable shortest path calculation explicitly as asked by requirements
        try {
            rc.keyValues = mapOf("shortest" to "1")
        } catch (e: Exception) {
            // Ignore if keyValues is missing
        }

        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val osmWaypoints = ArrayList<OsmNodeNamed>()
            for ((index, waypoint) in waypoints.withIndex()) {
                val lon = ((waypoint.longitude + 180.0) * 1000000.0).toInt()
                val lat = ((waypoint.latitude + 90.0) * 1000000.0).toInt()
                val node = OsmNodeNamed(OsmNode(lon, lat))
                node.name = "waypoint_$index"
                osmWaypoints.add(node)
            }

            val alternativePaths = mutableListOf<List<LatLong>>()
            var mainResult: RoutingResult? = null

            for (altIdx in 0..1) {
                rc.alternativeIdx = altIdx
                val engine = try {
                    RoutingEngine(null, null, segmentsDir, osmWaypoints, rc)
                } catch (e: Exception) {
                    if (altIdx == 0) return@withContext RoutingResult(emptyList(), emptyList())
                    continue
                }

                try {
                    engine.doRun(10000)
                } catch (e: Exception) {
                    if (altIdx == 0) return@withContext RoutingResult(emptyList(), emptyList())
                    continue
                }

                if (engine.errorMessage != null) {
                    if (altIdx == 0) return@withContext RoutingResult(emptyList(), emptyList())
                    continue
                }

                val track = engine.foundTrack
                if (track != null && track.nodes != null) {
                    val result = mutableListOf<LatLong>()
                    for (node in track.nodes) {
                        val lat = (node.getILat() / 1000000.0) - 90.0
                        val lng = (node.getILon() / 1000000.0) - 180.0
                        result.add(LatLong(lat, lng))
                    }

                    if (altIdx == 0) {
                        val instructions = mutableListOf<TurnInstruction>()
                        if (track.voiceHints != null && track.voiceHints.list != null) {
                            for (hint in track.voiceHints.list) {
                                val idx = hint.indexInTrack
                                if (idx >= 0 && idx < result.size) {
                                    instructions.add(TurnInstruction(
                                        coordinate = result[idx],
                                        command = hint.cmd,
                                        message = hint.getMessageString(rc.turnInstructionMode),
                                        distanceToNext = hint.distanceToNext,
                                        indexInTrack = hint.indexInTrack
                                    ))
                                }
                            }
                        }
                        mainResult = RoutingResult(result, instructions, track.totalSeconds, track.distance)
                    } else {
                        alternativePaths.add(result)
                    }
                }
            }
            return@withContext mainResult?.copy(alternativePaths = alternativePaths) ?: RoutingResult(emptyList(), emptyList())
        }
    }
}