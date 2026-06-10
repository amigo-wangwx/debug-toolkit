package com.debugtoolkit

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
