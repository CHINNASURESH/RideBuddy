import re

with open("android_project/gradle/libs.versions.toml", "r") as f:
    content = f.read()

content = content.replace('[versions]', '[versions]\nfirebaseCrashlyticsGradle = "2.9.9"\n')
content = content.replace('firebase-auth = { group = "com.google.firebase", name = "firebase-auth-ktx" }', 'firebase-auth = { group = "com.google.firebase", name = "firebase-auth-ktx" }\nfirebase-crashlytics = { group = "com.google.firebase", name = "firebase-crashlytics" }')
content = content.replace('[plugins]', '[plugins]\nfirebase-crashlytics = { id = "com.google.firebase.crashlytics", version.ref = "firebaseCrashlyticsGradle" }')

with open("android_project/gradle/libs.versions.toml", "w") as f:
    f.write(content)
