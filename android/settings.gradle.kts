pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "memorymap"

include(":app")
include(":core:model")
include(":core:common")
include(":core:designsystem")
include(":core:ui")
include(":core:network")
include(":domain")
include(":data:photo")
include(":data:space")
include(":data:region")
include(":feature:space")
include(":feature:map")
include(":feature:calendar")
include(":feature:upload")
