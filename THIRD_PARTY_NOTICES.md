# License and attribution notices

This repository is multi-license. The root `LICENSE` is a scope guide, not a
blanket license for every tracked file. The notices below describe the parts
whose licensing has been confirmed and identify inherited template material
that remains pending confirmation.

## GPL-3.0-or-later

NKU100-authored code in `webui/`, `webui-app/`, `.github/workflows/`, and
`scripts/`, plus NKU100-authored additions to
`module/src/main/cpp/zygisk_module.cpp`, is licensed under GPL-3.0-or-later, except
for third-party components identified separately in this file.

The original Zygisk sample content retained in `zygisk_module.cpp` remains under
its upstream 0BSD notice; this GPL grant covers only NKU100-authored additions
in that file.

The following files contain code derived from KernelSU's manager/WebUI, which
is licensed GPL-3.0-or-later outside its `kernel/` directory. The listed
files retain that license:

- `webui/src/androidMain/kotlin/io/github/nku100/webui/ui/animation/InteractiveHighlight.kt`
- `webui/src/androidMain/kotlin/io/github/nku100/webui/ui/util/DeferredContent.kt`
- `webui/src/commonMain/kotlin/io/github/nku100/webui/ui/animation/DampedDragAnimation.kt`
- `webui/src/commonMain/kotlin/io/github/nku100/webui/ui/animation/InteractiveHighlight.kt`
- `webui/src/commonMain/kotlin/io/github/nku100/webui/ui/component/FloatingBottomBar.kt`
- `webui/src/commonMain/kotlin/io/github/nku100/webui/ui/component/SearchStatus.kt`
- `webui/src/commonMain/kotlin/io/github/nku100/webui/ui/component/StatusTag.kt`
- `webui/src/commonMain/kotlin/io/github/nku100/webui/ui/component/SuperSearchBar.kt`
- `webui/src/commonMain/kotlin/io/github/nku100/webui/ui/modifier/DragGestureInspector.kt`
- `webui/src/commonMain/kotlin/io/github/nku100/webui/ui/screen/MainPagerState.kt`
- `webui/src/commonMain/kotlin/io/github/nku100/webui/ui/util/DeferredContent.kt`
- `webui/src/wasmJsMain/kotlin/io/github/nku100/webui/ui/animation/InteractiveHighlight.kt`
- `webui/src/wasmJsMain/kotlin/io/github/nku100/webui/ui/util/DeferredContent.kt`

The source reference is [KernelSU commit
402a5be64521888e631ac6a8194e97ce66d5297f](https://github.com/tiann/KernelSU/tree/402a5be64521888e631ac6a8194e97ce66d5297f).
The floating bottom bar rendering also tracks [KernelSU v3.3.0
(commit 932014ab5b2c9b74a3d11e2ec4d17dd10fc9442e)](https://github.com/tiann/KernelSU/tree/932014ab5b2c9b74a3d11e2ec4d17dd10fc9442e).
The full license text is in `LICENSES/GPL-3.0-or-later.txt`.

## Third-party components

| Component | Files or use | License and source |
|---|---|---|
| Zygisk module sample/API | Upstream portions of `module/src/main/cpp/zygisk.hpp`, `zygisk_module.cpp`, `Android.mk`, and `Application.mk` | 0BSD; [topjohnwu/zygisk-module-sample](https://github.com/topjohnwu/zygisk-module-sample) states its source code is released under 0BSD. NKU100-authored additions to `zygisk_module.cpp` are GPL-3.0-or-later. |
| yyjson | `module/src/main/cpp/external/yyjson.c` and `yyjson.h` | MIT; copyright © 2020 YaoYuan <ibireme@gmail.com>. The copyright and permission notice are included in `LICENSES/MIT.txt`. |
| AndroidLiquidGlass | Adapted code in `webui/src/commonMain/kotlin/io/github/nku100/webui/ui/component/liquid/` | Apache-2.0; [upstream repository](https://github.com/Kyant0/AndroidLiquidGlass). |
| Noto Sans SC | `webui/src/commonMain/composeResources/font/noto_sans_sc_regular.woff2` | SIL Open Font License 1.1. Embedded font metadata identifies Noto Sans SC and copyright © 2014–2021 Adobe, with “Source” as a Reserved Font Name. See [Noto CJK](https://github.com/notofonts/noto-cjk). |
| Android NDK libc++ | Static C++ runtime linked into native module binaries | Apache-2.0 WITH LLVM-exception; see the [LLVM libc++ license](https://github.com/llvm/llvm-project/blob/main/libcxx/LICENSE.TXT). The Apache license and LLVM exception text are included below. |
| Kotlin, Compose Multiplatform, AndroidX, Miuix, AppIconLoader, and KotlinX libraries | Dependencies resolved by the Gradle build | Their upstream projects publish Apache-2.0 notices; versions are pinned in `gradle/libs.versions.toml` and the build files. See [Miuix](https://github.com/compose-miuix-ui/miuix), [AppIconLoader](https://github.com/zhanghai/AppIconLoader), [Kotlin](https://github.com/JetBrains/kotlin), [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform), and [AndroidX](https://github.com/androidx/androidx). Transitive dependencies may carry separate notices. |

License texts shipped with the module ZIP are in `LICENSES/`:

- `0BSD.txt`
- `Apache-2.0.txt`
- `GPL-3.0-or-later.txt`
- `LLVM-exception.txt`
- `MIT.txt`
- `NotoSansSC.txt`
- `OFL-1.1.txt`

## License pending confirmation

This repository is a direct fork of the public
[5ec1cff/zygisk-module-template](https://github.com/5ec1cff/zygisk-module-template).
At the time of review, its repository root did not contain an explicit
open-source license. No separate permission to relicense its inherited
material has been confirmed. The license grants in this repository do not
cover the following inherited or adapted template material. This notice does
not assess the scope of rights GitHub's Terms of Service provide for
GitHub-hosted forks:

- `module/template/**`
- `build.gradle.kts`
- `module/build.gradle.kts`
- `settings.gradle.kts`
- `gradle/libs.versions.toml`
- `module/src/main/AndroidManifest.xml`
- `module/src/main/cpp/CMakeLists.txt`

This pending status is intentional; the license of another repository by the
same author does not establish permission for this template. No license is
granted for these pending files by this notice.
