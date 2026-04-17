import sys

filepath = 'android_project/app/src/main/java/com/example/ridebuddy/routing/LocalBRouterEngine.kt'
with open(filepath, 'r') as f:
    content = f.read()

search = "com.google.android.gms.maps.model.LatLng"
replace = "org.mapsforge.core.model.LatLong"

if search in content:
    content = content.replace(search, replace)
    content = content.replace("LatLng", "LatLong")
    with open(filepath, 'w') as f:
        f.write(content)
    print("Success")
else:
    print("Search block not found")
