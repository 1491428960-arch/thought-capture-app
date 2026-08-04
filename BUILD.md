# 想法捕捉 — 构建与安装指南

## 前置条件

- Android Studio Hedgehog (2023.1) 或更新版本
- JDK 17
- Android SDK 35

## 构建 Release APK

### 1. 生成签名密钥

```bash
keytool -genkey -v -keystore thought-capture.keystore \
  -alias thoughtcapture -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass android2026 -keypass android2026 \
  -dname "CN=ThoughtCapture, OU=Dev, O=Personal, L=Wuhan, ST=Hubei, C=CN"
```

### 2. 配置签名

在 `app/build.gradle.kts` 的 `android` 块中添加：

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../thought-capture.keystore")
        storePassword = "android2026"
        keyAlias = "thoughtcapture"
        keyPassword = "android2026"
    }
}
```

并在 `buildTypes.release` 中添加：
```kotlin
signingConfig = signingConfigs.getByName("release")
```

### 3. 构建

```bash
./gradlew assembleRelease
```

APK 输出：`app/build/outputs/apk/release/app-release.apk`

## 安装到小米手机

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

或通过文件快传/微信发送 APK 到手机直接安装。

## 首次使用

1. 在 GitHub 创建私有仓库 `ideas`
2. 生成 PAT（Settings → Developer settings → Personal access tokens → 勾选 repo 权限）
3. 打开 App → 填入 PAT + 仓库地址 → 点击"完成配置"
4. 下拉通知栏 → 编辑快捷开关 → 添加"快速记录"
5. 长按桌面 → 添加"想法捕捉"小组件
6. PC 端：`cd ~ && git clone <repo-url> ideas`
