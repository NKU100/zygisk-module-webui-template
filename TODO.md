# KernelSU UI 搬运计划

## 阶段 1：核心框架

- [x] Android 端：添加依赖 + 搬运核心框架（Miuix + FloatingBottomBar + 动画 + HazeExt）
- [x] Web 端：wasmJs 添加 miuix/haze/materialKolor 依赖
- [x] Web 端：MainScreen 使用 Miuix 组件（Scaffold + NavigationBar + HorizontalPager）
- [x] Web 端：修正 KernelSU JS API（exec 回调模式、补充 fullScreen/exit/moduleInfo 等）
- [x] Web 端：浏览器环境展示模拟数据
- [x] 合并：提取两端相同代码到 commonMain（BottomTab / PlaceholderPage / LetterIcon / AppTheme / MainScreenState）

## 阶段 2：逐页面搬运（页面 + 依赖组件一起搬，每搬一个编译验证）

- [x] Settings 主页面（Module Enabled / Theme Mode / UI Effects / About 入口）→ 双平台编译验证通过
- [x] Settings → About 子页面（Navigation 3 + NavDisplay 横向滑动转场 + 液态玻璃返回按钮）→ 双平台编译验证通过
- [x] Home 页面（StatusCard + InfoCard + SourceCodeCard + ModuleInfo 注入 + 跨平台 openUrl）→ 双平台编译验证通过
- [x] Apps 页面（应用列表 + SuperSearchBar + MoreCircle 菜单 + 显示系统应用过滤 + 下拉刷新 + 搜索结果显示全部）→ 双平台编译验证通过
- [ ] 最终双平台效果对比

## wasmJs 悬浮底栏对齐进度

### 已完全对齐 ✅
- [x] DampedDragAnimation 类结构/弹簧参数（和 KernelSU 一致）
- [x] DragGestureInspector（inspectDragGestures 双 awaitFirstDown）
- [x] InteractiveHighlight（Skia RuntimeEffect/SkSL 替代 AGSL RuntimeShader）
- [x] panelOffset（拖拽时底栏微偏）
- [x] press/release 缩放动画（独立 scaleX/scaleY 弹簧）
- [x] velocity 形变（拖拽速度→横向拉伸/纵向压缩）
- [x] pressProgress 动画
- [x] MutatorMutex
- [x] snapshotFlow + collectLatest 状态管理
- [x] selectedIndex 类型（`() -> Int` lambda）
- [x] LocalFloatingBottomBarTabScale（按下时 tab 内容放大 1.2x）
- [x] 底栏位置（Scaffold.bottomBar 内，和 Android 一致）

### 平台 API 替换（功能一致）
- [x] `awaitFrame()` → `withFrameNanos {}`（Compose Runtime 跨平台 API）
- [x] `System.currentTimeMillis()` → `kotlin.time.TimeSource.Monotonic`（`@JsFun Date.now()` 高频调用导致 wasmJs 卡死）
- [x] `fastFirstOrNull` → `firstOrNull`（wasmJs 无此扩展，功能一致）
- [x] `fastRoundToInt`/`fastCoerceIn` → `roundToInt`/`coerceIn`（同上）

### 已对齐（通过 Backdrop KMP 改造） ✅
- [x] 背景渲染：wasmJs 也使用 `drawBackdrop`（vibrancy/blur/lens/shadow），与 Android 一致
- [x] Layer 2 隐藏副本：wasmJs 也使用透明 Row + layerBackdrop 供 backdrop 合成
- [x] Indicator 底色：wasmJs 在 drawBackdrop.onDrawSurface 中绘制（含 pressProgress 渐变），与 Android 一致
- [x] 容器 pressProgress 缩放：wasmJs 在 backdrop layerBlock 中实现，与 Android 一致
- [x] Icon/Text 颜色：wasmJs 统一使用 onSurface（靠 backdrop ColorFilter 着色），与 Android 一致
- [x] 设置界面：wasmJs 也显示 Blur Effects / Glass Effect 开关

---

# 构建脚本优化清单

## 🔴 高优先级

- [x] 修复 `tagged-release.yml` 中 `prerelease: true` 应改为 `false`，添加 `name` 和 `workflow_dispatch`
- [x] 统一两个 workflow 的 `upload-artifact` 版本到 v7
- [x] `pre-release.yml` 的 `tags-ignore` 增加 `ci`，防止更新 ci tag 时重复触发

## 🟡 中优先级

- [x] `gradle.properties` 优化：开启 `parallel` 和 `caching`，JVM 内存提升到 4096m，移除 `enableJetifier`
- [x] CI workflow 添加 `concurrency` 并发控制，防止重复运行
- [x] CI changelog 生成优化：pre-release 用 `--unreleased --tag "CI Build"`，tagged 用 `--latest`；生成前先 `git tag -d ci`
- [x] `module/build.gradle.kts` 中 git 命令改为 Provider 懒加载；清理 ndkBuild 注释块
- [x] `webui/build.gradle.kts` 中 `activity-compose` 硬编码版本移入 `libs.versions.toml`

## 🟢 低优先级

- [x] `module.gradle.kts` 中 `var moduleLibName` 改为 `val`
- [x] `build.gradle.kts` 中废弃的 `buildDir` 改为 `layout.buildDirectory`

---

# Backdrop KMP 改造计划

## 阶段 0：构建系统改造 ✅

- [x] `backdrop/build.gradle.kts` 从 Android Library 转为 KMP（`kotlin.multiplatform` + `com.android.kotlin.multiplatform.library` + `compose.multiplatform`）
- [x] 源码目录 `src/main/java/` → `src/androidMain/kotlin/`，删除 `AndroidManifest.xml`
- [x] 根 `build.gradle.kts` / `settings.gradle.kts` / `libs.versions.toml` 适配 KMP
- [x] 主项目 `includeBuild("external/Backdrop")` + `implementation("com.kyant.backdrop:backdrop")`
- [x] Android 编译安装验证通过

## 阶段 1：纯 Compose 文件移入 commonMain（零改动或仅删注解） ✅

- [x] `Backdrop.kt` — interface，纯 Compose API，直接移
- [x] `InverseLayerScope.kt` — GraphicsLayerScope 实现，纯 Compose，直接移
- [x] `LayerRecorder.kt` — 用了 `context(node:)` 语法，API 全是 Compose，直接移
- [x] `Outline.kt` — Canvas.clipOutline 扩展，纯 Compose，直接移
- [x] `ShapeProvider.kt` — Shape 缓存，纯 Compose，直接移
- [x] `backdrops/` 全部 6 文件 — Backdrop, CanvasBackdrop, CombinedBackdrop, EmptyBackdrop, LayerBackdrop, LayerBackdropModifier，全部纯 Compose，直接移
- [x] `Shaders.kt` — 着色器字符串常量，删 `@Language("AGSL")` 注解后移入 commonMain
- [x] `Shadow.kt` — data class，删 `@FloatRange` 注解后移入 commonMain
- [x] `InnerShadow.kt` — data class，删 `@FloatRange` 注解后移入 commonMain
- [x] `Highlight.kt` — data class，删 `@FloatRange` 后移入 commonMain（阶段 4 完成）
- [x] `build.gradle.kts` 添加 `commonMain.dependencies`（compose.runtime/foundation/ui）
- [x] Android + wasmJs 双端安装验证通过

## 阶段 2：expect/actual 拆分核心类型 ✅

- [x] `PlatformRenderEffect` — expect 类型别名（Android → `android.graphics.RenderEffect`，wasmJs → `org.jetbrains.skia.ImageFilter`）
- [x] `RuntimeShaderCache` — commonMain expect sealed interface；androidMain 缓存 `RuntimeShader`；wasmJsMain 缓存 `RuntimeEffect` + `RuntimeShaderBuilder`
- [x] `BackdropEffectScope` — 接口 + 抽象类移入 commonMain，`renderEffect` 类型改为 `PlatformRenderEffect`
- [x] `CreateBackdropEffectScope` — expect/actual 工厂函数，各平台创建带正确 RuntimeShaderCache 的 scope
- [x] `PlatformEffects` — expect/actual `toComposeRenderEffect()` + `isPlatformEffectsSupported`
- [x] `DrawBackdropModifier` — 核心 Element/Node + `drawPlainBackdrop` 移入 commonMain；`drawBackdrop`（带 highlight/shadow）暂留 androidMain
- [x] `build.gradle.kts` 添加 `wasmJs { browser() }` target
- [x] Android + wasmJs 双端编译安装验证通过

## 阶段 3：effects/ 目录拆分 ✅

- [x] `effects/RenderEffect.kt` — Android: `RenderEffect.createChainEffect`；wasmJs: `ImageFilter.makeCompose`
- [x] `effects/Blur.kt` — Android: `RenderEffect.createBlurEffect`；wasmJs: `ImageFilter.makeBlur` + `FilterTileMode`
- [x] `effects/ColorFilter.kt` — Android: `android.graphics.ColorMatrix/ColorFilter/RenderEffect`；wasmJs: `org.jetbrains.skia.ColorFilter/ColorMatrix/ImageFilter`
- [x] `effects/Lens.kt` — Android: `RuntimeShader` + `RenderEffect.createRuntimeShaderEffect`；wasmJs: `RuntimeShaderBuilder` + `ImageFilter.makeRuntimeShader`；`fastCoerceAtLeast/Most` → `coerceAtLeast/Most`
- [x] Android + wasmJs 双端编译安装验证通过

## 阶段 4：highlight/ 和 shadow/ 拆分 ✅

- [x] `HighlightStyle.kt` — 接口 + Plain + getCornerRadii 移入 commonMain；Default/Ambient 用 expect 工厂（Android 用 RuntimeShader，wasmJs 用 SkSL RuntimeShaderBuilder）
- [x] `HighlightModifier.kt` — 移入 commonMain，用 `applyBlurMaskFilter` + `isPlatformEffectsSupported` + `coerceAtMost`
- [x] `Highlight.kt` — 移入 commonMain
- [x] `ShadowModifier.kt` — 移入 commonMain，用 `applyBlurMaskFilter`
- [x] `InnerShadowModifier.kt` — 移入 commonMain，用 `isPlatformEffectsSupported`（`BlurEffect` 本身是跨平台 Compose API）
- [x] `PlatformMaskFilter.kt` — expect/actual（Android: `BlurMaskFilter`；wasmJs: `MaskFilter.makeBlur`）
- [x] `DrawBackdropExt.kt`（`drawBackdrop` 函数）— 移入 commonMain（所有依赖已跨平台）
- [x] Android + wasmJs 双端编译安装验证通过

## 阶段 5：收尾 ✅

- [x] `build.gradle.kts` 添加 `wasmJs { browser() }` target + commonMain 依赖（已在阶段 2 完成）
- [x] 编译验证 Android + wasmJs 双端通过
- [x] Push Backdrop 子仓库到远端
- [x] 提交主仓库（更新子模块指针 + TODO.md）


---

# KSU 对齐待办（按优先级）

## 🔴 High

- [x] `rememberContentReady` — MainScreen 进入动画期间 `beyondViewportPageCount = 0`，动画结束后变 3
- [x] `isCurrentPage` 条件渲染 — HorizontalPager 每个 page 用 `settledPage` 判断是否渲染内容
- [x] `LocalMainPagerState` CompositionLocal — 在 MainScreenImpl 提供，子页面可通过它跳 tab
- [x] StatusCard 深色模式颜色适配 — 深色下背景/icon 颜色应换为深色友好值

## 🟡 Medium

- [x] `MainScreenBackHandler` — 返回键不在 tab 0 时先跳回 tab 0，而非直接退出
- [ ] `rememberViewModelStoreNavEntryDecorator` — NavDisplay entryDecorators 补充 ViewModel 作用域绑定（KMP commonMain 暂不支持，跳过）
- [x] `scrollEndHaptic()` — HomePage / SettingsPage / AppsPage 的 LazyColumn 补充滚动末端震动反馈
- [x] `contentWindowInsets` 横屏刘海屏 — HomePage / SettingsPage Scaffold 补充 displayCutout 处理

## 🟢 Low（平台差异，已确认不需要修改）

- [x] `isDark` 参数（FloatingBottomBar）— KSU 用 `isInDarkTheme()` CompositionLocal，我们用显式参数；KMP 适配，功能等价，无需改
- [x] `fastCoerceIn/fastRoundToInt`（FloatingBottomBar）— wasmJs 无此扩展，已在 InteractiveHighlight 等处统一降级，无需改

