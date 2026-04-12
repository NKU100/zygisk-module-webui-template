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

## CJK Font Loading

Compose for Web (Skia/CanvasKit) runs in a WASM sandbox with no access to system fonts, so CJK glyphs render as tofu (□) unless a font is explicitly loaded.

### Evolution

| Version | Font source | Loading mechanism | Size |
|---------|-------------|-------------------|------|
| v1 | System TTC via `post-fs-data.sh` symlink into `webroot/` | JS `fetch()` → `ArrayBuffer` → manual WASM memory copy → `Font(identity, bytes)` | ~16 MB (full NotoSansCJK-Regular.ttc) |
| v2 (current) | WOFF2 GB2312 subset in `composeResources/font/` | `Res.readBytes()` → `Font(identity, bytes)` + `fontFamilyResolver.preload()` | ~969 KB |

### Current Implementation (`main.kt`)

1. `Res.readBytes("font/noto_sans_sc_regular.woff2")` reads the font bytes via Compose Resources
2. `Font("NotoSansSC", bytes)` registers the font with Skia by identity name
3. `fontFamilyResolver.preload(FontFamily(...))` makes Skia pick it up as a CJK fallback
4. `fontsReady` gate prevents `App()` from rendering until the font is loaded, avoiding tofu flash

### Key Findings

- **`preloadFont(Res.font.xxx)` does NOT work as a CJK fallback**: the returned `Font` object wrapped in `FontFamily` and `preload()`-ed does not register with Skia's fallback chain. Only `Font(identity, bytes)` (from `androidx.compose.ui.text.platform`) properly registers as a fallback.
- **`fontsReady` gate is required**: without it, `App()` renders immediately with the built-in Latin-only font, causing a visible tofu→CJK flash.
- **WOFF2 format works**: Skia/CanvasKit on wasmJs can decode WOFF2 directly; no need for TTF/OTF conversion.
- The font subset covers GB2312 (~6,763 characters), sufficient for the UI's Chinese text. If additional characters are needed, regenerate the subset with a broader character set.

---

---

## wasmJs Back Navigation (Hash Guard)

在 KernelSU/KsuWebUIStandalone 的 WebView 中，侧滑返回时 WebView 检查 `canGoBack()`：为 true 调用 `goBack()`，为 false 直接关闭 Activity。Compose WASM 是单页应用，默认 `canGoBack()` 始终为 false，侧滑永远直接退出。

### 方案

利用 `location.hash` 创建真实导航记录（WebView 的 `canGoBack()` 识别 hash 变化但不识别 `pushState`）。离开退出点（Home tab + 无路由页）时预先 push 5 层 hash guard（`#g1..#g5`），侧滑时 `goBack()` 逐层消费并触发 `hashchange` 事件，在 handler 中执行 Compose 侧返回。

### 文件

| 文件 | 说明 |
|------|------|
| `commonMain/.../platform/BrowserHistorySync.kt` | expect 声明 |
| `androidMain/.../platform/BrowserHistorySync.kt` | no-op（Android 用原生 BackHandler） |
| `wasmJsMain/.../platform/BrowserHistorySync.kt` | hash guard 实现 |
| `commonMain/.../ui/App.kt` | 持有 mainPagerStateHolder，调用 BrowserHistorySync |
| `commonMain/.../ui/screen/MainScreenImpl.kt` | 通过 onPagerStateReady 回调向上传递 pagerState |

### 关键技术点

- **为什么不用 pushState**：Android WebView 的 `canGoBack()` 只追踪真实导航（loadUrl、hash 变化），不追踪 History API 的 `pushState`。
- **为什么预先 push 多层**：在 `hashchange` handler 中同步或异步 re-push hash，WebView 的 `canGoBack()` 来不及更新，第二次侧滑仍然退出。预先 push 足够多层避免了 re-push。
- **防级联 hashchange**：`pushHashGuards` 的 for 循环触发 N 次异步 hashchange。用 JS 全局 `window.__composeSyncing` flag + `setTimeout(0)` 延迟重置，在 handler 中检查 flag 跳过自触发事件。
- **BrowserHistorySync 放在 App 层级**：不能放在 MainScreen 内部，因为 push 路由（AppProfile/About）时 MainScreen unmount，listener 会丢失。通过 `rememberUpdatedState` 引用最新的 navigator 和 pagerState。

### 已知限制

- Guard depth 固定为 5 层，深层嵌套导航（>5 层）会耗尽 guard
- 回到首页时 `clearGuards` 的 `history.go(-n)` 产生的级联 hashchange 未被完全拦截（`setTimeout(0)` 不覆盖 `history.go` 的异步事件），但因 depth=0 时逻辑不执行任何操作，实际无害

## Remaining / Known Issues

- **wasmJs icon fallback**: When running under KsuWebUIStandalone (no `ksu.getPackagesInfo`), the `AppIconImage` wasmJs implementation falls back to `LetterIcon`. A proper fallback via `exec "pm list packages"` + icon fetch is not yet implemented.
- **`rememberViewModelStoreNavEntryDecorator`**: KernelSU manager binds ViewModel scope to NavDisplay entries via this decorator. KMP `commonMain` does not support this API yet; skipped intentionally.
- **`internal/colors.css`** in KsuWebUIStandalone: Always returns empty. WebUI modules that rely on this for dynamic theming will not receive Material You colors when running under KsuWebUIStandalone.

---

## UI 优化待办（P3）

以下在跨平台 UI 审查中识别，优先级较低，记录备忘。

### ~~1. 统一 wasmStatusBarPadding 处理~~ ✅ 已完成

已在 `InsetsExt.kt` 中封装 `topBarModifier()`、`topBarInsetsPadding()`、`topBarDefaultWindowInsetsPadding`，6 个页面统一使用。

### 2. 语义颜色集中管理

**现状**：StatusCard 和日志级别使用硬编码颜色值，分散在各页面中：

```
HomePage.kt — StatusCard 卡片背景（4 色）+ 状态图标（2 色）
  Color(0xFF1A3825) / Color(0xFFDFFAE4) / Color(0xFF310808) / Color(0xFFF8E2E2)
  Color(0xFF36D167) / Color(0xFFF72727)

LogsPage.kt — levelColor() 日志级别（5 色）
  DEBUG: Color(0xFF4DA6FF)  INFO: Color(0xFF4CAF50)  WARN: Color(0xFFFFA000)
  ERROR: Color(0xFFF44336)  FATAL: Color(0xFFD50000)
```

目前已通过 `isDark` 区分了深色/浅色变体，功能正确。但如果后续要支持 Monet 动态取色或自定义主题，这些硬编码值需要统一管理。

**建议方案**：在 `ui/theme/` 下新建 `SemanticColors.kt`，定义 `CompositionLocal` 提供语义颜色（如 `statusEnabled`、`statusDisabled`、`logDebug` 等），在 `AppTheme` 中根据深浅色模式注入对应颜色值。

**风险**：改动范围较广，需要同时修改 Theme 层和所有使用处；且当前作为模板项目，硬编码颜色的可读性反而更直观。

**预估工作量**：2 小时
