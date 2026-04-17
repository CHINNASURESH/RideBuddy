import sys

filepath = 'android_project/app/src/main/java/com/example/ridebuddy/ui/MainViewModel.kt'
with open(filepath, 'r') as f:
    content = f.read()

search = """                val intent = Intent(application, LocationService::class.java).apply {
                    action = LocationService.ACTION_START
                    putExtra(LocationService.EXTRA_USER_ID, currentUserId)
                    putExtra(LocationService.EXTRA_GROUP_ID, "default_group")
                    putExtra(LocationService.EXTRA_EXPIRY, expiry)
                    putExtra(LocationService.EXTRA_INTERVAL, intervalMillis)
                }
                application.startForegroundService(intent)"""

replace = """                val intent = Intent(application, LocationService::class.java).apply {
                    action = LocationService.ACTION_START
                    putExtra(LocationService.EXTRA_USER_ID, currentUserId)
                    putExtra(LocationService.EXTRA_GROUP_ID, "default_group")
                    putExtra(LocationService.EXTRA_EXPIRY, expiry)
                    putExtra(LocationService.EXTRA_INTERVAL, intervalMillis)
                }
                androidx.core.content.ContextCompat.startForegroundService(application, intent)"""

if search in content:
    content = content.replace(search, replace)
    with open(filepath, 'w') as f:
        f.write(content)
    print("Success")
else:
    print("Search block not found")
