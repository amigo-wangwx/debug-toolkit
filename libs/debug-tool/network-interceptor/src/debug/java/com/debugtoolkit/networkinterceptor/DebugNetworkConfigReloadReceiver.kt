package com.debugtoolkit.networkinterceptor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.util.Log

class DebugNetworkConfigReloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != DebugNetworkConfigManager.ACTION_RELOAD_CONFIG) {
            Log.d(TAG, "ignore action=${intent?.action}")
            return
        }
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) {
            Log.d(TAG, "skip reload because application is not debuggable")
            return
        }

        Log.d(TAG, "reload broadcast received")
        DebugNetworkConfigManager.init(context)
        val success = DebugNetworkConfigManager.reloadConfigFromFile()
        val mappings = DebugNetworkConfigManager.applySelectedMappings()
        Log.d(
            TAG,
            "reload after edit success=$success mappings=${mappings.size} " +
                    "selected=${DebugNetworkConfigManager.getSelectedRuleIds()}"
        )
    }

    private companion object {
        private const val TAG = "DebugNetwork-Reload"
    }
}
