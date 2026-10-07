pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // RootEncoder (RTMP ingest for the optional YouTube Live rebroadcast) and the WebRTC
        // Android SDK are published to JitPack, not to Google's or Maven Central's indexes.
        maven("https://jitpack.io") {
            content {
                includeGroupByRegex("com\\.github\\..*")
                includeGroup("io.github.webrtc-sdk")
            }
        }
    }
}

rootProject.name = "NexPlayAndroid"
include(":app")
