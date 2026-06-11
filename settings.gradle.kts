pluginManagement {
    includeBuild("build-logic/debug-network-interceptor-plugin") {
        name = "debug-network-interceptor-plugin-build"
    }

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "debug-toolkit"

include(":debug-tool:network-interceptor")
include(":debug-tool:debug-toolkit")
include(":debug-network-interceptor-plugin")
include(":sample-app")

project(":debug-tool:network-interceptor").projectDir = file("libs/debug-tool/network-interceptor")
project(":debug-tool:debug-toolkit").projectDir = file("libs/debug-tool/debug-toolkit")
project(":debug-network-interceptor-plugin").projectDir = file("build-logic/debug-network-interceptor-plugin")
