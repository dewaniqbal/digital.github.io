pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
    }
}
rootProject.name = "sakina"
include(":core")
// The Android module needs the Android SDK and Google Maven. Pass -PincludeApp=false to build and
// test only the pure-JVM :core module (this is what was verified in the authoring sandbox).
if ((providers.gradleProperty("includeApp").orNull ?: "true") != "false") {
    include(":app")
}
