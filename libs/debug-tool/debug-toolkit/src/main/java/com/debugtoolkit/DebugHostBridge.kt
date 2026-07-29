package com.debugtoolkit

import android.content.Context
import android.widget.Toast
import com.debugtoolkit.networkinterceptor.DebugOperationLog

object DebugHostBridge {
    data class HostAction(
        val id: String,
        val title: String,
        val iconResId: Int = R.drawable.ic_search,
        val backgroundColor: String = "#455A64",
        val group: String = "host",
        val visible: (Context) -> Boolean = { true },
        val onClick: (Context) -> Unit
    )

    fun interface HostInfoProvider {
        fun collect(context: Context): Map<String, String>
    }

    private val actions = mutableListOf<HostAction>()
    private var infoProvider: HostInfoProvider? = null

    @Synchronized
    fun registerAction(action: HostAction) {
        actions.removeAll { it.id == action.id }
        actions.add(action)
    }

    @Synchronized
    fun unregisterAction(actionId: String) {
        actions.removeAll { it.id == actionId }
    }

    @Synchronized
    fun clearActions() {
        actions.clear()
    }

    @Synchronized
    fun setInfoProvider(provider: HostInfoProvider?) {
        infoProvider = provider
    }

    @Synchronized
    fun createDebugActions(context: Context): List<DebugAction> {
        return actions
            .filter { action -> action.visible(context) }
            .map { action ->
                DebugAction(
                    id = "host.${action.id}",
                    title = action.title,
                    iconResId = action.iconResId,
                    backgroundColor = action.backgroundColor,
                    group = action.group,
                    visible = action.visible
                ) {
                    runHostAction(context, action)
                }
            }
    }

    @Synchronized
    fun collectInfo(context: Context): Map<String, String> {
        val provider = infoProvider ?: return emptyMap()
        return runCatching { provider.collect(context) }
            .onFailure { error ->
                DebugOperationLog.record(
                    category = "host",
                    action = "collect_info",
                    message = error.message.orEmpty(),
                    success = false
                )
            }
            .getOrDefault(emptyMap())
    }

    private fun runHostAction(context: Context, action: HostAction) {
        DebugOperationLog.record(
            category = "host",
            action = action.id,
            message = "start title=${action.title}"
        )
        runCatching { action.onClick(context) }
            .onSuccess {
                DebugOperationLog.record("host", action.id, "completed", success = true)
            }
            .onFailure { error ->
                DebugOperationLog.record("host", action.id, error.message.orEmpty(), success = false)
                Toast.makeText(context, "业务调试失败: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
