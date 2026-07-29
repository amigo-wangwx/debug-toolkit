package com.debugtoolkit.networkinterceptor

import org.json.JSONArray
import org.json.JSONObject

object DebugNetworkConfigValidator {
    data class ValidationError(
        val path: String,
        val message: String,
        val value: String = ""
    ) {
        fun toDisplayText(): String {
            val valueText = value.takeIf { it.isNotBlank() }?.let { ": $it" }.orEmpty()
            return "$path: $message$valueText"
        }
    }

    data class Result(val errors: List<ValidationError>) {
        val isValid: Boolean = errors.isEmpty()
        val firstError: String = errors.firstOrNull()?.toDisplayText().orEmpty()
        val firstErrorPath: String = errors.firstOrNull()?.path.orEmpty()
        val summary: String = errors.joinToString(separator = "\n") { it.toDisplayText() }
    }

    fun validate(json: JSONObject): Result {
        val errors = mutableListOf<ValidationError>()
        val environments = validateEnvironments(json, errors)
        val ruleIds = validateRules(json, environments, errors)
        validateSelectRuleIds(json, ruleIds, errors)
        return Result(errors)
    }

    private fun validateEnvironments(json: JSONObject, errors: MutableList<ValidationError>): Set<String> {
        val environments = json.optJSONObject("environments")
        if (environments == null) {
            errors.add(ValidationError("environments", "必须是对象"))
            return emptySet()
        }

        val envIds = linkedSetOf<String>()
        environments.forEachKey { envId ->
            envIds.add(envId)
            val envJson = environments.optJSONObject(envId)
            if (envJson == null) {
                errors.add(ValidationError("environments.$envId", "必须是对象"))
                return@forEachKey
            }
            envJson.forEachKey { serviceKey ->
                if (serviceKey == "name") return@forEachKey
                val path = "environments.$envId.$serviceKey"
                val value = envJson.opt(serviceKey)
                if (value !is String || value.trim().isEmpty()) {
                    errors.add(ValidationError(path, "必须是非空 baseUrl 字符串"))
                    return@forEachKey
                }
                if (DebugNetworkBaseUrlParser.parse(value) == null) {
                    errors.add(ValidationError(path, "不是合法 baseUrl", value))
                }
            }
        }
        return envIds
    }

    private fun validateRules(
        json: JSONObject,
        envIds: Set<String>,
        errors: MutableList<ValidationError>
    ): Set<String> {
        val rules = json.optJSONArray("rules")
        if (rules == null) {
            errors.add(ValidationError("rules", "必须是数组"))
            return emptySet()
        }

        val ruleIds = linkedSetOf<String>()
        val duplicatedRuleIds = linkedSetOf<String>()
        for (index in 0 until rules.length()) {
            val ruleJson = rules.optJSONObject(index)
            if (ruleJson == null) {
                errors.add(ValidationError("rules[$index]", "必须是对象"))
                continue
            }

            val ruleId = ruleJson.optString("id").trim()
            if (ruleId.isEmpty()) {
                errors.add(ValidationError("rules[$index].id", "不能为空"))
            } else if (!ruleIds.add(ruleId)) {
                duplicatedRuleIds.add(ruleId)
            }

            val envId = ruleJson.optString("env").trim()
            if (envId.isNotEmpty() && envId !in envIds) {
                errors.add(ValidationError("rules[$index].env", "引用了不存在的环境", envId))
            }

            validateMappings(index, ruleJson.optJSONArray("mappings"), errors)
        }

        duplicatedRuleIds.forEach { ruleId ->
            errors.add(ValidationError("rules.id", "rule id 重复", ruleId))
        }
        return ruleIds
    }

    private fun validateMappings(
        ruleIndex: Int,
        mappings: JSONArray?,
        errors: MutableList<ValidationError>
    ) {
        if (mappings == null) return
        for (index in 0 until mappings.length()) {
            val mappingJson = mappings.optJSONObject(index)
            if (mappingJson == null) {
                errors.add(ValidationError("rules[$ruleIndex].mappings[$index]", "必须是对象"))
                continue
            }

            val source = mappingJson.optString("source").trim()
            val target = mappingJson.optString("target").trim()
            val sourcePath = "rules[$ruleIndex].mappings[$index].source"
            val targetPath = "rules[$ruleIndex].mappings[$index].target"
            if (source.isEmpty()) {
                errors.add(ValidationError(sourcePath, "不能为空"))
            } else if (DebugNetworkBaseUrlParser.parse(source) == null) {
                errors.add(ValidationError(sourcePath, "不是合法 baseUrl", source))
            }

            if (target.isEmpty()) {
                errors.add(ValidationError(targetPath, "不能为空"))
            } else if (DebugNetworkBaseUrlParser.parse(target) == null) {
                errors.add(ValidationError(targetPath, "不是合法 baseUrl", target))
            }
        }
    }

    private fun validateSelectRuleIds(
        json: JSONObject,
        ruleIds: Set<String>,
        errors: MutableList<ValidationError>
    ) {
        val selected = json.optJSONArray("selectRuleIds")
        if (selected == null) {
            errors.add(ValidationError("selectRuleIds", "必须是数组"))
            return
        }

        for (index in 0 until selected.length()) {
            val ruleId = selected.optString(index).trim()
            val path = "selectRuleIds[$index]"
            if (ruleId.isEmpty()) {
                errors.add(ValidationError(path, "不能为空"))
            } else if (ruleId !in ruleIds) {
                errors.add(ValidationError(path, "引用了不存在的 rule", ruleId))
            }
        }
    }

    private inline fun JSONObject.forEachKey(action: (String) -> Unit) {
        val iterator = keys()
        while (iterator.hasNext()) {
            action(iterator.next())
        }
    }
}
