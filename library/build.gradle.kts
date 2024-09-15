plugins {
    id("com.android.library")
    id("com.vanniktech.maven.publish")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("android")
}

android {
    buildFeatures { compose = true }
    buildToolsVersion = "34.0.0"
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    compileSdk = 34
    defaultConfig {
        consumerProguardFiles("proguard-rules.pro")
        minSdk = 21
    }
    kotlinOptions { jvmTarget = JavaVersion.VERSION_1_8.toString() }
    namespace = "me.gm.selection"
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.01"))
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.5")
}
