import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

rootProject.name = "MyChat"

pluginManagement {
    plugins {
        // used by build.gradle.kts
        id("org.jetbrains.kotlin.jvm") version "2.4.0"  // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.jvm
        id("org.jetbrains.changelog") version "2.5.0"  // https://plugins.gradle.org/plugin/org.jetbrains.changelog
        id("org.jetbrains.intellij.platform") version "2.17.0"  // https://plugins.gradle.org/plugin/org.jetbrains.intellij.platform
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    // https://plugins.gradle.org/search?term=org.jetbrains.intellij.platform.settings
    id("org.jetbrains.intellij.platform.settings") version "2.17.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    // Configure all projects' repositories
    repositories {
        mavenCentral()

        // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
        intellijPlatform {
            defaultRepositories()
        }
    }
}
