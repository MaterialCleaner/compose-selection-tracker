plugins {
    val androidGradlePluginVersion = "8.4.1"
    id("com.android.application") version androidGradlePluginVersion apply false
    id("com.android.library") version androidGradlePluginVersion apply false
    kotlin("android") version "1.9.23" apply false
    id("com.vanniktech.maven.publish") version "0.27.0" apply false
}
