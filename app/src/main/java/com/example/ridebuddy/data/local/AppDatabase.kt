package com.example.ridebuddy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RideSession::class, RidePoint::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rideDao(): RideDao
}
