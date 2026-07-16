import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import kotlin.io.path.isDirectory
import kotlin.io.path.name

val redisStarsJvmArgs = listOf(
    "-Xms256m",
    "-Xmx2g",
    "-XX:+UseG1GC",
    "-Dfile.encoding=UTF-8",
)

abstract class VerifyPackagedJvmOptionsTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val distributableAppDir: DirectoryProperty

    @get:Input
    abstract val expectedJvmArgs: ListProperty<String>

    @get:Input
    abstract val launcherName: Property<String>

    @TaskAction
    fun verify() {
        val appDir = distributableAppDir.get().asFile.toPath()
        require(Files.exists(appDir) && appDir.isDirectory()) {
            "Distributable app directory not found: $appDir. Run createDistributable first."
        }

        val expectedArgs = expectedJvmArgs.get()
        val expectedLauncherName = launcherName.get()
        val cfgFiles = Files.walk(appDir, 4).use { paths ->
            paths
                .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".cfg", ignoreCase = true) }
                .filter { path ->
                    path.fileName.toString().removeSuffix(".cfg").equals(expectedLauncherName, ignoreCase = true) ||
                        path.parent?.name.equals("app", ignoreCase = true)
                }
                .toList()
        }

        require(cfgFiles.isNotEmpty()) {
            "No launcher .cfg file found under $appDir."
        }
        require(cfgFiles.size == 1) {
            "Expected exactly one launcher .cfg file under $appDir, found ${cfgFiles.size}: ${cfgFiles.joinToString()}"
        }

        val cfgFile = cfgFiles.single()
        val cfgText = Files.readString(cfgFile)
        require(cfgText.contains("[JavaOptions]")) {
            "Launcher config $cfgFile is missing the [JavaOptions] section."
        }

        val missingJvmArgs = expectedArgs.filterNot { expectedArg ->
            cfgText.lineSequence().any { line ->
                line.trim() == "java-options=$expectedArg"
            }
        }
        require(missingJvmArgs.isEmpty()) {
            "Launcher config $cfgFile is missing JVM options: ${missingJvmArgs.joinToString()}"
        }

        logger.lifecycle("Verified launcher config: $cfgFile")
        expectedArgs.forEach { logger.lifecycle("Verified JVM option: $it") }
    }
}

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.app.shared)
    implementation(projects.redisJvm)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.logback)
    implementation(libs.slf4j.api)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "org.roberthu.rs.MainKt"
        jvmArgs += redisStarsJvmArgs

        nativeDistributions {
            // Cross-compilation is not supported: build each format on its target OS.
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Pkg,
                TargetFormat.Msi,
                TargetFormat.Exe,
                TargetFormat.Deb,
                TargetFormat.Rpm,
            )

            packageName = "RedisStars"
            packageVersion = "1.0.0"
            description = "Cross-platform Redis desktop client"
            vendor = "RobertHU"
            copyright = "© 2026 RobertHU. All rights reserved."

            // Derived from src/main/resources/logo.png via icons/generate_icons.py
            windows {
                iconFile.set(project.file("icons/icon.ico"))
                menuGroup = "RedisStars"
                dirChooser = true
                // Keep this UUID stable across releases so Windows installers can upgrade in place.
                upgradeUuid = "A8E2C4F0-9B1D-4E6A-8C3F-2D7B5A9E1F04"
                msiPackageVersion = "1.0.0"
                exePackageVersion = "1.0.0"
            }

            linux {
                iconFile.set(project.file("icons/icon.png"))
                // Debian/RPM package names are conventionally lowercase.
                packageName = "redis-stars"
                menuGroup = "Development"
                appCategory = "Development"
                appRelease = "1"
                debPackageVersion = "1.0.0"
                rpmPackageVersion = "1.0.0"
            }

            macOS {
                iconFile.set(project.file("icons/icon.icns"))
                bundleID = "org.roberthu.rs"
                dockName = "RedisStars"
                dmgPackageVersion = "1.0.0"
                pkgPackageVersion = "1.0.0"
            }
        }
    }
}

tasks.register<VerifyPackagedJvmOptionsTask>("verifyPackagedJvmOptions") {
    group = "verification"
    description = "Verifies the distributable launcher config contains the default JVM options."
    dependsOn("createDistributable")
    distributableAppDir.set(layout.buildDirectory.dir("compose/binaries/main/app"))
    expectedJvmArgs.set(redisStarsJvmArgs)
    launcherName.set(rootProject.name)
}
