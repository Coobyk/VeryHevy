plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.veryhevy"
    compileSdk = 37

    // CI sets BUILD_NUMBER (GitHub run number); local builds default to 1.
    val buildNumber = (System.getenv("BUILD_NUMBER") ?: "1").toInt()

    defaultConfig {
        applicationId = "com.veryhevy"
        minSdk = 26
        targetSdk = 36
        versionCode = buildNumber
        versionName = "0.1.0.$buildNumber"
    }

    // A persistent key shared between CI and local builds so APKs install over
    // each other; falls back to the ephemeral debug key when unset.
    val keystoreFile = System.getenv("KEYSTORE_FILE")
    val hasSigningKey = keystoreFile != null && File(keystoreFile).exists()
    if (hasSigningKey) {
        signingConfigs {
            create("shared") {
                storeFile = File(keystoreFile!!)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            if (hasSigningKey) {
                signingConfig = signingConfigs.getByName("shared")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasSigningKey) {
                signingConfigs.getByName("shared")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.junit)
    debugImplementation(libs.compose.ui.tooling)
}
