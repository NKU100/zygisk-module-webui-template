# Zygisk Module WebUI Template

A Zygisk module template with **Compose Multiplatform** WebUI, based on [zygisk-module-template][zygisk-module-template].

## Features

- **Zygisk C++ module** with CMake build system and custom libcxx
- **Compose Multiplatform WebUI** — single codebase, dual platform:
  - **Web (Wasm)**: Renders in KernelSU manager's WebView via `webroot/`
  - **Android APK**: Standalone config app for Magisk users (no WebUI support)
- **Miuix UI framework** — KernelSU-style UI on both platforms, closely aligned with [KernelSU manager](https://github.com/tiann/KernelSU)
  - FloatingBottomBar with liquid glass (backdrop), haze blur, AGSL/SkSL shader highlights
  - Shared: DampedDragAnimation (spring physics, velocity deformation), InteractiveHighlight
  - SuperSearchBar, SearchStatus state machine, StatusTag — ported from KernelSU
- **[Capsule][capsule]** — G2 continuous smooth corners (cross-platform fork, included as git submodule)
- **KernelSU API abstraction** via `expect/actual` pattern (`PlatformBridge`)
  - Full v3.0.2 API: exec (async callback), toast, listPackages, getPackagesInfo, moduleInfo, fullScreen, enableEdgeToEdge, exit
  - Browser mock data for development preview
  - All APIs also available under **[KsuWebUIStandalone](https://github.com/NKU100/KsuWebUIStandalone)** (Magisk users without KernelSU can run the WebUI standalone)
- **ViewModel architecture** (Compose Multiplatform lifecycle 2.9.0)
  - `MainViewModel` with `viewModelScope`, `StateFlow<MainUiState>`, auto-managed search debounce
  - `MainPagerState` — cross-tab navigation with `isNavigating` guard (ported from KernelSU)
  - `LocalMainPagerState` CompositionLocal for child pages to trigger tab navigation
  - `rememberContentReady` — deferred pager rendering during enter animation
- **Apps page** fully aligned with KernelSU SuperUserMiuix
  - System app filter toggle, SuperSearchBar with full expand/collapse animation
  - Pull-to-refresh, IME-aware bottom padding, `contentWindowInsets` display cutout support
  - App icons via `AppIconImage` (expect/actual): Android uses `AppIconLoader` + `AppIconCache` (LRU, Semaphore, Hardware Bitmap); wasmJs fetches `ksu://icon/<pkg>` and decodes via Skia; falls back to `LetterIcon` when unavailable
- **App Profile page** — per-app settings with targeted toggle, log level, note, and app management actions (launch / force stop / restart)
- **JSON-based configuration** with kotlinx.serialization
- **Bundled CJK font** — NotoSansSC WOFF2 subset (~969 KB, GB2312 coverage) loaded via Compose Resources; no runtime symlink or system font dependency
- Supports **Magisk** and **KernelSU**
- GitHub Actions CI/CD with auto-release

## Project Structure

```
├── module/                          # Zygisk native module
│   ├── src/main/cpp/                # C++ source (Zygisk API v4)
│   └── template/                    # Magisk module template files
├── webui/                           # Compose Multiplatform UI
│   └── src/
│       ├── commonMain/              # Shared UI, data, platform expect declarations
│       │   ├── composeResources/
│       │   │   └── font/           # CJK font (NotoSansSC WOFF2 GB2312 subset)
│       │   ├── data/               # ModuleConfig, ConfigRepository
│       │   ├── platform/           # expect PlatformBridge, awaitNextFrame, PlatformBackHandler
│       │   └── ui/
│       │       ├── animation/      # DampedDragAnimation, InteractiveHighlight
│       │       ├── component/      # FloatingBottomBar, SuperSearchBar, SearchStatus, StatusTag
│       │       ├── screen/         # MainViewModel, MainPagerState, all page composables
│       │       │   ├── home/       # HomePage
│       │       │   ├── apps/       # AppsPage (+ AppsViewModel search logic)
│       │       │   └── settings/   # SettingsPage, AboutPage
│       │       ├── theme/          # AppTheme, ThemeMode, isSystemDarkTheme
│       │       └── util/           # DeferredContent (rememberContentReady), defaultHazeEffect
│       ├── androidMain/            # Android target
│       │   ├── platform/           # PlatformBridge.android, PlatformBackHandler.android
│       │   └── ui/
│       │       ├── component/      # AppIconImage.android (AppIconLoader + AppIconCache)
│       │       ├── modifier/       # DragGestureInspector (AGSL)
│       │       ├── util/           # AppIconCache (LRU, Semaphore, Hardware Bitmap)
│       │       └── theme/          # MaterialKolor dynamic color
│       └── wasmJsMain/             # Web (Wasm) target
│           ├── platform/           # KernelSU JS API bridge (v3.0.2), PlatformBackHandler.wasmJs
│           └── ui/
│               ├── component/      # AppIconImage.wasmJs (ksu://icon/ + Skia decode)
│               └── modifier/       # DragGestureInspector (SkSL)
├── external/
│   └── Capsule/                     # Cross-platform Capsule library (git submodule)
├── module.gradle.kts                # Module metadata (id, name, author)
└── build.gradle.kts                 # Global build config
```

## Usage

### 1. Clone with submodules

```bash
git clone --recursive https://github.com/NKU100/zygisk-module-webui-template.git
# Or if already cloned:
git submodule update --init --recursive
```

### 2. Configure module metadata

Edit [module.gradle.kts](./module.gradle.kts):

```kotlin
val moduleId by extra("zygisk_sample")
val moduleName by extra("Zygisk Module Sample")
val moduleAuthor by extra("NKU100")
val moduleDesc by extra("A sample module for zygisk")
val moduleApplicationId by extra("io.github.nku100.zygisk.sample")
```

### 3. Write native code

Edit `module/src/main/cpp/example.cpp` with your Zygisk logic.

### 4. Customize WebUI

Edit files under `webui/src/commonMain/` to build your configuration UI:

- `data/ModuleConfig.kt` — add config fields (auto-serialized to JSON)
- `platform/PlatformBridge.kt` — add `expect` platform APIs
- `ui/screen/MainViewModel.kt` — add business logic, state fields
- `ui/screen/home/HomePage.kt` — Home tab
- `ui/screen/apps/AppsPage.kt` — Apps tab (target package selection)
- `ui/screen/settings/SettingsPage.kt` — Settings tab

### 5. Build

```bash
# Build module zip (includes WebUI)
./gradlew :module:zipRelease

# Build WebUI only (production)
./gradlew :webui:buildWebUI

# Build Android APK
./gradlew :webui:assembleDebug

# Dev server (browser preview at http://localhost:8080)
./gradlew :webui:wasmJsBrowserDevelopmentRun

# Install both Android APK and WebUI to device
./gradlew :webui:installDebug :webui:install
```

The module zip will be generated under `module/release/`.

### 6. Install

```bash
# KernelSU
./gradlew :module:installKsuRelease

# Magisk
./gradlew :module:installMagiskRelease

# Hot-reload WebUI to device (dev)
./gradlew :webui:install
```

## Extending

- **Add config fields**: Edit `ModuleConfig.kt`, add corresponding state/actions in `MainViewModel.kt`
- **Add platform APIs**: Add `expect` methods in `PlatformBridge.kt`, implement in `wasmJsMain` and `androidMain`
- **Add UI pages**: Add a new entry to `BottomTab`, create a page composable in `ui/screen/`, wire it up in `PlaceholderPage.kt`
- **Add ViewModel state**: Extend `MainUiState` and add methods in `MainViewModel`

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Native module | C++20, CMake, Zygisk API v4 |
| UI framework | Compose Multiplatform 1.10.3 |
| Language | Kotlin 2.3.20 |
| Web target | Kotlin/Wasm |
| UI library | Miuix 0.8.8 |
| Glass effects | Backdrop 1.0.6 + Haze 1.7.2 |
| Smooth corners | [Capsule][capsule] — G2 continuous rounded rectangles (cross-platform) |
| Architecture | ViewModel (lifecycle-viewmodel 2.9.0) + StateFlow + Navigation 3 |
| Serialization | kotlinx.serialization (JSON) |
| Build system | Gradle 9.3, AGP 9.0 |

## See also

- [zygisk-module-sample](https://github.com/topjohnwu/zygisk-module-sample)
- [KernelSU Module WebUI](https://kernelsu.org/guide/module-webui.html)
- [KsuWebUIStandalone](https://github.com/NKU100/KsuWebUIStandalone) — run module WebUI standalone on Magisk (full KSU JS API aligned)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)
- [Miuix](https://compose-miuix-ui.github.io/miuix/)
- [KernelSU JS API](https://www.npmjs.com/package/kernelsu)
- [Capsule (KMP fork)][capsule]

[zygisk-module-template]: https://github.com/5ec1cff/zygisk-module-template
[capsule]: https://github.com/NKU100/Capsule
