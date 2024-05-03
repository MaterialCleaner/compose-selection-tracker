plugins {
    val androidGradlePluginVersion = "8.4.0"
    id("com.android.application") version androidGradlePluginVersion apply false
    id("com.android.library") version androidGradlePluginVersion apply false
    kotlin("android") version "1.9.22" apply false
    id("com.vanniktech.maven.publish") version "0.27.0" apply false
}
