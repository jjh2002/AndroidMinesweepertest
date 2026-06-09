# GitHub Actions 打包 APK

这个项目已经包含 `.github/workflows/build-apk.yml`，上传到 GitHub 后可以自动打包 debug APK。

## 使用方法

1. 在 GitHub 新建仓库。
2. 上传本项目里的全部文件和文件夹，包括隐藏的 `.github` 文件夹。
3. 打开仓库的 Actions 页面。
4. 选择 `Build Debug APK`。
5. 点击 `Run workflow`。
6. 构建成功后，在页面底部 Artifacts 下载 `AndroidMinesweeper-debug-apk`。
7. 解压下载的 zip，里面就是 APK。

## 安装到手机

把 APK 发到安卓手机，打开文件并允许“安装未知来源应用”即可安装。

## 说明

本项目没有 Gradle Wrapper，所以 workflow 使用 `gradle/actions/setup-gradle` 安装 Gradle 8.13，再执行 `gradle --no-daemon :app:assembleDebug`。
