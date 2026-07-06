import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("2026.1.4")

        // Add plugin dependencies for compilation here, for example,
        // bundledPlugin("com.intellij.java")
    }
}

kotlin {
    jvmToolchain(25)

    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        jvmDefault.set(JvmDefaultMode.NO_COMPATIBILITY)

        freeCompilerArgs.add(
            "-Xannotation-default-target=param-property"
        )
    }
}

tasks {
    runIde {
        // Enable IntelliJ internal actions and diagnostics.
        systemProperty("idea.is.internal", "true")

        // Sandbox IDE heap.
        jvmArgs("-Xmx4G")
        //jvmArgs("-Dide.browser.jcef.gpu.disable=true")
        // Off-Screen Rendering for JCEF (JetBrains Chromium Embedded Framework)
        //jvmArgs("-Dide.browser.jcef.osr.enabled=true")
        //jvmArgs("-Dide.browser.jcef.command.line.args=--ignore-gpu-blocklist --enable-gpu-rasterization --plugin-policy=everywhere")
    }
}
