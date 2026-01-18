package com.example.ridebuddy.data

import com.google.firebase.Timestamp

data class User(
    val userId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val lastUpdated: Timestamp? = null,
    val isSharing: Boolean = false,
    val sharingExpiry: Long = 0L // Timestamp in millis
)
