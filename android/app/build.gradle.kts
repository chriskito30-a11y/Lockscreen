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
        versionCode = 11
        versionName = "3.0.0"
    }

    signingConfigs {
        create("modulys") {
            storeFile = file("../modulys-magic-lock.keystore")
            storePassword = "modulysmagiclock"
            keyAlias = "modulysmagiclock"
            keyPassword = "modulysmagiclock"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("modulys")
        }
        release {
            signingConfig = signingConfigs.getByName("modulys")
            isMinifyEnabled = false
        }
    }
}
