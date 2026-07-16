# RedisStars

**English** | [简体中文](./README.zh-CN.md)

Cross-platform Redis desktop client built with **Kotlin** and **Compose Desktop**. Connect directly to Redis, browse keys, inspect metadata, and read or edit values — strings, hashes, lists, sets, and sorted sets.

RedisStars is **desktop-only**. Earlier Server, Web, HTTP API, and remote-client components have been removed.

## Features

- **Connection profiles** — Standalone, Sentinel, and Cluster deployments
- **SSH tunnel** — optional local port-forward for Standalone connections
- **Key browser** — pattern scan, TTL, rename, delete
- **Value editor** — view and edit all common Redis data types
- **TLS** — peer verification enabled by default when TLS is selected
- **Local persistence** — connection profiles and settings stored on disk outside the repository
- **Installers** — native packages for Windows, Linux, and macOS via jpackage

## Requirements

| Purpose | Requirement |
|---------|-------------|
| Build & run from source | JDK **17+**, Gradle wrapper |
| Windows EXE / MSI | [WiX Toolset](https://wixtoolset.org/) (jpackage dependency) |
| Linux DEB | `fakeroot`, `dpkg` |
| Linux RPM | `rpm-build` / `rpmbuild` |
| Integration tests | Docker (Testcontainers; tests skip when unavailable) |

Compose Desktop does **not** support cross-compilation — build each installer on its target OS.

## Quick start

**Windows (PowerShell)**

```powershell
.\gradlew.bat :app:desktopApp:run
```

**Linux / macOS**

```bash
./gradlew :app:desktopApp:run
```

Hot reload during UI development:

```bash
./gradlew :app:desktopApp:hotRun --auto
```

For local Redis instances, see [Redis test environments](./docs/redis-test-environments.md).

## Installers

Packaged output is written to `app/desktopApp/build/compose/binaries/`.

| OS | Formats | Gradle tasks |
|----|---------|--------------|
| Windows | `.exe`, `.msi` | `:app:desktopApp:packageExe`, `:app:desktopApp:packageMsi` |
| Linux | `.deb`, `.rpm` | `:app:desktopApp:packageDeb`, `:app:desktopApp:packageRpm` |
| macOS | `.dmg`, `.pkg` | `:app:desktopApp:packageDmg`, `:app:desktopApp:packagePkg` |

**Common tasks**

```bash
# Installer(s) for the current OS
./gradlew :app:desktopApp:packageDistributionForCurrentOS

# Unpacked app image (no installer)
./gradlew :app:desktopApp:createDistributable
./gradlew :app:desktopApp:runDistributable

# Single fat JAR for the current OS
./gradlew :app:desktopApp:packageUberJarForCurrentOS
```

**Windows**

```powershell
.\gradlew.bat :app:desktopApp:packageExe
.\gradlew.bat :app:desktopApp:packageMsi
```

**Linux**

```bash
./gradlew :app:desktopApp:packageDeb
./gradlew :app:desktopApp:packageRpm
```

If a packaged app fails at runtime with `ClassNotFoundException`, run `:app:desktopApp:suggestModules` and add the suggested JDK modules under `nativeDistributions { modules(...) }` in `app/desktopApp/build.gradle.kts`.

## JVM runtime options

Packaged RedisStars reads JVM startup flags from the jpackage launcher config file **`RedisStars.cfg`**. The native launcher applies these options **before** the JVM starts — heap size, GC, and other `-X`/`-XX` flags cannot be changed from application code.

Default options (defined in `app/desktopApp/build.gradle.kts`):

| Option | Purpose |
|--------|---------|
| `-Xms256m` | Initial heap |
| `-Xmx2g` | Maximum heap |
| `-XX:+UseG1GC` | G1 garbage collector |
| `-Dfile.encoding=UTF-8` | File encoding |

**Config file locations**

| Distribution | Path |
|--------------|------|
| Unpacked distributable | `app/desktopApp/build/compose/binaries/main/app/RedisStars/app/RedisStars.cfg` |
| Windows EXE / MSI (installed) | `<install-dir>\app\RedisStars.cfg` |
| Linux RPM / DEB (installed) | `/opt/redis-stars/lib/app/RedisStars.cfg` |

On Windows, `<install-dir>` is the directory chosen during installation. Application data lives separately under `%APPDATA%\RedisStars` — that is not the JVM config.

To increase heap memory, edit the `[JavaOptions]` section:

```ini
java-options=-Xmx4g
```

Save, fully quit RedisStars, and restart. Only modify `[JavaOptions]`; leave `[Application]` and classpath entries untouched. Back up the file before editing. **Installer upgrades may overwrite** custom settings.

Full details: [docs/jvm-options.md](./docs/jvm-options.md)

**Verify packaged defaults**

```powershell
.\gradlew.bat :app:desktopApp:createDistributable
.\gradlew.bat :app:desktopApp:verifyPackagedJvmOptions
```

## Project structure

```
RedisStars/
├── core/              Domain models, validation, use cases, ports
├── redis-jvm/         Lettuce adapter (Standalone, Sentinel, Cluster)
├── app/shared/        Compose UI, view models, i18n
└── app/desktopApp/    Entry point, composition root, packaging config
```

| Module | Role |
|--------|------|
| [`core`](./core/src) | Domain logic and Redis/persistence abstractions |
| [`redis-jvm`](./redis-jvm/src) | Lettuce-based Redis client |
| [`app/shared`](./app/shared/src) | Shared Compose UI and presentation layer |
| [`app/desktopApp`](./app/desktopApp/src) | Desktop launcher and platform wiring |

## Configuration and security

**Application data** (connection profiles, settings) is stored outside the repository:

| OS | Location |
|----|----------|
| Windows | `%APPDATA%\RedisStars` |
| Linux / macOS | `~/.config/redis-stars` |

**Passwords** are saved only when "remember passwords" is enabled. The JSON store then contains **plaintext credentials** — protect your OS account and do not sync or commit this file.

**TLS** peer verification is on by default whenever TLS is selected.

## Development

**Run tests**

```powershell
.\gradlew.bat :core:jvmTest
.\gradlew.bat :redis-jvm:test
.\gradlew.bat :app:shared:jvmTest
.\gradlew.bat :app:desktopApp:compileKotlin
```

**Packaging verification**

```powershell
.\gradlew.bat :app:desktopApp:createDistributable
.\gradlew.bat :app:desktopApp:verifyPackagedJvmOptions
```

The `redis-jvm` module uses disposable `redis:7-alpine` containers via Testcontainers. Tests are skipped automatically when Docker is not available.

## Documentation

| Document | Contents |
|----------|----------|
| [docs/jvm-options.md](./docs/jvm-options.md) | JVM launcher config, editing rules, upgrade behavior |
| [docs/redis-test-environments.md](./docs/redis-test-environments.md) | Local Redis, Sentinel, and Cluster setup |

## License

[MIT](./LICENSE) — Copyright (c) 2026 RobertHU
