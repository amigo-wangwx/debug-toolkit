package com.debugtoolkit.sample

import android.app.Activity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.debugtoolkit.networkinterceptor.DebugNetworkConfigManager
import com.debugtoolkit.networkinterceptor.DebugNetworkInterceptorManager
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var resultView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createContentView())

        DebugNetworkConfigManager.init(this)
        DebugNetworkConfigManager.applySelectedMappings()
        appendLine("Debug config: ${DebugNetworkConfigManager.getConfigFilePath()}")
        appendLine("Selected rules: ${DebugNetworkConfigManager.getSelectedRuleIds()}")
    }

    private fun createContentView(): ScrollView {
        val density = resources.displayMetrics.density
        val padding = (16 * density).toInt()

        resultView = TextView(this).apply {
            textSize = 14f
            movementMethod = ScrollingMovementMethod()
            setTextIsSelectable(true)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            addView(titleText())
            addView(actionButton("Manual apply request") {
                runRequest(
                    title = "manual",
                    client = DebugNetworkInterceptorManager.apply(OkHttpClient.Builder()).build()
                )
            })
            addView(actionButton("ASM injected request") {
                // 这里不手动调用 apply，用来验证 Gradle/ASM 插件是否在 Builder.build() 前自动注入。
                runRequest(title = "asm", client = OkHttpClient.Builder().build())
            })
            addView(resultView)
        }

        return ScrollView(this).apply {
            addView(
                container,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun titleText(): TextView {
        return TextView(this).apply {
            text = "DebugToolkit Sample"
            textSize = 22f
            setPadding(0, 0, 0, (12 * resources.displayMetrics.density).toInt())
        }
    }

    private fun actionButton(text: String, action: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setOnClickListener { action() }
        }
    }

    private fun runRequest(title: String, client: OkHttpClient) {
        appendLine("")
        appendLine("[$title] start")

        thread(name = "sample-$title-request") {
            val request = Request.Builder()
                .url("https://debugtoolkit.example/get?client=$title")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    appendLine("[$title] code=${response.code}")
                    appendLine("[$title] requestUrl=${request.url}")
                    appendLine("[$title] response=${body.take(360)}")
                }
            } catch (error: IOException) {
                appendLine("[$title] failed=${error.message}")
            }
        }
    }

    private fun appendLine(text: String) {
        runOnUiThread {
            resultView.append(text)
            resultView.append("\n")
        }
    }
}
