# Zygisk Module Template

An Zygisk Module Template based on [zygisk-module-template][zygisk-module-template].

## Usage

1. Edit your module prop in [module.gradle.kts](./module.gradle.kts).
2. Write your code in module/src/main/cpp .
3. Run gradle task `:zipDebug` or `:zipRelease` to build the module.
   Your module zip will be generated under `module/release`.
4. Run gradle task `:install(Magisk|Ksu)[AndReboot](Debug|Release)` to flash your module (optional).

## See also

https://github.com/topjohnwu/zygisk-module-sample

[zygisk-module-template]: https://github.com/5ec1cff/zygisk-module-template