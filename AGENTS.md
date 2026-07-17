# AGENTS.md

## Scope

This file applies to the repository root and every directory beneath it.

RedisStars is a desktop-only Redis client. Treat the `develop` branch as the working baseline when this file is used there. Do not reintroduce the removed Server, Web, HTTP API, or remote-client applications unless the task explicitly requires a separately reviewed architectural change.

## Project Snapshot

- Language: Kotlin 2.4
- UI: Compose Multiplatform / Compose Desktop
- Runtime: JVM, JDK 17+
- Build: Gradle Wrapper 9.1
- Redis client: Lettuce
- Supported deployments: Standalone, Sentinel, and Cluster
- Optional transport: SSH local port forwarding for Standalone connections
- Tests: Kotlin Test, JUnit, Compose UI Test, and Testcontainers
- Main package: `org.roberthu.rs`
- Desktop entry point: `org.roberthu.rs.MainKt`

Always use the checked-in Gradle wrapper. Do not assume a globally installed Gradle version.

## Repository Layout

```text
RedisStars/
├── core/              Domain models, validation, use cases, and ports
├── redis-jvm/         Lettuce and SSH implementations of core ports
├── app/shared/        Compose UI, presentation state, i18n, and platform abstractions
├── app/desktopApp/    Desktop entry point, JVM/platform implementations, wiring, logging, packaging
├── docs/              Runtime and Redis test-environment documentation
├── gradle/            Version catalog and wrapper configuration
├── build.gradle.kts
└── settings.gradle.kts
```

## Architectural Boundaries

Preserve this dependency direction:

```text
core <- redis-jvm
core <- app/shared
app/shared + redis-jvm <- app/desktopApp
```

### `core`

`core` is the platform-neutral domain layer.

- Keep `commonMain` free of Compose, Lettuce, AWT/Swing, Java file-system APIs, and desktop-specific behavior.
- Put domain models and invariants under `domain`.
- Put external contracts under `port`.
- Put application operations under `usecase`.
- Express expected failures with the existing `Result` and `RedisError` patterns instead of leaking adapter exceptions.
- Add validation close to the model or use case that owns the invariant.

### `redis-jvm`

`redis-jvm` is an infrastructure adapter.

- Implement interfaces defined in `core`; do not move Lettuce types into domain or presentation APIs.
- Keep Standalone, Sentinel, Cluster, TLS, timeout, reconnect, and SSH behavior consistent with `ConnectionProfile` invariants.
- Map Lettuce/network failures through `LettuceExceptionMapper` or an equivalent domain-safe mapping.
- Close clients, connections, tunnels, executors, and other resources deterministically.
- Keep Redis operations cancellable where the port supports cancellation.

### `app/shared`

`app/shared` owns reusable presentation logic and Compose UI.

- Keep `commonMain` free of direct AWT, Swing, `java.nio.file`, and desktop launcher dependencies.
- Add platform contracts under `platform`; implement JVM-specific `expect`/`actual` declarations in `jvmMain` when appropriate.
- Keep Redis access behind `core` ports and use cases. Composables must not instantiate Lettuce clients or perform direct Redis commands.
- Keep state transitions in view models or explicit state holders rather than embedding business logic in Composables.

### `app/desktopApp`

`app/desktopApp` is the desktop composition root and packaging module.

- Limit it to application startup, platform implementations, persistence adapters, logging infrastructure, dependency wiring, lifecycle, and native packaging.
- Wire dependencies in `DesktopCompositionRoot` or a clearly equivalent composition root.
- Keep domain decisions and reusable presentation logic out of this module.
- Ensure application shutdown closes Redis and platform resources.

## Build and Run

### Windows PowerShell

```powershell
.\gradlew.bat :app:desktopApp:run
```

### Linux or macOS

```bash
./gradlew :app:desktopApp:run
```

### Compose hot reload

```bash
./gradlew :app:desktopApp:hotRun --auto
```

Do not add platform-specific shell assumptions to cross-platform build logic.

## Verification Matrix

Run the narrowest relevant checks while developing, then run the complete baseline before finishing a non-trivial change.

| Changed area | Required minimum verification |
|---|---|
| `core/**` | `:core:jvmTest` |
| `redis-jvm/**` | `:redis-jvm:test` |
| `app/shared/**` | `:app:shared:jvmTest` |
| `app/desktopApp/**` | `:app:desktopApp:compileKotlin` |
| Desktop packaging/JVM options | `:app:desktopApp:createDistributable` and `:app:desktopApp:verifyPackagedJvmOptions` |
| Cross-module change | Run all baseline checks below |

### Complete baseline — Windows

```powershell
.\gradlew.bat :core:jvmTest :redis-jvm:test :app:shared:jvmTest :app:desktopApp:compileKotlin
```

### Complete baseline — Linux or macOS

```bash
./gradlew :core:jvmTest :redis-jvm:test :app:shared:jvmTest :app:desktopApp:compileKotlin
```

`redis-jvm` integration tests use Testcontainers and may skip when Docker is unavailable. Report skipped integration coverage explicitly; do not represent a skipped test suite as a verified Redis integration path.

## Kotlin Conventions

Follow the style already present in the repository.

- Use four-space indentation.
- Use trailing commas in multiline declarations and calls.
- Prefer immutable `data class` state and `copy`-based updates.
- Prefer exhaustive `when` expressions for sealed classes and enums.
- Use expression bodies for simple functions when readability improves.
- Keep public contracts explicit; avoid exposing implementation-specific types.
- Use descriptive names based on domain behavior, not UI widget position or temporary implementation details.
- Add concise KDoc for contracts, invariants, persistence formats, or platform behavior that is not obvious from the code.
- Do not add boilerplate author/date headers or comments that merely restate the code.
- Avoid unrelated formatting or refactoring in focused bug fixes.

There is no configured ktlint or detekt task in the current build. Match surrounding code and rely on compilation plus tests rather than inventing a new formatting gate inside an unrelated change.

## Coroutines and State

- Inject or receive a `CoroutineScope`; do not use `GlobalScope`.
- Expose immutable `StateFlow` or `Flow` views and keep mutable flows private.
- Update UI state atomically with `MutableStateFlow.update` where practical.
- Keep blocking platform I/O out of Composable functions.
- Do not swallow cancellation exceptions in broad error handlers.
- Prevent stale async work from overwriting newer state after selection, connection, database, or query changes.
- Reuse the existing scan cancellation mechanisms for long-running key scans.
- Keep connection lifecycle state aligned with `ConnectionState`.

When adding a view-model action, test both the successful state transition and the relevant failure or cancellation transition.

## Compose UI Rules

- Prefer state hoisting and event callbacks over hidden mutable state.
- Keep previews and UI tests independent of live Redis by using `AppContainer.preview()` or in-memory/fake ports.
- Use stable keys for dynamic lists and trees.
- Avoid launching Redis or persistence work directly from recomposition.
- Place expensive derived calculations behind `remember`/derived state only when the dependencies are correct.
- Preserve keyboard, focus, dialog dismissal, and window lifecycle behavior when editing desktop flows.
- Add Compose UI tests for user-visible regressions when logic-only tests cannot cover the behavior.

Large existing screens should not grow indefinitely. Extract focused state or UI components when a change materially increases complexity, but do not perform broad decomposition without tests.

## Internationalization

RedisStars supports English and Simplified Chinese.

- Do not hard-code user-visible strings in Composables, view models, validation presentation, dialogs, menus, or notifications.
- Inside `@Composable` code, prefer the composable `t(...)` lookup.
- Outside Compose, use `AppI18n.t(...)` when localized text is required.
- Add or update keys in `StringKeys` and keep `StringsEnUs` and `StringsZhCn` synchronized.
- Update catalog tests whenever keys, placeholders, or formatting arguments change.
- Preserve placeholder order and argument types across languages.
- Keep low-level domain errors stable enough for `ValidationI18n` or equivalent mapping; do not scatter ad hoc string matching throughout the UI.

## Redis Semantics

- Never replace cursor-based browsing with the blocking `KEYS` command.
- Preserve SCAN/HSCAN/SSCAN/ZSCAN cursor behavior and cancellation.
- Treat Redis Cluster as database `0` only.
- Preserve handling for MOVED/ASK redirection, cross-slot operations, read-only nodes, authentication failures, timeouts, closed connections, and network failures.
- Maintain binary-safe value behavior. Do not assume all Redis data is valid UTF-8.
- Respect configured maximum read sizes and avoid unbounded value or collection loading.
- Keep TTL behavior explicit: distinguish persistent keys, missing keys, and expiring keys.
- Validate create/edit operations before sending commands.
- SSH tunneling is supported only for Standalone mode unless the architecture is deliberately extended with tests and documentation.
- TLS peer verification must remain enabled by default whenever TLS is selected.

Changes to a port contract require coordinated updates to:

1. The interface in `core`.
2. Relevant use cases.
3. The Lettuce implementation in `redis-jvm`.
4. In-memory/fake implementations used by previews and tests.
5. View models and UI callers.
6. Unit and integration tests.

## Persistence and Security

Application data is stored outside the repository:

- Windows: `%APPDATA%\RedisStars`
- Linux/macOS: `~/.config/redis-stars`

Treat profile JSON, logs, imported text, Redis URLs, SSH material, and exported diagnostics as sensitive.

- Never commit local profile data, settings, logs, private keys, passwords, tokens, Redis dumps, or generated credentials.
- Do not log Redis passwords, SSH passwords, private-key contents, passphrases, authorization headers, or credential-bearing URLs.
- Use `SensitiveRedactor` for text that can contain user-controlled diagnostics or credentials.
- Preserve atomic file-write behavior for settings and profiles.
- Maintain backward-compatible decoding when changing stored models. Add migration/default-value tests in `StoredDataCodec` tests.
- Honor the application's password-retention behavior; do not broaden secret persistence implicitly.
- Do not weaken TLS verification or silently accept unknown host/security conditions.
- Do not expose raw adapter exception messages directly to users when they may contain endpoints or credentials.

## Testing Expectations

- Every bug fix should include a regression test when the behavior is testable.
- Every new domain invariant should have a `core` test.
- Every new view-model transition should have a presentation test.
- Every new Redis command path should have adapter tests and, when practical, a Testcontainers integration test.
- Keep tests deterministic; do not depend on a developer's real Redis instance, home directory, locale, or saved profiles.
- Use in-memory stores and fake ports for presentation tests.
- Test success, mapped failure, cancellation, and boundary values where relevant.
- For persisted data, test both current encoding and decoding of older/missing fields.
- For i18n, test key parity and formatting placeholders in both catalogs.

Use the local environments documented in `docs/redis-test-environments.md` only for manual validation or explicitly scoped integration work.

## Native Packaging

Compose Desktop does not support cross-compilation. Build each installer on its target operating system.

Common tasks:

```bash
./gradlew :app:desktopApp:packageDistributionForCurrentOS
./gradlew :app:desktopApp:createDistributable
./gradlew :app:desktopApp:runDistributable
./gradlew :app:desktopApp:packageUberJarForCurrentOS
```

Platform tasks:

```text
Windows: :app:desktopApp:packageExe, :app:desktopApp:packageMsi
Linux:   :app:desktopApp:packageDeb, :app:desktopApp:packageRpm
macOS:   :app:desktopApp:packageDmg, :app:desktopApp:packagePkg
```

Packaging constraints:

- Keep the Windows `upgradeUuid` stable across releases.
- When changing the application version, update all relevant general and platform-specific package version fields consistently.
- Keep Linux package naming lowercase.
- Generate icon formats from the canonical logo using `app/desktopApp/icons/generate_icons.py`; do not hand-edit generated binary icons.
- Preserve default JVM launcher options unless the task explicitly changes memory/runtime policy.
- After changing packaging or JVM options, run `verifyPackagedJvmOptions`.
- If jpackage reports missing runtime modules, use `suggestModules`, update the distribution modules deliberately, and verify the packaged app starts.
- Update `docs/jvm-options.md` when launcher configuration behavior changes.

Do not commit generated files from `**/build/` or native installer output.

## Dependency Changes

Dependencies and plugin versions are centralized in `gradle/libs.versions.toml`.

- Add or update versions through the version catalog.
- Do not duplicate literal dependency versions in module build files.
- Keep dependency scope minimal (`implementation` unless consumers require `api`).
- Avoid adding a library when Kotlin, Compose, Lettuce, or the existing utility code already provides the required behavior.
- Explain architecture, binary-size, security, and licensing impact for significant new dependencies.
- Run the complete baseline after Kotlin, Compose, coroutine, serialization, Lettuce, or Gradle changes.

## Documentation

Update documentation in the same change when behavior visible to users or developers changes.

- Update `README.md` and `README.zh-CN.md` together for user-facing setup, capabilities, requirements, or packaging changes.
- Update `docs/jvm-options.md` for launcher/JVM configuration changes.
- Update `docs/redis-test-environments.md` for Redis integration setup changes.
- Keep command examples valid on the stated operating system.
- Do not document removed Server/Web/API components as supported features.

## Agent Workflow

Before editing:

1. Read the relevant module build file.
2. Read the nearest production code and tests.
3. Identify the owning layer and existing port/use-case boundary.
4. Check whether the change affects i18n, persistence compatibility, security, Redis topology, or packaging.

While editing:

1. Make the smallest coherent change.
2. Preserve module boundaries and public contracts unless the task requires changing them.
3. Add or update tests with the implementation.
4. Avoid unrelated cleanup.
5. Never modify generated build output or user-local data.

Before finishing:

1. Run the targeted module checks.
2. Run the complete baseline for cross-module or non-trivial changes.
3. Run packaging verification when packaging/JVM configuration changed.
4. Review the diff for secrets, debug output, generated files, accidental binary files, and hard-coded UI strings.
5. Report commands run, tests passed, tests skipped, and any verification that could not be performed.

## Definition of Done

A change is complete only when:

- It is implemented in the correct architectural layer.
- Relevant tests cover the behavior or the lack of a test is explicitly justified.
- Required Gradle checks pass.
- User-visible strings are localized in both supported languages.
- Security-sensitive data is not logged or committed.
- Persistence remains compatible or includes a tested migration path.
- Redis topology and binary-safety semantics are preserved.
- Documentation is updated where required.
- Packaging is verified when affected.
- The final summary accurately distinguishes verified behavior from assumptions or skipped checks.
