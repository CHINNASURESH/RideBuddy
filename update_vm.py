import sys

filepath = 'android_project/app/src/main/java/com/example/ridebuddy/ui/MainViewModel.kt'
with open(filepath, 'r') as f:
    content = f.read()

search1 = """import com.google.android.gms.maps.model.LatLng"""
replace1 = """import org.mapsforge.core.model.LatLong"""

search2 = """    val activeFriends: StateFlow<List<UserUiModel>> = repository.getActiveFriends()
        .map { users ->
            users.map { user ->
                UserUiModel(
                    userId = user.userId,
                    position = LatLng(user.latitude, user.longitude),
                    lastSeenText = "Last seen: ${user.lastUpdated?.toDate()}"
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())"""
replace2 = """    val activeFriends: StateFlow<List<UserUiModel>> = repository.getActiveGroupRiders("default_group")
        .map { users ->
            users.map { user ->
                UserUiModel(
                    userId = user.userId,
                    position = LatLong(user.latitude, user.longitude),
                    lastSeenText = "Last seen: ${user.lastUpdated?.toDate()}"
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())"""

search3 = """                val intent = Intent(application, LocationService::class.java).apply {
                    action = LocationService.ACTION_START
                    putExtra(LocationService.EXTRA_USER_ID, currentUserId)
                    putExtra(LocationService.EXTRA_EXPIRY, expiry)
                    putExtra(LocationService.EXTRA_INTERVAL, intervalMillis)
                }"""
replace3 = """                val intent = Intent(application, LocationService::class.java).apply {
                    action = LocationService.ACTION_START
                    putExtra(LocationService.EXTRA_USER_ID, currentUserId)
                    putExtra(LocationService.EXTRA_GROUP_ID, "default_group")
                    putExtra(LocationService.EXTRA_EXPIRY, expiry)
                    putExtra(LocationService.EXTRA_INTERVAL, intervalMillis)
                }"""

if search1 in content and search2 in content and search3 in content:
    content = content.replace(search1, replace1)
    content = content.replace(search2, replace2)
    content = content.replace(search3, replace3)
    with open(filepath, 'w') as f:
        f.write(content)
    print("Success")
else:
    if search1 not in content: print("s1 fail")
    if search2 not in content: print("s2 fail")
    if search3 not in content: print("s3 fail")
