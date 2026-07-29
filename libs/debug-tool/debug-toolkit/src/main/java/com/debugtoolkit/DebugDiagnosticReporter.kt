package com.debugtoolkit

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import com.debugtoolkit.networkinterceptor.DebugNetworkConfigManager
import com.debugtoolkit.networkinterceptor.DebugOperationLog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugDiagnosticReporter {
    private const val AUTHORITY_SUFFIX = ".debugtoolkit.fileprovider"
    private const val REPORT_DIR_NAME = "debug_toolkit_reports"
    private const val REPORT_FILE_NAME = "debug_toolkit_report.txt"

    fun share(context: Context) {
        val result = runCatching {
            DebugNetworkConfigManager.init(context)
            val file = writeReportFile(context, buildReportText(context))
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + AUTHORITY_SUFFIX,
                file
            )
            shareReportUri(context, uri)
            DebugOperationLog.record("diagnostic", "share_report", file.absolutePath, success = true)
        }
        result.onFailure { error ->
            DebugOperationLog.record("diagnostic", "share_report", error.message.orEmpty(), success = false)
            Toast.makeText(context, "诊断报告导出失败: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildReportText(context: Context): String {
        val packageInfo = context.packageManager.getPackageInfoCompat(context.packageName)
        return buildString {
            appendLine("DebugToolkit Diagnostic Report")
            appendLine("GeneratedAt: ${REPORT_TIME_FORMAT.format(Date())}")
            appendLine()
            appendLine("[App]")
            appendLine("Package: ${context.packageName}")
            appendLine("VersionName: ${packageInfo.versionName.orEmpty()}")
            appendLine("VersionCode: ${packageInfo.versionCodeCompat()}")
            appendLine("Debuggable: ${context.isDebuggable()}")
            appendLine()
            appendLine("[Device]")
            appendLine("Manufacturer: ${Build.MANUFACTURER}")
            appendLine("Model: ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE}")
            appendLine("SDK: ${Build.VERSION.SDK_INT}")
            appendLine()
            appendLine("[NetworkConfig]")
            appendLine("Path: ${DebugNetworkConfigManager.getConfigFilePath()}")
            appendLine("SelectedRuleIds: ${DebugNetworkConfigManager.getSelectedRuleIds().joinToString()}")
            appendLine("RuleCount: ${DebugNetworkConfigManager.getRules().size}")
            appendLine("LastError: ${DebugNetworkConfigManager.getLastError().orEmpty()}")
            appendLine()
            appendLine("[Host]")
            val hostInfo = DebugHostBridge.collectInfo(context)
            if (hostInfo.isEmpty()) {
                appendLine("(empty)")
            } else {
                hostInfo.forEach { (key, value) ->
                    appendLine("$key: $value")
                }
            }
            appendLine()
            appendLine("[OperationLog]")
            val entries = DebugOperationLog.getRecentEntries()
            if (entries.isEmpty()) {
                appendLine("(empty)")
            } else {
                entries.forEach { entry ->
                    appendLine(entry.toReportLine())
                }
            }
        }
    }

    private fun writeReportFile(context: Context, text: String): File {
        val dir = File(context.cacheDir, REPORT_DIR_NAME)
        dir.mkdirs()
        val file = File(dir, REPORT_FILE_NAME)
        file.writeText(text)
        return file
    }

    private fun shareReportUri(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "DebugToolkit 诊断报告")
            clipData = ClipData.newUri(context.contentResolver, "DebugToolkit 诊断报告", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "分享诊断报告").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    }

    private fun android.content.pm.PackageManager.getPackageInfoCompat(packageName: String): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageInfo(packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            getPackageInfo(packageName, 0)
        }
    }

    private fun PackageInfo.versionCodeCompat(): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            longVersionCode
        } else {
            @Suppress("DEPRECATION")
            versionCode.toLong()
        }
    }

    private fun Context.isDebuggable(): Boolean {
        return (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    private fun DebugOperationLog.Entry.toReportLine(): String {
        val successText = success?.let { " success=$it" }.orEmpty()
        val messageText = message.takeIf { it.isNotBlank() }?.let { " message=$it" }.orEmpty()
        return "$timeText [$category] action=$action$successText$messageText"
    }

    private val REPORT_TIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US)
}
