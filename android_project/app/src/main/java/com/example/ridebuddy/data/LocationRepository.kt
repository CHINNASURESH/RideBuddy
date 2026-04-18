package com.example.ridebuddy.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
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
        groupId: String,
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
        firestore.collection("groups").document(groupId).collection("riders").document(userId).set(userMap, SetOptions.merge()).await()
    }

    suspend fun updateUserStatus(groupId: String, userId: String, status: String?) {
        val updateData = mutableMapOf<String, Any?>("status" to status)
        firestore.collection("groups").document(groupId).collection("riders").document(userId)
            .set(updateData, SetOptions.merge())
            .await()
    }

    suspend fun updateSharingStatus(groupId: String, userId: String, isSharing: Boolean) {
         firestore.collection("groups").document(groupId).collection("riders").document(userId)
            .update("isSharing", isSharing)
            .await()
    }

    // Fetch friends who are sharing and not expired
    fun getActiveGroupRiders(groupId: String): Flow<List<User>> {
        return firestore.collection("groups").document(groupId).collection("riders")
            .whereEqualTo("isSharing", true)
            .whereGreaterThan("sharingExpiry", System.currentTimeMillis())
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(User::class.java)
            }
            .flowOn(Dispatchers.Default)
    }
}
