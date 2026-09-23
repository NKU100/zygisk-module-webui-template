# KMP Migration Patterns

These anonymized examples preserve reusable engineering decisions without depending on a particular repository.

## Example 1: Geometry-Heavy Shape Library

**Profile:** A small Compose shape library whose core behavior is portable geometry and math.

**Approach:** Move common shape logic into commonMain, remove tooling-only Android annotations, replace AndroidX-only helpers where standard Kotlin behavior matches, and implement missing path operations with focused geometry code.

**Decision:** A short Bézier approximation can be preferable to introducing a graphics engine dependency for one path operation. Compare complexity, artifact size, and rendering fidelity before choosing.

**Pitfall:** Bulk-removing an annotation import can leave constructor or property use-site annotations behind. Search for @param:, @get:, @set:, and @field: forms, then compile every target.

## Example 2: Platform-Dependent Effects Library

**Profile:** A Compose effects library that relies on native blur, color-filter, and runtime-shader APIs.

**Approach:** Migrate in phases:

1. Convert the build and move platform-independent models and modifiers to common code.
2. Define narrow expect/actual APIs for effects, shader construction, and mask filters.
3. Keep native Android implementations on Android and use the target's supported graphics APIs elsewhere.
4. Move higher-level effects into common code once they depend only on shared abstractions.
5. Build and visually compare each requested target.

**Decision:** Shader source can be shared only when the languages and runtime semantics are compatible. Keep shader creation platform-specific and test compiled output on each target.

## Decision Guide

~~~text
Is there a common Kotlin or Compose API with matching behavior?
  Yes -> use it and test boundary cases.
  No  -> does each target have a suitable native equivalent?
           Yes -> isolate it behind expect/actual.
           No  -> use a focused portable algorithm or reassess the dependency.
~~~
