This is a Kotlin Multiplatform project targeting Web, Desktop (JVM), Server.

* [/app/shared](./app/shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./app/shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./app/shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./app/shared/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/core](./core/src) is for the code that will be shared between all targets in the project.
  The most important subfolder is [commonMain](./core/src/commonMain/kotlin). If preferred, you
  can add code to the platform-specific folders here too.

* [/server](./server/src/main/kotlin) is for the Ktor server application.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Desktop app:
  - Hot reload: `./gradlew :app:desktopApp:hotRun --auto`
  - Standard run: `./gradlew :app:desktopApp:run`
- Server: `./gradlew :server:run`
- Web app:
  - Wasm target (faster, modern browsers): `./gradlew :app:webApp:wasmJsBrowserDevelopmentRun`
  - JS target (slower, supports older browsers): `./gradlew :app:webApp:jsBrowserDevelopmentRun`

### Packaging the desktop app

Compose Desktop uses `jpackage` (JDK 17+). **No cross-compilation** — build each installer on the matching OS. Output lands under `app/desktopApp/build/compose/binaries/`.

| OS | Formats | Gradle tasks |
|----|---------|--------------|
| Windows | `.exe`, `.msi` | `:app:desktopApp:packageExe`, `:app:desktopApp:packageMsi` |
| Linux | `.deb`, `.rpm` | `:app:desktopApp:packageDeb`, `:app:desktopApp:packageRpm` |
| macOS | `.dmg`, `.pkg` | `:app:desktopApp:packageDmg`, `:app:desktopApp:packagePkg` |

Convenience tasks:

```bash
# Installer(s) for the OS you are on
./gradlew :app:desktopApp:packageDistributionForCurrentOS

# Unpacked app image (no installer) — useful for local smoke checks
./gradlew :app:desktopApp:createDistributable
./gradlew :app:desktopApp:runDistributable

# Single fat JAR for the current OS
./gradlew :app:desktopApp:packageUberJarForCurrentOS
```

Windows examples (PowerShell):

```powershell
.\gradlew.bat :app:desktopApp:packageExe
.\gradlew.bat :app:desktopApp:packageMsi
.\gradlew.bat :app:desktopApp:packageDistributionForCurrentOS
```

Linux examples:

```bash
./gradlew :app:desktopApp:packageDeb
./gradlew :app:desktopApp:packageRpm
```

**Tooling prerequisites**

| Target | Extra tools |
|--------|-------------|
| Windows EXE / MSI | [WiX Toolset](https://wixtoolset.org/) on `PATH` (jpackage dependency) |
| Linux DEB | `fakeroot`, `dpkg` (Debian/Ubuntu) |
| Linux RPM | `rpm-build` / `rpmbuild` (Fedora/RHEL/openSUSE) |
| All | JDK **17+** with `jpackage` (`JAVA_HOME` set) |

If a packaged app fails at runtime with `ClassNotFoundException`, run `./gradlew :app:desktopApp:suggestModules` and add the suggested modules under `nativeDistributions { modules(...) }` in `app/desktopApp/build.gradle.kts`.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Desktop tests: `./gradlew :app:shared:jvmTest`
- Server tests: `./gradlew :server:test`
- Web tests:
  - Wasm target: `./gradlew :app:shared:wasmJsTest`
  - JS target: `./gradlew :app:shared:jsTest`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).