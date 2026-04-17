import sys

filepath = 'android_project/app/src/test/java/com/example/ridebuddy/service/LocationServiceTest.kt'
with open(filepath, 'r') as f:
    content = f.read()

search = """                    repository.updateUserLocation(
                        userId,
                        location.latitude,
                        location.longitude,
                        true,
                        expiry
                    )"""
replace = """                    repository.updateUserLocation(
                        "default_group",
                        userId,
                        location.latitude,
                        location.longitude,
                        true,
                        expiry
                    )"""

search2 = "verify(repository, times(1)).updateUserLocation(org.mockito.kotlin.eq(userId), org.mockito.kotlin.eq(10.0), org.mockito.kotlin.eq(10.0), org.mockito.kotlin.any(), org.mockito.kotlin.any())"
replace2 = "verify(repository, times(1)).updateUserLocation(org.mockito.kotlin.eq(\"default_group\"), org.mockito.kotlin.eq(userId), org.mockito.kotlin.eq(10.0), org.mockito.kotlin.eq(10.0), org.mockito.kotlin.any(), org.mockito.kotlin.any())"

search3 = "verify(repository, times(1)).updateUserLocation(org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any())"
replace3 = "verify(repository, times(1)).updateUserLocation(org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any())"

search4 = "verify(repository, times(2)).updateUserLocation(org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any())"
replace4 = "verify(repository, times(2)).updateUserLocation(org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any())"

if search in content and search2 in content and search3 in content and search4 in content:
    content = content.replace(search, replace)
    content = content.replace(search2, replace2)
    content = content.replace(search3, replace3)
    content = content.replace(search4, replace4)
    with open(filepath, 'w') as f:
        f.write(content)
    print("Success")
else:
    print("Search block not found")
