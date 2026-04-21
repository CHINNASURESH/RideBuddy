package com.example.ridebuddy.routing

import org.mapsforge.core.model.LatLong

data class TurnInstruction(
    val coordinate: LatLong,
    val command: Int,
    val message: String,
    val distanceToNext: Double,
    val indexInTrack: Int
)
