---
name: kmp-migration
description: "This skill provides a systematic guide for migrating Android-only Compose libraries to Kotlin Multiplatform (KMP). It should be used when converting an Android library to support multiple targets (android, jvm, wasmJs, iOS), particularly Compose UI libraries that use Android-specific APIs. Covers build script conversion, source relocation, API replacement patterns, and common pitfalls."
---

# Android Library → Kotlin Multiplatform Migration Guide

## When to Use

- Converting an Android-only Compose library to KMP
- Adding wasmJs / jvm / iOS targets to an existing Android library
- Preparing a fork for upstream PR with KMP support

## Migration Workflow

### 1. Audit Android-only Dependencies

Before writing any code, scan the entire source tree for platform-specific references:

```bash
grep -rn "android\.\|androidx\.annotation\|fastCoerce\|asAndroidPath" src/
```

Common Android-only patterns to look for:

| Pattern | Replacement |
|---------|-------------|
| `import androidx.annotation.FloatRange` | Remove (lint-only annotation) |
| `import androidx.annotation.IntRange` | Remove (lint-only annotation) |
| `@param:FloatRange(...)` | Remove — easily missed, `@param:` prefix differs from `@FloatRange` |
| `@get:FloatRange(...)` | Remove — same as above |
| `import androidx.compose.ui.util.fastCoerceIn` | Use `kotlin.ranges.coerceIn` |
| `import androidx.compose.ui.util.fastCoerceAtLeast` | Use `kotlin.ranges.coerceAtLeast` |
| `import androidx.compose.ui.util.fastFirstOrNull` | Use `kotlin.collections.firstOrNull` |
| `import androidx.compose.ui.util.fastRoundToInt` | Use `kotlin.math.roundToInt` |
| `android.graphics.Path` / `asAndroidPath()` | Reimplement with Compose `Path` API or Bézier approximation |
| `android.graphics.RectF` | Use `androidx.compose.ui.geometry.Rect` |
| `android.graphics.RenderEffect` | Use `expect`/`actual` with Skia on non-Android targets |
| `android.graphics.RuntimeShader` (AGSL) | Use `expect`/`actual` with SkSL `RuntimeEffect` on non-Android targets |
| `android.graphics.BlurMaskFilter` | Use `expect`/`actual` with `org.jetbrains.skia.MaskFilter` |
| `@Language("AGSL")` | Remove (IntelliJ hint annotation, not functional) |
| `java.lang.Math.PI` | Use `kotlin.math.PI` |
| `System.currentTimeMillis()` | Use `kotlin.time.TimeSource.Monotonic` or `expect`/`actual` |

### 2. Relocate Sources

Use `git mv` to preserve history:

```bash
mkdir -p src/commonMain/kotlin/com/example/lib/
git mv src/main/java/com/example/lib/*.kt src/commonMain/kotlin/com/example/lib/
```

Delete `src/main/AndroidManifest.xml` if present — KMP uses `androidLibrary { namespace = "..." }` instead.

For libraries with platform-specific code that cannot be made common, use a split strategy:
- Pure logic / Compose UI → `commonMain`
- Android graphics APIs → `androidMain` (with `expect`/`actual`)
- Skia/Skiko equivalents → `wasmJsMain` or `jvmMain`

### 3. Convert Build Scripts

#### `capsule/build.gradle.kts` (library module)

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("com.android.kotlin.multiplatform.library")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    androidLibrary {
        namespace = "com.example.lib"
        compileSdk = 36
        minSdk = 21
    }
    jvm()
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
        }
    }
}
```

Key differences from Android Library:
- Plugin: `com.android.library` → `kotlin.multiplatform` + `com.android.kotlin.multiplatform.library`
- Android config: `android { ... }` → `kotlin { androidLibrary { ... } }`
- Dependencies: `implementation(libs.androidx.ui)` → `implementation(compose.ui)` (Compose Multiplatform provides these)

#### `build.gradle.kts` (root)

Add KMP and Compose Multiplatform plugin declarations:

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    // ... existing plugins
}
```

#### `gradle/libs.versions.toml`

Add entries:

```toml
[versions]
compose-multiplatform = "1.11.0-beta01"  # check compatibility!

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "compose-multiplatform" }
```

#### `settings.gradle.kts`

Remove `foojay-resolver-convention` plugin if present — KMP does not use the JVM toolchain resolver.

### 4. Handle Circular Arc Drawing (Common Case)

Android's `Path.arcTo` and `addCircle` have no direct Compose Multiplatform equivalent. Replace with cubic Bézier approximation using the standard tangent-length formula `t = (4/3) * tan(θ/4)`:

- Split arcs > 90° into segments ≤ 90° each
- Each segment becomes one cubic Bézier curve
- Mathematically equivalent for rendering purposes

This avoids depending on Skiko (which would add 4-8MB to Android APK).

### 5. Compose Multiplatform ↔ Kotlin Version Compatibility

**This is the most common source of build failures.** Compose Multiplatform versions are tightly coupled to Kotlin versions:

| Compose Multiplatform | Minimum Kotlin |
|----------------------|----------------|
| 1.10.x (stable)     | 2.2.x          |
| 1.11.x (beta)       | 2.3.x          |

Always check the official compatibility table before choosing versions:
https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html

### 6. Skia (Skiko) as Alternative

For libraries that heavily use Android graphics APIs (shaders, render effects, color filters), using Skiko directly is viable:

```kotlin
// commonMain
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
```

**Trade-offs:**
- Android: bundles an extra copy of Skia (~4-8MB), does NOT reuse system Skia
- Desktop/iOS/Web: Skiko is already included by Compose Multiplatform
- Use `expect`/`actual` pattern to keep Android using native APIs, Skiko only on other targets

**Decision rule:** If only 1-2 APIs need replacing → use stdlib/Bézier alternatives. If many graphics APIs → consider Skiko or `expect`/`actual` split.

## Common Pitfalls

1. **`@param:FloatRange` missed by bulk replacement** — `sed 's/@FloatRange//'` won't match `@param:FloatRange` or `@get:FloatRange`. Always grep for all annotation use-site forms.

2. **`fastCoerceIn` is a Float extension, `coerceIn` is on Comparable** — functionally identical, `fast` variants are AndroidX inline optimizations that skip NaN checks. Safe to replace in KMP.

3. **`group`/`version` in library `build.gradle.kts`** — only needed for `includeBuild` consumers. Do not include when the library publishes via Maven.

4. **Maven publish configuration** — when migrating for upstream PR, preserve the existing `mavenPublishing {}` block. KMP publish may need coordinate adjustments but the structure stays the same.

5. **Empty `src/main/` directory left behind** — after `git mv`, manually clean up empty directories and delete `AndroidManifest.xml`.

6. **Configuration cache issues** — after plugin changes, run `./gradlew clean` before building to avoid stale cache errors.

## PR Preparation Checklist

When preparing a KMP migration PR for an upstream repository:

- [ ] Create branch based on `upstream/master`, not fork's `master`
- [ ] Keep Kotlin version matching upstream (adjust Compose Multiplatform version accordingly)
- [ ] Preserve Maven publish configuration with appropriate version bump
- [ ] Do NOT include `group`/`version` for `includeBuild` support (that's local-only)
- [ ] Do NOT downgrade dependency versions unless required for compatibility
- [ ] Run `./gradlew :module:assemble` to verify all targets compile
- [ ] Write commit messages in upstream's language style
- [ ] Note beta dependencies in PR description with upgrade path
