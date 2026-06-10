package com.debugtoolkit.networkinterceptor

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.ApplicationInfo
import android.database.Cursor
import android.net.Uri
import android.util.Log

/**
 * Debug variant only.
 *
 * 这个 Provider 的职责不是提供数据，而是让网络拦截框架在 App 进程创建早期完成初始化：
 * 1. 把内置 JSON 配置模板复制到 Download/<AppName>/debug_network_config.json（仅文件不存在时）。
 * 2. 读取外部配置中的 selectRuleIds。
 * 3. 把 selectRuleIds 命中的 rule mappings 下发给 BaseUrlInterceptor。
 *
 * Provider 放在 src/debug，release 变体不会合入；这里仍保留 debuggable 判断，是为了防止宿主误用
 * implementation 引入 debug 库时，release 包里即使出现这个类也不会执行配置文件写入和网络改写。
 */
class DebugNetworkInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        val context = context ?: return false
        val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (!debuggable) {
            Log.d(TAG, "skip init because application is not debuggable")
            return false
        }

        Log.d(TAG, "init provider start package=${context.packageName}")
        DebugNetworkConfigManager.init(context)
        val mappings = DebugNetworkConfigManager.applySelectedMappings()
        Log.d(
            TAG,
            "init provider ready path=${DebugNetworkConfigManager.getConfigFilePath()} " +
                    "mappings=${mappings.size}"
        )
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0

    private companion object {
        private const val TAG = "DebugNetwork-Init"
    }
}
