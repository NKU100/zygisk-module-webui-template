# KernelSU UI 搬运计划

## 阶段 1：核心框架

- [x] Android 端：添加依赖 + 搬运核心框架（Miuix + FloatingBottomBar + 动画 + HazeExt）
- [x] Web 端：wasmJs 添加 miuix/haze/materialKolor 依赖
- [x] Web 端：MainScreen 使用 Miuix 组件（Scaffold + NavigationBar + HorizontalPager）
- [x] Web 端：修正 KernelSU JS API（exec 回调模式、补充 fullScreen/exit/moduleInfo 等）
- [x] Web 端：浏览器环境展示模拟数据
- [x] 合并：提取两端相同代码到 commonMain（BottomTab / PlaceholderPage / LetterIcon / AppTheme / MainScreenState）

## 阶段 2：搬运页面屏幕

- [ ] Home 页面（模块状态、版本信息）
- [ ] Apps 页面（SuperUser / 应用列表）
- [ ] Settings 页面（主题、ColorPalette 配色）
- [ ] 双平台验证

## 阶段 3：搬运组件 + 辅助

- [ ] SuperSearchBar（搜索栏）
- [ ] Dialog（各类对话框）
- [ ] 辅助组件（StatusTag 等）
- [ ] 双平台验证 + 最终效果对比

---

# 构建脚本优化清单

## 🔴 高优先级

- [ ] 修复 `tagged-release.yml` 中 `prerelease: true` 应改为 `false`
- [ ] 统一两个 workflow 的 `upload-artifact` 版本（pre-release 用 v6，tagged-release 用 v4）

## 🟡 中优先级

- [ ] `gradle.properties` 开启 `org.gradle.parallel=true` 和 `org.gradle.caching=true`
- [ ] WebUI Gradle 任务（prepare / build）添加 inputs/outputs 声明，避免每次重复执行
- [ ] CI workflow 添加 `setup-node` 步骤，固定 Node.js 版本
- [ ] `module/build.gradle.kts` 中 git 命令改为 Provider 懒加载；清理 ndkBuild 注释块和废弃的 `buildDir` API

## 🟢 低优先级

- [ ] `gradle.properties` 移除 `android.enableJetifier=true`
- [ ] `module.gradle.kts` 中 `var moduleLibName` 改为 `val`
