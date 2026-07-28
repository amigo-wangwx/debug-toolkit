package com.debugtoolkit.networkinterceptor

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
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
    private var editorMode: EditorMode = EditorMode.CONFIG
    private var pendingPermissionText: String? = null
    private var pendingPermissionFinishAfterSave: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isDebuggable()) {
            Log.d(TAG, "finish because application is not debuggable")
            finish()
            return
        }

        sourceUri = intent.data
        editorMode = if (sourceUri != null) EditorMode.EXTERNAL else EditorMode.CONFIG
        Log.d(
            TAG,
            "onCreate mode=$editorMode uri=$sourceUri " +
                    "action=${intent.action} type=${intent.type}"
        )
        buildContentView()
        loadConfigText()
    }

    private fun buildContentView() {
        val rootPaddingLeft = dp(16)
        val rootPaddingTop = dp(12)
        val rootPaddingRight = dp(16)
        val rootPaddingBottom = dp(12)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(rootPaddingLeft, rootPaddingTop, rootPaddingRight, rootPaddingBottom)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        applySystemInsetsPadding(root, rootPaddingLeft, rootPaddingTop, rootPaddingRight, rootPaddingBottom)

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
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, 0)
        }
        actions.addView(ActionFlowLayout(this).apply {
            horizontalSpacing = dp(8)
            verticalSpacing = dp(8)
            addView(createButton("导入") { openImportPicker() })
            addView(createButton("导入为配置") { markCurrentTextAsPendingImport() })
            addView(createButton("另存为") { openSaveAsPicker() })
            addView(createButton("格式化") { formatContent() })
            addView(createButton("保存") { saveContent(finishAfterSave = false) })
            addView(createButton("保存并关闭") { saveContent(finishAfterSave = true) })
        })
        root.addView(actions)

        setContentView(root)
    }

    private fun applySystemInsetsPadding(
        view: View,
        baseLeft: Int,
        baseTop: Int,
        baseRight: Int,
        baseBottom: Int
    ) {
        view.setOnApplyWindowInsetsListener { target, insets ->
            val systemTop: Int
            val systemBottom: Int
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                val ime = insets.getInsets(WindowInsets.Type.ime())
                systemTop = bars.top
                systemBottom = maxOf(bars.bottom, ime.bottom)
            } else {
                @Suppress("DEPRECATION")
                systemTop = insets.systemWindowInsetTop
                @Suppress("DEPRECATION")
                systemBottom = insets.systemWindowInsetBottom
            }
            target.setPadding(
                baseLeft,
                baseTop + systemTop,
                baseRight,
                baseBottom + systemBottom
            )
            insets
        }
        view.requestApplyInsets()
    }

    private fun createButton(text: String, action: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setOnClickListener { action() }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return

        val uri = data?.data
        if (uri == null) {
            Log.d(TAG, "document picker returned empty uri request=$requestCode")
            toast("未选择文件")
            return
        }

        when (requestCode) {
            REQUEST_IMPORT_CONFIG -> loadImportFromUri(uri)
            REQUEST_SAVE_AS_CONFIG -> saveAsConfigToUri(uri)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_WRITE_EXTERNAL_STORAGE) return

        val text = pendingPermissionText
        pendingPermissionText = null
        val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        if (!granted || text == null) {
            Log.d(TAG, "write external storage permission denied")
            toast("原文件无写入权限，可使用另存为")
            return
        }

        saveExternalText(text, pendingPermissionFinishAfterSave, retryAfterPermission = true)
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
            "save start mode=$editorMode uri=$uri " +
                    "length=${formatted.length} finishAfterSave=$finishAfterSave"
        )

        when (editorMode) {
            EditorMode.CONFIG -> saveConfigText(formatted, finishAfterSave)
            EditorMode.EXTERNAL -> saveExternalText(formatted, finishAfterSave)
            EditorMode.PENDING_IMPORT -> saveConfigText(formatted, finishAfterSave)
        }
    }

    private fun saveConfigText(formatted: String, finishAfterSave: Boolean) {
        DebugNetworkConfigManager.init(this)
        if (!DebugNetworkConfigManager.writeConfigText(formatted)) {
            Log.d(TAG, "save config failed mode=$editorMode")
            toast("保存失败，请确认配置文件可写")
            return
        }
        editorMode = EditorMode.CONFIG
        sourceUri = null
        editorView.setText(formatted)
        statusView.text = DebugNetworkConfigManager.getConfigFilePath()
        sendReloadBroadcast()
        Log.d(TAG, "save config success path=${DebugNetworkConfigManager.getConfigFilePath()}")
        toast("已保存")
        if (finishAfterSave) {
            finish()
        }
    }

    private fun saveExternalText(
        formatted: String,
        finishAfterSave: Boolean,
        retryAfterPermission: Boolean = false
    ) {
        val uri = sourceUri
        if (uri == null) {
            Log.d(TAG, "save external skipped because uri is null")
            toast("保存失败，外部文件地址为空")
            return
        }

        if (writeTextToExternalUri(uri, formatted, finishAfterSave, retryAfterPermission)) {
            editorView.setText(formatted)
            statusView.text = uri.toString()
            Log.d(TAG, "save external success uri=$uri")
            toast("已保存")
            if (finishAfterSave) {
                finish()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun openImportPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/*"))
        }
        val started = runCatching { startActivityForResult(intent, REQUEST_IMPORT_CONFIG) }
            .onFailure { error -> Log.e(TAG, "open import picker failed", error) }
            .isSuccess
        Log.d(TAG, "open import picker started=$started")
        if (!started) {
            toast("无法打开文件选择器")
        }
    }

    @Suppress("DEPRECATION")
    private fun openSaveAsPicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "debug_network_config.json")
        }
        val started = runCatching { startActivityForResult(intent, REQUEST_SAVE_AS_CONFIG) }
            .onFailure { error -> Log.e(TAG, "open save as picker failed", error) }
            .isSuccess
        Log.d(TAG, "open save as picker started=$started")
        if (!started) {
            toast("无法打开文件保存器")
        }
    }

    private fun loadImportFromUri(uri: Uri) {
        val text = readTextFromUri(uri)
        if (text == null) {
            toast("导入失败，请确认文件读取权限")
            return
        }

        val formatted = runCatching { JSONObject(text).toString(2) }
            .getOrElse { error ->
                Log.e(TAG, "import json parse failed uri=$uri length=${text.length}", error)
                toast("导入失败: JSON 格式错误")
                return
            }

        editorMode = EditorMode.PENDING_IMPORT
        sourceUri = null
        editorView.setText(formatted)
        statusView.text = "待导入为网络拦截配置: $uri"
        Log.d(TAG, "import config loaded uri=$uri length=${formatted.length}")
        toast("已载入，点击保存后生效")
    }

    private fun markCurrentTextAsPendingImport() {
        val formatted = parseEditorJson() ?: return
        editorMode = EditorMode.PENDING_IMPORT
        sourceUri = null
        editorView.setText(formatted)
        statusView.text = "待导入为网络拦截配置"
        Log.d(TAG, "current text marked as pending import length=${formatted.length}")
        toast("已标记为配置，点击保存后生效")
    }

    private fun saveAsConfigToUri(uri: Uri) {
        val formatted = parseEditorJson() ?: return
        if (!writeTextToUri(uri, formatted)) {
            toast("另存失败，请确认文件写入权限")
            return
        }

        Log.d(TAG, "save as config success uri=$uri length=${formatted.length}")
        toast("已另存")
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

    private fun writeTextToExternalUri(
        uri: Uri,
        text: String,
        finishAfterSave: Boolean,
        retryAfterPermission: Boolean
    ): Boolean {
        if (uri.scheme == "file") {
            return writeTextToFileUri(uri, text, finishAfterSave, retryAfterPermission)
        }

        return if (writeTextToUri(uri, text)) {
            true
        } else {
            toast("原文件无写入权限，可使用另存为")
            false
        }
    }

    private fun writeTextToFileUri(
        uri: Uri,
        text: String,
        finishAfterSave: Boolean,
        retryAfterPermission: Boolean
    ): Boolean {
        if (needsWriteExternalStoragePermission() && !retryAfterPermission) {
            pendingPermissionText = text
            pendingPermissionFinishAfterSave = finishAfterSave
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_EXTERNAL_STORAGE
            )
            Log.d(TAG, "request write external storage permission uri=$uri")
            return false
        }

        return runCatching {
            val path = uri.path ?: error("file uri path is null")
            java.io.File(path).writeText(text)
            Log.d(TAG, "write file uri success uri=$uri length=${text.length}")
            true
        }.getOrElse { error ->
            Log.e(TAG, "write file uri failed uri=$uri", error)
            toast("原文件无写入权限，可使用另存为")
            false
        }
    }

    private fun needsWriteExternalStoragePermission(): Boolean {
        return Build.VERSION.SDK_INT in Build.VERSION_CODES.M..Build.VERSION_CODES.P &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
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
        private const val REQUEST_IMPORT_CONFIG = 1001
        private const val REQUEST_SAVE_AS_CONFIG = 1002
        private const val REQUEST_WRITE_EXTERNAL_STORAGE = 1003
    }

    private enum class EditorMode {
        CONFIG,
        EXTERNAL,
        PENDING_IMPORT
    }

    private class ActionFlowLayout(context: android.content.Context) : ViewGroup(context) {
        var horizontalSpacing: Int = 0
        var verticalSpacing: Int = 0

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val maxWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
            var lineWidth = 0
            var lineHeight = 0
            var totalHeight = paddingTop + paddingBottom
            var measuredWidth = 0

            forEachVisibleChild { child ->
                measureChild(child, widthMeasureSpec, heightMeasureSpec)
                val childWidth = child.measuredWidth
                val childHeight = child.measuredHeight
                if (lineWidth > 0 && lineWidth + horizontalSpacing + childWidth > maxWidth) {
                    totalHeight += lineHeight + verticalSpacing
                    measuredWidth = maxOf(measuredWidth, lineWidth)
                    lineWidth = childWidth
                    lineHeight = childHeight
                } else {
                    lineWidth += if (lineWidth == 0) childWidth else horizontalSpacing + childWidth
                    lineHeight = maxOf(lineHeight, childHeight)
                }
            }

            totalHeight += lineHeight
            measuredWidth = maxOf(measuredWidth, lineWidth) + paddingLeft + paddingRight
            setMeasuredDimension(
                resolveSize(measuredWidth, widthMeasureSpec),
                resolveSize(totalHeight, heightMeasureSpec)
            )
        }

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            val maxWidth = right - left - paddingLeft - paddingRight
            var x = paddingLeft
            var y = paddingTop
            var lineHeight = 0

            forEachVisibleChild { child ->
                val childWidth = child.measuredWidth
                val childHeight = child.measuredHeight
                if (x > paddingLeft && x - paddingLeft + horizontalSpacing + childWidth > maxWidth) {
                    x = paddingLeft
                    y += lineHeight + verticalSpacing
                    lineHeight = 0
                }

                child.layout(x, y, x + childWidth, y + childHeight)
                x += childWidth + horizontalSpacing
                lineHeight = maxOf(lineHeight, childHeight)
            }
        }

        private inline fun forEachVisibleChild(action: (View) -> Unit) {
            for (index in 0 until childCount) {
                val child = getChildAt(index)
                if (child.visibility != GONE) {
                    action(child)
                }
            }
        }
    }
}
