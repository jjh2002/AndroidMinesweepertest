# Android 扫雷（Kotlin + Jetpack Compose）

这是一个可以在 Android Studio 中打开并运行的扫雷小游戏源码项目。

## 功能

- 简单 / 中等 / 困难三种难度
- 首次点击安全，不会第一步踩雷
- 点击翻格，长按插旗
- 自动展开空白区域
- 雷数计数、计时器、胜负状态
- 横向和纵向滚动，困难模式也能在手机屏幕上玩

## 打开方式

1. 解压本项目。
2. 用 Android Studio 打开 `AndroidMinesweeper` 文件夹。
3. 等待 Gradle Sync 完成。
4. 连接安卓手机或启动模拟器。
5. 点击 Run 运行。

## 命令行构建

如果你本机已经安装 Gradle，可以在项目根目录运行：

```bash
gradle wrapper --gradle-version 8.13
./gradlew assembleDebug
```

生成的调试 APK 会在：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 主要技术栈

- Kotlin
- Jetpack Compose
- Material 3
- Android Gradle Plugin
