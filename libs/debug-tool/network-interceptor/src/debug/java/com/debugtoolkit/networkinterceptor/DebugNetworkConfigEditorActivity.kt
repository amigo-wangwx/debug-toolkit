package com.debugtoolkit.networkinterceptor

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
