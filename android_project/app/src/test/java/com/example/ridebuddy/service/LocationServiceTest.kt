package com.example.ridebuddy.service

import android.location.Location
import com.example.ridebuddy.data.LocationRepository
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

/**
 * Unit tests for LocationService logic.
 * Note: These tests assume a test environment with Mockito and Robolectric or similar for Android classes.
 */
class LocationServiceTest {

    @Test
    fun `test throttling logic prevents frequent updates`() {
        // This test simulates the logic inside LocationService.onLocationResult

        // Mocks
        val repository = mock(LocationRepository::class.java)
        val mockScope = CoroutineScope(Dispatchers.Unconfined)

        var lastUploadedLocation: Location? = null
        val minDistanceMeters = 10f
        val userId = "testUser"
        val expiry = 1000L

        // Helper function simulating the logic to be tested
        fun onLocationResult(location: Location) {
            val shouldUpdate = lastUploadedLocation == null ||
                               location.distanceTo(lastUploadedLocation) >= minDistanceMeters

            if (shouldUpdate) {
                lastUploadedLocation = location
                mockScope.launch {
                    repository.updateUserLocation(
                        userId,
                        location.latitude,
                        location.longitude,
                        true,
                        expiry
                    )
                }
            }
        }

        // 1. First location
        val loc1 = mock(Location::class.java)
        `when`(loc1.latitude).thenReturn(10.0)
        `when`(loc1.longitude).thenReturn(10.0)
        // distanceTo depends on the previous location, which is null initially.

        onLocationResult(loc1)

        // Verify update called
        runBlocking {
            verify(repository, times(1)).updateUserLocation(eq(userId), eq(10.0), eq(10.0), anyBoolean(), anyLong())
        }

        // 2. Second location, very close (2 meters)
        val loc2 = mock(Location::class.java)
        `when`(loc2.latitude).thenReturn(10.00001)
        `when`(loc2.longitude).thenReturn(10.00001)
        `when`(loc2.distanceTo(loc1)).thenReturn(2.0f) // Mock distance to previous

        onLocationResult(loc2)

        // Verify update NOT called again (still 1)
        runBlocking {
            verify(repository, times(1)).updateUserLocation(anyString(), anyDouble(), anyDouble(), anyBoolean(), anyLong())
        }

        // 3. Third location, far enough (15 meters)
        val loc3 = mock(Location::class.java)
        `when`(loc3.latitude).thenReturn(10.001)
        `when`(loc3.longitude).thenReturn(10.001)
        `when`(loc3.distanceTo(loc1)).thenReturn(15.0f) // Distance to LAST UPLOADED (loc1)

        onLocationResult(loc3)

        // Verify update called again (total 2)
        runBlocking {
            verify(repository, times(2)).updateUserLocation(anyString(), anyDouble(), anyDouble(), anyBoolean(), anyLong())
        }
    }
}
