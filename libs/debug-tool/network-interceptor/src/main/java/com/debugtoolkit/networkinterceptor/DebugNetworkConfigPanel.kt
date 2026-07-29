package com.debugtoolkit.networkinterceptor

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
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
     * - 每个 rule 是一套可独立应用的映射集合；配置面板一次只选择并应用一个 rule。
     * - onRestart 和 onEditorOpened 由宿主调试工具传入，因为只有宿主调试工具知道如何重启或收起浮窗菜单。
     */
    fun show(
        context: Context,
        onRestart: () -> Unit = {},
        onEditorOpened: () -> Unit = {}
    ) {
        DebugNetworkConfigManager.init(context)
        DebugOperationLog.record(
            category = "network",
            action = "panel_open",
            message = "path=${DebugNetworkConfigManager.getConfigFilePath()}"
        )

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
        val pendingRuleId = arrayOfNulls<String>(1)
        pendingRuleId[0] = DebugNetworkConfigManager.getSelectedRuleIds().firstOrNull()
        lateinit var previewResultView: TextView
        if (rules.isEmpty()) {
            content.addView(TextView(context).apply {
                text = "未读取到网络拦截配置，请检查当前配置文件或通过编辑器导入 JSON。"
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
            addRuleSelectors(context, content, rules) { ruleId ->
                pendingRuleId[0] = ruleId
            }
            content.addView(TextView(context).apply {
                text = "URL 命中预览"
                textSize = 15f
                setTextColor(0xFF222222.toInt())
                setPadding(0, context.dp(14), 0, context.dp(4))
            })
            val previewInput = EditText(context).apply {
                hint = "输入完整 URL"
                setSingleLine(true)
                textSize = 13f
            }
            content.addView(previewInput)
            previewResultView = TextView(context).apply {
                textSize = 12f
                setTextColor(0xFF666666.toInt())
                setPadding(0, context.dp(6), 0, 0)
            }
            content.addView(previewResultView)
            content.addView(createButton(context, "预览") {
                val preview = DebugNetworkConfigManager.previewRewrite(
                    url = previewInput.text.toString(),
                    ruleId = pendingRuleId[0]
                )
                DebugOperationLog.record(
                    category = "network",
                    action = "preview_url",
                    message = "ruleId=${pendingRuleId[0]} hit=${preview.hit} reason=${preview.reason}",
                    success = preview.hit
                )
                previewResultView.text = preview.toDisplayText()
                previewResultView.setTextColor(if (preview.hit) 0xFF2E7D32.toInt() else 0xFFD32F2F.toInt())
            })
        }

        lateinit var dialog: AlertDialog
        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, context.dp(12), 0, 0)
        }
        actions.addView(createButton(context, "重新读取配置") {
            val success = DebugNetworkConfigManager.reloadConfigFromFile()
            DebugOperationLog.record("network", "reload_config", success = success)
            Log.d(TAG, "reload config clicked success=$success")
            Toast.makeText(context, if (success) "配置已重新读取" else "配置读取失败", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            show(context, onRestart, onEditorOpened)
        })
        actions.addView(createButton(context, "编辑配置") {
            val intent = Intent(DebugNetworkConfigManager.ACTION_EDIT_CONFIG).apply {
                setPackage(context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val started = runCatching { context.startActivity(intent) }
                .onFailure { error -> Log.e(TAG, "open editor failed", error) }
                .isSuccess
            DebugOperationLog.record("network", "open_editor", success = started)
            Log.d(TAG, "open editor clicked started=$started")
            if (!started) {
                Toast.makeText(context, "无法打开配置编辑器", Toast.LENGTH_SHORT).show()
            } else {
                dialog.dismiss()
                onEditorOpened()
            }
        })
        actions.addView(createButton(context, "立即应用") {
            DebugNetworkConfigManager.setExclusiveSelection(pendingRuleId[0])
            val mappings = DebugNetworkConfigManager.applySelectedMappings()
            DebugOperationLog.record(
                category = "network",
                action = "apply",
                message = "selected=${pendingRuleId[0]} mappings=${mappings.size}",
                success = true
            )
            Log.d(TAG, "apply clicked mappings=${mappings.size} selected=${pendingRuleId[0]}")
            Toast.makeText(context, "已应用 ${mappings.size} 条映射", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        })
        actions.addView(createButton(context, "应用并重启") {
            DebugNetworkConfigManager.setExclusiveSelection(pendingRuleId[0])
            val mappings = DebugNetworkConfigManager.applySelectedMappings()
            DebugOperationLog.record(
                category = "network",
                action = "apply_and_restart",
                message = "selected=${pendingRuleId[0]} mappings=${mappings.size}",
                success = true
            )
            Log.d(TAG, "apply and restart clicked mappings=${mappings.size} selected=${pendingRuleId[0]}")
            dialog.dismiss()
            onRestart()
        })
        actions.addView(createButton(context, "恢复模板配置") {
            DebugOperationLog.record("network", "reset_template_request")
            Log.d(TAG, "reset template clicked")
            showResetConfirm(context, dialog, onRestart, onEditorOpened)
        })
        content.addView(actions)

        val scrollView = ScrollView(context).apply { addView(content) }
        dialog = AlertDialog.Builder(context)
            .setTitle("网络拦截")
            .setView(scrollView)
            .create()
        prepareOverlayDialog(dialog)
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
    }

    private fun addRuleSelectors(
        context: Context,
        content: LinearLayout,
        rules: List<DebugNetworkRule>,
        onSelectionChanged: (String?) -> Unit
    ) {
        val initialSelectedId = DebugNetworkConfigManager.getSelectedRuleIds().firstOrNull()
        val radioGroup = RadioGroup(context).apply {
            orientation = RadioGroup.VERTICAL
        }
        rules.forEach { rule ->
            radioGroup.addView(RadioButton(context).apply {
                id = android.view.View.generateViewId()
                text = rule.name
                isChecked = rule.id == initialSelectedId
                setOnCheckedChangeListener { _, checked ->
                    if (checked) {
                        onSelectionChanged(rule.id)
                    }
                }
            })
        }
        content.addView(radioGroup)
    }

    private fun createButton(context: Context, text: String, action: () -> Unit): Button {
        return Button(context).apply {
            this.text = text
            setOnClickListener { action() }
        }
    }

    private fun DebugNetworkRewritePreview.toDisplayText(): String {
        if (!hit) {
            return "未命中: $reason"
        }
        return buildString {
            appendLine("已命中: ${ruleName.orEmpty().ifEmpty { ruleId.orEmpty() }}")
            appendLine("source: ${source.orEmpty()}")
            appendLine("target: ${target.orEmpty()}")
            append("rewrite: ${rewrittenUrl.orEmpty()}")
        }
    }

    private fun showResetConfirm(
        context: Context,
        parentDialog: AlertDialog,
        onRestart: () -> Unit,
        onEditorOpened: () -> Unit
    ) {
        val confirmDialog = AlertDialog.Builder(context)
            .setTitle("恢复模板配置")
            .setMessage("会覆盖当前生效的 debug_network_config.json，已手动修改的映射关系会丢失。确定继续吗？")
            .setPositiveButton("确定") { dialog, _ ->
                val success = DebugNetworkConfigManager.resetConfigToTemplate()
                DebugOperationLog.record("network", "reset_template_confirm", success = success)
                Log.d(TAG, "reset template confirmed success=$success")
                Toast.makeText(context, if (success) "已恢复模板" else "恢复模板失败", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                parentDialog.dismiss()
                show(context, onRestart, onEditorOpened)
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
