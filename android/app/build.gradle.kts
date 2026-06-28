plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "fr.magiclockscreen.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.magiclockscreen.android"
        minSdk = 26
        targetSdk = 33
        versionCode = 2
        versionName = "0.2.0"
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
