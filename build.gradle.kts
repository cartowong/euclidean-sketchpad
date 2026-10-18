plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.runtime") version "1.13.1"
    application
}

group = "euclideansketchpad"
version = "0.1.0"

repositories {
    mavenCentral()
}

javafx {
    version = "21.0.1"
    modules("javafx.controls", "javafx.graphics", "javafx.swing")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("euclideansketchpad.MainKt")
}

kotlin {
    jvmToolchain(21)
}

runtime {
    modules.set(listOf("java.base", "java.desktop", "java.logging", "jdk.unsupported"))
    options.set(listOf("--strip-debug", "--no-header-files", "--no-man-pages"))

    val currentOs = org.gradle.internal.os.OperatingSystem.current()

    jpackage {
        // Read APP_VERSION from Main.kt so there's a single source of truth
        val mainKt = file("src/main/kotlin/euclideansketchpad/Main.kt").readText()
        val versionMatch = Regex("""APP_VERSION\s*=\s*"([^"]+)"""").find(mainKt)
        appVersion = versionMatch?.groupValues?.get(1) ?: "1.0.0"
        imageName = "Euclidean Sketchpad"
        installerName = "EuclideanSketchpad"

        if (currentOs.isMacOsX) {
            installerType = "dmg"
            imageOptions = listOf("--icon", "${project.projectDir}/packaging/macos/EuclideanSketchpad.icns")
            installerOptions = listOf(
                "--vendor", "MathVisuals",
                "--mac-package-name", "Euclidean Sketchpad"
            )
        } else if (currentOs.isWindows) {
            installerType = "msi"
            imageOptions = listOf("--icon", "${project.projectDir}/packaging/windows/EuclideanSketchpad.ico")
            installerOptions = listOf(
                "--vendor", "MathVisuals",
                "--win-dir-chooser",
                "--win-menu",
                "--win-shortcut-prompt"
            )
        }
    }
}
