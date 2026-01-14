package com.example.ridebuddy.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // Update current user's location and status
    suspend fun updateUserLocation(
        userId: String,
        lat: Double,
        lng: Double,
        isSharing: Boolean,
        expiry: Long
    ) {
        val userMap = mapOf(
            "userId" to userId,
            "latitude" to lat,
            "longitude" to lng,
            "lastUpdated" to com.google.firebase.Timestamp.now(),
            "isSharing" to isSharing,
            "sharingExpiry" to expiry
        )
        // Using merge to avoid overwriting other fields if we add them later
        firestore.collection("users").document(userId).set(userMap).await()
    }

    suspend fun updateSharingStatus(userId: String, isSharing: Boolean) {
         firestore.collection("users").document(userId)
            .update("isSharing", isSharing)
            .await()
    }

    // Fetch friends who are sharing and not expired
    fun getActiveFriends(): Flow<List<User>> {
        return firestore.collection("users")
            .whereEqualTo("isSharing", true)
            .snapshots()
            .map { snapshot ->
                val currentTime = System.currentTimeMillis()
                snapshot.toObjects(User::class.java).filter {
                    it.sharingExpiry > currentTime
                }
            }
    }
}
