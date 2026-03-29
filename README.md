# Zygisk Module WebUI Template

A Zygisk module template with **Compose Multiplatform** WebUI, based on [zygisk-module-template][zygisk-module-template].

## Features

- **Zygisk C++ module** with CMake build system and custom libcxx
- **Compose Multiplatform WebUI** — single codebase, dual platform:
  - **Web (Wasm)**: Renders in KernelSU manager's WebView via `webroot/`
  - **Android APK**: Standalone config app for Magisk users (no WebUI support)
- **Miuix UI framework** — KernelSU-style Material You theme on both platforms
  - Android: FloatingBottomBar with liquid glass (backdrop), haze blur, AGSL shader highlights
  - Web: Miuix Scaffold + NavigationBar (standard bottom bar)
- **KernelSU API abstraction** via `expect/actual` pattern (`PlatformBridge`)
  - Full v3.0.2 API: exec (async callback), toast, listPackages, getPackagesInfo, moduleInfo, fullScreen, enableEdgeToEdge, exit
  - Browser mock data for development preview
- **JSON-based configuration** with kotlinx.serialization
- Supports **Magisk** and **KernelSU**
- GitHub Actions CI/CD with auto-release

## Project Structure

```
├── module/                          # Zygisk native module
│   ├── src/main/cpp/                # C++ source (Zygisk API v4)
│   └── template/                    # Magisk module template files
├── webui/                           # Compose Multiplatform UI
│   └── src/
│       ├── commonMain/              # Shared data & expect declarations
│       │   ├── data/               # ModuleConfig, ConfigRepository
│       │   ├── platform/           # expect PlatformBridge
│       │   └── ui/                 # expect MainScreen, AppTheme, shared components
│       ├── androidMain/            # Android target (Miuix + FloatingBottomBar)
│       │   └── ui/
│       │       ├── animation/      # DampedDragAnimation, InteractiveHighlight
│       │       ├── component/      # FloatingBottomBar (liquid glass)
│       │       ├── modifier/       # DragGestureInspector
│       │       ├── screen/         # MainScreen (Miuix Scaffold + Pager)
│       │       ├── theme/          # MiuixTheme
│       │       └── util/           # HazeExt
│       └── wasmJsMain/             # Web target (Miuix + NavigationBar)
│           ├── platform/           # KernelSU JS API bridge (v3.0.2)
│           └── ui/                 # MainScreen (Miuix Scaffold + Pager)
├── module.gradle.kts                # Module metadata (id, name, author)
└── build.gradle.kts                 # Global build config
```

## Usage

### 1. Configure module metadata

Edit [module.gradle.kts](./module.gradle.kts):

```kotlin
val moduleId by extra("zygisk_sample")
val moduleName by extra("Zygisk Module Sample")
val moduleAuthor by extra("NKU100")
val moduleDesc by extra("A sample module for zygisk")
val moduleApplicationId by extra("io.github.nku100.zygisk.sample")
```

### 2. Write native code

Edit `module/src/main/cpp/example.cpp` with your Zygisk logic.

### 3. Customize WebUI

Edit files under `webui/src/` to build your configuration UI:
- `commonMain/data/ModuleConfig.kt` — add config fields (auto-serialized to JSON)
- `commonMain/platform/PlatformBridge.kt` — add `expect` platform APIs
- `androidMain/ui/screen/` — Android UI with Miuix components
- `wasmJsMain/ui/screen/` — Web UI with Miuix components

### 4. Build

```bash
# Build module zip (includes WebUI)
./gradlew :module:zipRelease

# Build WebUI only (production)
./gradlew :webui:buildWebUI

# Build Android APK
./gradlew :webui:assembleDebug

# Dev server (browser preview at http://localhost:8080)
./gradlew :webui:wasmJsBrowserDevelopmentRun
```

The module zip will be generated under `module/release/`.

### 5. Install

```bash
# KernelSU
./gradlew :module:installKsuRelease

# Magisk
./gradlew :module:installMagiskRelease

# Hot-reload WebUI to device (dev)
./gradlew :webui:install
```

## Extending

- **Add config fields**: Edit `ModuleConfig.kt`, they auto-serialize to JSON
- **Add platform APIs**: Add `expect` methods in `PlatformBridge.kt`, implement in `wasmJsMain` and `androidMain`
- **Add UI pages**: Create new `@Composable` functions in each platform's `screen/` directory

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Native module | C++20, CMake, Zygisk API v4 |
| UI framework | Compose Multiplatform 1.10.3 |
| Language | Kotlin 2.3.20 |
| Web target | Kotlin/Wasm |
| Android UI | Miuix 0.8.8 + Backdrop 1.0.6 + Capsule 2.1.3 + Haze 1.7.2 |
| Web UI | Miuix 0.8.8 + Haze 1.7.2 + MaterialKolor 4.1.1 |
| Serialization | kotlinx.serialization (JSON) |
| Build system | Gradle 8.14, AGP 8.11 |

## See also

- [zygisk-module-sample](https://github.com/topjohnwu/zygisk-module-sample)
- [KernelSU Module WebUI](https://kernelsu.org/guide/module-webui.html)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)
- [Miuix](https://compose-miuix-ui.github.io/miuix/)
- [KernelSU JS API](https://www.npmjs.com/package/kernelsu)

[zygisk-module-template]: https://github.com/5ec1cff/zygisk-module-template
