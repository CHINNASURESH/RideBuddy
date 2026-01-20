package com.example.ridebuddy.ui

import com.google.android.gms.maps.model.LatLng

data class UserUiModel(
    val userId: String,
    val position: LatLng,
    val lastSeenText: String
)
