import java.net.URI

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
        maven( "https://jitpack.io")
    }
}

sourceControl {
    gitRepository(URI.create("https://github.com/material-components/material-components-android.git")) {
        producesModule("com.google.android.material:material-components-android")
    }
}

rootProject.name = "OpenTAK ICU"
include(":app")
 