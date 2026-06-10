package com.debugtoolkit

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.tencent.mmkv.MMKV
import java.io.File

object DebugConfig {
    const val TAG = "DebugConfig"

    private const val PREFS_NAME = "debug_toolkit_prefs"
    private const val KEY_SELECTED_HOST = "key_selected_host"
    private const val KEY_INTERCEPTOR_ENABLED = "key_interceptor_enabled"
    private const val KEY_LOG_ENABLED = "key_log_enabled"
    private const val PRODUCTION_ENV_NAME = "生产环境"

    // ==================== 悬浮窗位置持久化 ====================
    private const val KEY_LAST_FLOAT_X = "key_last_float_x"
    private const val KEY_LAST_FLOAT_Y = "key_last_float_y"
    private const val KEY_HAS_SAVED_POSITION = "key_has_saved_position"

    private var prefs: SharedPreferences? = null

    // TOOD 等待后面使用 Plugin 注入 实现
    // 环境配置（可扩展）
    val ENVIRONMENTS = mapOf(
        "开发环境" to "http://dev.example.com",
        "测试环境" to "http://test.example.com",
        "生产环境" to "http://prod.example.com"
    )

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // 当前选中环境
    var selectedHost: String
        get() = prefs?.getString(KEY_SELECTED_HOST, ENVIRONMENTS.values.first()) ?: ENVIRONMENTS.values.first()
        set(value) {
            prefs?.edit()?.putString(KEY_SELECTED_HOST, value)?.apply()
        }

    // 拦截器开关
    var isInterceptorEnabled: Boolean
        get() = prefs?.getBoolean(KEY_INTERCEPTOR_ENABLED, true) ?: true
        set(value) {
            prefs?.edit()?.putBoolean(KEY_INTERCEPTOR_ENABLED, value)?.apply()
        }

    // 日志开关
    var isLogEnabled: Boolean
        get() = prefs?.getBoolean(KEY_LOG_ENABLED, true) ?: true
        set(value) {
            prefs?.edit()?.putBoolean(KEY_LOG_ENABLED, value)?.apply()
        }

    // 兼容旧环境配置，网络映射入口已迁移到底层网络拦截配置面板。
    fun selectedHostEnvName(): String {
        return ENVIRONMENTS.entries.firstOrNull { it.value == selectedHost }?.key ?: "未知环境"
    }

    fun selectedBaseUrlMappings(): Map<String, String> {
        val sourceBaseUrl = ENVIRONMENTS[PRODUCTION_ENV_NAME] ?: return emptyMap()
        val targetBaseUrl = selectedHost
        // 浮窗环境切换沿用旧逻辑：以生产环境作为 source，只把命中的生产 baseUrl 改到当前目标环境。
        if (sourceBaseUrl == targetBaseUrl) return emptyMap()
        return mapOf(sourceBaseUrl to targetBaseUrl)
    }

    // ==================== 悬浮窗位置持久化 ====================

    fun saveFloatPosition(x: Int, y: Int) {
        prefs?.edit()
            ?.putInt(KEY_LAST_FLOAT_X, x)
            ?.putInt(KEY_LAST_FLOAT_Y, y)
            ?.putBoolean(KEY_HAS_SAVED_POSITION, true)
            ?.apply()
    }

    fun getLastFloatX(): Int? {
        val currentPrefs = prefs ?: return null
        return if (currentPrefs.getBoolean(KEY_HAS_SAVED_POSITION, false)) {
            currentPrefs.getInt(KEY_LAST_FLOAT_X, 0)
        } else null
    }

    fun getLastFloatY(): Int? {
        val currentPrefs = prefs ?: return null
        return if (currentPrefs.getBoolean(KEY_HAS_SAVED_POSITION, false)) {
            currentPrefs.getInt(KEY_LAST_FLOAT_Y, 200)
        } else null
    }

    fun hasSavedPosition(): Boolean {
        return prefs?.getBoolean(KEY_HAS_SAVED_POSITION, false) ?: false
    }

        // ==================== 清除 MMKV 数据 ====================


        /**
         * 清除所有 MMKV 数据（直接调用 API，无需反射）
         */
        fun clearMMKVData(): Boolean {
            return try {
                // 1. 清除默认实例
                MMKV.defaultMMKV().clearAll()

                // 2. 遍历 mmkv 目录，清除所有自定义 mmapID 的实例
                val mmkvDir = File(MMKV.getRootDir())
                if (mmkvDir.exists() && mmkvDir.isDirectory) {
                    mmkvDir.listFiles()
                        ?.filter { it.name.endsWith(".crc") }
                        ?.forEach { crcFile ->
                            // 文件名格式: {mmapID}.crc
                            val mmapID = crcFile.name.removeSuffix(".crc")
                            try {
                                MMKV.mmkvWithID(mmapID).clearAll()
                            } catch (e: Exception) {
                                Log.d(TAG, "clearMMKVData: ${e.message }")
                                false
                            }
                        }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

}
