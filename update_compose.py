import sys

filepath = 'android_project/app/build.gradle.kts'
with open(filepath, 'r') as f:
    content = f.read()

search = """    buildFeatures {
        compose = true
    }"""
replace = """    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8" // for Kotlin 1.9.22
    }"""

if search in content:
    content = content.replace(search, replace)
    with open(filepath, 'w') as f:
        f.write(content)
    print("Success")
else:
    print("Search block not found")
