# Debug Toolkit

[![](https://jitpack.io/v/amigo-wangwx/debug-toolkit.svg)](https://jitpack.io/#amigo-wangwx/debug-toolkit)

JitPack 地址：https://jitpack.io/#amigo-wangwx/debug-toolkit

## 项目简介

Debug Toolkit 是一套 Android 开发调试工具包，包含三个模块：

- **debug-toolkit**：调试悬浮窗、功能开关面板、日志查看、网络配置入口等调试 UI。
- **network-interceptor**：网络拦截底层库，支持 URL 重写、规则选择、JSON 配置文件管理、独立配置编辑器。
- **debug-network-interceptor-plugin**：Gradle 插件，通过 ASM 字节码注入自动为 OkHttp Builder 挂载拦截器，并在非主进程跳过 Application.onCreate。

所有模块面向 `debug` 构建类型使用，不编译进 Release 包。

## 接入方式

### 1. 配置仓库

在项目根 `settings.gradle.kts` 中新增 JitPack 仓库：

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.debugtoolkit.debug-network-interceptor") {
                useModule("com.github.amigo-wangwx.debug-toolkit:debug-network-interceptor-plugin:<version>")
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

### 2. 引入插件

在 App 模块的 `build.gradle` 或 `build.gradle.kts` 中添加：

```gradle
plugins {
    id 'com.debugtoolkit.debug-network-interceptor' version '<version>'
}
```

也可以使用传统 `buildscript` 方式接入：

```gradle
buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        classpath 'com.github.amigo-wangwx.debug-toolkit:debug-network-interceptor-plugin:<version>'
    }
}

apply plugin: 'com.debugtoolkit.debug-network-interceptor'
```

### 3. 添加依赖

```gradle
dependencies {
    debugImplementation 'com.github.amigo-wangwx.debug-toolkit:debug-toolkit:<version>'
}
```

`debug-toolkit` 已依赖 `network-interceptor`，通常只需一行依赖即可。`<version>` 使用 JitPack 页面或上方 badge 显示的最新版本。

## 宿主输入 Action

浮窗底部输入框会优先将内容交给宿主注册的 `HostInputAction`；全部 action 返回 `NotHandled` 时，继续按普通 URI 使用 `Intent.ACTION_VIEW` 打开。

```kotlin
DebugHostBridge.registerInputAction(
    DebugHostBridge.HostInputAction(id = "business_protocol") { context, input ->
        val uri = parseBusinessProtocol(input)
            ?: return@HostInputAction DebugHostBridge.HostInputResult.NotHandled
        DebugHostBridge.HostInputResult.OpenUri(uri)
    }
)
```

`Rejected` 用于展示协议校验错误并保留输入内容，`Handled` 表示宿主已经完成处理。多个 action 按注册顺序执行，第一个非 `NotHandled` 结果生效。

## 网络配置文件

调用方可以在 App 的 source set 根目录放置 `debug_network_config.json`，例如：

```text
apps/FunShorts/src/funshorts/debug_network_config.json
apps/ReelRush/src/reelrush/debug_network_config.json
```

插件只处理 `debug` variant，并按完整 variant、组合 flavor、`debug`、单个 flavor、`main` 的顺序查找当前 variant 对应的配置文件。命中后，插件会把它复制为 App 的生成 asset；没有命中时，不生成 App asset，由 `network-interceptor` 使用 AAR 内置的 `assets/debug_network_config.json`。

运行时统一通过 `context.assets.open("debug_network_config.json")` 读取最终合并进 APK 的文件。配置面板一次最多选择一个 rule；`selectRuleIds` 保持数组格式以兼容已有 JSON，应用时只持久化唯一 rule。

## 从旧版 FunShorts 本地依赖迁移

如果之前是通过 `includeBuild` 和 `project()` 本地引用，可按以下步骤切换到远程依赖：

1. 删除 `settings.gradle.kts` 中的 `includeBuild("build-logic/debug-network-interceptor-plugin")`
2. 从 `settings.gradle.kts` 的 `include` 列表中移除 `:debug-tool:network-interceptor` 和 `:debug-tool:debug-toolkit`
3. 删除 App 模块的 `debugImplementation project(':debug-tool:debug-toolkit')`
4. 参照上方"接入方式"添加 JitPack 仓库、插件和远程依赖
5. 将插件 ID 从旧的 `com.vcokey.debug-network-interceptor` 或 `io.github.wangwx.debug-network-interceptor` 替换为 `com.debugtoolkit.debug-network-interceptor`

## 许可证

本项目遵循 MIT 许可证，可自由使用和修改。

## 三方库引用

本项目使用和借鉴了以下优秀的开源库和文章。

| 模块 | 三方库 | 地址 | 用途 |
|---|---|---|---|
| **network-interceptor** | OkHttp | [square/okhttp](https://github.com/square/okhttp) | HTTP 客户端，UrlRewriter 运行基础 |
| **debug-toolkit** | Retrofit + Moshi | [square/retrofit](https://github.com/square/retrofit)、[square/moshi](https://github.com/square/moshi) | 调试面板网络请求与 JSON 序列化 |
| | MMKV | [Tencent/MMKV](https://github.com/Tencent/MMKV) | 本地配置键值存储 |
| | Logcat | [getActivity/Logcat](https://github.com/getActivity/Logcat) | 日志查看器 |
| **debug-network-interceptor-plugin** | ASM | [美团技术博客：Java 字节码增强](https://tech.meituan.com/2019/09/05/java-bytecode-enhancement.html) | 字节码注入 OkHttp.Builder 拦截器与 Application.onCreate 非主进程跳过 |
