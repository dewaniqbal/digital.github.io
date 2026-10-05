pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "quran-audio"

// Milestone 1: JVM-only domain modules (buildable without the Android SDK).
// Android modules are listed in docs/ARCHITECTURE.md and are added feature by feature.
include(":core:model")
include(":core:common")
