package com.example.ridebuddy.ui

import org.mapsforge.core.model.LatLong

data class UserUiModel(
    val userId: String,
    val position: LatLong,
    val lastSeenText: String
)
