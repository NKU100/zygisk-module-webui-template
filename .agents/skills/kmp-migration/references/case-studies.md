# KMP Migration Case Studies

## Case 1: Capsule (Pure Algorithm Library)

**Repository:** kyant0/Capsule
**Type:** Jetpack Compose smooth corner shapes
**Difficulty:** Low — pure math + Compose Shape API

### What Changed

| File Category | Count | Change Type |
|---------------|-------|-------------|
| Kotlin sources (pure move) | 10 | `git mv` only |
| Kotlin sources (API fix) | 6 | Remove annotations + replace `fastCoerce*` |
| PathSegment.kt | 1 | Replace `android.graphics.Path.arcTo` with Bézier |
| Build scripts | 4 | KMP plugin migration |
| AndroidManifest.xml | 1 | Deleted |
| .gitignore | 1 | Add `.kotlin/` |

### Key Decisions

- **Arc drawing**: Bézier approximation instead of Skiko (library is <100KB, Skiko would add 4-8MB)
- **Compose Multiplatform version**: 1.11.0-beta01 (upstream uses Kotlin 2.3.0, stable CMP only supports 2.2)
- **Maven publish**: Preserved with version bump to 2.2.0

### Pitfall Encountered

`sed` batch replacement of `@FloatRange` missed `@param:FloatRange(...)` on data class constructor parameters. Caught at compile time. Lesson: always grep for `@param:`, `@get:`, `@set:`, `@field:` annotation use-site prefixes.

---

## Case 2: Backdrop (Heavy Graphics Library)

**Repository:** kyant0/Backdrop
**Type:** Compose backdrop blur/shadow/highlight effects
**Difficulty:** High — extensive use of Android graphics APIs (RenderEffect, RuntimeShader, BlurMaskFilter, ColorMatrix)

### Migration Strategy: Phased expect/actual

Unlike Capsule, Backdrop could NOT simply replace APIs — it needed platform-specific implementations.

**Phase 0:** Build system conversion (same as Capsule)
**Phase 1:** Move pure Compose files to commonMain (interfaces, data classes, Canvas extensions)
**Phase 2:** Define `expect`/`actual` for core platform types:
  - `PlatformRenderEffect` — Android: `android.graphics.RenderEffect`, wasmJs: `org.jetbrains.skia.ImageFilter`
  - `RuntimeShaderCache` — Android: caches `RuntimeShader` (AGSL), wasmJs: caches `RuntimeEffect` + `RuntimeShaderBuilder` (SkSL)
  - `PlatformMaskFilter` — Android: `BlurMaskFilter`, wasmJs: `MaskFilter.makeBlur`
**Phase 3:** Split effects/ directory with `expect`/`actual` for Blur, ColorFilter, Lens, RenderEffect chaining
**Phase 4:** Move highlight/ and shadow/ modifiers to commonMain (they now depend on cross-platform abstractions)
**Phase 5:** Verify both Android and wasmJs compile and render identically

### Key Insight: AGSL ↔ SkSL

Android's AGSL (Android Graphics Shading Language) and Skia's SkSL are nearly identical syntax. Shader string constants can live in commonMain; only the runtime shader creation differs:

```kotlin
// commonMain — shader source string (same for both platforms)
const val LENS_SHADER = "uniform float2 resolution; ..."

// androidMain
RuntimeShader(LENS_SHADER)

// wasmJsMain  
RuntimeEffect.makeForShader(LENS_SHADER)
```

### Decision Tree Used

```
Can the API be replaced with stdlib/Compose common API?
  YES → Replace directly (annotations, fastCoerce*, etc.)
  NO  → Is there a Skia equivalent?
         YES → Use expect/actual (Android native, Skia on others)
         NO  → Reimplement with cross-platform math (Bézier arcs, etc.)
```
