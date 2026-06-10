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
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.debugtoolkit.networkinterceptor.DebugNetworkConfigPanel
import kotlin.system.exitProcess

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
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startMyForegroundService()

        // 获取并保存宿主App图标
        getHostAppIcon()

        // 使用 Handler 延迟检查，确保服务初始化完成
        Handler(Looper.getMainLooper()).post {
            if (Settings.canDrawOverlays(this)) {
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

    @RequiresApi(Build.VERSION_CODES.R)
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

    @RequiresApi(Build.VERSION_CODES.R)
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

    @RequiresApi(Build.VERSION_CODES.R)
    private fun snapToEdge(view: View) {
        val windowMetrics = windowManager.currentWindowMetrics
        val bounds = windowMetrics.bounds
        val screenWidth = bounds.width()

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

        val targetY = currentY.coerceAtLeast(0f).coerceAtMost(bounds.height().toFloat() - viewHeight)

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
     * 1. 直接 startActivity(CLEAR_TASK) 拉起新的启动页
     * 2. 延迟 300ms 后 killProcess 杀掉旧进程
     *
     * 不使用 AlarmManager，避免精确闹钟权限问题
     */
    private fun restartApp() {
        try {
            // 先移除悬浮窗
            try { windowManager.removeView(floatingView) } catch (_: Exception) {}

            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent == null) {
                Toast.makeText(this, "无法获取启动Intent", Toast.LENGTH_SHORT).show()
                return
            }
            // CLEAR_TASK: 销毁所有现有 Activity 栈
            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
            startActivity(launchIntent)

            Toast.makeText(this, "重启中，请稍后", Toast.LENGTH_LONG).show()

            // 延迟杀进程，确保新 Activity 已创建
            // Handler(Looper.getMainLooper()).postDelayed({
                android.os.Process.killProcess(android.os.Process.myPid())
            // }, 1000)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "重启失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ==================== 清除 MMKV ====================

    private fun clearMMKVData() {
        val success = DebugConfig.clearMMKVData()
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
            Toast.makeText(this, "归因Mock: $methodName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "归因Mock失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        toggleMenu()
    }

    private fun resetAttributionFromApp() {
        try {
            val method = application.javaClass.getMethod("debugResetAttribution")
            method.invoke(application)
            Toast.makeText(this, "归因状态已重置", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "重置失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        toggleMenu()
    }

    // ==================== UI 权限与通知 ====================

    private fun requestOverlayPermission() {
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

        // ==================== 网格按钮（含重启 + 清MMKV） ====================
        val buttonItems = listOf(
            // 第一行
            ButtonItem("日志", R.drawable.ic_search, "#FF5722") {
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
            ButtonItem("权限", R.drawable.ic_search, "#2196F3") {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                toggleMenu()
            },
            ButtonItem("开发者", R.drawable.ic_search, "#4CAF50") {
                val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                toggleMenu()
            },
            // 第二行
            ButtonItem("主页", R.drawable.ic_search, "#9C27B0") {
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
            ButtonItem("WebView", R.drawable.ic_search, "#FF9800") {
                val intent = Intent(this, DebugWebViewActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                toggleMenu()
            },
            ButtonItem("关闭", R.drawable.ic_search, "#F44336") {
                android.os.Process.killProcess(android.os.Process.myPid())
                toggleMenu()
            },
            // 第三行（新增）
            ButtonItem("重启", R.drawable.ic_search, "#009688") {
                restartApp()
            },
            ButtonItem("清MMKV", R.drawable.ic_search, "#795548") {
                clearMMKVData()
            },
            ButtonItem("网络", R.drawable.ic_search, "#607D8B") {
                DebugNetworkConfigPanel.show(this) { restartApp() }
            },
            // 第四行（归因测试 - 模拟真实 AF/HTM 归因回调链路）
            ButtonItem("AF→短剧", R.drawable.ic_search, "#E91E63") {
                mockAttributionFromApp("debugMockAFAttribution", "applovin_drama_123_0__debug__001")
            },
            ButtonItem("AF→小说", R.drawable.ic_search, "#3F51B5") {
                mockAttributionFromApp("debugMockAFAttribution", "applovin_novel_456_0__debug__001")
            },
            ButtonItem("HTM→短剧", R.drawable.ic_search, "#009688") {
                mockAttributionFromApp("debugMockHTMAttribution", "funshorts://navigator/video/player/123/0")
            },
            // 第五行（归因测试续）
            ButtonItem("HTM→小说", R.drawable.ic_search, "#795548") {
                mockAttributionFromApp("debugMockHTMAttribution", "funshorts://navigator/novel/read/456/0")
            },
            ButtonItem("重置归因", R.drawable.ic_search, "#FF9800") {
                resetAttributionFromApp()
            }
        )

        // 设置网格适配器
        val gridAdapter = DebugButtonAdapter(this, buttonItems)
        gridViewButtons.adapter = gridAdapter

        btnOpenUri.setOnClickListener {
            val uriString = etUri.text.toString().trim()
            if (uriString.isNotEmpty()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    toggleMenu()
                    // 打开链接后，恢复不获取焦点状态
                    updateWindowFocus(false)
                } catch (e: Exception) {
                    Toast.makeText(this, "无效的 URI: $uriString", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "请输入 URI", Toast.LENGTH_SHORT).show()
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

    // 按钮数据类
    data class ButtonItem(
        val text: String,
        val iconResId: Int,
        val backgroundColor: String,
        val onClick: () -> Unit
    )
}
