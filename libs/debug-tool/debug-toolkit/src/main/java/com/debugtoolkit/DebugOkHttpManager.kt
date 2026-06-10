package com.debugtoolkit

import android.util.Log
import com.debugtoolkit.networkinterceptor.DebugNetworkInterceptorManager
import okhttp3.OkHttpClient

object DebugOkHttpManager {
    private const val TAG = "DebugOkHttpManager"

    interface OnConfigChangeListener {
        fun onConfigChanged(newBuilder: OkHttpClient.Builder)
    }

    private var configChangeListener: OnConfigChangeListener? = null

    fun setBaseUrlMappings(mappings: Map<String, String>) {
        Log.d(TAG, "set base url mappings count=${mappings.size}")
        DebugNetworkInterceptorManager.setBaseUrlMappings(mappings)
        applyNewConfig()
    }

    fun clearBaseUrlMappings() {
        Log.d(TAG, "clear base url mappings")
        DebugNetworkInterceptorManager.clearBaseUrlMappings()
        applyNewConfig()
    }

    fun setFlowLogEnabled(enabled: Boolean) {
        Log.d(TAG, "set flow log enabled=$enabled")
        DebugNetworkInterceptorManager.setFlowLogEnabled(enabled)
    }

    // 获取OkHttpClient.Builder（供业务模块使用）
    fun getBuilder(): OkHttpClient.Builder {
        val builder = OkHttpClient.Builder()
        apply(builder)
        return builder
    }

    /**
     * Gradle Plugin/ASM 会在 OkHttpClient.Builder.build() 前注入这个入口。
     * 这里必须保持幂等：同一个 Builder 可能被手动接入和 ASM 同时处理，重复添加会导致请求被改写/打印多次。
     */
    @JvmStatic
    fun apply(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        syncDebugConfig()
        return DebugNetworkInterceptorManager.apply(builder)
    }

    // 设置配置变更监听（用于业务模块响应）
    fun setOnConfigChangeListener(listener: OnConfigChangeListener) {
        this.configChangeListener = listener
        Log.d(TAG, "config change listener set=${listener.javaClass.name}")
    }

    // 应用新配置（触发监听）
    fun applyNewConfig() {
        syncDebugConfig()
        val listener = configChangeListener
        Log.d(TAG, "apply new config listener=${listener != null}")
        listener?.onConfigChanged(getBuilder())
    }

    private fun syncDebugConfig() {
        val interceptorEnabled = DebugConfig.isInterceptorEnabled
        val logEnabled = DebugConfig.isLogEnabled
        Log.d(TAG, "sync debug config interceptor=$interceptorEnabled flowLog=$logEnabled")
        DebugNetworkInterceptorManager.setEnabled(interceptorEnabled)
        DebugNetworkInterceptorManager.setFlowLogEnabled(logEnabled)
    }
}
