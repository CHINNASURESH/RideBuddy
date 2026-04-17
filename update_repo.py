import sys

filepath = 'android_project/app/src/main/java/com/example/ridebuddy/data/LocationRepository.kt'
with open(filepath, 'r') as f:
    content = f.read()

search = """
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
            .whereGreaterThan("sharingExpiry", System.currentTimeMillis())
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(User::class.java)
            }
            .flowOn(Dispatchers.Default)
    }
"""

replace = """
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
        firestore.collection("groups").document(groupId).collection("riders").document(userId).set(userMap).await()
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
"""

if search in content:
    content = content.replace(search, replace)
    with open(filepath, 'w') as f:
        f.write(content)
    print("Success")
else:
    print("Search block not found")
