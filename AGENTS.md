# AGENTS.md

Repository guidance for coding agents working in this project.

## Project Overview

Zygisk module template with Compose Multiplatform WebUI. Two deployment targets:
- **KernelSU**: WebUI renders in the KSU manager's WebView (`webroot/` in module zip)
- **Magisk**: Standalone Android APK (`webui-app`) since Magisk lacks WebUI support

## Build Commands

```bash
# Full module zip (includes native libs + WebUI) → module/release/
./gradlew :module:zipRelease

# WebUI Wasm production build
./gradlew :webui:buildWebUI

# Focused common tests on the JVM (no emulator or browser required)
./gradlew :webui:testAndroidHostTest

# Browser dev server (http://localhost:8080, mock data when window.ksu unavailable)
./gradlew :webui:wasmJsBrowserDevelopmentRun

# Install to device
./gradlew :module:installKsuRelease    # KernelSU
./gradlew :module:installMagiskRelease  # Magisk
./gradlew :webui:install               # Hot-reload WebUI only
./gradlew :webui-app:installDebug       # Standalone APK

# CI release (zip + update.json with SHA-256 hashes)
./gradlew :module:ciRelease
```

Before editing, read this guide and the relevant README, build files, and scripts. If a module or Gradle task is unfamiliar, inspect `./gradlew projects` and `./gradlew tasks --all` instead of guessing task or package names. Keep edits within the requested scope. Because this is a template, prioritize reusable infrastructure and representative sample flows over exhaustive tests for placeholder domain logic.

Focused `commonTest` logic tests run on the Android host JVM with
`:webui:testAndroidHostTest`. Browser-based `wasmJsTest` still requires a local
Chrome installation. Choose UI verification based on the changed path: use
browser preview when browser behavior is the target; use an Android device or
AVD for Android APIs, WebView integration, permissions, or platform bridges.

## Verification Workflow

Run the narrowest relevant build and tests first. Inspect the resulting artifact
and install it on the target when feasible. For embedded WebUI, a successful
build or browser preview alone does not verify KernelSU manager integration;
test the packaged assets in the intended Android host when practical.

```bash
# Build and install both targets to device
./gradlew :webui-app:installDebug :webui:install

# Launch Android side
adb shell am start -n io.github.nku100.zygisk.sample/io.github.nku100.webui.MainActivity

# Launch wasmJs side (requires root + KsuWebUIStandalone)
adb shell am start -n io.github.a13e300.ksuwebui/.WebUIActivity \
  --es id zygisk_sample --es name "Zygisk Module WebUI"
```

The emulator must be rooted (Magisk) for full wasmJs testing. Root AVD
instructions, app-level root grants, and staged-module reboot guidance are in
`.agents/skills/root-avd/SKILL.md`.

For WebView tests, verify asset responses, JavaScript errors, and a screenshot of
the rendered screen; HTTP 200 responses alone do not prove the UI rendered.
Filter device logs to the relevant process and time window. When they affect the
result, record the Android API, ABI, system-image type, page size, root
implementation, and WebView version. Report environment limitations separately
from application failures.

## Architecture

### Three Subprojects

| Project | Role |
|---------|------|
| `:module` | Zygisk native C++ module + Magisk/KSU packaging. CMake builds `libsample.so`. Shell scripts handle install/verify. |
| `:webui` | Kotlin Multiplatform library — the main codebase. Targets: **wasmJs** and **androidLibrary**. |
| `:webui-app` | Minimal Android APK hosting `:webui` for Magisk users. |

### External Build Dependencies

The native module keeps `module/src/main/cpp/external/libcxx` as a git submodule. UI dependencies are resolved from Maven; there are no UI composite builds or Capsule/Backdrop submodules.

### Platform Abstraction (expect/actual)

`commonMain` declares `expect` interfaces; `androidMain` and `wasmJsMain` provide `actual` implementations:

| expect | Android | wasmJs |
|--------|---------|--------|
| `PlatformBridge` | Root shell via `Runtime.exec()` | KernelSU JS API v3.0.2 via `@JsFun` |
| `PlatformBackHandler` | Android `BackHandler` | Hash guard interception |
| `BrowserHistorySync` | No-op | 5-layer hash guard for WebView back nav |
| `AppIconImage` | `AppIconLoader` + LRU cache (Hardware Bitmap) | `ksu://icon/<pkg>` URI + Skia decode |
| `InteractiveHighlight` | AGSL `RuntimeShader` + `RenderEffect` | SkSL `RuntimeShaderBuilder` + `ImageFilter` |

When adding new platform-specific code, always create the `expect` declaration in `commonMain/platform/` first, then implement both `actual` variants.

### Data Flow

`ModuleConfig` (kotlinx.serialization) → JSON at `/data/adb/<moduleId>/config.json` → read by native C++ via companion IPC (`OP_READ_CONFIG`). Config metadata is in `module.gradle.kts` (module ID, name, author, ABIs).

### UI Architecture

- **ViewModel**: `MainViewModel` with `StateFlow<MainUiState>`, lifecycle-viewmodel 2.11.0
- **Navigation**: JetBrains Navigation 3 with `Route` sealed interface (`Main`, `About`, `AppProfile(packageName)`)
- **Pager**: Custom `MainPagerState` (ported from KernelSU) with `isNavigating` guard for cross-tab animation
- **4 tabs**: Home, Apps, Logs, Settings (`BottomTab` enum)
- **i18n**: Compose Resources, English + Chinese (Simplified) in `composeResources/values/`

## Key Conventions

### Gradle

- **Configuration cache is enabled**. All build script access to extra properties must use lazy `providers.extra {}` — never eager `extra["key"]`.
- Module metadata lives in `module.gradle.kts` (root level), imported into root `build.gradle.kts`.
- `module.gradle.kts` is applied to the root project via `apply(from = ...)` — its `extra` properties are available in root `build.gradle.kts` only.

### Native Build (C++)

- CMake 3.22.1+, C++20, **no RTTI, no exceptions, hidden visibility**
- Custom libcxx (topjohnwu/libcxx) — does not use Android's STL
- ccache enabled for faster rebuilds
- yyjson used for JSON parsing in C++

### Shell Scripts & Token Replacement

Module template files (`module/template/`) use `@TOKEN@` placeholders replaced at build time via Groovy `expand` in `module/build.gradle.kts`. The `module.prop` uses this pattern — values come from `module.gradle.kts` extras.

### wasmJs Development

- Browser preview uses mock data when `window.ksu` is undefined (dev mode)
- CJK font (~969 KB WOFF2) loaded via Compose Resources with `fontsReady` gate to prevent tofu flash
- `Date.now()` at high frequency causes wasmJs hangs — use `kotlin.time.TimeSource.Monotonic` + `@JsFun` for timestamps
- `fastFirstOrNull`/`fastRoundToInt`/`fastCoerceIn` are not available in wasmJs — use stdlib equivalents
- **`@JsFun` JS string escaping**: Kotlin string escape → JS string escape → shell escape三层嵌套极易出错。简单方案优于精巧方案（URL 不加 shell 引号比多层转义安全）
- `PREFER_PROJECT` in `settings.gradle.kts` is required — Kotlin/Wasm plugin dynamically adds `nodejs.org/dist` repository, which `PREFER_SETTINGS` blocks

### External Libraries

| Library | Purpose |
|---------|---------|
| Miuix 0.9.4 | KernelSU-style UI components |
| Miuix Blur 0.9.4 | Cross-platform liquid-glass and blur effects |
| Haze 1.7.2 | Supplementary blur for top bars and navigation surfaces |
| Haze 1.7.2 | Blur effects (Android + Compose) |
| MaterialKolor 5.0.1 | Dynamic color theming (Android) |

## Version Pinning

All dependency versions are in `gradle/libs.versions.toml`. Key versions: AGP 9.2.1, Kotlin 2.4.20, Compose Multiplatform 1.12.0, Gradle 9.7.1 (per wrapper). Miuix Blur declares Android API 33; `webui-app` uses a manifest override while retaining the template's minSdk 26.
