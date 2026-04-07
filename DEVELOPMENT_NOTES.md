# Development Notes

This document records the development history, design decisions, and implementation details of this project.

---

## KsuWebUIStandalone API Alignment

> Repo: [NKU100/KsuWebUIStandalone](https://github.com/NKU100/KsuWebUIStandalone) (fork of [5ec1cff/KsuWebUIStandalone](https://github.com/5ec1cff/KsuWebUIStandalone))

KsuWebUIStandalone allows Magisk users (without KernelSU) to run a module's WebUI standalone. This section documents the work done to align its JS bridge API with the official [KernelSU manager](https://github.com/tiann/KernelSU), using the [kernelsu npm package v3.0.2](https://www.npmjs.com/package/kernelsu) as the authoritative API reference.

### New Files

| File | Source | Description |
|------|--------|-------------|
| `Insets.kt` | Ported from KernelSU manager (originally from [MMRLApp/WebUI-X-Portable](https://github.com/MMRLApp/WebUI-X-Portable)) | Data class for window insets (top/bottom/left/right). Provides `.css` property for injecting CSS variables (`--safe-area-inset-*`, `--window-inset-*`, `--f7-safe-area-*`) and `.js` property for dynamic injection via `evaluateJavascript`. |
| `AppIconUtil.kt` | Ported from KernelSU manager | LRU-cached app icon loader (max 200 icons). Provides `loadAppIconSync(context, packageName, sizePx): Bitmap?`. Used to serve `ksu://icon/{packageName}` requests. |

### Modified Files

**`WebViewInterface.kt`**

Added `activity: WebUIActivity` constructor parameter. New and enhanced `@JavascriptInterface` methods:

| Method | Description |
|--------|-------------|
| `enableEdgeToEdge(enable: Boolean)` | Delegates to `activity.setInsetsEnabled()` to toggle edge-to-edge mode |
| `fullScreen(enable: Boolean)` | Enhanced: now also calls `enableEdgeToEdge(enable)` in addition to hiding system UI |
| `moduleInfo(): String` | Enhanced: reads `module.prop` via root shell and checks `disable`/`update`/`remove` marker files; returns full JSON including `enabled`, `update`, `remove` fields |
| `listPackages(type: String): String` | Returns JSON array of package names filtered by `"user"` / `"system"` / `"all"` |
| `getPackagesInfo(packageNamesJson: String): String` | Returns JSON array with `packageName`, `versionName`, `versionCode`, `appLabel`, `isSystem`, `uid` for each requested package |
| `exit()` | Calls `activity.finish()` on UI thread |

**`RemoteFsPathHandler.java`**

Added `InsetsSupplier` and `OnInsetsRequestedListener` constructor parameters. New virtual path handling in `handle()`:

| Path | Behavior |
|------|----------|
| `internal/insets.css` | Triggers `onInsetsRequestedListener` (enables edge-to-edge), returns CSS variables from current `Insets` |
| `internal/colors.css` | Returns empty CSS (intentional — KernelSU manager implements this via `MonetColorsProvider` which reads from Compose `MaterialTheme`/`MiuixTheme` at runtime; KsuWebUIStandalone has no Compose dependency, so the WebUI module is expected to handle its own theming) |

**`WebUIActivity.kt`**

- Added `isInsetsEnabled` / `currentInsets` state fields
- Added `setInsetsEnabled(enable)` / `updateInsetsMode()` for edge-to-edge management
- `shouldInterceptRequest`: intercepts `ksu://icon/{packageName}` scheme, loads icon via `AppIconUtil`, returns PNG response
- `doUpdateVisitedHistory`: re-injects `currentInsets.js` on navigation when insets are enabled
- Passes `this` as `WebUIActivity` to `WebViewInterface` constructor

### Verification

All APIs were verified on an emulator (Magisk 30700 + root granted via `magisk --sqlite`):

| API | Result |
|-----|--------|
| `moduleInfo()` | Returns full JSON with `id`, `moduleDir`, `updateJson`, `enabled`, `update`, `remove` |
| `enableEdgeToEdge()` | Toggles edge-to-edge mode correctly |
| `listPackages('user'/'system'/'all')` | Returns correct package name arrays |
| `getPackagesInfo([...])` | Returns correct metadata including `appLabel`, `isSystem`, `uid` |
| `exit()` | Closes the activity |
| `ksu://icon/{packageName}` | Returns PNG bitmap (verified with `com.android.settings` icon) |
| `internal/insets.css` | Returns CSS variables with correct safe area values |
| `internal/colors.css` | Returns empty string (expected) |

---

## KernelSU UI Port (Phases 1–2)

Ported the KernelSU manager UI to Compose Multiplatform (Android + Wasm), including:

- Miuix UI framework + FloatingBottomBar with liquid glass backdrop, haze blur, AGSL/SkSL shader highlights
- DampedDragAnimation (spring physics, velocity deformation), InteractiveHighlight
- SuperSearchBar, SearchStatus state machine, StatusTag
- ViewModel architecture with `MainPagerState`, `rememberContentReady`, `LocalMainPagerState`
- All 4 pages: Home, Apps, Settings, About
- `AppIconImage` expect/actual: Android uses `AppIconLoader + AppIconCache`; wasmJs fetches `ksu://icon/<pkg>` and decodes via Skia; falls back to `LetterIcon`

### KSU Alignment Details

- `rememberContentReady` — deferred pager rendering during enter animation (`beyondViewportPageCount = 0` → 3)
- `isCurrentPage` conditional rendering via `settledPage`
- `LocalMainPagerState` CompositionLocal for cross-tab navigation
- `MainScreenBackHandler` — back key returns to tab 0 before exiting
- `scrollEndHaptic()` on LazyColumn scroll end
- `contentWindowInsets` display cutout support for HomePage / SettingsPage
- StatusCard dark mode color adaptation

### Platform API Substitutions (wasmJs)

| Android | wasmJs | Reason |
|---------|--------|--------|
| `awaitFrame()` | `withFrameNanos {}` | Cross-platform Compose Runtime API |
| `System.currentTimeMillis()` | `kotlin.time.TimeSource.Monotonic` (+ `@JsFun Date.now()`) | `Date.now()` at high frequency caused wasmJs hang |
| `fastFirstOrNull` / `fastRoundToInt` / `fastCoerceIn` | `firstOrNull` / `roundToInt` / `coerceIn` | Not available in wasmJs |
| `@Language("AGSL")` annotation | removed | Kotlin annotation not available in commonMain |
| `RuntimeShader` + `RenderEffect` | `RuntimeShaderBuilder` + `ImageFilter` (Skia) | Android vs wasmJs graphics API |

---

## Backdrop KMP Migration

Migrated the [Backdrop](https://github.com/KyantStudio/Backdrop) library (originally Android-only) to Kotlin Multiplatform to support wasmJs.

**Stages:**
1. Build system: Android Library → KMP (`kotlin.multiplatform` + `com.android.kotlin.multiplatform.library`)
2. Pure Compose files → `commonMain` (zero or minimal changes)
3. `expect/actual` for `PlatformRenderEffect`, `RuntimeShaderCache`, `BackdropEffectScope`, `PlatformEffects`
4. `effects/` directory: `RenderEffect`, `Blur`, `ColorFilter`, `Lens`
5. `highlight/` and `shadow/`: `HighlightStyle`, `HighlightModifier`, `ShadowModifier`, `InnerShadowModifier`, `PlatformMaskFilter`

The migrated library is included as a git submodule at `external/Backdrop`.

---

## Build Script Optimizations

- Fixed `tagged-release.yml`: `prerelease: true` → `false`, added `name` and `workflow_dispatch`
- Unified `upload-artifact` to v7 across workflows
- `pre-release.yml` `tags-ignore` adds `ci` to prevent re-triggering on CI tag updates
- `gradle.properties`: enabled `parallel` + `caching`, JVM heap → 4096m, removed `enableJetifier`
- CI `concurrency` control added
- CI changelog: pre-release uses `--unreleased --tag "CI Build"`, tagged uses `--latest`
- `module/build.gradle.kts`: git commands as Provider (lazy); removed ndkBuild comment block
- `webui/build.gradle.kts`: `activity-compose` version moved to `libs.versions.toml`
- `module.gradle.kts`: `var moduleLibName` → `val`
- `build.gradle.kts`: deprecated `buildDir` → `layout.buildDirectory`

---

## Remaining / Known Issues

- **wasmJs icon fallback**: When running under KsuWebUIStandalone (no `ksu.getPackagesInfo`), the `AppIconImage` wasmJs implementation falls back to `LetterIcon`. A proper fallback via `exec "pm list packages"` + icon fetch is not yet implemented.
- **`rememberViewModelStoreNavEntryDecorator`**: KernelSU manager binds ViewModel scope to NavDisplay entries via this decorator. KMP `commonMain` does not support this API yet; skipped intentionally.
- **`internal/colors.css`** in KsuWebUIStandalone: Always returns empty. WebUI modules that rely on this for dynamic theming will not receive Material You colors when running under KsuWebUIStandalone.
