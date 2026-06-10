package com.debugtoolkit.networkinterceptor

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
