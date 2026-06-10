package com.debugtoolkit.networkinterceptor

data class DebugNetworkMapping(
    val source: String,
    val target: String
)

data class DebugNetworkRule(
    val id: String,
    val name: String,
    val mappings: List<DebugNetworkMapping>
)

data class DebugNetworkConfig(
    val version: Int,
    val templateVersion: Int,
    val selectRuleIds: List<String>,
    val rules: List<DebugNetworkRule>
)
