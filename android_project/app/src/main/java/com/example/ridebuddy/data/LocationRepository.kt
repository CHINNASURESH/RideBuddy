package com.example.ridebuddy.data

import com.example.ridebuddy.sms.SmsPayload
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // Current user local state for SMS payload
    val localUserStatus = MutableStateFlow<String?>(null)
    val localUserHeading = MutableStateFlow(0f)

    val isProActive = MutableStateFlow(false)

    // Local state to track updates received via SMS when offline
    private val offlineRiders = MutableStateFlow<Map<String, User>>(emptyMap())

    suspend fun updateOfflineRiderFromSms(payload: SmsPayload) {
        val user = User(
            userId = payload.userId,
            latitude = payload.lat,
            longitude = payload.lon,
            heading = payload.heading,
            lastUpdated = com.google.firebase.Timestamp.now(),
            isSharing = true,
            sharingExpiry = System.currentTimeMillis() + (10 * 60 * 1000), // assume valid for 10 min
            status = payload.status
        )
        val currentMap = offlineRiders.value.toMutableMap()
        currentMap[user.userId] = user
        offlineRiders.value = currentMap
    }

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
            "heading" to localUserHeading.value,
            "lastUpdated" to com.google.firebase.Timestamp.now(),
            "isSharing" to isSharing,
            "sharingExpiry" to expiry
        )
        // Using merge to avoid overwriting other fields if we add them later
        firestore.collection("groups").document(groupId).collection("riders").document(userId).set(userMap, SetOptions.merge()).await()
    }

    suspend fun updateUserStatus(groupId: String, userId: String, status: String?) {
        localUserStatus.value = status
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

    fun listenToUserEntitlements(userId: String) {
        firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    isProActive.value = snapshot.getBoolean("isProActive") ?: false
                }
            }
    }

    // Update current user's pro status globally
    suspend fun updateUserProStatus(userId: String, isProActive: Boolean) {
         firestore.collection("users").document(userId)
            .set(mapOf("isProActive" to isProActive), SetOptions.merge())
            .await()
    }

    // Fetch friends who are sharing and not expired
    fun getActiveGroupRiders(groupId: String): Flow<List<User>> {
        val onlineRidersFlow = firestore.collection("groups").document(groupId).collection("riders")
            .whereEqualTo("isSharing", true)
            .whereGreaterThan("sharingExpiry", System.currentTimeMillis())
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(User::class.java)
            }

        return combine(onlineRidersFlow, offlineRiders) { online, offlineMap ->
            val result = online.associateBy { it.userId }.toMutableMap()

            // Clean up expired offline riders and merge with online ones
            val currentTime = System.currentTimeMillis()
            val validOfflineMap = offlineMap.filterValues { it.sharingExpiry > currentTime }

            // Update the map for the next run
            if (validOfflineMap.size != offlineMap.size) {
                 offlineRiders.value = validOfflineMap
            }

            validOfflineMap.forEach { (id, offlineUser) ->
                val onlineUser = result[id]
                // Only override if the offline user data is newer than the online user data
                if (onlineUser == null ||
                    (offlineUser.lastUpdated != null && onlineUser.lastUpdated != null &&
                     offlineUser.lastUpdated.seconds > onlineUser.lastUpdated.seconds)) {
                    result[id] = offlineUser
                }
            }
            result.values.toList()
        }.flowOn(Dispatchers.Default)
    }
}
