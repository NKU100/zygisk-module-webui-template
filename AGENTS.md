# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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

No test suite exists. Verification is manual (browser preview or device install).

## Verification Workflow

```bash
# Build and install both targets to device
./gradlew :webui-app:installDebug :webui:install

# Launch Android side
adb shell am start -n io.github.nku100.zygisk.sample/io.github.nku100.webui.MainActivity

# Launch wasmJs side (requires root + KsuWebUIStandalone)
adb shell am start -n io.github.a13e300.ksuwebui/.WebUIActivity \
  --es id zygisk_sample --es name "Zygisk Module WebUI"
```

The emulator must be rooted (Magisk) for full wasmJs testing. Root AVD instructions are in `.codebuddy/skills/root-avd/SKILL.md`.

## Architecture

### Three Subprojects

| Project | Role |
|---------|------|
| `:module` | Zygisk native C++ module + Magisk/KSU packaging. CMake builds `libsample.so`. Shell scripts handle install/verify. |
| `:webui` | Kotlin Multiplatform library — the main codebase. Targets: **wasmJs** and **androidLibrary**. |
| `:webui-app` | Minimal Android APK hosting `:webui` for Magisk users. |

### Composite Builds

`external/Capsule` (G2 smooth corners) and `external/Backdrop` (liquid glass) are git submodules included via `includeBuild` in `settings.gradle.kts`. They are KMP libraries, not regular dependencies.

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

- **ViewModel**: `MainViewModel` with `StateFlow<MainUiState>`, lifecycle-viewmodel 2.9.0
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
| Miuix 0.8.8 | KernelSU-style UI components |
| Capsule (submodule) | G2 continuous smooth rounded corners (KMP) |
| Backdrop (submodule) | Liquid glass / blur effects (KMP) |
| Haze 1.7.2 | Blur effects (Android + Compose) |
| MaterialKolor 4.1.1 | Dynamic color theming (Android) |

## Version Pinning

All dependency versions are in `gradle/libs.versions.toml`. Key versions: AGP 9.0.0, Kotlin 2.3.20, Compose Multiplatform 1.10.3, Gradle 9.4.0 (per wrapper).
