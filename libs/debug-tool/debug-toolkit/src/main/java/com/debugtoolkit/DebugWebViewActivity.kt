package com.debugtoolkit

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
