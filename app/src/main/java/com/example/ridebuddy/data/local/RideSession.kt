package com.example.ridebuddy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ride_sessions")
data class RideSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val totalDistanceMeters: Double = 0.0
)
