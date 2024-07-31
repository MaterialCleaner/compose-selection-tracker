plugins {
    val androidGradlePluginVersion = "8.5.1"
    id("com.android.application") version androidGradlePluginVersion apply false
    id("com.android.library") version androidGradlePluginVersion apply false
    kotlin("android") version "2.0.0" apply false
    id("com.vanniktech.maven.publish") version "0.29.0" apply false
}
