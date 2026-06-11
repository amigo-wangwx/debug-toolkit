package com.debugtoolkit.networkinterceptor

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object DebugNetworkConfigManager {
    const val ACTION_EDIT_CONFIG = "com.debugtoolkit.networkinterceptor.action.EDIT_CONFIG"
    const val ACTION_RELOAD_CONFIG = "com.debugtoolkit.networkinterceptor.action.RELOAD_CONFIG"

    private const val TAG = "DebugNetwork-Config"
    private const val TEMPLATE_ASSET_NAME = "debug_network_config.json"
    private const val CONFIG_FILE_NAME = "debug_network_config.json"
    private const val LEGACY_CONFIG_FILE_NAME = "debug_network_config.txt"
    private const val DOWNLOAD_DIR = "Download"

    @Volatile
    private var config: DebugNetworkConfig = DebugNetworkConfig(1, 0, emptyList(), emptyList())

    @Volatile
    private var configFilePath: String = ""

    @Volatile
    private var lastError: String? = null

    private var appContext: Context? = null

    @Synchronized
    fun init(context: Context) {
        val applicationContext = context.applicationContext
        appContext = applicationContext

        if (!applicationContext.isDebuggable()) {
            log("skip init because application is not debuggable")
            config = DebugNetworkConfig(1, 0, emptyList(), emptyList())
            return
        }

        log("init package=${applicationContext.packageName}")
        reloadConfigFromFile()
    }

    fun getConfigFilePath(): String {
        ensureInitialized()
        return configFilePath
    }

    fun getLastError(): String? {
        ensureInitialized()
        return lastError
    }

    fun getRules(): List<DebugNetworkRule> {
        ensureInitialized()
        return config.rules
    }

    fun getSelectedRuleIds(): Set<String> {
        ensureInitialized()
        return config.selectRuleIds.toCollection(linkedSetOf())
    }

    fun setRuleSelected(ruleId: String, selected: Boolean): Boolean {
        ensureInitialized()
        val selectedRuleIds = config.selectRuleIds.toMutableList()
        if (selected) {
            if (ruleId !in selectedRuleIds) {
                selectedRuleIds.add(ruleId)
            }
        } else {
            selectedRuleIds.removeAll { it == ruleId }
        }
        return updateSelectedRuleIds(selectedRuleIds)
    }

    /**
     * 设置唯一选中的 rule，清除其他所有选中状态。
     * ruleId 为 null 时清除所有选中状态。
     * 只在调用方点击"立即应用"或"应用并重启"时使用，避免即时持久化。
     */
    fun setExclusiveSelection(ruleId: String?): Boolean {
        ensureInitialized()
        return updateSelectedRuleIds(if (ruleId != null) listOf(ruleId) else emptyList())
    }

    fun applySelectedMappings(): Map<String, String> {
        ensureInitialized()
        val mappings = buildSelectedMappings()
        DebugNetworkInterceptorManager.setBaseUrlMappings(mappings)
        log("apply selected mappings selected=${config.selectRuleIds} count=${mappings.size}")
        return mappings
    }

    fun reloadConfigFromFile(): Boolean {
        val context = appContext ?: return false
        return runCatching {
            val templateJson = readTemplateJson(context)
            val external = ExternalConfigFile(context)
            configFilePath = external.displayPath
            log("reload config start path=$configFilePath")
            val configJson = external.ensureConfigFile(templateJson)
            val mergedJson = mergeTemplateIfNeeded(configJson, templateJson)
            if (mergedJson.toString() != configJson.toString()) {
                external.writeText(mergedJson.toString(2))
                log(
                    "template merged path=$configFilePath " +
                            "templateVersion=${mergedJson.optInt("templateVersion", 0)}"
                )
            }
            config = parseConfig(mergedJson)
            lastError = null
            log(
                "config loaded path=$configFilePath templateVersion=${config.templateVersion} " +
                        "rules=${config.rules.size} selected=${config.selectRuleIds}"
            )
            true
        }.getOrElse { error ->
            lastError = error.message ?: error.javaClass.simpleName
            config = DebugNetworkConfig(1, 0, emptyList(), emptyList())
            logError("config load failed path=$configFilePath", error)
            false
        }
    }

    fun resetConfigToTemplate(): Boolean {
        val context = appContext ?: return false
        return runCatching {
            val templateJson = readTemplateJson(context)
            val external = ExternalConfigFile(context)
            configFilePath = external.displayPath
            external.writeText(templateJson.toString(2))
            config = parseConfig(templateJson)
            lastError = null
            log("config reset to template path=$configFilePath rules=${config.rules.size}")
            true
        }.getOrElse { error ->
            lastError = error.message ?: error.javaClass.simpleName
            logError("config reset failed path=$configFilePath", error)
            false
        }
    }

    fun readConfigText(): String? {
        val context = appContext ?: return null
        return runCatching {
            val templateJson = readTemplateJson(context)
            val external = ExternalConfigFile(context)
            configFilePath = external.displayPath
            val configJson = external.ensureConfigFile(templateJson)
            val text = mergeTemplateIfNeeded(configJson, templateJson).toString(2)
            lastError = null
            log("config text read path=$configFilePath length=${text.length}")
            text
        }.getOrElse { error ->
            lastError = error.message ?: error.javaClass.simpleName
            logError("config text read failed path=$configFilePath", error)
            null
        }
    }

    fun writeConfigText(text: String): Boolean {
        val context = appContext ?: return false
        return runCatching {
            val configJson = JSONObject(text)
            val parsedConfig = parseConfig(configJson)
            val external = ExternalConfigFile(context)
            configFilePath = external.displayPath
            external.writeText(configJson.toString(2))
            config = parsedConfig
            lastError = null
            log(
                "config text wrote path=$configFilePath length=${text.length} " +
                        "rules=${config.rules.size} selected=${config.selectRuleIds}"
            )
            true
        }.getOrElse { error ->
            lastError = error.message ?: error.javaClass.simpleName
            logError("config text write failed path=$configFilePath", error)
            false
        }
    }

    private fun updateSelectedRuleIds(selectedRuleIds: List<String>): Boolean {
        val validRuleIds = config.rules.map { it.id }.toSet()
        val nextSelectedRuleIds = selectedRuleIds
            .filter { it in validRuleIds }
            .distinct()
        val previousConfig = config
        config = config.copy(selectRuleIds = nextSelectedRuleIds)
        val persisted = persistSelectedRuleIds(nextSelectedRuleIds)
        if (!persisted) {
            config = previousConfig
        }
        log("selection update selected=$nextSelectedRuleIds persisted=$persisted")
        return persisted
    }

    private fun persistSelectedRuleIds(selectedRuleIds: List<String>): Boolean {
        val context = appContext ?: return false
        return runCatching {
            val templateJson = readTemplateJson(context)
            val external = ExternalConfigFile(context)
            configFilePath = external.displayPath
            val currentJson = external.readText()?.let { JSONObject(it) } ?: templateJson
            val mergedJson = mergeTemplateIfNeeded(currentJson, templateJson)
            mergedJson.put("selectRuleIds", selectedRuleIds.toJsonArray())
            external.writeText(mergedJson.toString(2))
            lastError = null
            log("selection persisted path=$configFilePath selected=$selectedRuleIds")
            true
        }.getOrElse { error ->
            lastError = error.message ?: error.javaClass.simpleName
            logError("selection persist failed path=$configFilePath selected=$selectedRuleIds", error)
            false
        }
    }

    private fun buildSelectedMappings(): Map<String, String> {
        val result = linkedMapOf<String, String>()
        val rulesById = config.rules.associateBy { it.id }
        config.selectRuleIds.forEach { ruleId ->
            rulesById[ruleId]?.mappings?.forEach { mapping ->
                // 多个 rule 命中同一个 source 时，selectRuleIds 中靠后的 rule 覆盖靠前的 rule。
                if (result.containsKey(mapping.source)) {
                    log("mapping override source=${mapping.source} ruleId=$ruleId target=${mapping.target}")
                }
                result[mapping.source] = mapping.target
            }
        }
        return result
    }

    private fun readTemplateJson(context: Context): JSONObject {
        val text = context.assets.open(TEMPLATE_ASSET_NAME).bufferedReader().use { it.readText() }
        return JSONObject(text)
    }

    private fun parseConfig(json: JSONObject): DebugNetworkConfig {
        val normalizedJson = normalizeConfigJson(json)
        val rules = normalizedJson.optJSONArray("rules").orEmpty().mapObjects { ruleJson ->
            val mappings = buildRuleMappings(normalizedJson, ruleJson)

            DebugNetworkRule(
                id = ruleJson.optString("id").trim(),
                name = resolveRuleName(normalizedJson, ruleJson),
                mappings = mappings
            )
        }.filter { it.id.isNotEmpty() }

        val ruleIds = rules.map { it.id }.toSet()
        val selectedRuleIds = normalizedJson.optJSONArray("selectRuleIds")
            .orEmpty()
            .mapStrings()
            .filter { it in ruleIds }
            .distinct()

        return DebugNetworkConfig(
            version = normalizedJson.optInt("version", 1),
            templateVersion = normalizedJson.optInt("templateVersion", 0),
            selectRuleIds = selectedRuleIds,
            rules = rules
        )
    }

    private fun mergeTemplateIfNeeded(configJson: JSONObject, templateJson: JSONObject): JSONObject {
        val normalizedJson = normalizeConfigJson(configJson)
        val templateVersion = templateJson.optInt("templateVersion", 0)
        if (normalizedJson.optInt("templateVersion", 0) >= templateVersion) {
            return normalizedJson
        }

        log(
            "template merge required current=${normalizedJson.optInt("templateVersion", 0)} " +
                    "template=$templateVersion"
        )
        val merged = JSONObject(normalizedJson.toString())
        merged.put("version", templateJson.optInt("version", merged.optInt("version", 1)))
        merged.put("templateVersion", templateVersion)
        merged.put(
            "rules",
            mergeRules(
                merged.optJSONArray("rules").orEmpty(),
                templateJson.optJSONArray("rules").orEmpty()
            )
        )
        merged.put(
            "environments",
            mergeEnvironments(
                merged.optJSONObject("environments"),
                templateJson.optJSONObject("environments")
            )
        )
        if (!merged.has("selectRuleIds")) {
            merged.put("selectRuleIds", JSONArray())
        }
        return merged
    }

    private fun normalizeConfigJson(json: JSONObject): JSONObject {
        val normalizedJson = JSONObject(json.toString())
        val legacyGroups = normalizedJson.optJSONArray("groups")
        if (legacyGroups != null) {
            log("legacy groups detected, migrate count=${legacyGroups.length()}")
            if (!normalizedJson.has("rules")) {
                normalizedJson.put("rules", flattenLegacyRules(legacyGroups))
            }
            if (!normalizedJson.has("selectRuleIds")) {
                normalizedJson.put("selectRuleIds", inferLegacySelectedRuleIds(legacyGroups))
            }
            normalizedJson.remove("groups")
        }
        if (!normalizedJson.has("rules")) {
            normalizedJson.put("rules", JSONArray())
        }
        if (!normalizedJson.has("environments")) {
            normalizedJson.put("environments", JSONObject())
        }
        if (!normalizedJson.has("selectRuleIds")) {
            normalizedJson.put("selectRuleIds", JSONArray())
        }
        return normalizedJson
    }

    private fun buildRuleMappings(configJson: JSONObject, ruleJson: JSONObject): List<DebugNetworkMapping> {
        val mappingsBySource = linkedMapOf<String, DebugNetworkMapping>()
        buildEnvMappings(configJson, ruleJson).forEach { mapping ->
            mappingsBySource[mapping.source] = mapping
        }
        parseExplicitMappings(ruleJson).forEach { mapping ->
            // 显式 mappings 用来处理单个接口的特殊覆盖，优先级高于 env 自动展开结果。
            mappingsBySource[mapping.source] = mapping
        }
        return mappingsBySource.values.toList()
    }

    private fun buildEnvMappings(configJson: JSONObject, ruleJson: JSONObject): List<DebugNetworkMapping> {
        val envId = ruleJson.optString("env").trim()
        if (envId.isEmpty()) return emptyList()

        val environments = configJson.optJSONObject("environments") ?: return emptyList()
        val targetEnv = environments.optJSONObject(envId)
        if (targetEnv == null) {
            log("rule env not found ruleId=${ruleJson.optString("id")} env=$envId")
            return emptyList()
        }

        val result = mutableListOf<DebugNetworkMapping>()
        targetEnv.forEachKey { serviceKey ->
            if (serviceKey == "name") return@forEachKey
            if (targetEnv.opt(serviceKey) !is String) return@forEachKey

            val target = targetEnv.optString(serviceKey).trim()
            if (target.isEmpty()) {
                log("empty target env=$envId service=$serviceKey")
                return@forEachKey
            }

            environments.forEachObjectValue { sourceEnvId, sourceEnv ->
                val source = sourceEnv.optString(serviceKey).trim()
                if (source.isEmpty()) {
                    log("missing source env=$sourceEnvId service=$serviceKey targetEnv=$envId")
                    return@forEachObjectValue
                }
                result.add(DebugNetworkMapping(source = source, target = target))
            }
        }
        return result
    }

    private fun parseExplicitMappings(ruleJson: JSONObject): List<DebugNetworkMapping> {
        return ruleJson.optJSONArray("mappings").orEmpty().mapObjects { mappingJson ->
            DebugNetworkMapping(
                source = mappingJson.optString("source").trim(),
                target = mappingJson.optString("target").trim()
            )
        }.filter { it.source.isNotEmpty() && it.target.isNotEmpty() }
    }

    private fun resolveRuleName(configJson: JSONObject, ruleJson: JSONObject): String {
        val explicitName = ruleJson.optString("name").trim()
        if (explicitName.isNotEmpty()) return explicitName

        val envId = ruleJson.optString("env").trim()
        val envName = configJson.optJSONObject("environments")
            ?.optJSONObject(envId)
            ?.optString("name")
            .orEmpty()
            .trim()
        return envName.ifEmpty { ruleJson.optString("id").trim() }
    }

    private fun flattenLegacyRules(groups: JSONArray): JSONArray {
        val result = JSONArray()
        val seenRuleIds = mutableSetOf<String>()
        groups.forEachObject { groupJson ->
            groupJson.optJSONArray("rules").orEmpty().forEachObject { ruleJson ->
                val ruleId = ruleJson.optString("id").trim()
                if (ruleId.isNotEmpty() && seenRuleIds.add(ruleId)) {
                    result.put(JSONObject(ruleJson.toString()))
                }
            }
        }
        return result
    }

    private fun inferLegacySelectedRuleIds(groups: JSONArray): JSONArray {
        val result = JSONArray()
        groups.forEachObject { groupJson ->
            val rules = groupJson.optJSONArray("rules").orEmpty()
            if (groupJson.optString("selectMode", "single").lowercase() == "multiple") {
                rules.forEachObject { ruleJson ->
                    if (ruleJson.optBoolean("defaultChecked", false)) {
                        val ruleId = ruleJson.optString("id").trim()
                        if (ruleId.isNotEmpty()) result.put(ruleId)
                    }
                }
            } else {
                val defaultRuleId = groupJson.optString("defaultRuleId").trim()
                    .ifBlank { rules.optJSONObject(0)?.optString("id").orEmpty().trim() }
                if (defaultRuleId.isNotEmpty()) result.put(defaultRuleId)
            }
        }
        return result
    }

    private fun mergeRules(currentRules: JSONArray, templateRules: JSONArray): JSONArray {
        val result = JSONArray(currentRules.toString())
        templateRules.forEachObject { templateRule ->
            val ruleId = templateRule.optString("id")
            val currentRule = result.findObjectById(ruleId)
            if (currentRule == null) {
                result.put(JSONObject(templateRule.toString()))
                return@forEachObject
            }

            copyIfMissing(currentRule, templateRule, "name")
            copyIfMissing(currentRule, templateRule, "env")
            // mappings 是调试人员最可能改成真实域名的字段，模板升级只在完全缺失时补齐，避免覆盖手工配置。
            copyIfMissing(currentRule, templateRule, "mappings")
        }
        return result
    }

    private fun mergeEnvironments(currentEnvironments: JSONObject?, templateEnvironments: JSONObject?): JSONObject {
        val result = JSONObject(currentEnvironments?.toString() ?: "{}")
        templateEnvironments?.forEachObjectValue { envId, templateEnv ->
            val currentEnv = result.optJSONObject(envId)
            if (currentEnv == null) {
                result.put(envId, JSONObject(templateEnv.toString()))
                return@forEachObjectValue
            }

            templateEnv.forEachKey { key ->
                copyIfMissing(currentEnv, templateEnv, key)
            }
        }
        return result
    }

    private fun copyIfMissing(target: JSONObject, source: JSONObject, key: String) {
        if (!target.has(key) && source.has(key)) {
            target.put(key, source.get(key))
        }
    }

    private fun JSONArray?.orEmpty(): JSONArray = this ?: JSONArray()

    private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> {
        val result = mutableListOf<T>()
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            result.add(transform(item))
        }
        return result
    }

    private fun JSONArray.mapStrings(): List<String> {
        val result = mutableListOf<String>()
        for (index in 0 until length()) {
            val item = optString(index).trim()
            if (item.isNotEmpty()) result.add(item)
        }
        return result
    }

    private inline fun JSONArray.forEachObject(action: (JSONObject) -> Unit) {
        for (index in 0 until length()) {
            action(optJSONObject(index) ?: continue)
        }
    }

    private fun JSONArray.findObjectById(id: String): JSONObject? {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            if (item.optString("id") == id) return item
        }
        return null
    }

    private inline fun JSONObject.forEachKey(action: (String) -> Unit) {
        val iterator = keys()
        while (iterator.hasNext()) {
            action(iterator.next())
        }
    }

    private inline fun JSONObject.forEachObjectValue(action: (String, JSONObject) -> Unit) {
        forEachKey { key ->
            val value = optJSONObject(key) ?: return@forEachKey
            action(key, value)
        }
    }

    private fun List<String>.toJsonArray(): JSONArray {
        val result = JSONArray()
        forEach { result.put(it) }
        return result
    }

    private class ExternalConfigFile(private val context: Context) {
        private val appName = context.readableAppName()
        private val relativePath = "$DOWNLOAD_DIR/$appName/"

        val displayPath: String = "$relativePath$CONFIG_FILE_NAME"

        fun ensureConfigFile(templateJson: JSONObject): JSONObject {
            val currentText = readText()
            if (currentText != null) {
                Log.d(TAG, "config file found path=$displayPath length=${currentText.length}")
                return JSONObject(currentText)
            }

            val legacyText = readLegacyText()
            if (legacyText != null) {
                writeText(legacyText)
                Log.d(
                    TAG,
                    "legacy config migrated from $LEGACY_CONFIG_FILE_NAME to $CONFIG_FILE_NAME " +
                            "path=$displayPath length=${legacyText.length}"
                )
                return JSONObject(legacyText)
            }

            val templateText = templateJson.toString(2)
            writeText(templateText)
            Log.d(TAG, "config file created from template path=$displayPath length=${templateText.length}")
            return JSONObject(templateText)
        }

        fun readText(): String? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                readTextFromMediaStore(CONFIG_FILE_NAME)
            } else {
                legacyFile(CONFIG_FILE_NAME).takeIf { it.exists() }?.readText()
            }
        }

        fun readLegacyText(): String? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                readTextFromMediaStore(LEGACY_CONFIG_FILE_NAME)
            } else {
                legacyFile(LEGACY_CONFIG_FILE_NAME).takeIf { it.exists() }?.readText()
            }
        }

        fun writeText(text: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                writeTextToMediaStore(text)
            } else {
                val file = legacyFile(CONFIG_FILE_NAME)
                file.parentFile?.mkdirs()
                file.writeText(text)
            }
        }

        private fun readTextFromMediaStore(displayName: String): String? {
            val uri = findMediaStoreUri(displayName) ?: return null
            return context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }

        private fun writeTextToMediaStore(text: String) {
            val uri = findMediaStoreUri(CONFIG_FILE_NAME) ?: createMediaStoreUri()
            context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                output.write(text.toByteArray(Charsets.UTF_8))
            }
        }

        private fun findMediaStoreUri(displayName: String): Uri? {
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.Downloads._ID)
            val selection = "${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.RELATIVE_PATH}=?"
            val args = arrayOf(displayName, relativePath)
            context.contentResolver.query(collection, projection, selection, args, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                return ContentUris.withAppendedId(collection, id)
            }
            return null
        }

        private fun createMediaStoreUri(): Uri {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, CONFIG_FILE_NAME)
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
            }
            return context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Failed to create $displayPath")
        }

        private fun legacyFile(fileName: String): File {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            return File(File(downloads, appName), fileName)
        }

        private fun Context.readableAppName(): String {
            val label = runCatching {
                packageManager.getApplicationLabel(applicationInfo).toString()
            }.getOrDefault(packageName)
            return label.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { packageName }
        }
    }

    private fun ensureInitialized() {
        if (appContext != null) return
        log("DebugNetworkConfigManager is not initialized")
    }

    private fun Context.isDebuggable(): Boolean {
        return applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    private fun log(message: String) {
        Log.d(TAG, message)
    }

    private fun logError(message: String, error: Throwable) {
        Log.e(TAG, "$message: ${error.message ?: error.javaClass.simpleName}", error)
    }
}
