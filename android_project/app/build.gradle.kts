import java.util.Properties
import java.util.Date
import java.text.SimpleDateFormat

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
    id("kotlin-kapt")
    alias(libs.plugins.ksp)
    alias(libs.plugins.firebase.crashlytics)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

val buildTime = Date()
val computedVersionCode = SimpleDateFormat("yyMMddHH").format(buildTime).toInt()
val computedVersionName = "1.0.${SimpleDateFormat("yyMMddHH").format(buildTime)}"

android {

    namespace = "com.example.ridebuddy"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.ridebuddy"
        minSdk = 24
        targetSdk = 35
        versionCode = computedVersionCode
        versionName = computedVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("MAPS_API_KEY", "")
    }

    signingConfigs {
        create("release") {
            storeFile = file(localProperties.getProperty("STORE_FILE", System.getenv("STORE_FILE") ?: "keystore.jks"))
            storePassword = localProperties.getProperty("STORE_PASSWORD", System.getenv("STORE_PASSWORD") ?: "")
            keyAlias = localProperties.getProperty("KEY_ALIAS", System.getenv("KEY_ALIAS") ?: "")
            keyPassword = localProperties.getProperty("KEY_PASSWORD", System.getenv("KEY_PASSWORD") ?: "")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8" // for Kotlin 1.9.22
    }
}

dependencies {
    implementation(project(":brouter-core"))
    implementation(project(":brouter-codec"))
    implementation(project(":brouter-util"))
    implementation(project(":brouter-mapaccess"))
    implementation(project(":brouter-expressions"))
    implementation(project(":brouter-core"))
    implementation(project(":brouter-codec"))
    implementation(project(":brouter-util"))
    implementation(project(":brouter-mapaccess"))
    implementation(project(":brouter-expressions"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Google Maps
    implementation(libs.google.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    implementation(libs.play.billing)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.functions)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)
    implementation(libs.hilt.navigation.compose)

    // Accompanist Permissions
    implementation(libs.accompanist.permissions)

    // Coroutines
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.play.services)

    // Mapsforge
    implementation(libs.mapsforge.core)
    implementation(libs.mapsforge.map)
    implementation(libs.mapsforge.map.android)
    implementation(libs.mapsforge.map.reader)
    implementation(libs.mapsforge.themes)

    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
