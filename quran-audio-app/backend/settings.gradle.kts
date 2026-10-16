rootProject.name = "quran-audio-backend"

pluginManagement {
    repositories { gradlePluginPortal(); mavenCentral() }
}
dependencyResolutionManagement {
    repositories { mavenCentral() }
    versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } }
}
includeBuild("../shared")
