rootProject.name = "within-means"

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

// Kernel compartido
include(":shared")
project(":shared").projectDir = file("src/shared")

// Bounded contexts del MVP
listOf("users", "categories", "concepts", "transactions", "analytics").forEach { ctx ->
    include(":$ctx")
    project(":$ctx").projectDir = file("src/$ctx")
}

// Apps
include(":apps:android")
project(":apps:android").projectDir = file("apps/android")
