import re

with open('app/src/main/java/com/example/ridebuddy/ui/MapScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('import org.mapsforge.core.model.Dimension\n', '')

with open('app/src/main/java/com/example/ridebuddy/ui/MapScreen.kt', 'w') as f:
    f.write(content)
