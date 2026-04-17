import sys

filepath = 'android_project/app/src/main/java/com/example/ridebuddy/service/LocationService.kt'
with open(filepath, 'r') as f:
    content = f.read()

search1 = """
    private var userId: String? = null
    private var sharingExpiry: Long = 0L
"""
replace1 = """
    private var userId: String? = null
    private var groupId: String? = null
    private var sharingExpiry: Long = 0L
"""

search2 = """
        const val EXTRA_INTERVAL = "EXTRA_INTERVAL"
        const val EXTRA_USER_ID = "EXTRA_USER_ID"
        const val EXTRA_EXPIRY = "EXTRA_EXPIRY"
"""
replace2 = """
        const val EXTRA_INTERVAL = "EXTRA_INTERVAL"
        const val EXTRA_USER_ID = "EXTRA_USER_ID"
        const val EXTRA_GROUP_ID = "EXTRA_GROUP_ID"
        const val EXTRA_EXPIRY = "EXTRA_EXPIRY"
"""

search3 = """
            ACTION_START -> {
                userId = intent.getStringExtra(EXTRA_USER_ID)
                sharingExpiry = intent.getLongExtra(EXTRA_EXPIRY, 0L)
"""
replace3 = """
            ACTION_START -> {
                userId = intent.getStringExtra(EXTRA_USER_ID)
                groupId = intent.getStringExtra(EXTRA_GROUP_ID)
                sharingExpiry = intent.getLongExtra(EXTRA_EXPIRY, 0L)
"""

search4 = """
                        lastUploadedLocation = location
                        userId?.let { uid ->
                            serviceScope.launch {
                                repository.updateUserLocation(
                                    uid,
                                    location.latitude,
                                    location.longitude,
                                    true,
                                    sharingExpiry
                                )
                            }
                        }
"""
replace4 = """
                        lastUploadedLocation = location
                        userId?.let { uid ->
                            groupId?.let { gid ->
                                serviceScope.launch {
                                    repository.updateUserLocation(
                                        gid,
                                        uid,
                                        location.latitude,
                                        location.longitude,
                                        true,
                                        sharingExpiry
                                    )
                                }
                            }
                        }
"""

search5 = """
    private fun stopService() {
        userId?.let { uid ->
            serviceScope.launch {
                repository.updateSharingStatus(uid, false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }
"""
replace5 = """
    private fun stopService() {
        userId?.let { uid ->
            groupId?.let { gid ->
                serviceScope.launch {
                    repository.updateSharingStatus(gid, uid, false)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            } ?: run {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        } ?: run {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }
"""

if search1 in content and search2 in content and search3 in content and search4 in content and search5 in content:
    content = content.replace(search1, replace1)
    content = content.replace(search2, replace2)
    content = content.replace(search3, replace3)
    content = content.replace(search4, replace4)
    content = content.replace(search5, replace5)
    with open(filepath, 'w') as f:
        f.write(content)
    print("Success")
else:
    if search1 not in content: print("s1 fail")
    if search2 not in content: print("s2 fail")
    if search3 not in content: print("s3 fail")
    if search4 not in content: print("s4 fail")
    if search5 not in content: print("s5 fail")
