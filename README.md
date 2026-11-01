# VolPlus 音量调节

一个轻量的 Android 音量管理工具，可分别调节「媒体 / 铃声 / 通知 / 闹钟 / 通话」五类音频流的音量，支持滑块、加减、静音与恢复，并可选择在调节时弹出系统音量面板。

<p>
  <img alt="API" src="https://img.shields.io/badge/API-23%20~%2037-brightgreen" />
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-1.9-blue" />
  <img alt="Version" src="https://img.shields.io/badge/version-1.0-orange" />
  <img alt="Package" src="https://img.shields.io/badge/package-com.wells.volplus-informational" />
</p>

## 功能特性

| 功能 | 说明 |
| --- | --- |
| 五类音频流 | 媒体（MUSIC）、铃声（RING）、通知（NOTIFICATION）、闹钟（ALARM）、通话（VOICE_CALL）独立调节 |
| 滑块调节 | 拖动即时生效，显示「当前值 / 最大值」 |
| 步进加减 | `−` / `+` 按钮，每次增减 1 格 |
| 单类静音 | 一键静音并记住调节前的音量，再次点击即恢复 |
| 单类恢复 | 直接恢复到静音前的音量 |
| 全部静音 / 全部恢复 | 一次性处理全部五类音频流 |
| VolumeDialog 开关 | 控制调节音量时是否弹出系统音量面板（`FLAG_SHOW_UI`） |
| 状态同步 | 监听系统音量变化广播，按物理音量键时界面数值自动刷新 |

所有操作均在本地完成，**不需要网络权限**，无广告、无后台常驻服务。

## 系统兼容性

| 项目 | 值 |
| --- | --- |
| 最低版本 | Android 6.0（API 23） |
| 目标版本 | Android 17（API 37） |
| 编译版本 | API 37 |

低版本使用圆形/方形 PNG 图标，Android 8.0+ 使用自适应图标（Adaptive Icon）。

## 界面说明

启动后自上而下依次是：

1. **VolumeDialog 开关** —— 打开后，用本应用调节音量会同时弹出系统音量面板；关闭时仅静默改变音量。
2. **全部静音 / 全部恢复** —— 全局操作按钮。
3. **五张音频流卡片** —— 每张包含标题、当前数值、滑块，以及 `−`、`静音`、`+`、`恢复` 四个按钮。

> 音量数值显示为红色代表该音频流当前为 0（静音状态）。

### 关于 VolumeDialog 开关

Android 并不向第三方应用开放「显示系统音量条」的普通接口，本应用通过给 `AudioManager.setStreamVolume()` 传入 `FLAG_SHOW_UI` 来唤出系统自带音量面板。

- 开关状态保存在 `SharedPreferences`（`volplus_settings` / `show_volume_dialog`），默认关闭。
- 滑块拖动、加减按钮、静音、恢复、全部静音/恢复都会遵循该开关。

## 技术栈

- 语言：Kotlin
- 界面：原生 View（`LinearLayout` + `ScrollView` 动态构建）
- 依赖：AndroidX AppCompat、Material Components（`SwitchMaterial`）
- 构建：Gradle 9.6.0 + Android Gradle Plugin 9.4.1，JDK 11
- 核心 API：`AudioManager#getStreamVolume / getStreamMaxVolume / setStreamVolume`

## 项目结构

```
VolPlus/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/wells/volplus/
│       │   └── MainActivity.kt          # 全部界面与音量逻辑
│       └── res/
│           ├── drawable/bg_card.xml     # 卡片圆角背景
│           ├── mipmap-*/ic_launcher.png # 各密度图标
│           ├── mipmap-anydpi-v26/       # 自适应图标
│           └── values/                  # 字符串、颜色、主题
├── gradle/
│   ├── libs.versions.toml               # 依赖版本集中管理
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 构建与安装

### 方式一：Android Studio

1. `File → Open` 选择项目根目录。
2. 等待 Gradle 同步完成（首次会自动下载 Gradle 9.6.0 与依赖）。
3. 连接设备或启动模拟器，点击 Run。

### 方式二：命令行

```bash
# Windows
gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

产物路径：

```
app/build/outputs/apk/debug/app-debug.apk
```

安装到已连接设备：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> `local.properties` 中的 `sdk.dir` 指向本机 SDK 路径，不建议提交到仓库；Android Studio 会自动生成。

## 权限说明

```xml
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```

`MODIFY_AUDIO_SETTINGS` 属于普通权限，安装时自动授予，无需运行时申请。

## 已知限制

- **静音前的音量只保存在内存中**，应用进程被回收后不会保留；重新打开应用时显示的是系统当前音量。
- 部分机型（尤其 Android 7+）把「铃声」和「通知」合并为同一个音量通道，调节其中一个会同时影响另一个，这是系统行为。
- 「通话」音量依赖通话音频通道，部分 ROM 在无通话时调节可能无效。
- 静音只把音量置为 0，不会开启系统的「勿扰模式」。

## 许可证

本项目仅供学习与个人使用。
