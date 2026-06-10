package com.debugtoolkit.networkinterceptor

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
