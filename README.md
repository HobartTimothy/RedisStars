# RedisStars

RedisStars is a JVM desktop client for Redis built with Kotlin and Compose Desktop. It connects directly to Redis and supports key scanning, metadata, TTL and rename/delete operations, plus reading and editing strings, hashes, lists, sets, and sorted sets.

The former Server and Web applications, HTTP API, and remote client have been removed. RedisStars is now Desktop-only.

## Architecture

The Gradle build contains four modules:

- [`core`](./core/src) — domain models, validation, use cases, and Redis/persistence ports.
- [`redis-jvm`](./redis-jvm/src) — Lettuce-based Redis adapter with Standalone, Sentinel, and Cluster connection support.
- [`app/shared`](./app/shared/src) — Compose UI, unidirectional UI state, and view models, compiled for JVM only.
- [`app/desktopApp`](./app/desktopApp/src) — desktop entry point and composition root, wiring the shared UI to Lettuce and local JSON persistence.

The connection editor dialog supports Standalone, Sentinel, and Cluster profiles.
Standalone profiles can optionally reach Redis through an SSH local port-forward tunnel.
See [Redis test environments](./docs/redis-test-environments.md) for local setup notes.

## Security and local data

- TLS peer verification defaults to enabled whenever TLS is selected.
- Saved passwords are omitted unless “remember passwords” is enabled. When enabled, the desktop JSON store contains plaintext credentials; protect the OS account and do not sync or commit the file.
- Local config is stored outside this repository (`%APPDATA%\RedisStars` on Windows, otherwise `~/.config/redis-stars`).

## Run

Windows PowerShell:

```powershell
.\gradlew.bat :app:desktopApp:run
.\gradlew.bat :app:desktopApp:hotRun --auto
```

Unix:

```bash
./gradlew :app:desktopApp:run
./gradlew :app:desktopApp:hotRun --auto
```

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

## Tests and verification

Run the Desktop verification matrix on Windows:

```powershell
.\gradlew.bat :core:jvmTest
.\gradlew.bat :redis-jvm:test
.\gradlew.bat :app:shared:jvmTest
.\gradlew.bat :app:desktopApp:compileKotlin
```

The `redis-jvm` integration tests use disposable `redis:7-alpine` containers through Testcontainers. They skip through JUnit assumptions when Docker is unavailable.