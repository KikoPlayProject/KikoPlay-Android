# KikoPlay Android 项目结构

## 概述

KikoPlay Android 是 KikoPlay PC 的局域网配套播放器客户端。

当前版本基于 Kotlin + Jetpack Compose + AGP 9.1 开发，主要能力包括：

- 通过自动扫描、手动输入、历史记录与二维码扫描连接局域网内的 KikoPlay PC 服务；二维码可包含多个服务地址，客户端会优先选择与当前设备处于同一局域网的地址进行连接，未命中时再依次尝试全部地址
- 浏览 PC 播放列表并发起播放，支持按标题搜索、目录面包屑、分级返回、目录滚动位置恢复、同目录剧集联动与本地进度同步；当条目已有当前连接对应的完整缓存时，会优先使用本地缓存进入播放器
- 本地视频扫描与播放
- 视频内嵌字幕与 PC Web 字幕显示，并支持在横屏播放设置内切换字幕或关闭字幕；应用设置中的“播放”分组提供字幕样式与字幕大小预设
- 弹幕加载、增量刷新、来源信息查看与同步调整、存在弹幕池时发送与基础设置
- 观看历史记录、首页最近观看缩略图与恢复播放；连接 PC 后可获取 PC 端最近播放并合并到首页最近观看，已缓存记录进入播放器时可继续保留 PC 剧集上下文，并在同目录剧集切换时优先复用本地缓存
- 视频缓存与后台下载
- 缓存媒体的本地离线播放、弹幕复用，以及缓存页“缓存中 / 已缓存”分栏切换
- 应用启动统计埋点，每天首次冷启动会匿名上报一次 UV 和启动耗时到统计服务

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Kotlin | 2.2.10 |
| 构建 | Android Gradle Plugin | 9.1.0 |
| Gradle | Gradle Wrapper | 9.3.1 |
| UI | Jetpack Compose + Material3 | Compose BOM 2026.02.01 |
| 架构 | MVVM | - |
| 依赖注入 | Hilt + KSP | 2.59.2 |
| 数据库 | Room + KSP | 2.7.1 |
| 网络 | Retrofit 3 + OkHttp + kotlinx.serialization | 3.0.0 / 4.12.0 / 1.8.1 |
| 播放器 | Media3 ExoPlayer | 1.6.0 |
| 弹幕 | DanmakuFlameMaster | 0.9.25 |
| 图片加载 | Coil 3 | 3.1.0 |
| 偏好存储 | DataStore Preferences | 1.1.4 |
| 后台任务 | WorkManager | 2.10.1 |
| 二维码扫描 | zxing-android-embedded | 4.3.0 |

## 构建说明

### 推荐方式

由于 Android Studio 的 Gradle Tooling 导入在当前环境下容易触发用户目录 `C:\Users\Kikyou\.gradle` 中的临时缓存损坏，项目内已提供独立构建脚本，优先使用该脚本生成 APK。

相关文件：

- `build-apk.ps1`
- `build-apk.bat`

默认行为：

- 自动优先使用 Android Studio 自带 JBR
- 自动使用项目内独立 `GRADLE_USER_HOME`
- 直接执行 `assembleDebug` 或 `assembleRelease`
- 尽量绕开全局 Gradle 缓存污染
- Gradle Wrapper 已切换为 `gradle-9.3.1-all.zip` 镜像分发包，避免 Android Studio 额外拉取源码包时反复超时

项目内优先使用的 Gradle 缓存目录：

- `.gradle-user-home-allzip`
- `.gradle-user-home-build`

### 常用命令

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

### 脚本参数

| 参数 | 说明 |
|------|------|
| `-Variant debug` | 生成 Debug APK，默认值 |
| `-Variant release` | 生成 Release APK |
| `-Clean` | 先执行 `clean` 再构建 |
| `-NoDaemon` | 禁用 Gradle Daemon，适合规避一次性缓存状态问题 |

### APK 输出目录

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`（配置签名时）或 `app-release-unsigned.apk`（未配置签名时）

### Release 签名

Release 构建支持读取项目根目录的本地 `keystore.properties`：

```properties
storeFile=kikoplay-release.jks
storePassword=...
keyAlias=kikoplay
keyPassword=...
```

当前本地环境已生成 `kikoplay-release.jks` 与 `keystore.properties`，执行 `powershell -ExecutionPolicy Bypass -File .\build-apk.ps1 -Variant release -NoDaemon` 会产出已签名的 `app-release.apk`。这两个文件包含发布密钥或密码，已在 `.gitignore` 中忽略，不应提交到仓库。

如果缺少 `keystore.properties`，release 构建会回退为未签名 APK；此时需要手动签名后才能安装。

### 仍需注意

- `gradle.properties` 中保留了 `android.disallowKotlinSourceSets=false`
- `settings.gradle.kts` 中包含 JitPack 仓库，用于 `DanmakuFlameMaster`
- Android Studio 自身如果继续走用户目录 `C:\Users\Kikyou\.gradle`，仍可能触发 IDE 侧的缓存损坏问题；脚本构建不依赖这套全局缓存
- 当前环境可能仍会出现 `SDK XML version 4` 的 warning，这通常与 Android Studio / SDK command-line tools 版本不一致有关，但不一定阻塞脚本构建
- Release 构建已关闭 APK 依赖信息写入与 `lintVitalRelease`，用于规避当前环境下 `collectReleaseDependencies` / lint 输出目录的本地缓存或权限问题

## 测试构建策略

当前项目默认关闭 Android 仪器测试变体，以规避 `debugAndroidTestRuntimeClasspath` 在当前环境中的依赖解析损坏问题。

对应实现位于：

- `app/build.gradle.kts`

当前行为：

- 默认不创建 Android Test variant
- 默认不解析 `androidTestImplementation(...)` 依赖
- 常规 APK 构建不受影响

如果后续需要恢复仪器测试，可显式传入：

```powershell
.\gradlew.bat -PenableAndroidTests=true connectedDebugAndroidTest
```

## 模拟器调试

当前环境已经验证可用的模拟器调试方式如下：

- 雷电模拟器 adb 路径：`D:\Program Files\leidian\LDPlayer9\adb.exe`
- 当前常用设备 serial：`emulator-5554`
- 推荐构建方式仍然是项目脚本：`powershell -ExecutionPolicy Bypass -File .\build-apk.ps1 -Variant debug -NoDaemon`
- 推荐安装方式：`D:\Program Files\leidian\LDPlayer9\adb.exe -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk`
- 推荐启动方式：`D:\Program Files\leidian\LDPlayer9\adb.exe -s emulator-5554 shell am start -n com.kiko.kikoplay/.MainActivity`

常用排查命令：

```powershell
# 查看设备
D:\Program Files\leidian\LDPlayer9\adb.exe devices

# 启动应用
D:\Program Files\leidian\LDPlayer9\adb.exe -s emulator-5554 shell am start -n com.kiko.kikoplay/.MainActivity

# 停止应用
D:\Program Files\leidian\LDPlayer9\adb.exe -s emulator-5554 shell am force-stop com.kiko.kikoplay

# 抓取日志
D:\Program Files\leidian\LDPlayer9\adb.exe -s emulator-5554 logcat -d

# 导出界面树
D:\Program Files\leidian\LDPlayer9\adb.exe -s emulator-5554 exec-out uiautomator dump /dev/tty

# 截图
D:\Program Files\leidian\LDPlayer9\adb.exe -s emulator-5554 shell screencap -p /sdcard/screen.png
D:\Program Files\leidian\LDPlayer9\adb.exe -s emulator-5554 pull /sdcard/screen.png .\screen.png
```

注意：

- 不建议直接使用默认 `.\gradlew.bat installDebug` 进行安装，当前环境容易命中 `C:\Users\Kikyou\.gradle` 下的锁文件或权限问题
- 如果需要定位播放器 / 弹幕问题，优先结合 `logcat`、`uiautomator dump` 和截图一起排查

## 顶层文件

| 路径 | 说明 |
|------|------|
| `build.gradle.kts` | 根构建脚本 |
| `settings.gradle.kts` | 模块与仓库配置 |
| `gradle.properties` | Gradle 项目级参数 |
| `gradle/libs.versions.toml` | 版本目录 |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle Wrapper 配置 |
| `build-apk.ps1` | 推荐的 APK 构建脚本 |
| `build-apk.bat` | Windows 包装脚本 |
| `AGENTS.md` | 仓库内协作约定与默认构建指引 |
| `project_structure.md` | 本文档 |

## 代码结构

```text
com.kiko.kikoplay/
├── KikoPlayApplication.kt
├── MainActivity.kt
├── di/
│   ├── AppModule.kt
│   ├── DatabaseModule.kt
│   ├── NetworkModule.kt
│   └── StatsNetworkModule.kt
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   │   ├── CacheTaskDao.kt
│   │   │   ├── ConnectionDao.kt
│   │   │   └── WatchHistoryDao.kt
│   │   ├── entity/
│   │       ├── CacheTaskEntity.kt
│   │       ├── ConnectionEntity.kt
│   │       └── WatchHistoryEntity.kt
│   │   └── model/
│   │       └── CachedDanmakuPayload.kt
│   ├── model/
│   │   └── PlayerPreferences.kt
│   ├── remote/
│   │   ├── BaseUrlInterceptor.kt
│   │   ├── ConnectionManager.kt
│   │   ├── KikoPlayApi.kt
│   │   ├── StatsApi.kt
│   │   └── model/
│   │       ├── DanmakuModels.kt
│   │       ├── PlaylistModels.kt
│   │       ├── PlayStateModels.kt
│   │       ├── RequestModels.kt
│   │       ├── StatsModels.kt
│   │       └── SubtitleModels.kt
│   └── repository/
│       ├── CacheRepository.kt
│       ├── ConnectionRepository.kt
│       ├── HistoryPlaybackCoordinator.kt
│       ├── HistoryThumbnailRepository.kt
│       ├── LocalVideoRepository.kt
│       ├── PlaylistRepository.kt
│       ├── SettingsRepository.kt
│       ├── StatsRepository.kt
│       └── WatchHistoryRepository.kt
├── service/
│   └── CacheDownloadWorker.kt
├── ui/
│   ├── cache/
│   │   ├── CacheScreen.kt
│   │   └── CacheViewModel.kt
│   ├── common/
│   │   └── EmptyState.kt
│   ├── connection/
│   │   ├── ConnectionScreen.kt
│   │   └── ConnectionViewModel.kt
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   ├── HomeViewModel.kt
│   │   ├── WatchHistoryScreen.kt
│   │   └── WatchHistoryViewModel.kt
│   ├── local/
│   │   ├── LocalVideosScreen.kt
│   │   └── LocalVideosViewModel.kt
│   ├── navigation/
│   │   ├── KikoBottomBar.kt
│   │   ├── KikoNavHost.kt
│   │   ├── PlaceholderScreen.kt
│   │   ├── Route.kt
│   │   └── TopLevelDestination.kt
│   ├── player/
│   │   ├── PlayerPictureInPictureState.kt
│   │   ├── VideoPlayerScreen.kt
│   │   ├── VideoPlayerViewModel.kt
│   │   ├── components/
│   │   │   ├── KikoSlider.kt
│   │   │   └── ScreenshotClipDialog.kt
│   │   ├── danmaku/
│   │   │   ├── DanmakuParser.kt
│   │   │   ├── DanmakuSettingsPanel.kt
│   │   │   └── SendDanmakuDialog.kt
│   │   └── subtitle/
│   │       └── SubtitleTracks.kt
│   ├── playlist/
│   │   ├── PlaylistBrowserScreen.kt
│   │   └── PlaylistBrowserViewModel.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── util/
    ├── CacheFileHelper.kt
    ├── MediaUrlBuilder.kt
    ├── NetworkScanner.kt
    └── QrConnectionParser.kt
```

## 主要模块说明

### 应用入口

- `KikoPlayApplication.kt`: Hilt 应用入口，同时提供 WorkManager 配置
- `MainActivity.kt`: 根 Activity，承载 Scaffold、导航和底部栏，并展示缓存任务角标

### 依赖注入

- `AppModule.kt`: 应用级对象，如 Json / DataStore / 连接状态对象
- `DatabaseModule.kt`: Room 数据库与 DAO 注入
- `NetworkModule.kt`: OkHttp / Retrofit / API 注入
- `StatsNetworkModule.kt`: 独立统计上报网络栈，不受局域网服务的 baseUrl 改写影响

### 数据层

- `data/local`: Room 数据库、实体和 DAO
- `data/model`: DataStore 与跨模块共享的数据模型，当前包含播放器持久化偏好
- `data/remote`: REST API、连接状态、统计上报接口与网络 DTO
- `data/repository`: 面向 ViewModel 的仓库层封装，包含历史续播目标解析、缓存 / PC 播放上下文衔接、播放列表与 PC 最近播放缓存、历史缩略图生成、观看历史串行写入以保留退出播放器时的最终帧缩略图、首页最近观看复用、启动统计上报，以及设置页 / 播放器页偏好的 DataStore 持久化逻辑；其中 `PlaylistRepository` 会在拉取 `/api/playlist` 时读取响应头 `X-Kiko` 并记录远端版本，供后续能力兼容判断使用

### UI 层

- `ui/home`: 首页与观看历史入口，首页最近观看使用纵向缩略图列表展示，包含 PC 最近播放与 App 本地历史合并、最近观看去重、缓存标记、进度条与恢复播放；PC 最近播放只作为首页合并来源，不写入 Room，后续 App 播放记录会按本地更新时间正常排到前面；当最近观看条目已缓存时，会优先使用本地缓存进入播放器，同时尽量保留对应 PC 同目录剧集上下文
- `ui/connection`: 局域网连接管理，支持自动扫描、手动输入、历史记录与二维码扫描连接；二维码内容可包含多条 `http://ipv4:port/` 或兼容 `http://ipv4/:port` 形式的服务地址，客户端会优先尝试与当前设备位于同一局域网的地址
- `ui/playlist`: PC 播放列表浏览，支持顶部搜索按钮按标题过滤、breadcrumb 自动滚动、系统返回逐级回退、按目录恢复列表滚动位置、从播放页返回后保留进入前列表位置、本地播放进度联动；已完成缓存的条目会在列表中显示“已缓存”标记，点击时会优先使用当前连接对应的本地缓存进入播放器；顶部左上角返回按钮直接返回上一个页面，目录级跳转由系统返回与 breadcrumb 负责，批量“标记已看”会跟随“同步播放进度”设置决定是否向 PC 端发送 `updateTime`
- `ui/player`: 播放器、同目录剧集列表、进入播放页后剧集列表自动定位当前播放项、剧集长按多选缓存、续播进度同步、自动下一集、纵向布局下支持“剧集 / 弹幕”分页与左右滑动切换；当剧集列表为空时隐藏剧集页并直接显示弹幕页；在横向宽屏设备上，非全屏状态会改用左侧播放器 / 右侧内容栏的双栏布局，以便同时展示剧集与弹幕信息；弹幕来源卡片展示与编辑、弹幕增量刷新、延迟与时间轴同步调整、无弹幕池时隐藏发送弹幕入口，以及弹幕与截图片段交互；已完成缓存的剧集会在列表中显示“已缓存”标记，当当前或目标剧集存在本地可播放缓存时，剧集切换与自动下一集会优先使用缓存播放，否则继续走 PC 串流。支持 Media3 文本轨道显示视频内嵌字幕与远程 Web 字幕，默认优先选择第一个内嵌字幕、无内嵌字幕时回退到 `/api/subtitle` 返回字幕，横屏“设置 > 播放设置”可在“无 / 内嵌字幕 / Web 字幕”之间切换；播放器会根据应用设置中的字幕样式与字幕大小预设应用 `SubtitleView` 显示风格，当前提供跟随系统、黑底白字、半透明黑底白字、白字描边、白字阴影、黄字描边与黄字阴影等预置样式。支持普通播放页黑色系统栏、全屏/横屏隐藏系统栏、横屏自动下一集时保持横屏与隐藏系统栏、传感器横屏 180 度翻转、顶部/底部渐变控制层、横向手势精细 seek、横屏顶部标题区域手势排除、左右三分区上下滑分别调节亮度与音量、中间三分区上下滑不触发亮度/音量、下半区域长按 1 秒临时 2 倍速并带轻震动反馈与居中偏下的轻量 `2.0x` 提示，横屏时会在顶部中央显示当前时间，并会在竖屏下为竖版视频提供独立的竖版全屏入口，弹幕显示开关、显示区域、不透明度、字体大小、弹幕速度、类型屏蔽与播放速度会持久化恢复，同时“同步播放进度”设置会实际控制是否向 PC 端回传进度；播放器在退出/播完/切集等终态同步时会把当前帧 PNG 缩略图一并回传到 `/api/updateTime.preview`，但只有在远端 Web API 响应头携带 `X-Kiko` 且版本号不低于 `200100` 时才会附带该字段，旧版本或未声明能力的服务端仍只同步时间本身。播放器在实际播放或缓冲待播时保持屏幕常亮，自动跳转下一集期间持续保持常亮不中断，暂停后恢复系统息屏策略；弹幕渲染保留 DanmakuFlameMaster 绘制缓存并仅在实际 seek 时校准时间，对 `/api/danmu/full/` 返回的来源裁剪、时间轴与延迟按 PC 端一致顺序进行本地换算（先裁剪并减去裁剪起点，再累计应用时间轴偏移，最后加整体延迟），使用自定义滑杆组件并按原视频比例渲染画面，同时会在退出播放器或自动切到下一集前保存当前帧历史缩略图，并在低内存设备上启用更保守的缓冲、seek、抓图与播放错误提示兜底
- `ui/local`: 本地视频列表
- `ui/cache`: 缓存任务管理，支持“缓存中 / 已缓存”分栏、左右滑动切换，以及从缓存完成项直接进入离线播放
- `ui/settings`: 应用设置，包含外观（深色模式三选一：跟随系统 / 浅色 / 深色）、播放（小窗播放、后台继续播放、字幕样式、字幕大小）、同步、是否获取 PC 最近播放、关于与反馈；反馈入口会复制 QQ 群号
- `ui/navigation`: 底部导航和路由，包含缓存任务气泡提示；点击缓存入口时会根据当前任务状态优先落到“缓存中”或“已缓存”；统一处理页面返回与防重复触发，避免快速连点把导航栈弹空
- `ui/theme`: Compose 主题配置，支持浅色与深色配色方案，主题模式通过 DataStore 持久化并在 MainActivity 中响应式应用

### 后台任务

- `CacheDownloadWorker.kt`: 负责缓存下载、暂停保留任务状态、断点续传、总大小探测与弹幕 sidecar 缓存

### 工具类

- `CacheFileHelper.kt`: 统一生成缓存媒体对应的弹幕 sidecar 文件路径
- `NetworkScanner.kt`: 局域网扫描与本机 IPv4 网段识别
- `MediaUrlBuilder.kt`: 构建媒体与字幕地址
- `QrConnectionParser.kt`: 解析二维码中的服务地址列表，并按当前局域网优先级排序

## 测试目录

```text
app/src/test/
└── java/com/kiko/kikoplay/
    ├── ExampleUnitTest.kt
    ├── ui/player/danmaku/
    │   └── DanmakuParserTest.kt
    ├── ui/player/subtitle/
    │   └── SubtitleTrackSelectorTest.kt
    └── util/
        └── QrConnectionParserTest.kt

app/src/androidTest/
└── java/com/kiko/kikoplay/ExampleInstrumentedTest.kt
```

说明：

- `QrConnectionParserTest.kt` 覆盖二维码地址解析与“同局域网优先”排序逻辑
- `DanmakuParserTest.kt` 覆盖完整弹幕来源裁剪、时间轴累计偏移、延迟与负时间丢弃逻辑
- `SubtitleTrackSelectorTest.kt` 覆盖字幕候选列表构建与默认选择规则
- 其余两个测试文件仍是模板级示例
- `androidTest` 目录保留，但默认不参与构建解析

## KikoPlay LAN API 对接情况

| API | 用途 | 状态 |
|-----|------|------|
| `GET /api/playlist` | 获取播放列表树 | 已接入 |
| `GET /api/recent` | 获取 PC 端最近播放条目并合并到首页最近观看 | 已接入 |
| `GET /api/playstate` | 获取播放状态 / 连接验证 | 已接入 |
| `GET /media/{mediaID}` | 串流媒体文件 | 已接入 |
| `GET /sub/{format}/{mediaID}` | 获取字幕 | 已接入 |
| `GET /api/subtitle?id=` | 检查字幕是否存在 | 已接入 |
| `GET /api/danmu/v3/` | 获取过滤弹幕 | 已接入 |
| `GET /api/danmu/full/` | 获取完整弹幕、来源裁剪/时间轴/延迟信息与增量更新 | 已接入 |
| `GET /api/danmu/local/` | 获取本地 XML 弹幕 | 已接入 |
| `POST /api/updateTime` | 同步播放进度 | 已接入 |
| `POST /api/updateDelay` | 更新弹幕延迟 | 已接入 |
| `POST /api/updateTimeline` | 更新时间轴 | 已接入 |
| `POST /api/screenshot` | 远程截图 / 截取片段 | 已接入 |
| `POST /api/danmu/launch` | 发送弹幕 | 已接入 |

## Room 数据库

数据库名：`kikoplay.db`  
版本：`1`

| 表名 | 用途 |
|------|------|
| `connections` | 连接历史 |
| `watch_history` | 观看历史 |
| `cache_tasks` | 缓存任务 |

补充说明：

- `watch_history.thumbnailData` 继续复用于首页最近观看缩略图，不涉及 Room 表结构变更或数据库升级

## 已知待完善项

1. KService gRPC 集成尚未完成
2. 弹幕与播放器在倍速播放、缓冲恢复等场景下的精确时钟同步仍有优化空间
3. DanmakuFlameMaster 0.9.25 的类型过滤能力有限
4. 播放手势灵敏度与 seek 手感仍可继续调校
5. 缓存恢复下载仍缺少真机充分验证
6. 首页目录快捷入口仍可继续优化
7. 缓存总大小探测仍依赖服务端是否返回 `Content-Length` / `Content-Range`
