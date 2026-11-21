plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ir.h3xnull.nerkh"
    compileSdk = 35

    defaultConfig {
        applicationId = "ir.h3xnull.nerkh"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // A fixed key, so each new APK installs over the old one (no uninstall needed).
    signingConfigs {
        getByName("debug") {
            storeFile = file("nerkh.keystore")
            storePassword = "android"
            keyAlias = "nerkh"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation("androidx.work:work-runtime:2.9.1")
}
