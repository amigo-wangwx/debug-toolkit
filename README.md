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

## 从旧版 FunShorts 本地依赖迁移

如果之前是通过 `includeBuild` 和 `project()` 本地引用，可按以下步骤切换到远程依赖：

1. 删除 `settings.gradle.kts` 中的 `includeBuild("build-logic/debug-network-interceptor-plugin")`
2. 从 `settings.gradle.kts` 的 `include` 列表中移除 `:debug-tool:network-interceptor` 和 `:debug-tool:debug-toolkit`
3. 删除 App 模块的 `debugImplementation project(':debug-tool:debug-toolkit')`
4. 参照上方"接入方式"添加 JitPack 仓库、插件和远程依赖
5. 将插件 ID 从旧的 `com.vcokey.debug-network-interceptor` 或 `io.github.wangwx.debug-network-interceptor` 替换为 `com.debugtoolkit.debug-network-interceptor`

## 许可证

本项目遵循 MIT 许可证，可自由使用和修改。
