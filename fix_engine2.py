import sys

filepath = 'android_project/app/src/main/java/com/example/ridebuddy/routing/LocalBRouterEngine.kt'
with open(filepath, 'r') as f:
    content = f.read()

search = "override fun calculateRoute(start: LatLong, destination: LatLong): List<LatLong>"
replace = "override suspend fun calculateRoute(start: LatLong, destination: LatLong): List<LatLong>"

if search in content:
    content = content.replace(search, replace)
    with open(filepath, 'w') as f:
        f.write(content)
    print("Success")
else:
    print("Search block not found")
