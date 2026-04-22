#!/bin/bash
echo "Looking for room dependencies..."
grep -r -i "room" app/build.gradle.kts || echo "No room in app/build.gradle.kts"
