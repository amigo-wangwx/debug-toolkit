#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Generate the debug toolkit modules from the latest in-repo templates.

This script is the single source of truth for the generated debug-toolkit,
network-interceptor, and debug-network-interceptor-plugin files. Keep every
runtime optimization in GENERATED_FILES so a fresh run does not fall back to
older inline templates or local pre-existing files.
"""

import os
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]

GENERATED_ROOTS = [
    'libs/debug-tool/debug-toolkit',
    'libs/debug-tool/network-interceptor',
    'build-logic/debug-network-interceptor-plugin',
]

LEGACY_DEBUG_TOOLKIT_PACKAGE_PATH = "com/" + "vco" + "key/common/debugtoolkit"
LEGACY_NETWORK_INTERCEPTOR_PACKAGE_PATH = "com/" + "vco" + "key/common/debugnetworkinterceptor"

WANGWX_DEBUG_TOOLKIT_PACKAGE_PATH = "io/" + "github/wang" + "wx/debugtoolkit"
WANGWX_NETWORK_INTERCEPTOR_PACKAGE_PATH = "io/" + "github/wang" + "wx/debugnetworkinterceptor"

STALE_PATHS = [
    # vcokey legacy
    'libs/debug-tool/debug-toolkit/src/main/assets/deeplink-test.html',
    f'libs/debug-tool/debug-toolkit/src/main/java/{LEGACY_DEBUG_TOOLKIT_PACKAGE_PATH}/DebugButtonAdapter.kt',
    f'libs/debug-tool/debug-toolkit/src/main/java/{LEGACY_DEBUG_TOOLKIT_PACKAGE_PATH}/DebugConfig.kt',
    f'libs/debug-tool/debug-toolkit/src/main/java/{LEGACY_DEBUG_TOOLKIT_PACKAGE_PATH}/DebugFloatingWindowService.kt',
    f'libs/debug-tool/debug-toolkit/src/main/java/{LEGACY_DEBUG_TOOLKIT_PACKAGE_PATH}/DebugInitProvider.kt',
    f'libs/debug-tool/debug-toolkit/src/main/java/{LEGACY_DEBUG_TOOLKIT_PACKAGE_PATH}/DebugOkHttpManager.kt',
    f'libs/debug-tool/debug-toolkit/src/main/java/{LEGACY_DEBUG_TOOLKIT_PACKAGE_PATH}/DebugWebViewActivity.kt',
    f'libs/debug-tool/debug-toolkit/src/main/java/{LEGACY_DEBUG_TOOLKIT_PACKAGE_PATH}/interceptor/BaseUrlInterceptor.kt',
    f'libs/debug-tool/debug-toolkit/src/main/java/{LEGACY_DEBUG_TOOLKIT_PACKAGE_PATH}/interceptor/DetailLogInterceptor.kt',
    f'libs/debug-tool/network-interceptor/src/debug/java/{LEGACY_NETWORK_INTERCEPTOR_PACKAGE_PATH}/DebugNetworkConfigEditorActivity.kt',
    f'libs/debug-tool/network-interceptor/src/debug/java/{LEGACY_NETWORK_INTERCEPTOR_PACKAGE_PATH}/DebugNetworkConfigReloadReceiver.kt',
    f'libs/debug-tool/network-interceptor/src/debug/java/{LEGACY_NETWORK_INTERCEPTOR_PACKAGE_PATH}/DebugNetworkInitProvider.kt',
    f'libs/debug-tool/network-interceptor/src/debug/java/{LEGACY_NETWORK_INTERCEPTOR_PACKAGE_PATH}/DebugNetworkProcessGuard.kt',
    f'libs/debug-tool/network-interceptor/src/main/java/{LEGACY_NETWORK_INTERCEPTOR_PACKAGE_PATH}/BaseUrlInterceptor.kt',
    f'libs/debug-tool/network-interceptor/src/main/java/{LEGACY_NETWORK_INTERCEPTOR_PACKAGE_PATH}/DebugNetworkConfigManager.kt',
    f'libs/debug-tool/network-interceptor/src/main/java/{LEGACY_NETWORK_INTERCEPTOR_PACKAGE_PATH}/DebugNetworkConfigModel.kt',
    f'libs/debug-tool/network-interceptor/src/main/java/{LEGACY_NETWORK_INTERCEPTOR_PACKAGE_PATH}/DebugNetworkConfigPanel.kt',
    f'libs/debug-tool/network-interceptor/src/main/java/{LEGACY_NETWORK_INTERCEPTOR_PACKAGE_PATH}/DebugNetworkInterceptorManager.kt',
    # wangwx legacy (io.github.wangwx)
    f'libs/debug-tool/debug-toolkit/src/main/java/{WANGWX_DEBUG_TOOLKIT_PACKAGE_PATH}/DebugButtonAdapter.kt',
    f'libs/debug-tool/debug-toolkit/src/main/java/{WANGWX_DEBUG_TOOLKIT_PACKAGE_PATH}/DebugConfig.kt',
    f'libs/debug-tool/debug-toolkit/src/main/java/{WANGWX_DEBUG_TOOLKIT_PACKAGE_PATH}/DebugFloatingWindowService.kt',
    f'libs/debug-tool/debug-toolkit/src/main/java/{WANGWX_DEBUG_TOOLKIT_PACKAGE_PATH}/DebugInitProvider.kt',
    f'libs/debug-tool/debug-toolkit/src/main/java/{WANGWX_DEBUG_TOOLKIT_PACKAGE_PATH}/DebugOkHttpManager.kt',
    f'libs/debug-tool/debug-toolkit/src/main/java/{WANGWX_DEBUG_TOOLKIT_PACKAGE_PATH}/DebugWebViewActivity.kt',
    f'libs/debug-tool/debug-toolkit/src/main/java/{WANGWX_DEBUG_TOOLKIT_PACKAGE_PATH}/interceptor/BaseUrlInterceptor.kt',
    f'libs/debug-tool/debug-toolkit/src/main/java/{WANGWX_DEBUG_TOOLKIT_PACKAGE_PATH}/interceptor/DetailLogInterceptor.kt',
    f'libs/debug-tool/network-interceptor/src/debug/java/{WANGWX_NETWORK_INTERCEPTOR_PACKAGE_PATH}/DebugNetworkConfigEditorActivity.kt',
    f'libs/debug-tool/network-interceptor/src/debug/java/{WANGWX_NETWORK_INTERCEPTOR_PACKAGE_PATH}/DebugNetworkConfigReloadReceiver.kt',
    f'libs/debug-tool/network-interceptor/src/debug/java/{WANGWX_NETWORK_INTERCEPTOR_PACKAGE_PATH}/DebugNetworkInitProvider.kt',
    f'libs/debug-tool/network-interceptor/src/debug/java/{WANGWX_NETWORK_INTERCEPTOR_PACKAGE_PATH}/DebugNetworkProcessGuard.kt',
    f'libs/debug-tool/network-interceptor/src/main/java/{WANGWX_NETWORK_INTERCEPTOR_PACKAGE_PATH}/BaseUrlInterceptor.kt',
    f'libs/debug-tool/network-interceptor/src/main/java/{WANGWX_NETWORK_INTERCEPTOR_PACKAGE_PATH}/DebugNetworkConfigManager.kt',
    f'libs/debug-tool/network-interceptor/src/main/java/{WANGWX_NETWORK_INTERCEPTOR_PACKAGE_PATH}/DebugNetworkConfigModel.kt',
    f'libs/debug-tool/network-interceptor/src/main/java/{WANGWX_NETWORK_INTERCEPTOR_PACKAGE_PATH}/DebugNetworkConfigPanel.kt',
    f'libs/debug-tool/network-interceptor/src/main/java/{WANGWX_NETWORK_INTERCEPTOR_PACKAGE_PATH}/DebugNetworkInterceptorManager.kt',
]

GENERATED_FILES = {
    'libs/debug-tool/debug-toolkit/build.gradle': r'''plugins {
    id 'com.android.library'
    id 'kotlin-android'
    id 'maven-publish'
}

android {
    namespace 'com.debugtoolkit'
    compileSdk 36

    defaultConfig {
        minSdk 21
        targetSdk 36
        versionCode 1
        versionName "1.0"

       // testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles "consumer-rules.pro"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    /*
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = '1.8'
    }
    */

    publishing {
        singleVariant('release')
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = '17'
    }
}

dependencies {
    api project(':debug-tool:network-interceptor')

    implementation libs.bundles.jetpack
    implementation libs.material
    implementation(libs.bundles.network.client)

    implementation libs.mmkv

    // 添加 Logcat 依赖 (根据文档)
    implementation 'com.github.getActivity:Logcat:12.5'

    // 如果不使用 libs 方式，可以打开下面的注释
    // implementation 'androidx.core:core-ktx:1.12.0'
    // implementation 'androidx.appcompat:appcompat:1.6.1'
    // implementation 'com.google.android.material:material:1.11.0'
    // implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    // testImplementation 'junit:junit:4.13.2'
    // androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    // androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}

afterEvaluate {
    publishing {
        publications {
            release(MavenPublication) {
                from components.release
                artifactId = 'debug-toolkit'
            }
        }
    }
}
''',
    'libs/debug-tool/debug-toolkit/consumer-rules.pro': r'''# Consumer rules for apps that depend on debug-toolkit.
''',
    'libs/debug-tool/debug-toolkit/proguard-rules.pro': r'''# Module shrinker rules for debug-toolkit release builds.
''',
    'libs/debug-tool/debug-toolkit/src/main/AndroidManifest.xml': r'''<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- 悬浮窗权限（必须） -->
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />

    <application>
        <!-- 初始化Provider（自动加载配置并启动服务） -->
        <provider
            android:name=".DebugInitProvider"
            android:authorities="${applicationId}.debugtoolkit.init"
            android:exported="false"
            android:multiprocess="false" />

        <!-- 浮窗服务（显示调试工具） -->
        <service
            android:name=".DebugFloatingWindowService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="specialUse" />

        <!-- WebView Activity -->
        <activity
            android:name=".DebugWebViewActivity"
            android:exported="false"
            android:theme="@style/Theme.DebugToolkit.NoActionBar"
            android:configChanges="orientation|screenSize|keyboardHidden" />

        <!-- Logcat 配置 (根据文档) -->
        <!-- 关闭悬浮窗入口，使用通知栏入口 -->
        <meta-data
            android:name="LogcatWindowEntrance"
            android:value="false" />
        <meta-data
            android:name="LogcatNotifyEntrance"
            android:value="false" />

        <!-- 设置默认日志级别为 ERROR -->
        <meta-data
            android:name="LogcatDefaultLogLevel"
            android:value="E" />

        <!-- 关闭日志自动合并打印 -->
        <meta-data
            android:name="LogcatAutoMergePrint"
            android:value="false" />

        <!-- 设置日志样式为带边框 -->
        <meta-data
            android:name="LogcatLogStyle"
            android:value="1" />

    </application>

</manifest>
''',
    'libs/debug-tool/debug-toolkit/src/main/assets/deeplink-reference.html': r'''<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>深度链接参考列表</title>

    <script>
        function goToApp() {
          var links = generateLinks();
            alert(links.deepLink);
            try {
              checkAndroidAppInstalled();
            } catch (e) {
              alert(e);
              window.location.href = links.googlePlayLink;
            }
          }

        function checkAndroidAppInstalled() {
          var iframe = document.getElementById("iframe");
          var { deepLink, googlePlayLink, universalLink } = generateLinks();
          iframe.src = deepLink;
          setTimeout(function() {
            if (iframe.contentWindow.location.href === deepLink) {
              alert("App is installed");
            } else {
              alert("App is not installed");
              window.location.href = universalLink;
              // window.open(universalLink);
              // iframe.src = universalLink;
            }
          }, 1000);
        }

        function generateLinks() {
          var universalLink = 'https://b4scdn.shortdramalabs.com/apps/reelrush/';

          // 安卓自启动的链接
          // var deepLink = 'intent://apps/reelrush#Intent;scheme=https;package=company.reelrush.and;host=b4scdn.shortdramalabs.com;end;';
          deepLink = 'intent://b4scdn.shortdramalabs.com/apps/reelrush#Intent;scheme=https;package=company.reelrush.and;end;';
          deepLink = 'reelrush://navigator/ranking/123?section=53';

          // 谷歌应用商店的链接
          var googlePlayLink = "market://details?id=company.reelrush.and";
          return { deepLink, universalLink, googlePlayLink };
        }
    </script>

    <style>
        * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; background-color: #f7f7f7; margin: 0; padding: 20px; color: #333; line-height: 1.5; }
        .container { max-width: 600px; margin: 0 auto; }

        /* --- 卡片通用样式 --- */
        .card {
            background: #ffffff;
            border-radius: 12px;
            padding: 20px;
            margin-bottom: 15px;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
            /* 使用变量 var(--site-color)，如果没有定义，默认为蓝色 #007AFF */
            border-top: 4px solid var(--site-color, #007AFF);
        }

        .card-title {
            font-size: 16px;
            font-weight: 700;
            margin-bottom: 15px;
            /* 标题颜色也跟随变量 */
            color: var(--site-color, #007AFF);
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .sub-title {
            font-size: 13px;
            color: #888;
            margin-top: -10px;
            margin-bottom: 12px;
            padding-left: 5px;
            font-weight: 500;
        }

        /* --- 按钮通用样式 --- */
        .btn-link {
            display: flex; align-items: center; justify-content: center;
            width: 100%; padding: 12px 15px; margin-bottom: 10px;
            color: white; text-decoration: none;
            border-radius: 8px; font-size: 15px; font-weight: 500;
            transition: opacity 0.2s, transform 0.1s;
            border: none; cursor: pointer;
            /* 按钮背景色跟随变量 */
            background-color: var(--site-color, #007AFF);
        }

        /* Intent 按钮稍微淡一点，便于区分 (可选) */
        .btn-link.intent {
            opacity: 0.9;
        }

        .btn-link:active { opacity: 0.8; transform: scale(0.98); }
        .card .btn-link:last-child { margin-bottom: 0; }

        /* --- 特殊样式（不受变量影响） --- */
        .btn-link.back { background-color: #8E8E93; }


        .bottom-box {
          opacity: 1;
          position: fixed;
          bottom: 0.9rem;
          /* 15px / 100 */
          display: flex;
          width: 100%;
          height: 100px;
          padding: 0.25rem 0.12rem;
          /* 25px 12px / 100 */
          box-sizing: border-box;
          z-index: 20;
        }
    </style>
</head>
<body>

<div class="container">

    <!-- 站点 1: TapShort -->
    <!-- 只需要在这里定义 style="--site-color: #FF9500"，下面所有元素会自动变成橙色 -->
    <div class="card" style="--site-color: #FF9500;">
        <div class="card-title">
            TapShort (FinalNovel)
        </div>

        <div class="sub-title">通用链接 (HTTPS)</div>
        <a href="https://b4scdn.finalnovel.net/apps/tapshort" class="btn-link">打开 App 详情页面</a>
        <a href="https://b4scdn.finalnovel.net/apps/tapshort/app" class="btn-link">打开 App 详情页面 1</a>

        <div class="sub-title">Intent 调用 (Android)</div>
        <a href="intent://apps/tapshort#Intent;scheme=https;package=com.finalnovel.app;host=b4scdn.finalnovel.net;end;" class="btn-link intent">打开 App 详情页面 2</a>
        <a href="intent://b4scdn.finalnovel.net/apps/tapshort#Intent;scheme=https;package=com.tapshort.app;end;" class="btn-link intent">打开 App 详情页面 3</a>
        <a href="intent://b4scdn.finalnovel.net/apps/tapshort/xxx#Intent;scheme=https;package=com.tapshort.app;end;" class="btn-link intent">打开 App 详情页面 4</a>
        <a href="intent://apps/tapshort#Intent;scheme=finalnovel;package=com.finalnovel.app;S.browser_fallback_url=https://play.google.com/store/apps/details?id=com.finalnovel.app;end;" class="btn-link intent">TapShort (Apps: Play Store)</a>
        <a href="intent://deeplink/apps/tapshort#Intent;scheme=finalnovel;package=com.finalnovel.app;S.browser_fallback_url=https://play.google.com/store/apps/details?id=com.finalnovel.app;end;" class="btn-link intent">TapShort (Fallback: Play Store)</a>

        <div class="sub-title">Scheme (自定义协议)</div>
        <a href="finalnovel://navigator/video/player/174875?ndl_pid=snapchat_int&ndl_campaign_id={{campaign.id}}&ndl_ad_id={{ad.id}}&ndl_adset_id={{adSet.id}}&ndl_act_id=bed53a6d-0670-4868-a58b-1a3393ef9a43&ndl_book_id=174875&lang=en-us&pid=snapchat_int&utm_source={{site_source_name}}" class="btn-link">TapShort (Snap 测试链接)</a>
        <a href="finalnovel://navigator/ranking/123?section=53" class="btn-link">TapShort (RankingMore 测试链接)</a>
    </div>

    <!-- 站点 2: ReelRush -->
    <!-- 修改这里的颜色变量即可改变整个卡片颜色 -->
    <div class="card" style="--site-color: #FF3B30;">
        <div class="card-title">
            ReelRush
        </div>

        <div class="sub-title">通用链接 (HTTPS)</div>
        <a href="https://b4scdn.shortdramalabs.com/apps/reelrush/" class="btn-link">打开 App 详情页面</a>

        <div class="sub-title">Intent 调用 (Android)</div>
        <a href="intent://apps/reelrush#Intent;scheme=reelrush;package=company.reelrush.and;S.browser_fallback_url=https://play.google.com/store/apps/details?id=company.reelrush.and;end;" class="btn-link intent">ReelRush (Apps: Play Store)</a>
        <a href="intent://apps/reelrush#Intent;scheme=reelrush;package=company.reelrush.and;host=b4scdn.shortdramalabs.com;end;" class="btn-link intent">ReelRush (Apps: HOST )</a>
        <a href="intent://deeplink/apps/reelrush#Intent;scheme=reelrush;package=company.reelrush.and;host=b4scdn.shortdramalabs.com;end;" class="btn-link intent">ReelRush (deeplink Apps: HOST )</a>
        <a href="intent://deeplink/apps/reelrush#Intent;scheme=reelrush;package=company.reelrush.and;S.browser_fallback_url=https://play.google.com/store/apps/details?id=company.reelrush.and;end;" class="btn-link intent">ReelRush (Fallback: Play Store)</a>

        <div class="sub-title">Scheme (自定义协议)</div>
        <a href="reelrush://navigator/video/player/172842?ndl_pid=snapchat_int&ndl_campaign_id={{campaign.id}}&ndl_ad_id={{ad.id}}&ndl_adset_id={{adSet.id}}&ndl_act_id=bed53a6d-0670-4868-a58b-1a3393ef9a43&ndl_book_id=174875&lang=en-us&pid=snapchat_int&utm_source={{site_source_name}}" class="btn-link">ReelRush (Snap 测试链接)</a>
        <a href="reelrush://navigator/ranking/123?section=53" class="btn-link">ReelRush (RankingMore 测试链接)</a>

        <div class="sub-title">IFrame Scheme (自定义协议)</div>

        <div class="sub-title" onclick="goToApp()">IFrame
            <iframe id="iframe"></iframe>

        </div>
    </div>

    <!-- 站点 3: 测试/通用 -->
    <div class="card" style="--site-color: #5856D6;">
        <div class="card-title">
            测试 & 工具
        </div>

        <div class="sub-title">Intent 测试 (Fallback Zxing)</div>
        <a href="intent://apps/tapshort#Intent;scheme=finalnovel;package=com.tapshort.app;S.browser_fallback_url=http%3A%2F%2Fzxing.org;end;" class="btn-link intent">Take a QR code (Fallback: zxing.org)</a>
    </div>

    <!-- 返回按钮 -->
    <div class="card" id="back-btn-container">
        <button onclick="handleBack()" class="btn-link back">⬅️ 返回工具主页</button>
    </div>

</div>

<script>
    // --- 逻辑 1: 判断环境，决定是否显示返回按钮 ---
    if (window.self === window.top) {
        var backContainer = document.getElementById('back-btn-container');
        if (backContainer) {
            backContainer.style.display = 'none';
        }
    }

    // --- 逻辑 2: 处理返回操作 ---
    function handleBack() {
        window.top.postMessage({ action: 'close-frame' }, '*');
    }
</script>

</body>
</html>
''',
    'libs/debug-tool/debug-toolkit/src/main/java/com/debugtoolkit/DebugButtonAdapter.kt': r'''package com.debugtoolkit

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView

class DebugButtonAdapter(
    private val context: Context,
    private val items: List<DebugFloatingWindowService.ButtonItem>
) : BaseAdapter() {

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): DebugFloatingWindowService.ButtonItem = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            // 创建新视图
            view = LayoutInflater.from(context).inflate(R.layout.item_debug_button, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            // 重用视图
            view = convertView
            holder = view.tag as ViewHolder
        }

        val item = getItem(position)

        // 设置按钮文本和背景色
        holder.button.text = item.text
        holder.button.setBackgroundColor(Color.parseColor(item.backgroundColor))
        holder.button.setOnClickListener { item.onClick() }

        return view
    }

    // ViewHolder模式优化性能
    private class ViewHolder(view: View) {
        val button: Button = view.findViewById(R.id.btn_debug)
    }
}
''',
    'libs/debug-tool/debug-toolkit/src/main/java/com/debugtoolkit/DebugConfig.kt': r'''package com.debugtoolkit

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.tencent.mmkv.MMKV
import java.io.File

object DebugConfig {
    const val TAG = "DebugConfig"

    private const val PREFS_NAME = "debug_toolkit_prefs"
    private const val KEY_SELECTED_HOST = "key_selected_host"
    private const val KEY_INTERCEPTOR_ENABLED = "key_interceptor_enabled"
    private const val KEY_LOG_ENABLED = "key_log_enabled"
    private const val PRODUCTION_ENV_NAME = "生产环境"

    // ==================== 悬浮窗位置持久化 ====================
    private const val KEY_LAST_FLOAT_X = "key_last_float_x"
    private const val KEY_LAST_FLOAT_Y = "key_last_float_y"
    private const val KEY_HAS_SAVED_POSITION = "key_has_saved_position"

    private var prefs: SharedPreferences? = null

    // TOOD 等待后面使用 Plugin 注入 实现
    // 环境配置（可扩展）
    val ENVIRONMENTS = mapOf(
        "开发环境" to "http://dev.example.com",
        "测试环境" to "http://test.example.com",
        "生产环境" to "http://prod.example.com"
    )

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // 当前选中环境
    var selectedHost: String
        get() = prefs?.getString(KEY_SELECTED_HOST, ENVIRONMENTS.values.first()) ?: ENVIRONMENTS.values.first()
        set(value) {
            prefs?.edit()?.putString(KEY_SELECTED_HOST, value)?.apply()
        }

    // 拦截器开关
    var isInterceptorEnabled: Boolean
        get() = prefs?.getBoolean(KEY_INTERCEPTOR_ENABLED, true) ?: true
        set(value) {
            prefs?.edit()?.putBoolean(KEY_INTERCEPTOR_ENABLED, value)?.apply()
        }

    // 日志开关
    var isLogEnabled: Boolean
        get() = prefs?.getBoolean(KEY_LOG_ENABLED, true) ?: true
        set(value) {
            prefs?.edit()?.putBoolean(KEY_LOG_ENABLED, value)?.apply()
        }

    // 兼容旧环境配置，网络映射入口已迁移到底层网络拦截配置面板。
    fun selectedHostEnvName(): String {
        return ENVIRONMENTS.entries.firstOrNull { it.value == selectedHost }?.key ?: "未知环境"
    }

    fun selectedBaseUrlMappings(): Map<String, String> {
        val sourceBaseUrl = ENVIRONMENTS[PRODUCTION_ENV_NAME] ?: return emptyMap()
        val targetBaseUrl = selectedHost
        // 浮窗环境切换沿用旧逻辑：以生产环境作为 source，只把命中的生产 baseUrl 改到当前目标环境。
        if (sourceBaseUrl == targetBaseUrl) return emptyMap()
        return mapOf(sourceBaseUrl to targetBaseUrl)
    }

    // ==================== 悬浮窗位置持久化 ====================

    fun saveFloatPosition(x: Int, y: Int) {
        prefs?.edit()
            ?.putInt(KEY_LAST_FLOAT_X, x)
            ?.putInt(KEY_LAST_FLOAT_Y, y)
            ?.putBoolean(KEY_HAS_SAVED_POSITION, true)
            ?.apply()
    }

    fun getLastFloatX(): Int? {
        val currentPrefs = prefs ?: return null
        return if (currentPrefs.getBoolean(KEY_HAS_SAVED_POSITION, false)) {
            currentPrefs.getInt(KEY_LAST_FLOAT_X, 0)
        } else null
    }

    fun getLastFloatY(): Int? {
        val currentPrefs = prefs ?: return null
        return if (currentPrefs.getBoolean(KEY_HAS_SAVED_POSITION, false)) {
            currentPrefs.getInt(KEY_LAST_FLOAT_Y, 200)
        } else null
    }

    fun hasSavedPosition(): Boolean {
        return prefs?.getBoolean(KEY_HAS_SAVED_POSITION, false) ?: false
    }

        // ==================== 清除 MMKV 数据 ====================


        /**
         * 清除所有 MMKV 数据（直接调用 API，无需反射）
         */
        fun clearMMKVData(): Boolean {
            return try {
                // 1. 清除默认实例
                MMKV.defaultMMKV().clearAll()

                // 2. 遍历 mmkv 目录，清除所有自定义 mmapID 的实例
                val mmkvDir = File(MMKV.getRootDir())
                if (mmkvDir.exists() && mmkvDir.isDirectory) {
                    mmkvDir.listFiles()
                        ?.filter { it.name.endsWith(".crc") }
                        ?.forEach { crcFile ->
                            // 文件名格式: {mmapID}.crc
                            val mmapID = crcFile.name.removeSuffix(".crc")
                            try {
                                MMKV.mmkvWithID(mmapID).clearAll()
                            } catch (e: Exception) {
                                Log.d(TAG, "clearMMKVData: ${e.message }")
                                false
                            }
                        }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

}
''',
    'libs/debug-tool/debug-toolkit/src/main/java/com/debugtoolkit/DebugFloatingWindowService.kt': r'''package com.debugtoolkit

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.annotation.SuppressLint
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.GridView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.debugtoolkit.networkinterceptor.DebugNetworkConfigPanel
import kotlin.system.exitProcess

class DebugFloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var triggerView: TextView
    private lateinit var menuContainer: View

    // 保存宿主App图标
    private var hostAppIcon: Drawable? = null

    private val CHANNEL_ID = "DebugToolkit_Channel"
    private val NOTIFICATION_ID = 1004
    private var isMenuOpen = false
    private var layoutParams: WindowManager.LayoutParams? = null

    // 拖拽相关变量
    private var dX = 0f
    private var dY = 0f

    // 点击判定变量
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var isDragging = false

    companion object {
        private const val REQUEST_CODE_RESTART = 10086
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startMyForegroundService()

        // 获取并保存宿主App图标
        getHostAppIcon()

        // 使用 Handler 延迟检查，确保服务初始化完成
        Handler(Looper.getMainLooper()).post {
            if (Settings.canDrawOverlays(this)) {
                showFloatingWindow()
            } else {
                requestOverlayPermission()
            }
        }
    }

    private fun startMyForegroundService() {
        val notification = getNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /**
     * 获取并保存宿主App图标
     */
    private fun getHostAppIcon() {
        try {
            hostAppIcon = packageManager.getApplicationIcon(application.packageName)
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果获取失败，保持为null，使用默认背景
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun showFloatingWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_debug_floating_window, null)

        triggerView = floatingView.findViewById<TextView>(R.id.btn_trigger)
        menuContainer = floatingView.findViewById(R.id.layout_menu)

        menuContainer.visibility = View.GONE

        // 设置宿主App图标作为背景
        setHostAppIconAsBackground()

        // 初始设置为 WRAP_CONTENT
        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },

           // 默认状态：不获取焦点 + 允许点击穿透

            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        layoutParams?.gravity = Gravity.TOP or Gravity.START

        // ==================== 恢复上次保存的位置 ====================
        if (DebugConfig.hasSavedPosition()) {
            val savedX = DebugConfig.getLastFloatX()
            val savedY = DebugConfig.getLastFloatY()
            if (savedX != null && savedY != null) {
                layoutParams?.x = savedX
                layoutParams?.y = savedY
            } else {
                layoutParams?.x = 0
                layoutParams?.y = 200
            }
        } else {
            layoutParams?.x = 0
            layoutParams?.y = 200
        }

        try {
            windowManager.addView(floatingView, layoutParams)
            setupViews()
            setupDragListener()
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    /**
     * 🔥 核心方法：设置宿主App图标作为背景
     */
    private fun setHostAppIconAsBackground() {
        // 如果有保存的图标，则设置
        hostAppIcon?.let { triggerView.background = it }
        // 如果没有，保持XML中设置的默认背景

    }

    /**
     * 🔥 核心方法：动态切换窗口焦点状态
     * @param needFocus true: 移除 FLAG_NOT_FOCUSABLE (允许输入，会拦截返回键)
     *                 false: 添加 FLAG_NOT_FOCUSABLE (不允许输入，返回键传给背景)
     */
    private fun updateWindowFocus(needFocus: Boolean) {
        val params = layoutParams ?: return
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        if (needFocus) {
            // 需要输入：移除 FLAG_NOT_FOCUSABLE，让窗口能获取焦点，从而弹出键盘
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            imm.showSoftInput(floatingView.findFocus(), 0)
        } else {
            // 不需要输入：恢复 FLAG_NOT_FOCUSABLE，让返回键传递给背景应用
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            imm.hideSoftInputFromWindow(floatingView.windowToken, 0)
            // 清除输入框的焦点
            floatingView.findFocus()?.clearFocus()
        }
        windowManager.updateViewLayout(floatingView, params)
    }

    private fun toggleMenu() {
        isMenuOpen = !isMenuOpen
        if (isMenuOpen) {
            menuContainer.visibility = View.VISIBLE
            // 恢复宿主App图标背景
            hostAppIcon?.let {
                triggerView.background = it
            }
        } else {
            // 关闭菜单：隐藏容器
            menuContainer.visibility = View.GONE
            // 恢复宿主App图标背景
            hostAppIcon?.let {
                triggerView.background = it
            }
            // 关闭菜单时，确保退出输入模式
            updateWindowFocus(false)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragListener() {
        // 获取系统推荐触摸阈值
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop.toFloat()

        triggerView.setOnTouchListener { view, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 记录按下的位置
                    touchDownX = event.rawX
                    touchDownY = event.rawY
                    // 记录 Window 的位移
                    dX = params.x.toFloat() - event.rawX
                    dY = params.y.toFloat() - event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    // 移动 Window
                    val moveDistance = Math.hypot(
                        (event.rawX - touchDownX).toDouble(),
                        (event.rawY - touchDownY).toDouble()
                    )
                    // ✅ 只有超过阈值才开始拖拽
                    if (moveDistance > touchSlop) {
                        isDragging = true
                        params.x = (event.rawX + dX).toInt()
                        params.y = (event.rawY + dY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        snapToEdge(view)
                    } else {
                        // ✅ 未超过阈值，才是真正的点击
                        toggleMenu()
                    }
                    true
                }
                else -> false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun snapToEdge(view: View) {
        val windowMetrics = windowManager.currentWindowMetrics
        val bounds = windowMetrics.bounds
        val screenWidth = bounds.width()

        // 简化：始终操作 Window 坐标
        val currentX = layoutParams?.x?.toFloat() ?: 0f
        val currentY = layoutParams?.y?.toFloat() ?: 0f
        val viewWidth = view.width
        val viewHeight = view.height

        val centerX = currentX + viewWidth / 2
        val targetX = if (centerX < screenWidth / 2) {
            0f
        } else {
            (screenWidth - viewWidth).toFloat()
        }

        val targetY = currentY.coerceAtLeast(0f).coerceAtMost(bounds.height().toFloat() - viewHeight)

        // 瞬移窗口位置
        layoutParams?.x = targetX.toInt()
        layoutParams?.y = targetY.toInt()
        windowManager.updateViewLayout(floatingView, layoutParams)

        // 保存吸附后的位置
        saveFloatPosition()
    }

    // ==================== 位置持久化 ====================

    private fun saveFloatPosition() {
        val params = layoutParams ?: return
        DebugConfig.saveFloatPosition(params.x, params.y)
    }

    // ==================== 重启 App ====================

    /**
     * 重启原理：
     * 1. 直接 startActivity(CLEAR_TASK) 拉起新的启动页
     * 2. 延迟 300ms 后 killProcess 杀掉旧进程
     *
     * 不使用 AlarmManager，避免精确闹钟权限问题
     */
    private fun restartApp() {
        try {
            // 先移除悬浮窗
            try { windowManager.removeView(floatingView) } catch (_: Exception) {}

            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent == null) {
                Toast.makeText(this, "无法获取启动Intent", Toast.LENGTH_SHORT).show()
                return
            }
            // CLEAR_TASK: 销毁所有现有 Activity 栈
            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
            startActivity(launchIntent)

            Toast.makeText(this, "重启中，请稍后", Toast.LENGTH_LONG).show()

            // 延迟杀进程，确保新 Activity 已创建
            // Handler(Looper.getMainLooper()).postDelayed({
                android.os.Process.killProcess(android.os.Process.myPid())
            // }, 1000)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "重启失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ==================== 清除 MMKV ====================

    private fun clearMMKVData() {
        val success = DebugConfig.clearMMKVData()
        val message = if (success) "MMKV 数据已清除" else "清除 MMKV 失败"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        if (success) {
            Handler(Looper.getMainLooper()).postDelayed({
                restartApp()
            }, 100)
        }
    }

    // ==================== 归因 Mock ====================

    /**
     * 通过反射调用宿主 App 的归因 Mock 方法，走完整归因链路。
     * @param methodName 宿主 Application 的方法名，如 "debugMockAFAttribution" 或 "debugMockHTMAttribution"
     * @param linkValue 归因链接或追踪链接
     */
    private fun mockAttributionFromApp(methodName: String, linkValue: String) {
        try {
            val app = application
            val mockMethod = app.javaClass.getMethod(methodName, String::class.java)
            mockMethod.invoke(app, linkValue)
            Toast.makeText(this, "归因Mock: $methodName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "归因Mock失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        toggleMenu()
    }

    private fun resetAttributionFromApp() {
        try {
            val method = application.javaClass.getMethod("debugResetAttribution")
            method.invoke(application)
            Toast.makeText(this, "归因状态已重置", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "重置失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        toggleMenu()
    }

    // ==================== UI 权限与通知 ====================

    private fun requestOverlayPermission() {
        Toast.makeText(this, "请授予悬浮窗权限以显示调试工具", Toast.LENGTH_LONG).show()
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        stopSelf() // 权限被拒绝时停止服务
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "DebugToolkit Service",
                NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun getNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DebugToolkit")
            .setContentText("调试工具正在运行")
            .setSmallIcon(R.drawable.ic_search)
            .build()
    }

    // ==================== 设置视图 ====================

    private fun setupViews() {
        val gridViewButtons = menuContainer.findViewById<GridView>(R.id.grid_buttons)
        val etUri = menuContainer.findViewById<EditText>(R.id.et_uri)
        val btnOpenUri = menuContainer.findViewById<Button>(R.id.btn_open_uri)
        val btnClearUri = menuContainer.findViewById<Button>(R.id.btn_clear_uri)

        // ==================== 网格按钮（含重启 + 清MMKV） ====================
        val buttonItems = listOf(
            // 第一行
            ButtonItem("日志", R.drawable.ic_search, "#FF5722") {
                try {
                    val intent = Intent(this, Class.forName("com.hjq.logcat.LogcatActivity"))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "启动日志页面失败", Toast.LENGTH_SHORT).show()
                }
                toggleMenu()
            },
            ButtonItem("权限", R.drawable.ic_search, "#2196F3") {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                toggleMenu()
            },
            ButtonItem("开发者", R.drawable.ic_search, "#4CAF50") {
                val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                toggleMenu()
            },
            // 第二行
            ButtonItem("主页", R.drawable.ic_search, "#9C27B0") {
                try {
                    val intent = packageManager.getLaunchIntentForPackage(packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent?.let { startActivity(it) }
                    toggleMenu()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "打开主页失败", Toast.LENGTH_SHORT).show()
                }
            },
            ButtonItem("WebView", R.drawable.ic_search, "#FF9800") {
                val intent = Intent(this, DebugWebViewActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                toggleMenu()
            },
            ButtonItem("关闭", R.drawable.ic_search, "#F44336") {
                android.os.Process.killProcess(android.os.Process.myPid())
                toggleMenu()
            },
            // 第三行（新增）
            ButtonItem("重启", R.drawable.ic_search, "#009688") {
                restartApp()
            },
            ButtonItem("清MMKV", R.drawable.ic_search, "#795548") {
                clearMMKVData()
            },
            ButtonItem("网络", R.drawable.ic_search, "#607D8B") {
                DebugNetworkConfigPanel.show(this) { restartApp() }
            },
            // 第四行（归因测试 - 模拟真实 AF/HTM 归因回调链路）
            ButtonItem("AF→短剧", R.drawable.ic_search, "#E91E63") {
                mockAttributionFromApp("debugMockAFAttribution", "applovin_drama_123_0__debug__001")
            },
            ButtonItem("AF→小说", R.drawable.ic_search, "#3F51B5") {
                mockAttributionFromApp("debugMockAFAttribution", "applovin_novel_456_0__debug__001")
            },
            ButtonItem("HTM→短剧", R.drawable.ic_search, "#009688") {
                mockAttributionFromApp("debugMockHTMAttribution", "funshorts://navigator/video/player/123/0")
            },
            // 第五行（归因测试续）
            ButtonItem("HTM→小说", R.drawable.ic_search, "#795548") {
                mockAttributionFromApp("debugMockHTMAttribution", "funshorts://navigator/novel/read/456/0")
            },
            ButtonItem("重置归因", R.drawable.ic_search, "#FF9800") {
                resetAttributionFromApp()
            }
        )

        // 设置网格适配器
        val gridAdapter = DebugButtonAdapter(this, buttonItems)
        gridViewButtons.adapter = gridAdapter

        btnOpenUri.setOnClickListener {
            val uriString = etUri.text.toString().trim()
            if (uriString.isNotEmpty()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    toggleMenu()
                    // 打开链接后，恢复不获取焦点状态
                    updateWindowFocus(false)
                } catch (e: Exception) {
                    Toast.makeText(this, "无效的 URI: $uriString", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "请输入 URI", Toast.LENGTH_SHORT).show()
            }
        }

        // 🔥 关键修改：处理输入框的焦点和粘贴问题
        etUri.setOnFocusChangeListener { _, hasFocus ->
            updateWindowFocus(hasFocus)
        }

        // 点击时也确保获取焦点
        etUri.setOnClickListener {
            etUri.requestFocus()
            updateWindowFocus(true)
        }

        // 监听回车键，完成输入后自动收起键盘
        etUri.setOnEditorActionListener { _, _, _ ->
            updateWindowFocus(false)
            false
        }

        btnClearUri.setOnClickListener {
            etUri.text?.clear()
            etUri.requestFocus()
            updateWindowFocus(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 销毁时保存当前位置
        saveFloatPosition()
        try {
            windowManager.removeView(floatingView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // 按钮数据类
    data class ButtonItem(
        val text: String,
        val iconResId: Int,
        val backgroundColor: String,
        val onClick: () -> Unit
    )
}
''',
    'libs/debug-tool/debug-toolkit/src/main/java/com/debugtoolkit/DebugInitProvider.kt': r'''package com.debugtoolkit

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri

class DebugInitProvider : ContentProvider() {

    companion object {
        private var isRegistered = false
        private var alertPermissionDialog: AlertDialog? = null
    }

    override fun onCreate(): Boolean {
        val context = context ?: return false
        Log.d("DebugToolkit", "DebugInitProvider onCreate.")

        // 初始化配置
        DebugConfig.init(context)

        // 注册生命周期回调（确保单次注册）
        if (!isRegistered) {
            registerAppLifecycle(context)
            isRegistered = true
        }

        return true
    }

    /**
     * 注册应用生命周期监听，处理权限授予后的启动
     */
    private fun registerAppLifecycle(context: Context) {
        try {
            (context.applicationContext as Application).registerActivityLifecycleCallbacks(
                object : Application.ActivityLifecycleCallbacks {
                    override fun onActivityResumed(activity: Activity) {
                        if (Settings.canDrawOverlays(activity)) {
                            tryStartService(activity)
                            (context.applicationContext as Application).unregisterActivityLifecycleCallbacks(this)
                        } else {
                            showPermissionDialog(activity, this)
                        }
                    }

                    // 其他方法不需要实现
                    override fun onActivityPaused(activity: Activity) {}
                    override fun onActivityStarted(activity: Activity) {}
                    override fun onActivityDestroyed(activity: Activity) {}
                    override fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) {}
                    override fun onActivityStopped(activity: Activity) {}
                    override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) {}
                }
            )

            Log.d("DebugToolkit", "ActivityLifecycleCallbacks registered.")
        } catch (e: Exception) {
            Log.e("DebugToolkit", "Failed to register lifecycle callback: ${e.message}")
        }
    }

    private fun showPermissionDialog(activity: Activity, callback: Application.ActivityLifecycleCallbacks) {
        if (alertPermissionDialog?.isShowing == true) alertPermissionDialog?.dismiss()

        // 创建 Dialog
        alertPermissionDialog = AlertDialog.Builder(activity)
            .setTitle("提示")
            .setMessage("请授予悬浮窗权限以显示调试工具")
            .setPositiveButton("确定") { _, _ -> requestOverlayPermission(activity) }
            .setNegativeButton("取消") { _, _ ->
                (activity.applicationContext as Application).unregisterActivityLifecycleCallbacks(callback)
            }
            .create()

        // 显示 Dialog
        alertPermissionDialog?.show()

    }

    private fun requestOverlayPermission(activity: Activity) {
        // Toast.makeText(activity, "请授予悬浮窗权限以显示调试工具", Toast.LENGTH_LONG).show()

        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${activity.packageName}".toUri()
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent)
    }

    /**
     * 尝试启动服务
     */
    private fun tryStartService(context: Context) {
        try {
            val serviceIntent = Intent(context, DebugFloatingWindowService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.d("DebugToolkit", "Attempted to start DebugFloatingWindowService.")
        } catch (e: Exception) {
            Log.e("DebugToolkit", "Failed to start service: ${e.message}")
        }
    }

    // ContentProvider必须实现的方法（此处无需实际功能）
    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
}
''',
    'libs/debug-tool/debug-toolkit/src/main/java/com/debugtoolkit/DebugOkHttpManager.kt': r'''package com.debugtoolkit

import android.util.Log
import com.debugtoolkit.networkinterceptor.DebugNetworkInterceptorManager
import okhttp3.OkHttpClient

object DebugOkHttpManager {
    private const val TAG = "DebugOkHttpManager"

    interface OnConfigChangeListener {
        fun onConfigChanged(newBuilder: OkHttpClient.Builder)
    }

    private var configChangeListener: OnConfigChangeListener? = null

    fun setBaseUrlMappings(mappings: Map<String, String>) {
        Log.d(TAG, "set base url mappings count=${mappings.size}")
        DebugNetworkInterceptorManager.setBaseUrlMappings(mappings)
        applyNewConfig()
    }

    fun clearBaseUrlMappings() {
        Log.d(TAG, "clear base url mappings")
        DebugNetworkInterceptorManager.clearBaseUrlMappings()
        applyNewConfig()
    }

    fun setFlowLogEnabled(enabled: Boolean) {
        Log.d(TAG, "set flow log enabled=$enabled")
        DebugNetworkInterceptorManager.setFlowLogEnabled(enabled)
    }

    // 获取OkHttpClient.Builder（供业务模块使用）
    fun getBuilder(): OkHttpClient.Builder {
        val builder = OkHttpClient.Builder()
        apply(builder)
        return builder
    }

    /**
     * Gradle Plugin/ASM 会在 OkHttpClient.Builder.build() 前注入这个入口。
     * 这里必须保持幂等：同一个 Builder 可能被手动接入和 ASM 同时处理，重复添加会导致请求被改写/打印多次。
     */
    @JvmStatic
    fun apply(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        syncDebugConfig()
        return DebugNetworkInterceptorManager.apply(builder)
    }

    // 设置配置变更监听（用于业务模块响应）
    fun setOnConfigChangeListener(listener: OnConfigChangeListener) {
        this.configChangeListener = listener
        Log.d(TAG, "config change listener set=${listener.javaClass.name}")
    }

    // 应用新配置（触发监听）
    fun applyNewConfig() {
        syncDebugConfig()
        val listener = configChangeListener
        Log.d(TAG, "apply new config listener=${listener != null}")
        listener?.onConfigChanged(getBuilder())
    }

    private fun syncDebugConfig() {
        val interceptorEnabled = DebugConfig.isInterceptorEnabled
        val logEnabled = DebugConfig.isLogEnabled
        Log.d(TAG, "sync debug config interceptor=$interceptorEnabled flowLog=$logEnabled")
        DebugNetworkInterceptorManager.setEnabled(interceptorEnabled)
        DebugNetworkInterceptorManager.setFlowLogEnabled(logEnabled)
    }
}
''',
    'libs/debug-tool/debug-toolkit/src/main/java/com/debugtoolkit/DebugWebViewActivity.kt': r'''package com.debugtoolkit

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DebugWebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var titleText: TextView
    private lateinit var backButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_webview)

        // 初始化视图
        webView = findViewById(R.id.webview)
        titleText = findViewById(R.id.title_text)
        backButton = findViewById(R.id.back_button)

        // 设置标题
        titleText.text = "DeepLink 测试"

        // 设置返回按钮点击事件
        backButton.setOnClickListener {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                finish()
            }
        }

        // 配置WebView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }

        // 设置WebChromeClient，处理alert对话框
        webView.webChromeClient = object : WebChromeClient() {
            override fun onJsAlert(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult
            ): Boolean {
                AlertDialog.Builder(this@DebugWebViewActivity)
                    .setTitle("提示")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok) { _, _ -> result.confirm() }
                    .setCancelable(false)
                    .show()
                return true
            }

            override fun onJsConfirm(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult
            ): Boolean {
                AlertDialog.Builder(this@DebugWebViewActivity)
                    .setTitle("确认")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok) { _, _ -> result.confirm() }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> result.cancel() }
                    .setCancelable(false)
                    .show()
                return true
            }
        }

        // 设置WebViewClient，处理页面跳转
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url.toString()
                return if (url.startsWith("http://") || url.startsWith("https://")) {
                    // HTTP/HTTPS链接在WebView中加载
                    view?.loadUrl(url)
                    true
                } else {
                    // 其他scheme（如myapp://）尝试启动外部应用
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                        true
                    } catch (e: ActivityNotFoundException) {
                        // 没有找到处理该scheme的应用，显示错误信息
                        view?.loadUrl("javascript:alert('无法打开链接: $url')")
                        true
                    }
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // 更新标题
                titleText.text = view?.title ?: "DeepLink 测试"
            }
        }

        // 加载assets中的HTML文件
        webView.loadUrl("file:///android_asset/deeplink-reference.html")
    }

    // 处理返回键
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
''',
    'libs/debug-tool/debug-toolkit/src/main/java/com/debugtoolkit/interceptor/DetailLogInterceptor.kt': r'''package com.debugtoolkit.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import okio.GzipSource
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class DetailLogInterceptor : Interceptor {
    private val UTF8 = Charset.forName("UTF-8")
    private val TAG = "Network-Log"

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.nanoTime()

        // 打印请求信息
        log("================================================================")
        log("[REQUEST] ${request.method} ${request.url}")
        log("--- Headers ---")
        request.headers.forEach { (name, value) -> log("  $name: $value") }

        val requestBody = request.body
        if (requestBody != null) {
            val buffer = Buffer()
            requestBody.writeTo(buffer)
            var charset = UTF8
            val contentType = requestBody.contentType()
            if (contentType != null) {
                charset = contentType.charset(UTF8) ?: UTF8
            }
            log("--- Body ---")
            if (isPlaintext(buffer)) {
                log(formatJson(buffer.readString(charset)))
            } else {
                log("[binary ${requestBody.contentLength()}-byte body]")
            }
        }
        log("================================================================")

        try {
            val response = chain.proceed(request)
            val endTime = System.nanoTime()
            val duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime)

            // 处理响应体（解压Gzip）
            val responseBody = response.body
            if (responseBody == null) {
                log("================================================================")
                log("[RESPONSE] ${request.method} ${request.url} (${response.code} in ${duration}ms)")
                log("--- Headers ---")
                response.headers.forEach { (name, value) -> log("  $name: $value") }
                log("--- Body ---")
                log("[empty body]")
                log("================================================================")
                return response
            }
            val source = responseBody.source()
            source.request(Long.MAX_VALUE)
            var buffer = source.buffer

            if ("gzip".equals(response.headers["Content-Encoding"], ignoreCase = true)) {
                GzipSource(buffer.clone()).use { gzippedResponseBody ->
                    buffer = Buffer()
                    buffer.writeAll(gzippedResponseBody)
                }
            }

            var charset = UTF8
            val contentType = responseBody.contentType()
            if (contentType != null) {
                charset = contentType.charset(UTF8) ?: UTF8
            }

            log("================================================================")
            log("[RESPONSE] ${request.method} ${request.url} (${response.code} in ${duration}ms)")
            log("--- Headers ---")
            response.headers.forEach { (name, value) -> log("  $name: $value") }
            log("--- Body ---")
            if (isPlaintext(buffer)) {
                log(formatJson(buffer.clone().readString(charset)))
            } else {
                log("[binary ${buffer.size}-byte body]")
            }
            log("================================================================")

            return response
        } catch (e: Exception) {
            Log.e(TAG, "[ERROR] ${request.method} ${request.url}: ${e.message}", e)
            throw e
        }
    }

    // 判断是否为文本内容（避免打印二进制数据）
    private fun isPlaintext(buffer: Buffer): Boolean {
        try {
            val prefix = Buffer()
            val byteCount = if (buffer.size < 64) buffer.size else 64
            buffer.copyTo(prefix, 0, byteCount)
            for (i in 0..15) {
                if (prefix.exhausted()) break
                val codePoint = prefix.readUtf8CodePoint()
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) return false
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun formatJson(json: String): String {
        return try {
            if (json.startsWith("{")) JSONObject(json).toString(2)
            else if (json.startsWith("[")) JSONArray(json).toString(2)
            else json
        } catch (e: Exception) {
            json
        }
    }

    private fun log(message: String) {
        Log.d(TAG, message)
    }
}
''',
    'libs/debug-tool/debug-toolkit/src/main/res/drawable/bg_float_circle.xml': r'''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="@android:color/holo_blue_bright" />
    <stroke android:width="2dp" android:color="#FFFFFF" />
</shape>''',
    'libs/debug-tool/debug-toolkit/src/main/res/drawable/ic_search.xml': r'''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24.0"
    android:viewportHeight="24.0">
    <path
        android:fillColor="#FF000000"
        android:pathData="M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z" />
</vector>''',
    'libs/debug-tool/debug-toolkit/src/main/res/layout/activity_debug_webview.xml': r'''<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:fitsSystemWindows="true">

    <!-- 顶部工具栏 -->
    <androidx.appcompat.widget.Toolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="?attr/colorPrimaryVariant"
        android:elevation="4dp"
        android:theme="@style/ThemeOverlay.AppCompat.Dark.ActionBar"
        app:popupTheme="@style/ThemeOverlay.AppCompat.Light">

        <ImageButton
            android:id="@+id/back_button"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="start"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:src="@android:drawable/ic_menu_revert"
            android:contentDescription="返回" />

        <TextView
            android:id="@+id/title_text"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:textColor="@android:color/white"
            android:textSize="18sp"
            android:textStyle="bold" />
    </androidx.appcompat.widget.Toolbar>

    <!-- WebView -->
    <WebView
        android:id="@+id/webview"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</LinearLayout>
''',
    'libs/debug-tool/debug-toolkit/src/main/res/layout/item_debug_button.xml': r'''<?xml version="1.0" encoding="utf-8"?>
<Button xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/btn_debug"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:minHeight="60dp"
    android:padding="12dp"
    android:textSize="14sp"
    android:textColor="#FFFFFF"
    android:textStyle="bold" />
''',
    'libs/debug-tool/debug-toolkit/src/main/res/layout/layout_debug_floating_window.xml': r'''<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content">

    <!-- 圆形触发按钮 -->
    <TextView
        android:id="@+id/btn_trigger"
        android:layout_width="50dp"
        android:layout_height="50dp"
        android:text="D"
        android:textColor="#FFFFFF"
        android:gravity="center"
        android:textSize="20sp"
        android:textStyle="bold"
        android:background="@drawable/bg_float_circle"
        android:elevation="10dp"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <!-- 弹出菜单容器 -->
    <androidx.constraintlayout.widget.ConstraintLayout
        android:id="@+id/layout_menu"
        android:layout_width="260dp"
        android:layout_height="wrap_content"
        android:background="#FFFFFF"
        android:padding="12dp"
        android:elevation="12dp"
        android:visibility="gone"
        tools:visibility="visible"
        android:clickable="true"
        android:focusable="true"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/btn_trigger">

        <!-- 使用网格布局排列按钮 -->
        <GridView
            android:id="@+id/grid_buttons"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:numColumns="3"
            android:verticalSpacing="8dp"
            android:horizontalSpacing="8dp"
            android:stretchMode="columnWidth"
            android:layout_marginBottom="12dp"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent" />

        <LinearLayout
            android:id="@+id/layout_uri_container"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginBottom="8dp"
            app:layout_constraintTop_toBottomOf="@id/grid_buttons"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent">

            <EditText
                android:id="@+id/et_uri"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:hint="输入 URI (如 http://...)"
                android:inputType="textUri"
                android:textSize="14sp"
                android:padding="8dp"
                android:maxLines="1"
                android:background="@android:color/transparent" />

            <Button
                android:id="@+id/btn_clear_uri"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginStart="8dp"
                android:text="X"
                android:textColor="#FFFFFF"
                android:textSize="14sp"
                android:background="#9E9E9E"
                android:paddingLeft="8dp"
                android:paddingRight="8dp"
                android:minWidth="24dp"
                android:minHeight="24dp" />
        </LinearLayout>

        <Button
            android:id="@+id/btn_open_uri"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:text="打开链接"
            android:textColor="#FFFFFF"
            android:textSize="14sp"
            android:padding="8dp"
            android:background="#607D8B"
            app:layout_constraintTop_toBottomOf="@id/layout_uri_container"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent" />
    </androidx.constraintlayout.widget.ConstraintLayout>

</androidx.constraintlayout.widget.ConstraintLayout>
''',
    'libs/debug-tool/debug-toolkit/src/main/res/values/colors.xml': r'''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="purple_500">#673AB7</color>
    <color name="purple_700">#512DA8</color>
    <color name="teal_200">#80CBC4</color>
    <color name="teal_700">#00695C</color>
    <color name="white">#FFFFFF</color>
    <color name="black">#000000</color>
</resources>
''',
    'libs/debug-tool/debug-toolkit/src/main/res/values/strings.xml': r'''<resources>
    <string name="app_name">DebugToolkit</string>
</resources>''',
    'libs/debug-tool/debug-toolkit/src/main/res/values/themes.xml': r'''<resources xmlns:tools="http://schemas.android.com/tools">
    <!-- 基础主题 -->
    <style name="Theme.DebugToolkit" parent="Theme.MaterialComponents.DayNight.DarkActionBar">
        <item name="colorPrimary">@color/purple_500</item>
        <item name="colorPrimaryVariant">@color/purple_700</item>
        <item name="colorOnPrimary">@color/white</item>
        <item name="colorSecondary">@color/teal_200</item>
        <item name="colorSecondaryVariant">@color/teal_700</item>
        <item name="colorOnSecondary">@color/black</item>
        <item name="android:statusBarColor">@color/purple_500</item>
    </style>

    <!-- 无ActionBar主题，用于WebViewActivity -->
    <style name="Theme.DebugToolkit.NoActionBar" parent="Theme.MaterialComponents.DayNight.NoActionBar">
        <item name="colorPrimary">@color/teal_200</item>
        <item name="colorPrimaryVariant">@color/purple_700</item>
        <item name="colorOnPrimary">@color/white</item>
        <item name="colorSecondary">@color/teal_200</item>
        <item name="colorSecondaryVariant">@color/teal_700</item>
        <item name="colorOnSecondary">@color/black</item>
        <item name="android:statusBarColor">@color/purple_500</item>
    </style>
</resources>
''',
    'libs/debug-tool/network-interceptor/build.gradle': r'''plugins {
    id 'com.android.library'
    id 'kotlin-android'
    id 'maven-publish'
}

android {
    namespace 'com.debugtoolkit.networkinterceptor'
    compileSdk 36

    defaultConfig {
        minSdk 21
    }

    publishing {
        singleVariant('release')
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = '17'
    }
}

dependencies {
    api libs.okhttp
}

afterEvaluate {
    publishing {
        publications {
            release(MavenPublication) {
                from components.release
                artifactId = 'network-interceptor'
            }
        }
    }
}
''',
    'libs/debug-tool/network-interceptor/src/debug/AndroidManifest.xml': r'''<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Android 9 及以下写公共 Download 需要存储权限；Android 10+ 通过 MediaStore 写入，不需要权限。 -->
    <uses-permission
        android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="28" />

    <application>
        <provider
            android:name=".DebugNetworkInitProvider"
            android:authorities="${applicationId}.debugnetwork.init"
            android:exported="false"
            android:multiprocess="false" />

        <activity
            android:name=".DebugNetworkConfigEditorActivity"
            android:exported="true"
            android:grantUriPermissions="true"
            android:process=":debug_network_config"
            android:theme="@android:style/Theme.Material.Light.NoActionBar">

            <intent-filter>
                <action android:name="com.debugtoolkit.networkinterceptor.action.EDIT_CONFIG" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>

            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />

                <data android:mimeType="application/json" />
                <data android:mimeType="text/json" />
            </intent-filter>

            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />

                <data
                    android:scheme="file"
                    android:pathPattern=".*\\.json" />
            </intent-filter>
        </activity>

        <receiver
            android:name=".DebugNetworkConfigReloadReceiver"
            android:exported="false" />
    </application>

</manifest>
''',
    'libs/debug-tool/network-interceptor/src/debug/assets/debug_network_config.json': r'''{
  "version": 1,
  "templateVersion": 2,
  "rules": [
    {
      "id": "dev",
      "name": "开发环境",
      "mappings": [
        {
          "source": "https:\/\/api.a.com",
          "target": "https:\/\/dev-api.a.com"
        },
        {
          "source": "https:\/\/api.b.com",
          "target": "https:\/\/dev-api.b.com"
        },
        {
          "source": "www.baidu.com",
          "target": "www.baidu1.com"
        }
      ]
    },
    {
      "id": "test",
      "name": "测试环境",
      "mappings": [
        {
          "source": "https:\/\/api.a.com",
          "target": "https:\/\/test-api.a.com"
        },
        {
          "source": "https:\/\/api.b.com",
          "target": "https:\/\/test-api.b.com"
        }
      ]
    },
    {
      "id": "prod",
      "name": "生产环境",
      "mappings": []
    }
  ],
  "selectRuleIds": [
    "dev"
  ]
}''',
    'libs/debug-tool/network-interceptor/src/debug/java/com/debugtoolkit/networkinterceptor/DebugNetworkConfigEditorActivity.kt': r'''package com.debugtoolkit.networkinterceptor

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject

class DebugNetworkConfigEditorActivity : Activity() {
    private lateinit var titleView: TextView
    private lateinit var editorView: EditText
    private lateinit var statusView: TextView

    private var sourceUri: Uri? = null
    private var editingExternalUri: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isDebuggable()) {
            Log.d(TAG, "finish because application is not debuggable")
            finish()
            return
        }

        sourceUri = intent.data
        editingExternalUri = sourceUri != null
        Log.d(
            TAG,
            "onCreate external=$editingExternalUri uri=$sourceUri " +
                    "action=${intent.action} type=${intent.type}"
        )
        buildContentView()
        loadConfigText()
    }

    private fun buildContentView() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        titleView = TextView(this).apply {
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFF222222.toInt())
        }
        root.addView(titleView)

        statusView = TextView(this).apply {
            textSize = 12f
            setTextColor(0xFF666666.toInt())
            setPadding(0, dp(4), 0, dp(8))
        }
        root.addView(statusView)

        editorView = EditText(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 13f
            gravity = Gravity.TOP or Gravity.START
            minLines = 12
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setHorizontallyScrolling(true)
        }
        root.addView(
            editorView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(12), 0, 0)
        }
        actions.addView(createButton("格式化") { formatContent() })
        actions.addView(createButton("保存") { saveContent(finishAfterSave = false) })
        actions.addView(createButton("保存并关闭") { saveContent(finishAfterSave = true) })
        root.addView(actions)

        setContentView(root)
    }

    private fun createButton(text: String, action: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setOnClickListener { action() }
        }
    }

    private fun loadConfigText() {
        val uri = sourceUri
        if (uri != null) {
            titleView.text = "编辑 JSON 配置"
            statusView.text = uri.toString()
            val text = readTextFromUri(uri)
            if (text == null) {
                Log.d(TAG, "external config read empty uri=$uri")
                toast("读取 JSON 文件失败")
                return
            }
            Log.d(TAG, "external config loaded uri=$uri length=${text.length}")
            editorView.setText(text)
            return
        }

        DebugNetworkConfigManager.init(this)
        titleView.text = "编辑网络拦截配置"
        val path = DebugNetworkConfigManager.getConfigFilePath()
        val text = DebugNetworkConfigManager.readConfigText().orEmpty()
        statusView.text = path
        Log.d(TAG, "default config loaded path=$path length=${text.length}")
        editorView.setText(text)
    }

    private fun formatContent() {
        val formatted = parseEditorJson() ?: return
        editorView.setText(formatted)
        Log.d(TAG, "content formatted length=${formatted.length}")
        toast("已格式化")
    }

    private fun saveContent(finishAfterSave: Boolean) {
        val formatted = parseEditorJson() ?: return
        val uri = sourceUri
        Log.d(
            TAG,
            "save start external=$editingExternalUri uri=$uri " +
                    "length=${formatted.length} finishAfterSave=$finishAfterSave"
        )
        val success = if (editingExternalUri && uri != null) {
            writeTextToUri(uri, formatted)
        } else {
            DebugNetworkConfigManager.init(this)
            DebugNetworkConfigManager.writeConfigText(formatted)
        }

        if (!success) {
            Log.d(TAG, "save failed external=$editingExternalUri uri=$uri")
            toast("保存失败，请确认文件写入权限")
            return
        }

        editorView.setText(formatted)
        sendReloadBroadcast()
        Log.d(TAG, "save success external=$editingExternalUri uri=$uri")
        toast("已保存")
        if (finishAfterSave) {
            finish()
        }
    }

    private fun parseEditorJson(): String? {
        return runCatching {
            JSONObject(editorView.text.toString()).toString(2)
        }.getOrElse { error ->
            Log.e(TAG, "json parse failed length=${editorView.text.length}", error)
            toast("JSON 格式错误: ${error.message}")
            null
        }
    }

    private fun readTextFromUri(uri: Uri): String? {
        return runCatching {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("openInputStream returned null")
        }.getOrElse { error ->
            Log.e(TAG, "read uri failed uri=$uri", error)
            null
        }
    }

    private fun writeTextToUri(uri: Uri, text: String): Boolean {
        return runCatching {
            contentResolver.openOutputStream(uri, "wt")?.use { output ->
                output.write(text.toByteArray(Charsets.UTF_8))
            } ?: error("openOutputStream returned null")
            Log.d(TAG, "write uri success uri=$uri length=${text.length}")
            true
        }.getOrElse { error ->
            Log.e(TAG, "write uri failed uri=$uri", error)
            false
        }
    }

    private fun sendReloadBroadcast() {
        val intent = Intent(this, DebugNetworkConfigReloadReceiver::class.java).apply {
            action = DebugNetworkConfigManager.ACTION_RELOAD_CONFIG
        }
        sendBroadcast(intent)
        Log.d(TAG, "reload broadcast sent")
    }

    private fun isDebuggable(): Boolean {
        return applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private companion object {
        private const val TAG = "DebugNetwork-Editor"
    }
}
''',
    'libs/debug-tool/network-interceptor/src/debug/java/com/debugtoolkit/networkinterceptor/DebugNetworkConfigReloadReceiver.kt': r'''package com.debugtoolkit.networkinterceptor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.util.Log

class DebugNetworkConfigReloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != DebugNetworkConfigManager.ACTION_RELOAD_CONFIG) {
            Log.d(TAG, "ignore action=${intent?.action}")
            return
        }
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) {
            Log.d(TAG, "skip reload because application is not debuggable")
            return
        }

        Log.d(TAG, "reload broadcast received")
        DebugNetworkConfigManager.init(context)
        val success = DebugNetworkConfigManager.reloadConfigFromFile()
        val mappings = DebugNetworkConfigManager.applySelectedMappings()
        Log.d(
            TAG,
            "reload after edit success=$success mappings=${mappings.size} " +
                    "selected=${DebugNetworkConfigManager.getSelectedRuleIds()}"
        )
    }

    private companion object {
        private const val TAG = "DebugNetwork-Reload"
    }
}
''',
    'libs/debug-tool/network-interceptor/src/debug/java/com/debugtoolkit/networkinterceptor/DebugNetworkInitProvider.kt': r'''package com.debugtoolkit.networkinterceptor

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.ApplicationInfo
import android.database.Cursor
import android.net.Uri
import android.util.Log

/**
 * Debug variant only.
 *
 * 这个 Provider 的职责不是提供数据，而是让网络拦截框架在 App 进程创建早期完成初始化：
 * 1. 把内置 JSON 配置模板复制到 Download/<AppName>/debug_network_config.json（仅文件不存在时）。
 * 2. 读取外部配置中的 selectRuleIds。
 * 3. 把 selectRuleIds 命中的 rule mappings 下发给 BaseUrlInterceptor。
 *
 * Provider 放在 src/debug，release 变体不会合入；这里仍保留 debuggable 判断，是为了防止宿主误用
 * implementation 引入 debug 库时，release 包里即使出现这个类也不会执行配置文件写入和网络改写。
 */
class DebugNetworkInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        val context = context ?: return false
        val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (!debuggable) {
            Log.d(TAG, "skip init because application is not debuggable")
            return false
        }

        Log.d(TAG, "init provider start package=${context.packageName}")
        DebugNetworkConfigManager.init(context)
        val mappings = DebugNetworkConfigManager.applySelectedMappings()
        Log.d(
            TAG,
            "init provider ready path=${DebugNetworkConfigManager.getConfigFilePath()} " +
                    "mappings=${mappings.size}"
        )
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0

    private companion object {
        private const val TAG = "DebugNetwork-Init"
    }
}
''',
    'libs/debug-tool/network-interceptor/src/debug/java/com/debugtoolkit/networkinterceptor/DebugNetworkProcessGuard.kt': r'''package com.debugtoolkit.networkinterceptor

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Process
import android.util.Log

object DebugNetworkProcessGuard {
    private const val TAG = "DebugNetwork-Process"
    private const val CONFIG_EDITOR_PROCESS_SUFFIX = ":debug_network_config"

    @JvmStatic
    fun shouldSkipApplicationOnCreate(application: Application): Boolean {
        if (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) {
            return false
        }

        val processName = resolveCurrentProcessName(application) ?: return false
        val shouldSkip = processName == application.packageName + CONFIG_EDITOR_PROCESS_SUFFIX
        if (shouldSkip) {
            // 配置编辑器运行在独立进程，只需要系统完成 Activity 启动；宿主业务初始化可能依赖主进程组件。
            Log.d(TAG, "skip host Application.onCreate in process=$processName")
        }
        return shouldSkip
    }

    private fun resolveCurrentProcessName(context: Context): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Application.getProcessName()?.let { return it }
        }

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return null
        val currentPid = Process.myPid()
        return activityManager.runningAppProcesses
            ?.firstOrNull { it.pid == currentPid }
            ?.processName
    }
}
''',
    'libs/debug-tool/network-interceptor/src/main/AndroidManifest.xml': r'''<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
''',
    'libs/debug-tool/network-interceptor/src/main/java/com/debugtoolkit/networkinterceptor/BaseUrlInterceptor.kt': r'''package com.debugtoolkit.networkinterceptor

import android.util.Log
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

class BaseUrlInterceptor : Interceptor {
    @Volatile
    private var rewriteRules: List<BaseUrlRewriteRule> = emptyList()

    @Volatile
    private var flowLogEnabled: Boolean = true

    @Volatile
    private var enabled: Boolean = true

    /**
     * 设置 BaseUrl 映射关系，例如：
     * https://a.com/api -> http://b.com/test
     * a.com -> b.com
     *
     * 命中 source 前缀后，只替换 source 对应的 baseUrl 部分，保留剩余 path 和 query。
     */
    fun setBaseUrlMappings(mappings: Map<String, String>) {
        val rules = mappings.mapNotNull { (source, target) ->
            createRule(source, target)
        }.sortedWith(BaseUrlRewriteRule.MATCH_PRIORITY)
        rewriteRules = rules
        controlLog("mappings updated count=${rules.size}")
        rules.forEach { rule -> controlLog("mapping ${rule.describe()}") }
    }

    fun clearBaseUrlMappings() {
        rewriteRules = emptyList()
        controlLog("mappings cleared")
    }

    fun setFlowLogEnabled(enabled: Boolean) {
        flowLogEnabled = enabled
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        if (!enabled) {
            return chain.proceed(originalRequest)
        }

        val originalUrl = originalRequest.url
        val rewriteRule = rewriteRules.firstOrNull { it.matches(originalUrl) }

        if (rewriteRule == null) {
            return chain.proceed(originalRequest)
        }

        val newScheme = rewriteRule.target.scheme ?: originalUrl.scheme
        val newUrl = originalUrl.newBuilder()
            .scheme(newScheme)
            .host(rewriteRule.target.host)
            // 目标未显式指定端口时，使用新 scheme 的默认端口，避免把旧环境的非默认端口带过去。
            .port(rewriteRule.target.port ?: HttpUrl.defaultPort(newScheme))
            .encodedPath(rewriteRule.rewriteEncodedPath(originalUrl.encodedPath))
            .build()

        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            // 域名切换后让 OkHttp 基于新 URL 生成 Host，避免沿用业务侧手动写入的旧 Host。
            .removeHeader("Host")
            .build()

        flowLog("hit $originalUrl -> $newUrl")
        return chain.proceed(newRequest)
    }

    private fun createRule(source: String, target: String): BaseUrlRewriteRule? {
        val sourceBaseUrl = BaseUrlSource.parse(source)
        val targetBaseUrl = BaseUrlTarget.parse(target)
        if (sourceBaseUrl == null || targetBaseUrl == null) {
            controlLog("invalid mapping $source -> $target")
            return null
        }
        return BaseUrlRewriteRule(sourceBaseUrl, targetBaseUrl)
    }

    private data class BaseUrlRewriteRule(
        val source: BaseUrlSource,
        val target: BaseUrlTarget
    ) {
        fun matches(url: HttpUrl): Boolean {
            if (source.scheme != null && source.scheme != url.scheme) return false
            if (source.host != url.host) return false
            if (source.port != null && source.port != url.port) return false
            return isPathPrefixMatch(source.encodedPathPrefix, url.encodedPath)
        }

        fun rewriteEncodedPath(originalPath: String): String {
            val remainPath = when (source.encodedPathPrefix) {
                "/" -> originalPath
                originalPath -> ""
                else -> originalPath.removePrefix(source.encodedPathPrefix)
            }
            return mergeEncodedPath(target.encodedPathPrefix, remainPath)
        }

        fun describe(): String {
            return "${source.describe()} -> ${target.describe()}"
        }

        companion object {
            val MATCH_PRIORITY = compareByDescending<BaseUrlRewriteRule> { it.source.explicitPartCount }
                .thenByDescending { it.source.encodedPathPrefix.length }
        }
    }

    private data class BaseUrlSource(
        val scheme: String?,
        val host: String,
        val port: Int?,
        val encodedPathPrefix: String,
        val explicitPartCount: Int
    ) {
        fun describe(): String {
            return buildString {
                if (scheme != null) append(scheme).append("://")
                append(host)
                if (port != null) append(":").append(port)
                if (encodedPathPrefix != "/") append(encodedPathPrefix)
            }
        }

        companion object {
            fun parse(value: String): BaseUrlSource? {
                val parsed = parseBaseUrl(value) ?: return null
                val explicitPartCount = listOfNotNull(
                    parsed.scheme,
                    parsed.port,
                    parsed.encodedPath.takeIf { it != "/" }
                ).size
                return BaseUrlSource(
                    scheme = parsed.scheme,
                    host = parsed.url.host,
                    port = parsed.port,
                    encodedPathPrefix = normalizeEncodedPath(parsed.encodedPath),
                    explicitPartCount = explicitPartCount
                )
            }
        }
    }

    private data class BaseUrlTarget(
        val scheme: String?,
        val host: String,
        val port: Int?,
        val encodedPathPrefix: String
    ) {
        fun describe(): String {
            return buildString {
                if (scheme != null) append(scheme).append("://")
                append(host)
                if (port != null) append(":").append(port)
                if (encodedPathPrefix != "/") append(encodedPathPrefix)
            }
        }

        companion object {
            fun parse(value: String): BaseUrlTarget? {
                val parsed = parseBaseUrl(value) ?: return null
                return BaseUrlTarget(
                    scheme = parsed.scheme,
                    host = parsed.url.host,
                    port = parsed.port,
                    encodedPathPrefix = normalizeEncodedPath(parsed.encodedPath)
                )
            }
        }
    }

    private data class ParsedBaseUrl(
        val url: HttpUrl,
        val scheme: String?,
        val port: Int?,
        val encodedPath: String
    )

    private fun controlLog(message: String) {
        Log.d(TAG, message)
    }

    private fun flowLog(message: String) {
        if (flowLogEnabled) {
            Log.d(TAG, message)
        }
    }

    private companion object {
        private const val TAG = "DebugNetwork-Rewrite"

        fun parseBaseUrl(value: String): ParsedBaseUrl? {
            val normalizedValue = value.trim().takeIf { it.isNotEmpty() } ?: return null
            if (normalizedValue.contains('?') || normalizedValue.contains('#')) return null

            val hasScheme = normalizedValue.contains("://")
            val parseValue = if (hasScheme) normalizedValue else "https://$normalizedValue"
            val url = parseValue.toHttpUrlOrNull() ?: return null

            return ParsedBaseUrl(
                url = url,
                scheme = url.scheme.takeIf { hasScheme },
                port = url.port.takeIf { hasExplicitPort(normalizedValue, hasScheme) },
                encodedPath = url.encodedPath
            )
        }

        fun hasExplicitPort(value: String, hasScheme: Boolean): Boolean {
            val withoutScheme = if (hasScheme) value.substringAfter("://") else value
            val authority = withoutScheme.substringBefore('/').substringBefore('?').substringBefore('#')
            val portText = authority.substringAfterLast(':', missingDelimiterValue = "")
            return portText.isNotEmpty() && portText.all { it.isDigit() }
        }

        fun normalizeEncodedPath(path: String): String {
            if (path.isEmpty() || path == "/") return "/"
            return path.trimEnd('/')
        }

        fun isPathPrefixMatch(sourcePath: String, requestPath: String): Boolean {
            if (sourcePath == "/") return true
            if (requestPath == sourcePath) return true
            return requestPath.startsWith("$sourcePath/")
        }

        fun mergeEncodedPath(targetPath: String, remainPath: String): String {
            val normalizedRemain = remainPath.takeIf { it.isNotEmpty() } ?: return targetPath
            if (targetPath == "/") return normalizedRemain
            return "$targetPath$normalizedRemain"
        }
    }
}
''',
    'libs/debug-tool/network-interceptor/src/main/java/com/debugtoolkit/networkinterceptor/DebugNetworkConfigManager.kt': r'''package com.debugtoolkit.networkinterceptor

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object DebugNetworkConfigManager {
    const val ACTION_EDIT_CONFIG = "com.debugtoolkit.networkinterceptor.action.EDIT_CONFIG"
    const val ACTION_RELOAD_CONFIG = "com.debugtoolkit.networkinterceptor.action.RELOAD_CONFIG"

    private const val TAG = "DebugNetwork-Config"
    private const val TEMPLATE_ASSET_NAME = "debug_network_config.json"
    private const val CONFIG_FILE_NAME = "debug_network_config.json"
    private const val LEGACY_CONFIG_FILE_NAME = "debug_network_config.txt"
    private const val DOWNLOAD_DIR = "Download"

    @Volatile
    private var config: DebugNetworkConfig = DebugNetworkConfig(1, 0, emptyList(), emptyList())

    @Volatile
    private var configFilePath: String = ""

    @Volatile
    private var lastError: String? = null

    private var appContext: Context? = null

    @Synchronized
    fun init(context: Context) {
        val applicationContext = context.applicationContext
        appContext = applicationContext

        if (!applicationContext.isDebuggable()) {
            log("skip init because application is not debuggable")
            config = DebugNetworkConfig(1, 0, emptyList(), emptyList())
            return
        }

        log("init package=${applicationContext.packageName}")
        reloadConfigFromFile()
    }

    fun getConfigFilePath(): String {
        ensureInitialized()
        return configFilePath
    }

    fun getLastError(): String? {
        ensureInitialized()
        return lastError
    }

    fun getRules(): List<DebugNetworkRule> {
        ensureInitialized()
        return config.rules
    }

    fun getSelectedRuleIds(): Set<String> {
        ensureInitialized()
        return config.selectRuleIds.toCollection(linkedSetOf())
    }

    fun setRuleSelected(ruleId: String, selected: Boolean): Boolean {
        ensureInitialized()
        val selectedRuleIds = config.selectRuleIds.toMutableList()
        if (selected) {
            if (ruleId !in selectedRuleIds) {
                selectedRuleIds.add(ruleId)
            }
        } else {
            selectedRuleIds.removeAll { it == ruleId }
        }
        return updateSelectedRuleIds(selectedRuleIds)
    }

    fun applySelectedMappings(): Map<String, String> {
        ensureInitialized()
        val mappings = buildSelectedMappings()
        DebugNetworkInterceptorManager.setBaseUrlMappings(mappings)
        log("apply selected mappings selected=${config.selectRuleIds} count=${mappings.size}")
        return mappings
    }

    fun reloadConfigFromFile(): Boolean {
        val context = appContext ?: return false
        return runCatching {
            val templateJson = readTemplateJson(context)
            val external = ExternalConfigFile(context)
            configFilePath = external.displayPath
            log("reload config start path=$configFilePath")
            val configJson = external.ensureConfigFile(templateJson)
            val mergedJson = mergeTemplateIfNeeded(configJson, templateJson)
            if (mergedJson.toString() != configJson.toString()) {
                external.writeText(mergedJson.toString(2))
                log(
                    "template merged path=$configFilePath " +
                            "templateVersion=${mergedJson.optInt("templateVersion", 0)}"
                )
            }
            config = parseConfig(mergedJson)
            lastError = null
            log(
                "config loaded path=$configFilePath templateVersion=${config.templateVersion} " +
                        "rules=${config.rules.size} selected=${config.selectRuleIds}"
            )
            true
        }.getOrElse { error ->
            lastError = error.message ?: error.javaClass.simpleName
            config = DebugNetworkConfig(1, 0, emptyList(), emptyList())
            logError("config load failed path=$configFilePath", error)
            false
        }
    }

    fun resetConfigToTemplate(): Boolean {
        val context = appContext ?: return false
        return runCatching {
            val templateJson = readTemplateJson(context)
            val external = ExternalConfigFile(context)
            configFilePath = external.displayPath
            external.writeText(templateJson.toString(2))
            config = parseConfig(templateJson)
            lastError = null
            log("config reset to template path=$configFilePath rules=${config.rules.size}")
            true
        }.getOrElse { error ->
            lastError = error.message ?: error.javaClass.simpleName
            logError("config reset failed path=$configFilePath", error)
            false
        }
    }

    fun readConfigText(): String? {
        val context = appContext ?: return null
        return runCatching {
            val templateJson = readTemplateJson(context)
            val external = ExternalConfigFile(context)
            configFilePath = external.displayPath
            val configJson = external.ensureConfigFile(templateJson)
            val text = mergeTemplateIfNeeded(configJson, templateJson).toString(2)
            lastError = null
            log("config text read path=$configFilePath length=${text.length}")
            text
        }.getOrElse { error ->
            lastError = error.message ?: error.javaClass.simpleName
            logError("config text read failed path=$configFilePath", error)
            null
        }
    }

    fun writeConfigText(text: String): Boolean {
        val context = appContext ?: return false
        return runCatching {
            val configJson = JSONObject(text)
            val parsedConfig = parseConfig(configJson)
            val external = ExternalConfigFile(context)
            configFilePath = external.displayPath
            external.writeText(configJson.toString(2))
            config = parsedConfig
            lastError = null
            log(
                "config text wrote path=$configFilePath length=${text.length} " +
                        "rules=${config.rules.size} selected=${config.selectRuleIds}"
            )
            true
        }.getOrElse { error ->
            lastError = error.message ?: error.javaClass.simpleName
            logError("config text write failed path=$configFilePath", error)
            false
        }
    }

    private fun updateSelectedRuleIds(selectedRuleIds: List<String>): Boolean {
        val validRuleIds = config.rules.map { it.id }.toSet()
        val nextSelectedRuleIds = selectedRuleIds
            .filter { it in validRuleIds }
            .distinct()
        val previousConfig = config
        config = config.copy(selectRuleIds = nextSelectedRuleIds)
        val persisted = persistSelectedRuleIds(nextSelectedRuleIds)
        if (!persisted) {
            config = previousConfig
        }
        log("selection update selected=$nextSelectedRuleIds persisted=$persisted")
        return persisted
    }

    private fun persistSelectedRuleIds(selectedRuleIds: List<String>): Boolean {
        val context = appContext ?: return false
        return runCatching {
            val templateJson = readTemplateJson(context)
            val external = ExternalConfigFile(context)
            configFilePath = external.displayPath
            val currentJson = external.readText()?.let { JSONObject(it) } ?: templateJson
            val mergedJson = mergeTemplateIfNeeded(currentJson, templateJson)
            mergedJson.put("selectRuleIds", selectedRuleIds.toJsonArray())
            external.writeText(mergedJson.toString(2))
            lastError = null
            log("selection persisted path=$configFilePath selected=$selectedRuleIds")
            true
        }.getOrElse { error ->
            lastError = error.message ?: error.javaClass.simpleName
            logError("selection persist failed path=$configFilePath selected=$selectedRuleIds", error)
            false
        }
    }

    private fun buildSelectedMappings(): Map<String, String> {
        val result = linkedMapOf<String, String>()
        val rulesById = config.rules.associateBy { it.id }
        config.selectRuleIds.forEach { ruleId ->
            rulesById[ruleId]?.mappings?.forEach { mapping ->
                // 多个 rule 命中同一个 source 时，selectRuleIds 中靠后的 rule 覆盖靠前的 rule。
                if (result.containsKey(mapping.source)) {
                    log("mapping override source=${mapping.source} ruleId=$ruleId target=${mapping.target}")
                }
                result[mapping.source] = mapping.target
            }
        }
        return result
    }

    private fun readTemplateJson(context: Context): JSONObject {
        val text = context.assets.open(TEMPLATE_ASSET_NAME).bufferedReader().use { it.readText() }
        return JSONObject(text)
    }

    private fun parseConfig(json: JSONObject): DebugNetworkConfig {
        val normalizedJson = normalizeConfigJson(json)
        val rules = normalizedJson.optJSONArray("rules").orEmpty().mapObjects { ruleJson ->
            val mappings = ruleJson.optJSONArray("mappings").orEmpty().mapObjects { mappingJson ->
                DebugNetworkMapping(
                    source = mappingJson.optString("source").trim(),
                    target = mappingJson.optString("target").trim()
                )
            }.filter { it.source.isNotEmpty() && it.target.isNotEmpty() }

            DebugNetworkRule(
                id = ruleJson.optString("id").trim(),
                name = ruleJson.optString("name", ruleJson.optString("id")).trim(),
                mappings = mappings
            )
        }.filter { it.id.isNotEmpty() }

        val ruleIds = rules.map { it.id }.toSet()
        val selectedRuleIds = normalizedJson.optJSONArray("selectRuleIds")
            .orEmpty()
            .mapStrings()
            .filter { it in ruleIds }
            .distinct()

        return DebugNetworkConfig(
            version = normalizedJson.optInt("version", 1),
            templateVersion = normalizedJson.optInt("templateVersion", 0),
            selectRuleIds = selectedRuleIds,
            rules = rules
        )
    }

    private fun mergeTemplateIfNeeded(configJson: JSONObject, templateJson: JSONObject): JSONObject {
        val normalizedJson = normalizeConfigJson(configJson)
        val templateVersion = templateJson.optInt("templateVersion", 0)
        if (normalizedJson.optInt("templateVersion", 0) >= templateVersion) {
            return normalizedJson
        }

        log(
            "template merge required current=${normalizedJson.optInt("templateVersion", 0)} " +
                    "template=$templateVersion"
        )
        val merged = JSONObject(normalizedJson.toString())
        merged.put("version", templateJson.optInt("version", merged.optInt("version", 1)))
        merged.put("templateVersion", templateVersion)
        merged.put(
            "rules",
            mergeRules(
                merged.optJSONArray("rules").orEmpty(),
                templateJson.optJSONArray("rules").orEmpty()
            )
        )
        if (!merged.has("selectRuleIds")) {
            merged.put("selectRuleIds", JSONArray())
        }
        return merged
    }

    private fun normalizeConfigJson(json: JSONObject): JSONObject {
        val normalizedJson = JSONObject(json.toString())
        val legacyGroups = normalizedJson.optJSONArray("groups")
        if (legacyGroups != null) {
            log("legacy groups detected, migrate count=${legacyGroups.length()}")
            if (!normalizedJson.has("rules")) {
                normalizedJson.put("rules", flattenLegacyRules(legacyGroups))
            }
            if (!normalizedJson.has("selectRuleIds")) {
                normalizedJson.put("selectRuleIds", inferLegacySelectedRuleIds(legacyGroups))
            }
            normalizedJson.remove("groups")
        }
        if (!normalizedJson.has("rules")) {
            normalizedJson.put("rules", JSONArray())
        }
        if (!normalizedJson.has("selectRuleIds")) {
            normalizedJson.put("selectRuleIds", JSONArray())
        }
        return normalizedJson
    }

    private fun flattenLegacyRules(groups: JSONArray): JSONArray {
        val result = JSONArray()
        val seenRuleIds = mutableSetOf<String>()
        groups.forEachObject { groupJson ->
            groupJson.optJSONArray("rules").orEmpty().forEachObject { ruleJson ->
                val ruleId = ruleJson.optString("id").trim()
                if (ruleId.isNotEmpty() && seenRuleIds.add(ruleId)) {
                    result.put(JSONObject(ruleJson.toString()))
                }
            }
        }
        return result
    }

    private fun inferLegacySelectedRuleIds(groups: JSONArray): JSONArray {
        val result = JSONArray()
        groups.forEachObject { groupJson ->
            val rules = groupJson.optJSONArray("rules").orEmpty()
            if (groupJson.optString("selectMode", "single").lowercase() == "multiple") {
                rules.forEachObject { ruleJson ->
                    if (ruleJson.optBoolean("defaultChecked", false)) {
                        val ruleId = ruleJson.optString("id").trim()
                        if (ruleId.isNotEmpty()) result.put(ruleId)
                    }
                }
            } else {
                val defaultRuleId = groupJson.optString("defaultRuleId").trim()
                    .ifBlank { rules.optJSONObject(0)?.optString("id").orEmpty().trim() }
                if (defaultRuleId.isNotEmpty()) result.put(defaultRuleId)
            }
        }
        return result
    }

    private fun mergeRules(currentRules: JSONArray, templateRules: JSONArray): JSONArray {
        val result = JSONArray(currentRules.toString())
        templateRules.forEachObject { templateRule ->
            val ruleId = templateRule.optString("id")
            val currentRule = result.findObjectById(ruleId)
            if (currentRule == null) {
                result.put(JSONObject(templateRule.toString()))
                return@forEachObject
            }

            copyIfMissing(currentRule, templateRule, "name")
            // mappings 是调试人员最可能改成真实域名的字段，模板升级只在完全缺失时补齐，避免覆盖手工配置。
            copyIfMissing(currentRule, templateRule, "mappings")
        }
        return result
    }

    private fun copyIfMissing(target: JSONObject, source: JSONObject, key: String) {
        if (!target.has(key) && source.has(key)) {
            target.put(key, source.get(key))
        }
    }

    private fun JSONArray?.orEmpty(): JSONArray = this ?: JSONArray()

    private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> {
        val result = mutableListOf<T>()
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            result.add(transform(item))
        }
        return result
    }

    private fun JSONArray.mapStrings(): List<String> {
        val result = mutableListOf<String>()
        for (index in 0 until length()) {
            val item = optString(index).trim()
            if (item.isNotEmpty()) result.add(item)
        }
        return result
    }

    private inline fun JSONArray.forEachObject(action: (JSONObject) -> Unit) {
        for (index in 0 until length()) {
            action(optJSONObject(index) ?: continue)
        }
    }

    private fun JSONArray.findObjectById(id: String): JSONObject? {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            if (item.optString("id") == id) return item
        }
        return null
    }

    private fun List<String>.toJsonArray(): JSONArray {
        val result = JSONArray()
        forEach { result.put(it) }
        return result
    }

    private class ExternalConfigFile(private val context: Context) {
        private val appName = context.readableAppName()
        private val relativePath = "$DOWNLOAD_DIR/$appName/"

        val displayPath: String = "$relativePath$CONFIG_FILE_NAME"

        fun ensureConfigFile(templateJson: JSONObject): JSONObject {
            val currentText = readText()
            if (currentText != null) {
                Log.d(TAG, "config file found path=$displayPath length=${currentText.length}")
                return JSONObject(currentText)
            }

            val legacyText = readLegacyText()
            if (legacyText != null) {
                writeText(legacyText)
                Log.d(
                    TAG,
                    "legacy config migrated from $LEGACY_CONFIG_FILE_NAME to $CONFIG_FILE_NAME " +
                            "path=$displayPath length=${legacyText.length}"
                )
                return JSONObject(legacyText)
            }

            val templateText = templateJson.toString(2)
            writeText(templateText)
            Log.d(TAG, "config file created from template path=$displayPath length=${templateText.length}")
            return JSONObject(templateText)
        }

        fun readText(): String? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                readTextFromMediaStore(CONFIG_FILE_NAME)
            } else {
                legacyFile(CONFIG_FILE_NAME).takeIf { it.exists() }?.readText()
            }
        }

        fun readLegacyText(): String? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                readTextFromMediaStore(LEGACY_CONFIG_FILE_NAME)
            } else {
                legacyFile(LEGACY_CONFIG_FILE_NAME).takeIf { it.exists() }?.readText()
            }
        }

        fun writeText(text: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                writeTextToMediaStore(text)
            } else {
                val file = legacyFile(CONFIG_FILE_NAME)
                file.parentFile?.mkdirs()
                file.writeText(text)
            }
        }

        private fun readTextFromMediaStore(displayName: String): String? {
            val uri = findMediaStoreUri(displayName) ?: return null
            return context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }

        private fun writeTextToMediaStore(text: String) {
            val uri = findMediaStoreUri(CONFIG_FILE_NAME) ?: createMediaStoreUri()
            context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                output.write(text.toByteArray(Charsets.UTF_8))
            }
        }

        private fun findMediaStoreUri(displayName: String): Uri? {
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.Downloads._ID)
            val selection = "${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.RELATIVE_PATH}=?"
            val args = arrayOf(displayName, relativePath)
            context.contentResolver.query(collection, projection, selection, args, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                return ContentUris.withAppendedId(collection, id)
            }
            return null
        }

        private fun createMediaStoreUri(): Uri {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, CONFIG_FILE_NAME)
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
            }
            return context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Failed to create $displayPath")
        }

        private fun legacyFile(fileName: String): File {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            return File(File(downloads, appName), fileName)
        }

        private fun Context.readableAppName(): String {
            val label = runCatching {
                packageManager.getApplicationLabel(applicationInfo).toString()
            }.getOrDefault(packageName)
            return label.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { packageName }
        }
    }

    private fun ensureInitialized() {
        if (appContext != null) return
        log("DebugNetworkConfigManager is not initialized")
    }

    private fun Context.isDebuggable(): Boolean {
        return applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    private fun log(message: String) {
        Log.d(TAG, message)
    }

    private fun logError(message: String, error: Throwable) {
        Log.e(TAG, "$message: ${error.message ?: error.javaClass.simpleName}", error)
    }
}
''',
    'libs/debug-tool/network-interceptor/src/main/java/com/debugtoolkit/networkinterceptor/DebugNetworkConfigModel.kt': r'''package com.debugtoolkit.networkinterceptor

data class DebugNetworkMapping(
    val source: String,
    val target: String
)

data class DebugNetworkRule(
    val id: String,
    val name: String,
    val mappings: List<DebugNetworkMapping>
)

data class DebugNetworkConfig(
    val version: Int,
    val templateVersion: Int,
    val selectRuleIds: List<String>,
    val rules: List<DebugNetworkRule>
)
''',
    'libs/debug-tool/network-interceptor/src/main/java/com/debugtoolkit/networkinterceptor/DebugNetworkConfigPanel.kt': r'''package com.debugtoolkit.networkinterceptor

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

object DebugNetworkConfigPanel {
    /**
     * 展示网络拦截配置面板。
     *
     * 这个 UI 放在底层网络拦截库中，debug-toolkit 浮窗只需要调用 show()：
     * - JSON 配置文件读取路径、模板升级、selectRuleIds 选择状态、最终 mappings 都由 DebugNetworkConfigManager 管理。
     * - 每个 rule 是一套可独立应用的映射集合；勾选多个 rule 时会把它们的 mappings 合并后应用。
     * - onRestart 由宿主调试工具传入，因为只有宿主调试工具知道如何重启当前 App。
     */
    fun show(context: Context, onRestart: () -> Unit = {}) {
        DebugNetworkConfigManager.init(context)

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(16), context.dp(8), context.dp(16), context.dp(8))
        }

        content.addView(TextView(context).apply {
            text = "配置文件: ${DebugNetworkConfigManager.getConfigFilePath()}"
            textSize = 12f
            setTextColor(0xFF666666.toInt())
        })

        DebugNetworkConfigManager.getLastError()?.let { error ->
            Log.d(TAG, "show panel lastError=$error")
            content.addView(TextView(context).apply {
                text = "读取失败: $error"
                textSize = 12f
                setTextColor(0xFFD32F2F.toInt())
            })
        }

        val rules = DebugNetworkConfigManager.getRules()
        Log.d(
            TAG,
            "show panel path=${DebugNetworkConfigManager.getConfigFilePath()} " +
                    "rules=${rules.size} selected=${DebugNetworkConfigManager.getSelectedRuleIds()}"
        )
        if (rules.isEmpty()) {
            content.addView(TextView(context).apply {
                text = "未读取到网络拦截配置，请检查 Download 目录中的 JSON 文件。"
                textSize = 14f
                setPadding(0, context.dp(12), 0, context.dp(12))
            })
        } else {
            content.addView(TextView(context).apply {
                text = "选择要应用的映射规则"
                textSize = 15f
                setTextColor(0xFF222222.toInt())
                setPadding(0, context.dp(14), 0, context.dp(4))
            })
            addRuleSelectors(context, content, rules)
        }

        lateinit var dialog: AlertDialog
        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, context.dp(12), 0, 0)
        }
        actions.addView(createButton(context, "重新读取配置") {
            val success = DebugNetworkConfigManager.reloadConfigFromFile()
            Log.d(TAG, "reload config clicked success=$success")
            Toast.makeText(context, if (success) "配置已重新读取" else "配置读取失败", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            show(context, onRestart)
        })
        actions.addView(createButton(context, "编辑配置") {
            val intent = Intent(DebugNetworkConfigManager.ACTION_EDIT_CONFIG).apply {
                setPackage(context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val started = runCatching { context.startActivity(intent) }
                .onFailure { error -> Log.e(TAG, "open editor failed", error) }
                .isSuccess
            Log.d(TAG, "open editor clicked started=$started")
            if (!started) {
                Toast.makeText(context, "无法打开配置编辑器", Toast.LENGTH_SHORT).show()
            }
        })
        actions.addView(createButton(context, "立即应用") {
            val mappings = DebugNetworkConfigManager.applySelectedMappings()
            Log.d(TAG, "apply clicked mappings=${mappings.size}")
            Toast.makeText(context, "已应用 ${mappings.size} 条映射", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        })
        actions.addView(createButton(context, "应用并重启") {
            val mappings = DebugNetworkConfigManager.applySelectedMappings()
            Log.d(TAG, "apply and restart clicked mappings=${mappings.size}")
            dialog.dismiss()
            onRestart()
        })
        actions.addView(createButton(context, "恢复模板配置") {
            Log.d(TAG, "reset template clicked")
            showResetConfirm(context, dialog, onRestart)
        })
        content.addView(actions)

        val scrollView = ScrollView(context).apply { addView(content) }
        dialog = AlertDialog.Builder(context)
            .setTitle("网络拦截")
            .setView(scrollView)
            .create()
        prepareOverlayDialog(dialog)
        dialog.show()
    }

    private fun addRuleSelectors(
        context: Context,
        content: LinearLayout,
        rules: List<DebugNetworkRule>
    ) {
        val selectedRuleIds = DebugNetworkConfigManager.getSelectedRuleIds()
        rules.forEach { rule ->
            content.addView(CheckBox(context).apply {
                text = rule.name
                isChecked = rule.id in selectedRuleIds
                setOnCheckedChangeListener { _, checked ->
                    val success = DebugNetworkConfigManager.setRuleSelected(rule.id, checked)
                    Log.d(TAG, "rule checked id=${rule.id} checked=$checked success=$success")
                    if (!success) {
                        Toast.makeText(context, "选择状态写入失败", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }

    private fun createButton(context: Context, text: String, action: () -> Unit): Button {
        return Button(context).apply {
            this.text = text
            setOnClickListener { action() }
        }
    }

    private fun showResetConfirm(context: Context, parentDialog: AlertDialog, onRestart: () -> Unit) {
        val confirmDialog = AlertDialog.Builder(context)
            .setTitle("恢复模板配置")
            .setMessage("会覆盖 Download 目录中的 debug_network_config.json，已手动修改的映射关系会丢失。确定继续吗？")
            .setPositiveButton("确定") { dialog, _ ->
                val success = DebugNetworkConfigManager.resetConfigToTemplate()
                Log.d(TAG, "reset template confirmed success=$success")
                Toast.makeText(context, if (success) "已恢复模板" else "恢复模板失败", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                parentDialog.dismiss()
                show(context, onRestart)
            }
            .setNegativeButton("取消", null)
            .create()
        prepareOverlayDialog(confirmDialog)
        confirmDialog.show()
    }

    private fun prepareOverlayDialog(dialog: AlertDialog) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        } else {
            @Suppress("DEPRECATION")
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_PHONE)
        }
    }

    private fun Context.dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private const val TAG = "DebugNetwork-Panel"
}
''',
    'libs/debug-tool/network-interceptor/src/main/java/com/debugtoolkit/networkinterceptor/DebugNetworkInterceptorManager.kt': r'''package com.debugtoolkit.networkinterceptor

import android.util.Log
import okhttp3.OkHttpClient

object DebugNetworkInterceptorManager {
    private const val TAG = "DebugNetwork-OkHttp"

    private val baseUrlInterceptor = BaseUrlInterceptor()

    @Volatile
    private var enabled: Boolean = true

    @Volatile
    private var flowLogEnabled: Boolean = true

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        baseUrlInterceptor.setEnabled(enabled)
        controlLog("enabled=$enabled")
    }

    fun setFlowLogEnabled(enabled: Boolean) {
        val previous = flowLogEnabled
        flowLogEnabled = enabled
        baseUrlInterceptor.setFlowLogEnabled(enabled)
        controlLog("flowLogEnabled=$enabled previous=$previous")
    }

    fun setBaseUrlMappings(mappings: Map<String, String>) {
        controlLog("set base url mappings count=${mappings.size}")
        baseUrlInterceptor.setBaseUrlMappings(mappings)
    }

    fun clearBaseUrlMappings() {
        controlLog("clear base url mappings")
        baseUrlInterceptor.clearBaseUrlMappings()
    }

    /**
     * Gradle Plugin/ASM 会在 OkHttpClient.Builder.build() 前注入这个入口。
     * 这里保持幂等，避免手动接入和 ASM 同时处理同一个 Builder 时重复安装。
     */
    @JvmStatic
    fun apply(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        if (!enabled) {
            controlLog("apply skipped disabled, builder=${System.identityHashCode(builder)}")
            return builder
        }

        baseUrlInterceptor.setFlowLogEnabled(flowLogEnabled)
        if (builder.interceptors().none { it is BaseUrlInterceptor }) {
            builder.addInterceptor(baseUrlInterceptor)
            controlLog("base url interceptor installed, builder=${System.identityHashCode(builder)}")
        } else {
            controlLog("base url interceptor skipped duplicate, builder=${System.identityHashCode(builder)}")
        }
        return builder
    }

    private fun controlLog(message: String) {
        Log.d(TAG, message)
    }
}
''',
    'build-logic/debug-network-interceptor-plugin/build.gradle.kts': r'''plugins {
    id("org.jetbrains.kotlin.jvm")
    `java-gradle-plugin`
    `maven-publish`
}

group = "com.github.amigo-wangwx.debug-toolkit"
version = providers.gradleProperty("VERSION_NAME").getOrElse("1.0.0")

dependencies {
    implementation(gradleApi())
    implementation("com.android.tools.build:gradle-api:8.13.2")
}

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    plugins {
        register("debugNetworkInterceptor") {
            id = "com.debugtoolkit.debug-network-interceptor"
            implementationClass = "DebugNetworkInterceptorPlugin"
        }
    }
}
''',
    'build-logic/debug-network-interceptor-plugin/settings.gradle.kts': r'''pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "debug-network-interceptor-plugin"
''',
    'build-logic/debug-network-interceptor-plugin/src/main/kotlin/DebugNetworkInterceptorPlugin.kt': r'''import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationParameters
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import javax.inject.Inject

open class DebugNetworkInterceptorExtension @Inject constructor(objects: ObjectFactory) {
    val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val logEnabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val includeDependencies: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
}

class DebugNetworkInterceptorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "debugNetworkInterceptor",
            DebugNetworkInterceptorExtension::class.java
        )

        project.pluginManager.withPlugin("com.android.application") {
            val androidComponents = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
            project.logger.lifecycle("[DebugNetworkInterceptorPlugin] applied project=${project.path}")

            androidComponents.onVariants(androidComponents.selector().withBuildType("debug")) { variant ->
                if (!extension.enabled.get()) {
                    project.logger.lifecycle(
                        "[DebugNetworkInterceptorPlugin] variant=${variant.name} enabled=false"
                    )
                    return@onVariants
                }

                val scope = if (extension.includeDependencies.get()) {
                    InstrumentationScope.ALL
                } else {
                    InstrumentationScope.PROJECT
                }

                project.logger.lifecycle(
                    "[DebugNetworkInterceptorPlugin] variant=${variant.name} enabled=true " +
                            "scope=$scope logEnabled=${extension.logEnabled.get()}"
                )

                variant.instrumentation.transformClassesWith(
                    DebugNetworkInterceptorClassVisitorFactory::class.java,
                    scope
                ) { parameters ->
                    parameters.variantName.set(variant.name)
                    parameters.logEnabled.set(extension.logEnabled)
                }
                variant.instrumentation.setAsmFramesComputationMode(
                    FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS
                )
            }
        }
    }
}

interface DebugNetworkInterceptorParameters : InstrumentationParameters {
    @get:Input
    val variantName: Property<String>

    @get:Input
    val logEnabled: Property<Boolean>
}

abstract class DebugNetworkInterceptorClassVisitorFactory :
    AsmClassVisitorFactory<DebugNetworkInterceptorParameters> {

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        return DebugNetworkInterceptorClassVisitor(
            nextClassVisitor,
            parameters.get().variantName.get(),
            parameters.get().logEnabled.get(),
            classContext.currentClassData.isApplicationSubclass()
        )
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        val className = classData.className
        return !className.startsWith("com.debugtoolkit.networkinterceptor.")
                && !className.startsWith("com/debugtoolkit/networkinterceptor/")
                && !className.startsWith("okhttp3.")
                && !className.startsWith("okhttp3/")
    }
}

private class DebugNetworkInterceptorClassVisitor(
    nextClassVisitor: ClassVisitor,
    private val variantName: String,
    private val logEnabled: Boolean,
    private val isApplicationClass: Boolean
) : ClassVisitor(Opcodes.ASM9, nextClassVisitor) {

    private var currentClassName: String = ""

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        currentClassName = name.orEmpty()
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val nextMethodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
        return DebugNetworkInterceptorMethodVisitor(
            nextMethodVisitor,
            variantName,
            logEnabled,
            isApplicationClass,
            currentClassName,
            name.orEmpty(),
            descriptor.orEmpty()
        )
    }
}

private class DebugNetworkInterceptorMethodVisitor(
    nextMethodVisitor: MethodVisitor,
    private val variantName: String,
    private val logEnabled: Boolean,
    private val isApplicationClass: Boolean,
    private val className: String,
    private val methodName: String,
    private val methodDescriptor: String
) : MethodVisitor(Opcodes.ASM9, nextMethodVisitor) {

    override fun visitCode() {
        super.visitCode()
        if (!isApplicationClass || methodName != "onCreate" || methodDescriptor != "()V") {
            return
        }

        val continueLabel = Label()
        super.visitVarInsn(Opcodes.ALOAD, 0)
        super.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            DEBUG_NETWORK_PROCESS_GUARD_OWNER,
            "shouldSkipApplicationOnCreate",
            DEBUG_NETWORK_PROCESS_GUARD_DESCRIPTOR,
            false
        )
        super.visitJumpInsn(Opcodes.IFEQ, continueLabel)
        super.visitInsn(Opcodes.RETURN)
        super.visitLabel(continueLabel)

        if (logEnabled) {
                println(
                    "[DebugNetworkInterceptorPlugin] inject application guard variant=$variantName " +
                            "class=$className method=$methodName$methodDescriptor " +
                            "guard=$DEBUG_NETWORK_PROCESS_GUARD_OWNER.shouldSkipApplicationOnCreate"
                )
        }
    }

    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        if (
            opcode == Opcodes.INVOKEVIRTUAL &&
            owner == OKHTTP_BUILDER_OWNER &&
            name == "build" &&
            descriptor == OKHTTP_BUILD_DESCRIPTOR
        ) {
            super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                DEBUG_NETWORK_MANAGER_OWNER,
                "apply",
                DEBUG_NETWORK_APPLY_DESCRIPTOR,
                false
            )
            if (logEnabled) {
                println(
                    "[DebugNetworkInterceptorPlugin] inject variant=$variantName " +
                            "class=$className method=$methodName$methodDescriptor " +
                            "target=$owner.$name$descriptor"
                )
            }
        }

        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }

    private companion object {
        private const val OKHTTP_BUILDER_OWNER = "okhttp3/OkHttpClient\$Builder"
        private const val OKHTTP_BUILD_DESCRIPTOR = "()Lokhttp3/OkHttpClient;"
        private const val DEBUG_NETWORK_MANAGER_OWNER =
            "com/debugtoolkit/networkinterceptor/DebugNetworkInterceptorManager"
        private const val DEBUG_NETWORK_APPLY_DESCRIPTOR =
            "(Lokhttp3/OkHttpClient\$Builder;)Lokhttp3/OkHttpClient\$Builder;"
        private const val DEBUG_NETWORK_PROCESS_GUARD_OWNER =
            "com/debugtoolkit/networkinterceptor/DebugNetworkProcessGuard"
        private const val DEBUG_NETWORK_PROCESS_GUARD_DESCRIPTOR =
            "(Landroid/app/Application;)Z"
    }
}

private fun ClassData.isApplicationSubclass(): Boolean {
    return superClasses.any { superClass ->
        superClass == "android.app.Application" || superClass == "android/app/Application"
    }
}
''',
}

def write_file(relative_path: str, content: str) -> None:
    file_path = REPO_ROOT / relative_path
    file_path.parent.mkdir(parents=True, exist_ok=True)
    file_path.write_text(content, encoding='utf-8')


def remove_stale_file(relative_path: str) -> None:
    file_path = REPO_ROOT / relative_path
    if not file_path.exists():
        return
    file_path.unlink()
    cleanup_empty_parents(file_path.parent)


def cleanup_empty_parents(directory: Path) -> None:
    root_bounds = [(REPO_ROOT / root).resolve() for root in GENERATED_ROOTS]
    current = directory.resolve()
    while current != REPO_ROOT and any(str(current).startswith(str(root)) for root in root_bounds):
        try:
            current.rmdir()
        except OSError:
            return
        current = current.parent


def generate() -> None:
    os.chdir(REPO_ROOT)
    for path, content in GENERATED_FILES.items():
        write_file(path, content)
    for path in STALE_PATHS:
        remove_stale_file(path)


def main() -> None:
    generate()
    print(f"✅ DebugToolkit 生成完成，已写入 {len(GENERATED_FILES)} 个最新模板文件")
    print("   - libs/debug-tool/debug-toolkit")
    print("   - libs/debug-tool/network-interceptor")
    print("   - build-logic/debug-network-interceptor-plugin")


if __name__ == '__main__':
    main()
