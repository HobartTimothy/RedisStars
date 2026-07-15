import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.app.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "org.roberthu.rs.MainKt"

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

            // Optional icons (uncomment when assets exist):
            // windows { iconFile.set(project.file("icons/icon.ico")) }
            // linux { iconFile.set(project.file("icons/icon.png")) }
            // macOS { iconFile.set(project.file("icons/icon.icns")) }

            windows {
                menuGroup = "RedisStars"
                dirChooser = true
                // Keep this UUID stable across releases so Windows installers can upgrade in place.
                upgradeUuid = "A8E2C4F0-9B1D-4E6A-8C3F-2D7B5A9E1F04"
                msiPackageVersion = "1.0.0"
                exePackageVersion = "1.0.0"
            }

            linux {
                // Debian/RPM package names are conventionally lowercase.
                packageName = "redis-stars"
                menuGroup = "Development"
                appCategory = "Development"
                appRelease = "1"
                debPackageVersion = "1.0.0"
                rpmPackageVersion = "1.0.0"
            }

            macOS {
                bundleID = "org.roberthu.rs"
                dockName = "RedisStars"
                dmgPackageVersion = "1.0.0"
                pkgPackageVersion = "1.0.0"
            }
        }
    }
}
