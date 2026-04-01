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
- [ ] Settings → About 子页面 → 编译验证
- [ ] Home 页面（模块状态、版本信息 + StatusTag 等依赖组件）→ 编译验证
- [ ] Apps 页面（SuperUser / 应用列表 + SuperSearchBar + Dialog 等依赖组件）→ 编译验证
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

### 无法对齐（平台限制）❌
- [ ] 背景渲染：Android 用 `drawBackdrop`（vibrancy/blur/lens/shadow），wasmJs 用 haze + background
- [ ] Layer 2 隐藏副本：Android 用透明 Row + layerBackdrop 供 backdrop 合成，wasmJs 无需
- [ ] Indicator 底色：Android 在 drawBackdrop.onDrawSurface 中绘制（含 pressProgress 渐变），wasmJs 用固定 background
- [ ] 容器 pressProgress 缩放：Android 在 backdrop layerBlock 中实现，wasmJs 无 backdrop
- [ ] Icon/Text 颜色：Android 固定 onSurface（靠 backdrop ColorFilter 着色），wasmJs 用条件 primary/onSurface

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
