package com.debugtoolkit

interface DebugModule {
    val id: String
    val title: String
    fun createActions(): List<DebugAction>
}

class SimpleDebugModule(
    override val id: String,
    override val title: String,
    private val actionsProvider: () -> List<DebugAction>
) : DebugModule {
    override fun createActions(): List<DebugAction> = actionsProvider()
}

object DebugModuleRegistry {
    private val modules = mutableListOf<DebugModule>()

    @Synchronized
    fun register(module: DebugModule) {
        modules.removeAll { it.id == module.id }
        modules.add(module)
    }

    @Synchronized
    fun unregister(moduleId: String) {
        modules.removeAll { it.id == moduleId }
    }

    @Synchronized
    fun getModules(): List<DebugModule> = modules.toList()
}
