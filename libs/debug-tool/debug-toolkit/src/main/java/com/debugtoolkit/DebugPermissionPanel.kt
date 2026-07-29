package com.debugtoolkit

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationManagerCompat
import com.debugtoolkit.networkinterceptor.DebugOperationLog

object DebugPermissionPanel {
    fun show(context: Context) {
        DebugOperationLog.record("permission", "panel_open")
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(16), context.dp(8), context.dp(16), context.dp(8))
        }

        addPermissionStatus(
            content,
            "悬浮窗",
            if (context.canDrawOverlaysCompat()) "已允许" else "未允许",
            if (context.canDrawOverlaysCompat()) {
                "调试浮窗可以显示在当前 App 上方。"
            } else {
                "需要允许后才能显示调试浮窗。"
            }
        )
        addPermissionStatus(
            content,
            "通知",
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) "已允许" else "可能受限",
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                "Android 13+ 通知权限会影响前台服务通知可见性。"
            } else {
                "当前系统不需要运行时通知权限。"
            }
        )
        addPermissionStatus(
            content,
            "前台服务",
            "已声明",
            "用于保持调试浮窗服务运行。"
        )
        addPermissionStatus(
            content,
            "特殊前台服务类型",
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) "已声明" else "无需检查",
            "Android 14+ 使用 specialUse 类型启动调试浮窗服务。"
        )
        addPermissionStatus(
            content,
            "外部 file:// 写入",
            context.externalFileWriteStatus(),
            "仅 Android 6-9 打开 file:// JSON 时可能需要写存储权限；content:// 由文件管理器授权。"
        )
        addPermissionStatus(
            content,
            "SAF 导入/另存",
            if (context.canResolveDocumentAction(Intent.ACTION_OPEN_DOCUMENT)) "可用" else "不可用",
            "编辑器的导入和另存为依赖系统文件选择器。"
        )
        addPermissionStatus(
            content,
            "诊断报告分享",
            "可用",
            "通过 ${context.packageName}.debugtoolkit.fileprovider 分享 cache 中的诊断文本。"
        )

        val scrollView = ScrollView(context).apply { addView(content) }
        val dialog = AlertDialog.Builder(context)
            .setTitle("权限状态")
            .setView(scrollView)
            .setPositiveButton("悬浮窗设置") { _, _ ->
                DebugOperationLog.record("permission", "open_overlay_settings")
                context.openOverlaySettings()
            }
            .setNegativeButton("App 设置") { _, _ ->
                DebugOperationLog.record("permission", "open_app_settings")
                context.openAppSettings()
            }
            .setNeutralButton("关闭", null)
            .create()
        prepareOverlayDialog(dialog)
        dialog.show()
    }

    private fun addPermissionStatus(
        content: LinearLayout,
        title: String,
        status: String,
        detail: String
    ) {
        content.addView(TextView(content.context).apply {
            text = "$title: $status\n$detail"
            textSize = 13f
            setTextColor(0xFF333333.toInt())
            setPadding(0, content.context.dp(8), 0, content.context.dp(8))
        })
    }

    private fun Context.canDrawOverlaysCompat(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
    }

    private fun Context.externalFileWriteStatus(): String {
        return if (Build.VERSION.SDK_INT in Build.VERSION_CODES.M..Build.VERSION_CODES.P) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                "已允许"
            } else {
                "未允许"
            }
        } else {
            "无需运行时权限"
        }
    }

    private fun Context.canResolveDocumentAction(action: String): Boolean {
        val intent = Intent(action).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        return intent.resolveActivity(packageManager) != null
    }

    private fun Context.openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun Context.openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
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
}
