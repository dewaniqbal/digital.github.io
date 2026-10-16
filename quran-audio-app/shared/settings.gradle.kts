rootProject.name = "shared"

dependencyResolutionManagement {
    repositories { mavenCentral() }
    versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } }
}
pluginManagement {
    repositories { gradlePluginPortal(); mavenCentral() }
}
