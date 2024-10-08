plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("android")
}

android {
    buildFeatures { compose = true }
    buildToolsVersion = "34.0.0"
    signingConfigs {
        create("release") {
            storeFile = File(System.getenv("STORE_FILE") ?: "/dev/null")
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    compileSdk = 34
    defaultConfig {
        applicationId = "me.gm.selection.sample"
        minSdk = 21
        targetSdk = 34
        versionCode = (extra["VERSION_CODE"] as String).toInt()
        versionName = extra["VERSION_NAME"] as String
    }
    kotlinOptions { jvmTarget = JavaVersion.VERSION_1_8.toString() }
    namespace = "me.gm.selection.sample"
}

dependencies {
    implementation(project(":library"))

    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(platform("androidx.compose:compose-bom:2024.09.01"))
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.runtime:runtime-livedata")
}
