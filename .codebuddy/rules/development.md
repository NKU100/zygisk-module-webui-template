# 工程上下文

## 安装验证流程

安装命令（同时构建并推送 Android APK 和 wasmJs webroot）：
```
./gradlew :webui:installDebug :webui:install
```

安装完成后需要分别打开两端验证：

**Android 端：**
```
adb shell am start -n io.github.nku100.zygisk.sample/io.github.nku100.webui.MainActivity
```

**wasmJs 端：**
用 KsuWebUIStandalone（`io.github.a13e300.ksuwebui`）直接启动模块 WebUI：
```
adb shell am start -n io.github.a13e300.ksuwebui/.WebUIActivity --es id zygisk_sample --es name "Zygisk Module WebUI"
```
