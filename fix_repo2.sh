git mv android_project/* .
git mv android_project/.* . 2>/dev/null || true
rmdir android_project
gradle wrapper --gradle-version 8.2
git add -u
git add .
git commit -m "chore: Fix directory structure and Gradle wrapper setup"
