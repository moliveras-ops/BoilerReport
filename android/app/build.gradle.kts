plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "cl.iqs.boilerreport"
    compileSdk = 34

    defaultConfig {
        applicationId = "cl.iqs.boilerreport"
        minSdk = 29          // Android 10 o superior
        targetSdk = 34
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "1." + (System.getenv("GITHUB_RUN_NUMBER") ?: "0")
    }

    // Clave fija: cada APK nuevo se instala encima del anterior sin perder los informes
    signingConfigs {
        create("fija") {
            storeFile = rootProject.file("keystore/boilerreport.jks")
            storePassword = "boilerreport"
            keyAlias = "boilerreport"
            keyPassword = "boilerreport"
        }
    }

    buildTypes {
        debug { signingConfig = signingConfigs.getByName("fija") }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fija")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.webkit:webkit:1.11.0")
}
