package com.example.ridebuddy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface RideDao {
    @Insert
    suspend fun insertSession(session: RideSession): Long

    @Update
    suspend fun updateSession(session: RideSession)

    @Insert
    suspend fun insertPoint(point: RidePoint)

    @Query("SELECT * FROM ride_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSession(sessionId: Long): RideSession?

    @Query("SELECT * FROM ride_sessions ORDER BY startTime DESC LIMIT 1")
    suspend fun getLastSession(): RideSession?

    @Query("SELECT * FROM ride_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getPointsForSession(sessionId: Long): List<RidePoint>
}
