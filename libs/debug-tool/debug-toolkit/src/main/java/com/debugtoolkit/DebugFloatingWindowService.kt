package com.debugtoolkit

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.annotation.SuppressLint
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.GridView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.debugtoolkit.networkinterceptor.DebugNetworkConfigPanel
import com.debugtoolkit.networkinterceptor.DebugOperationLog

class DebugFloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var triggerView: TextView
    private lateinit var menuContainer: View

    // 保存宿主App图标
    private var hostAppIcon: Drawable? = null

    private val CHANNEL_ID = "DebugToolkit_Channel"
    private val NOTIFICATION_ID = 1004
    private var isMenuOpen = false
    private var layoutParams: WindowManager.LayoutParams? = null

    // 拖拽相关变量
    private var dX = 0f
    private var dY = 0f

    // 点击判定变量
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var isDragging = false

    companion object {
        private const val REQUEST_CODE_RESTART = 10086
        private const val META_DATA_DEEPLINK_SCHEME = "debugtoolkit.deeplink.scheme"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startMyForegroundService()

        // 获取并保存宿主App图标
        getHostAppIcon()

        // 使用 Handler 延迟检查，确保服务初始化完成
        Handler(Looper.getMainLooper()).post {
            val canDrawOverlays = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                    Settings.canDrawOverlays(this)
            if (canDrawOverlays) {
                showFloatingWindow()
            } else {
                requestOverlayPermission()
            }
        }
    }

    private fun startMyForegroundService() {
        val notification = getNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /**
     * 获取并保存宿主App图标
     */
    private fun getHostAppIcon() {
        try {
            hostAppIcon = packageManager.getApplicationIcon(application.packageName)
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果获取失败，保持为null，使用默认背景
        }
    }

    private fun showFloatingWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_debug_floating_window, null)

        triggerView = floatingView.findViewById<TextView>(R.id.btn_trigger)
        menuContainer = floatingView.findViewById(R.id.layout_menu)

        menuContainer.visibility = View.GONE

        // 设置宿主App图标作为背景
        setHostAppIconAsBackground()

        // 初始设置为 WRAP_CONTENT
        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },

           // 默认状态：不获取焦点 + 允许点击穿透

            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        layoutParams?.gravity = Gravity.TOP or Gravity.START

        // ==================== 恢复上次保存的位置 ====================
        if (DebugConfig.hasSavedPosition()) {
            val savedX = DebugConfig.getLastFloatX()
            val savedY = DebugConfig.getLastFloatY()
            if (savedX != null && savedY != null) {
                layoutParams?.x = savedX
                layoutParams?.y = savedY
            } else {
                layoutParams?.x = 0
                layoutParams?.y = 200
            }
        } else {
            layoutParams?.x = 0
            layoutParams?.y = 200
        }

        try {
            windowManager.addView(floatingView, layoutParams)
            setupViews()
            setupDragListener()
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    /**
     * 🔥 核心方法：设置宿主App图标作为背景
     */
    private fun setHostAppIconAsBackground() {
        // 如果有保存的图标，则设置
        hostAppIcon?.let { triggerView.background = it }
        // 如果没有，保持XML中设置的默认背景

    }

    /**
     * 🔥 核心方法：动态切换窗口焦点状态
     * @param needFocus true: 移除 FLAG_NOT_FOCUSABLE (允许输入，会拦截返回键)
     *                 false: 添加 FLAG_NOT_FOCUSABLE (不允许输入，返回键传给背景)
     */
    private fun updateWindowFocus(needFocus: Boolean) {
        val params = layoutParams ?: return
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        if (needFocus) {
            // 需要输入：移除 FLAG_NOT_FOCUSABLE，让窗口能获取焦点，从而弹出键盘
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            imm.showSoftInput(floatingView.findFocus(), 0)
        } else {
            // 不需要输入：恢复 FLAG_NOT_FOCUSABLE，让返回键传递给背景应用
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            imm.hideSoftInputFromWindow(floatingView.windowToken, 0)
            // 清除输入框的焦点
            floatingView.findFocus()?.clearFocus()
        }
        windowManager.updateViewLayout(floatingView, params)
    }

    private fun toggleMenu() {
        isMenuOpen = !isMenuOpen
        DebugOperationLog.record(
            category = "floating",
            action = if (isMenuOpen) "menu_open" else "menu_close"
        )
        if (isMenuOpen) {
            menuContainer.visibility = View.VISIBLE
            // 恢复宿主App图标背景
            hostAppIcon?.let {
                triggerView.background = it
            }
        } else {
            // 关闭菜单：隐藏容器
            menuContainer.visibility = View.GONE
            // 恢复宿主App图标背景
            hostAppIcon?.let {
                triggerView.background = it
            }
            // 关闭菜单时，确保退出输入模式
            updateWindowFocus(false)
        }
    }

    /**
     * 供 HostAction 使用的安全关闭入口。
     *
     * HostAction 只能表达“点击成功后需要关闭菜单”，不能直接访问 Service 内部菜单状态。
     * 这里先判断 isMenuOpen，避免菜单已关闭时再次 toggle 导致反向打开。
     */
    private fun closeMenuIfOpen() {
        if (isMenuOpen) {
            toggleMenu()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragListener() {
        // 获取系统推荐触摸阈值
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop.toFloat()

        triggerView.setOnTouchListener { view, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 记录按下的位置
                    touchDownX = event.rawX
                    touchDownY = event.rawY
                    // 记录 Window 的位移
                    dX = params.x.toFloat() - event.rawX
                    dY = params.y.toFloat() - event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    // 移动 Window
                    val moveDistance = Math.hypot(
                        (event.rawX - touchDownX).toDouble(),
                        (event.rawY - touchDownY).toDouble()
                    )
                    // ✅ 只有超过阈值才开始拖拽
                    if (moveDistance > touchSlop) {
                        isDragging = true
                        params.x = (event.rawX + dX).toInt()
                        params.y = (event.rawY + dY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        snapToEdge(view)
                    } else {
                        // ✅ 未超过阈值，才是真正的点击
                        toggleMenu()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun snapToEdge(view: View) {
        val screenWidth: Int
        val screenHeight: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            screenWidth = bounds.width()
            screenHeight = bounds.height()
        } else {
            // Android 10 及以下没有 WindowMetrics，使用兼容的屏幕尺寸避免拖拽松手时崩溃。
            screenWidth = resources.displayMetrics.widthPixels
            screenHeight = resources.displayMetrics.heightPixels
        }

        // 简化：始终操作 Window 坐标
        val currentX = layoutParams?.x?.toFloat() ?: 0f
        val currentY = layoutParams?.y?.toFloat() ?: 0f
        val viewWidth = view.width
        val viewHeight = view.height

        val centerX = currentX + viewWidth / 2
        val targetX = if (centerX < screenWidth / 2) {
            0f
        } else {
            (screenWidth - viewWidth).toFloat()
        }

        val targetY = currentY.coerceAtLeast(0f).coerceAtMost(screenHeight.toFloat() - viewHeight)

        // 瞬移窗口位置
        layoutParams?.x = targetX.toInt()
        layoutParams?.y = targetY.toInt()
        windowManager.updateViewLayout(floatingView, layoutParams)

        // 保存吸附后的位置
        saveFloatPosition()
    }

    // ==================== 位置持久化 ====================

    private fun saveFloatPosition() {
        val params = layoutParams ?: return
        DebugConfig.saveFloatPosition(params.x, params.y)
    }

    // ==================== 重启 App ====================

    /**
     * 重启原理：
     * 1. 通过 AlarmManager 安排启动新的入口页。
     * 2. 结束旧进程，由系统在下一次 alarm 触发时创建新进程。
     *
     * 使用普通 alarm，不依赖精确闹钟权限。
     */
    private fun restartApp() {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent == null) {
                DebugOperationLog.record("app", "restart", "launchIntent is null", success = false)
                Toast.makeText(this, "无法获取启动Intent", Toast.LENGTH_SHORT).show()
                return
            }
            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
            val pendingIntentFlags = PendingIntent.FLAG_CANCEL_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            val restartIntent = PendingIntent.getActivity(
                this,
                REQUEST_CODE_RESTART,
                launchIntent,
                pendingIntentFlags
            )
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            // 使用普通 alarm 即可完成进程外重启，不要求精确闹钟权限。
            alarmManager.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 500L,
                restartIntent
            )

            Toast.makeText(this, "重启中，请稍后", Toast.LENGTH_LONG).show()
            DebugOperationLog.record("app", "restart", "alarm scheduled", success = true)

            try {
                windowManager.removeView(floatingView)
            } catch (_: Exception) {
            }
            android.os.Process.killProcess(android.os.Process.myPid())
        } catch (e: Exception) {
            e.printStackTrace()
            DebugOperationLog.record("app", "restart", e.message.orEmpty(), success = false)
            Toast.makeText(this, "重启失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ==================== 清除 MMKV ====================

    private fun clearMMKVData() {
        val success = DebugConfig.clearMMKVData()
        DebugOperationLog.record("app", "clear_mmkv", success = success)
        val message = if (success) "MMKV 数据已清除" else "清除 MMKV 失败"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        if (success) {
            Handler(Looper.getMainLooper()).postDelayed({
                restartApp()
            }, 100)
        }
    }

    // ==================== 归因 Mock ====================

    /**
     * 通过反射调用宿主 App 的归因 Mock 方法，走完整归因链路。
     * @param methodName 宿主 Application 的方法名，如 "debugMockAFAttribution" 或 "debugMockHTMAttribution"
     * @param linkValue 归因链接或追踪链接
     */
    private fun mockAttributionFromApp(methodName: String, linkValue: String) {
        try {
            val app = application
            val mockMethod = app.javaClass.getMethod(methodName, String::class.java)
            mockMethod.invoke(app, linkValue)
            DebugOperationLog.record("attribution", methodName, linkValue, success = true)
            Toast.makeText(this, "归因Mock: $methodName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            DebugOperationLog.record("attribution", methodName, e.message.orEmpty(), success = false)
            Toast.makeText(this, "归因Mock失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        toggleMenu()
    }

    private fun resetAttributionFromApp() {
        try {
            val method = application.javaClass.getMethod("debugResetAttribution")
            method.invoke(application)
            DebugOperationLog.record("attribution", "reset", success = true)
            Toast.makeText(this, "归因状态已重置", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            DebugOperationLog.record("attribution", "reset", e.message.orEmpty(), success = false)
            Toast.makeText(this, "重置失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        toggleMenu()
    }

    private fun buildDebugDeepLink(path: String): String {
        return "${getDebugDeepLinkScheme()}://$path"
    }

    private fun getDebugDeepLinkScheme(): String {
        val configuredScheme = runCatching {
            packageManager
                .getApplicationInfo(packageName, android.content.pm.PackageManager.GET_META_DATA)
                .metaData
                ?.getString(META_DATA_DEEPLINK_SCHEME)
                .orEmpty()
        }.getOrDefault("")

        return configuredScheme.sanitizeScheme()
            .ifEmpty { getReadableAppName().sanitizeScheme() }
            .ifEmpty { packageName.substringAfterLast('.').sanitizeScheme() }
            .ifEmpty { "app" }
    }

    private fun getReadableAppName(): String {
        return runCatching {
            packageManager.getApplicationLabel(applicationInfo).toString()
        }.getOrDefault("")
    }

    private fun String.sanitizeScheme(): String {
        return lowercase().filter { it in 'a'..'z' || it in '0'..'9' || it == '+' || it == '.' || it == '-' }
    }

    // ==================== UI 权限与通知 ====================

    private fun requestOverlayPermission() {
        DebugOperationLog.record("permission", "request_overlay")
        Toast.makeText(this, "请授予悬浮窗权限以显示调试工具", Toast.LENGTH_LONG).show()
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        stopSelf() // 权限被拒绝时停止服务
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "DebugToolkit Service",
                NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun getNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DebugToolkit")
            .setContentText("调试工具正在运行")
            .setSmallIcon(R.drawable.ic_search)
            .build()
    }

    // ==================== 设置视图 ====================

    private fun setupViews() {
        val gridViewButtons = menuContainer.findViewById<GridView>(R.id.grid_buttons)
        val etUri = menuContainer.findViewById<EditText>(R.id.et_uri)
        val btnOpenUri = menuContainer.findViewById<Button>(R.id.btn_open_uri)
        val btnClearUri = menuContainer.findViewById<Button>(R.id.btn_clear_uri)

        val buttonItems = createDebugActions()

        // 设置网格适配器
        val gridAdapter = DebugButtonAdapter(this, buttonItems)
        gridViewButtons.adapter = gridAdapter

        btnOpenUri.setOnClickListener {
            val uriString = etUri.text.toString().trim()
            if (uriString.isEmpty()) {
                DebugOperationLog.record("uri", "open", "empty uri", success = false)
                Toast.makeText(this, "请输入 URI", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 宿主输入 action 优先处理业务协议；未命中时保持原有 URI 打开行为。
            when (val result = DebugHostBridge.processInput(this, uriString)) {
                DebugHostBridge.HostInputResult.NotHandled -> openInputUri(uriString, Uri.parse(uriString))
                DebugHostBridge.HostInputResult.Handled -> finishInputHandling()
                is DebugHostBridge.HostInputResult.OpenUri -> openInputUri(uriString, result.uri)
                is DebugHostBridge.HostInputResult.Rejected -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 🔥 关键修改：处理输入框的焦点和粘贴问题
        etUri.setOnFocusChangeListener { _, hasFocus ->
            updateWindowFocus(hasFocus)
        }

        // 点击时也确保获取焦点
        etUri.setOnClickListener {
            etUri.requestFocus()
            updateWindowFocus(true)
        }

        // 监听回车键，完成输入后自动收起键盘
        etUri.setOnEditorActionListener { _, _, _ ->
            updateWindowFocus(false)
            false
        }

        btnClearUri.setOnClickListener {
            etUri.text?.clear()
            etUri.requestFocus()
            updateWindowFocus(true)
        }
    }

    /** 打开原始或宿主转换后的 URI；失败时保留菜单和输入内容，方便修正后重试。 */
    private fun openInputUri(input: String, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            DebugOperationLog.record("uri", "open", "$input -> $uri", success = true)
            finishInputHandling()
        } catch (error: Exception) {
            DebugOperationLog.record("uri", "open", input, success = false)
            Toast.makeText(this, "无效的 URI: $input", Toast.LENGTH_SHORT).show()
        }
    }

    /** 输入处理成功后收起菜单并释放输入焦点。 */
    private fun finishInputHandling() {
        toggleMenu()
        updateWindowFocus(false)
    }

    private fun createDebugActions(): List<DebugAction> {
        return createBuiltInDebugModules()
            .plus(DebugModuleRegistry.getModules())
            .flatMap { module -> module.createActions() }
            .filter { action -> action.visible(this) }
    }

    private fun createBuiltInDebugModules(): List<DebugModule> {
        return listOf(
            SimpleDebugModule("system", "系统") {
                listOf(
                    DebugAction("system.log", "日志", R.drawable.ic_search, "#FF5722", "system") {
                        try {
                            val intent = Intent(this, Class.forName("com.hjq.logcat.LogcatActivity"))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(this, "启动日志页面失败", Toast.LENGTH_SHORT).show()
                        }
                        toggleMenu()
                    },
                    DebugAction("system.permission", "权限", R.drawable.ic_search, "#2196F3", "system") {
                        DebugPermissionPanel.show(this)
                        toggleMenu()
                    },
                    DebugAction("system.developer", "开发者", R.drawable.ic_search, "#4CAF50", "system") {
                        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        toggleMenu()
                    }
                )
            },
            SimpleDebugModule("app", "应用") {
                listOf(
                    DebugAction("app.home", "主页", R.drawable.ic_search, "#9C27B0", "app") {
                        try {
                            val intent = packageManager.getLaunchIntentForPackage(packageName)
                            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            intent?.let { startActivity(it) }
                            toggleMenu()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(this, "打开主页失败", Toast.LENGTH_SHORT).show()
                        }
                    },
                    DebugAction("app.webview", "WebView", R.drawable.ic_search, "#FF9800", "app") {
                        val intent = Intent(this, DebugWebViewActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        toggleMenu()
                    },
                    DebugAction("app.close", "关闭", R.drawable.ic_search, "#F44336", "app") {
                        android.os.Process.killProcess(android.os.Process.myPid())
                        toggleMenu()
                    },
                    DebugAction("app.restart", "重启", R.drawable.ic_search, "#009688", "app") {
                        restartApp()
                    },
                    DebugAction("app.clear_mmkv", "清MMKV", R.drawable.ic_search, "#795548", "app") {
                        clearMMKVData()
                    },
                    DebugAction("app.diagnostic", "诊断", R.drawable.ic_search, "#607D8B", "app") {
                        DebugDiagnosticReporter.share(this)
                        toggleMenu()
                    }
                )
            },
            SimpleDebugModule("network", "网络") {
                listOf(
                    DebugAction("network.config", "网络", R.drawable.ic_search, "#607D8B", "network") {
                        DebugNetworkConfigPanel.show(
                            context = this,
                            onRestart = { restartApp() },
                            onEditorOpened = { toggleMenu() }
                        )
                    }
                )
            },
            SimpleDebugModule("host", "业务") {
                DebugHostBridge.createDebugActions(this, ::closeMenuIfOpen)
            },
            SimpleDebugModule("attribution", "归因") {
                listOf(
                    DebugAction("attribution.af_drama", "AF→短剧", R.drawable.ic_search, "#E91E63", "attribution") {
                        mockAttributionFromApp("debugMockAFAttribution", "applovin_drama_123_0__debug__001")
                    },
                    DebugAction("attribution.af_novel", "AF→小说", R.drawable.ic_search, "#3F51B5", "attribution") {
                        mockAttributionFromApp("debugMockAFAttribution", "applovin_novel_456_0__debug__001")
                    },
                    DebugAction("attribution.htm_drama", "HTM→短剧", R.drawable.ic_search, "#009688", "attribution") {
                        mockAttributionFromApp(
                            "debugMockHTMAttribution",
                            buildDebugDeepLink("navigator/video/player/123/0")
                        )
                    },
                    DebugAction("attribution.htm_novel", "HTM→小说", R.drawable.ic_search, "#795548", "attribution") {
                        mockAttributionFromApp(
                            "debugMockHTMAttribution",
                            buildDebugDeepLink("navigator/novel/read/456/0")
                        )
                    },
                    DebugAction("attribution.reset", "重置归因", R.drawable.ic_search, "#FF9800", "attribution") {
                        resetAttributionFromApp()
                    }
                )
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        // 销毁时保存当前位置
        saveFloatPosition()
        try {
            windowManager.removeView(floatingView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
