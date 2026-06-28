plugins {
    id("com.android.application")
}

android {
    namespace = "fr.magiclockscreen.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.magiclockscreen.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "1.0.0-standalone"
    }
}
