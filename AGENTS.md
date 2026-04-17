# KikoPlay Android App

## 概述

KikoPlay Android 是 KikoPlay PC 的局域网配套播放器客户端。

当前版本基于 Kotlin + Jetpack Compose + AGP 9.1 开发


## 构建
构建apk请使用下面的命令

```powershell
# 生成 debug APK
.\build-apk.bat

# 等价 PowerShell 命令
powershell -ExecutionPolicy Bypass -File .\build-apk.ps1 -Variant debug -NoDaemon

# 清理后重新生成 debug APK
powershell -ExecutionPolicy Bypass -File .\build-apk.ps1 -Variant debug -Clean -NoDaemon

# 生成 release APK
powershell -ExecutionPolicy Bypass -File .\build-apk.ps1 -Variant release -NoDaemon
```

更多关于工程结构、构建的信息，参考`project_structure.md`