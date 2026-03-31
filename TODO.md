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
