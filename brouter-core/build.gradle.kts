plugins {
    id("java-library")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(project(":brouter-codec"))
    implementation(project(":brouter-util"))
    implementation(project(":brouter-mapaccess"))
    implementation(project(":brouter-expressions"))
}
