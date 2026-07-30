package com.debugtoolkit

import android.content.Context
import android.os.SystemClock
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

    /**
     * 动态业务调试按钮提供方。
     *
     * 调用时机：debug-toolkit 每次生成浮窗菜单按钮时调用；实现方可以读取内存状态决定当前展示哪些按钮。
     * 约束：这里只构造按钮，不应执行耗时任务；打开页面、SDK 调试器等副作用应放在 HostAction.onClick 中。
     */
    fun interface HostActionProvider {
        fun createActions(context: Context): List<HostAction>
    }

    fun interface HostInfoProvider {
        fun collect(context: Context): Map<String, String>
    }

    private val actions = mutableListOf<HostAction>()
    private val actionProviders = linkedMapOf<String, HostActionProvider>()
    private var infoProvider: HostInfoProvider? = null

    @Synchronized
    fun registerAction(action: HostAction) {
        actions.removeAll { it.id == action.id }
        actions.add(action)
    }

    @Synchronized
    fun registerActionProvider(providerId: String, provider: HostActionProvider) {
        actionProviders[providerId] = provider
    }

    @Synchronized
    fun unregisterAction(actionId: String) {
        actions.removeAll { it.id == actionId }
    }

    @Synchronized
    fun unregisterActionProvider(providerId: String) {
        actionProviders.remove(providerId)
    }

    @Synchronized
    fun clearActions() {
        actions.clear()
    }

    @Synchronized
    fun clearActionProviders() {
        actionProviders.clear()
    }

    @Synchronized
    fun setInfoProvider(provider: HostInfoProvider?) {
        infoProvider = provider
    }

    fun createDebugActions(context: Context): List<DebugAction> {
        val staticActions: List<HostAction>
        val providers: List<Pair<String, HostActionProvider>>
        synchronized(this) {
            staticActions = actions.toList()
            providers = actionProviders.entries.map { entry -> entry.key to entry.value }
        }

        return (staticActions + createProviderActions(context, providers))
            .distinctBy { action -> action.id }
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

    private fun createProviderActions(
        context: Context,
        providers: List<Pair<String, HostActionProvider>>
    ): List<HostAction> {
        return providers.flatMap { (providerId, provider) ->
            val startMillis = SystemClock.elapsedRealtime()
            runCatching { provider.createActions(context) }
                .onSuccess {
                    val costMillis = SystemClock.elapsedRealtime() - startMillis
                    if (costMillis > SLOW_PROVIDER_THRESHOLD_MILLIS) {
                        DebugOperationLog.record(
                            category = "host",
                            action = "create_action_provider",
                            message = "provider=$providerId cost=${costMillis}ms",
                            success = true
                        )
                    }
                }
                .onFailure { error ->
                    DebugOperationLog.record(
                        category = "host",
                        action = "create_action_provider",
                        message = "provider=$providerId ${error.message.orEmpty()}",
                        success = false
                    )
                }
                .getOrDefault(emptyList())
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

    private const val SLOW_PROVIDER_THRESHOLD_MILLIS = 50L
}
