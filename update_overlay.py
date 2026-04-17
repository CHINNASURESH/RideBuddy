import sys

filepath = 'android_project/app/src/main/java/com/example/ridebuddy/ui/RidersOverlay.kt'
with open(filepath, 'r') as f:
    content = f.read()

search = """
    fun removeRider(id: String) {
        riders.remove(id)
        requestRedraw()
    }
"""
replace = """
    fun removeRider(id: String) {
        riders.remove(id)
        requestRedraw()
    }

    fun setRiders(newRiders: List<Rider>) {
        riders.clear()
        for (rider in newRiders) {
            riders[rider.id] = rider
        }
        requestRedraw()
    }
"""

if search in content:
    content = content.replace(search, replace)
    with open(filepath, 'w') as f:
        f.write(content)
    print("Success")
else:
    print("Search block not found")
