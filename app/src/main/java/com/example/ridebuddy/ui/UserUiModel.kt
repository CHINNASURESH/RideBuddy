package com.example.ridebuddy.ui

import org.mapsforge.core.model.LatLong

data class UserUiModel(
    val userId: String,
    val position: LatLong,
    val heading: Float? = null,
    val lastSeenText: String,
    val status: String? = null
)
