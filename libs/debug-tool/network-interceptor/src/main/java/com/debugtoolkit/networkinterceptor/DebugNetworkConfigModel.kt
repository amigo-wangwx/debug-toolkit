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

data class DebugNetworkRewritePreview(
    val inputUrl: String,
    val ruleId: String?,
    val ruleName: String? = null,
    val hit: Boolean,
    val source: String? = null,
    val target: String? = null,
    val rewrittenUrl: String? = null,
    val reason: String = ""
)
