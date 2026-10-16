// Pure-Kotlin module shared by the Android app and the backend proxy.
// Holds the Surah catalogue, the normalised reciter-catalogue contract, source mappers,
// and platform-independent playback logic (queue, sleep timer, volume mixing).
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    `java-library`
}

group = "app.quranaudio"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17 }
}

dependencies {
    api(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
