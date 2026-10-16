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

// Milestone 2: app shell + JVM domain modules. Further Android modules are added feature by feature.
include(":app")
include(":core:model")
include(":core:common")
