# JVM runtime options

RedisStars desktop packages use the **jpackage native launcher** configuration file (`RedisStars.cfg`) for JVM startup parameters. The launcher reads this file before creating the JVM process.

## How it works

1. During build, default JVM options are defined in `app/desktopApp/build.gradle.kts` and passed to Compose Desktop via `application { jvmArgs += ... }`.
2. jpackage writes these options into `RedisStars.cfg` under the `[JavaOptions]` section.
3. On every launch, the native launcher (`RedisStars.exe` on Windows, `redis-stars` on Linux) reads `RedisStars.cfg` and passes the options to the JVM **before** `main()` runs.
4. Users can edit the installed `.cfg` file directly; changes take effect after a full restart.

Do **not** read JVM options inside `MainKt` — flags like `-Xmx` must be applied at JVM creation time.

## Default options

| Option | Purpose |
|--------|---------|
| `-Xms256m` | Initial heap size |
| `-Xmx2g` | Maximum heap size |
| `-XX:+UseG1GC` | Use G1 garbage collector |
| `-Dfile.encoding=UTF-8` | Default file encoding |

jpackage and Compose Desktop also add their own `java-options` entries (app version, resource paths, Skiko library path, etc.). Leave those unchanged.

## Config file locations

### Unpacked distributable

After `./gradlew :app:desktopApp:createDistributable`:

```text
app/desktopApp/build/compose/binaries/main/app/RedisStars/app/RedisStars.cfg
```

### Windows EXE / MSI

After installation to a user-chosen directory:

```text
<install-dir>\app\RedisStars.cfg
```

Example: if you install to `D:\Apps\RedisStars`, the config is at `D:\Apps\RedisStars\app\RedisStars.cfg`.

### Linux RPM / DEB

After `rpm -i` or `dpkg -i` (package name `redis-stars`):

```text
/opt/redis-stars/lib/app/RedisStars.cfg
```

> **Note:** The Linux RPM path follows the standard jpackage layout for package name `redis-stars`. Verify with `rpm -ql redis-stars | grep '\.cfg$'` after installation on a Linux host.

## Editing options

Open `RedisStars.cfg` in a text editor. Find the `[JavaOptions]` section:

```ini
[JavaOptions]
java-options=-Djpackage.app-version=1.0.0
java-options=-Dcompose.application.resources.dir=$APPDIR\resources
java-options=-Dcompose.application.configure.swing.globals=true
java-options=-Xms256m
java-options=-Xmx2g
java-options=-XX:+UseG1GC
java-options=-Dfile.encoding=UTF-8
java-options=-Dskiko.library.path=$APPDIR
```

### Increase maximum heap memory

Change:

```ini
java-options=-Xmx2g
```

to:

```ini
java-options=-Xmx4g
```

Save, fully quit RedisStars, and restart.

## Rules and limitations

1. **Only edit `[JavaOptions]`** — do not modify `[Application]`, classpath entries, or launcher metadata.
2. **One option per line** — format is `java-options=<argument>`.
3. **Avoid duplicates** — repeating the same flag (e.g. two `-Xmx` lines) leads to undefined JVM behavior.
4. **Invalid options** — the JVM reports errors to the console; the app may fail to start.
5. **Back up before editing** — copy `RedisStars.cfg` to a safe location first.
6. **No runtime overwrite** — RedisStars does not rewrite this file on exit or startup.
7. **Installer upgrades** — EXE, MSI, RPM, and DEB installers may replace `RedisStars.cfg` with build defaults during upgrade. Back up custom settings before upgrading.

## Build-time verification

Gradle task `verifyPackagedJvmOptions` checks that the distributable `.cfg` contains all default JVM options:

```powershell
.\gradlew.bat :app:desktopApp:createDistributable
.\gradlew.bat :app:desktopApp:verifyPackagedJvmOptions
```

The task fails with a clear message if the `.cfg` file or any expected option is missing.

## Verifying options at runtime (development only)

During development or testing, you can confirm JVM arguments with:

```kotlin
ManagementFactory.getRuntimeMXBean().inputArguments
Runtime.getRuntime().maxMemory()
```

Do not add permanent UI panels for JVM diagnostics in the production app.
