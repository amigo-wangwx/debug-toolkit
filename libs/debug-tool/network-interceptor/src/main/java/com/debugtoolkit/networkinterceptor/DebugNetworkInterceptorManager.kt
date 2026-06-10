package com.debugtoolkit.networkinterceptor

import android.util.Log
import okhttp3.OkHttpClient

object DebugNetworkInterceptorManager {
    private const val TAG = "DebugNetwork-OkHttp"

    private val baseUrlInterceptor = BaseUrlInterceptor()

    @Volatile
    private var enabled: Boolean = true

    @Volatile
    private var flowLogEnabled: Boolean = true

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        baseUrlInterceptor.setEnabled(enabled)
        controlLog("enabled=$enabled")
    }

    fun setFlowLogEnabled(enabled: Boolean) {
        val previous = flowLogEnabled
        flowLogEnabled = enabled
        baseUrlInterceptor.setFlowLogEnabled(enabled)
        controlLog("flowLogEnabled=$enabled previous=$previous")
    }

    fun setBaseUrlMappings(mappings: Map<String, String>) {
        controlLog("set base url mappings count=${mappings.size}")
        baseUrlInterceptor.setBaseUrlMappings(mappings)
    }

    fun clearBaseUrlMappings() {
        controlLog("clear base url mappings")
        baseUrlInterceptor.clearBaseUrlMappings()
    }

    /**
     * Gradle Plugin/ASM 会在 OkHttpClient.Builder.build() 前注入这个入口。
     * 这里保持幂等，避免手动接入和 ASM 同时处理同一个 Builder 时重复安装。
     */
    @JvmStatic
    fun apply(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        if (!enabled) {
            controlLog("apply skipped disabled, builder=${System.identityHashCode(builder)}")
            return builder
        }

        baseUrlInterceptor.setFlowLogEnabled(flowLogEnabled)
        if (builder.interceptors().none { it is BaseUrlInterceptor }) {
            builder.addInterceptor(baseUrlInterceptor)
            controlLog("base url interceptor installed, builder=${System.identityHashCode(builder)}")
        } else {
            controlLog("base url interceptor skipped duplicate, builder=${System.identityHashCode(builder)}")
        }
        return builder
    }

    private fun controlLog(message: String) {
        Log.d(TAG, message)
    }
}
