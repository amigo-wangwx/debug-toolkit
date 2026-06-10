package com.debugtoolkit.networkinterceptor

import android.util.Log
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

class BaseUrlInterceptor : Interceptor {
    @Volatile
    private var rewriteRules: List<BaseUrlRewriteRule> = emptyList()

    @Volatile
    private var flowLogEnabled: Boolean = true

    @Volatile
    private var enabled: Boolean = true

    /**
     * 设置 BaseUrl 映射关系，例如：
     * https://a.com/api -> http://b.com/test
     * a.com -> b.com
     *
     * 命中 source 前缀后，只替换 source 对应的 baseUrl 部分，保留剩余 path 和 query。
     */
    fun setBaseUrlMappings(mappings: Map<String, String>) {
        val rules = mappings.mapNotNull { (source, target) ->
            createRule(source, target)
        }.sortedWith(BaseUrlRewriteRule.MATCH_PRIORITY)
        rewriteRules = rules
        controlLog("mappings updated count=${rules.size}")
        rules.forEach { rule -> controlLog("mapping ${rule.describe()}") }
    }

    fun clearBaseUrlMappings() {
        rewriteRules = emptyList()
        controlLog("mappings cleared")
    }

    fun setFlowLogEnabled(enabled: Boolean) {
        flowLogEnabled = enabled
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        if (!enabled) {
            return chain.proceed(originalRequest)
        }

        val originalUrl = originalRequest.url
        val rewriteRule = rewriteRules.firstOrNull { it.matches(originalUrl) }

        if (rewriteRule == null) {
            return chain.proceed(originalRequest)
        }

        val newScheme = rewriteRule.target.scheme ?: originalUrl.scheme
        val newUrl = originalUrl.newBuilder()
            .scheme(newScheme)
            .host(rewriteRule.target.host)
            // 目标未显式指定端口时，使用新 scheme 的默认端口，避免把旧环境的非默认端口带过去。
            .port(rewriteRule.target.port ?: HttpUrl.defaultPort(newScheme))
            .encodedPath(rewriteRule.rewriteEncodedPath(originalUrl.encodedPath))
            .build()

        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            // 域名切换后让 OkHttp 基于新 URL 生成 Host，避免沿用业务侧手动写入的旧 Host。
            .removeHeader("Host")
            .build()

        flowLog("hit $originalUrl -> $newUrl")
        return chain.proceed(newRequest)
    }

    private fun createRule(source: String, target: String): BaseUrlRewriteRule? {
        val sourceBaseUrl = BaseUrlSource.parse(source)
        val targetBaseUrl = BaseUrlTarget.parse(target)
        if (sourceBaseUrl == null || targetBaseUrl == null) {
            controlLog("invalid mapping $source -> $target")
            return null
        }
        return BaseUrlRewriteRule(sourceBaseUrl, targetBaseUrl)
    }

    private data class BaseUrlRewriteRule(
        val source: BaseUrlSource,
        val target: BaseUrlTarget
    ) {
        fun matches(url: HttpUrl): Boolean {
            if (source.scheme != null && source.scheme != url.scheme) return false
            if (source.host != url.host) return false
            if (source.port != null && source.port != url.port) return false
            return isPathPrefixMatch(source.encodedPathPrefix, url.encodedPath)
        }

        fun rewriteEncodedPath(originalPath: String): String {
            val remainPath = when (source.encodedPathPrefix) {
                "/" -> originalPath
                originalPath -> ""
                else -> originalPath.removePrefix(source.encodedPathPrefix)
            }
            return mergeEncodedPath(target.encodedPathPrefix, remainPath)
        }

        fun describe(): String {
            return "${source.describe()} -> ${target.describe()}"
        }

        companion object {
            val MATCH_PRIORITY = compareByDescending<BaseUrlRewriteRule> { it.source.explicitPartCount }
                .thenByDescending { it.source.encodedPathPrefix.length }
        }
    }

    private data class BaseUrlSource(
        val scheme: String?,
        val host: String,
        val port: Int?,
        val encodedPathPrefix: String,
        val explicitPartCount: Int
    ) {
        fun describe(): String {
            return buildString {
                if (scheme != null) append(scheme).append("://")
                append(host)
                if (port != null) append(":").append(port)
                if (encodedPathPrefix != "/") append(encodedPathPrefix)
            }
        }

        companion object {
            fun parse(value: String): BaseUrlSource? {
                val parsed = parseBaseUrl(value) ?: return null
                val explicitPartCount = listOfNotNull(
                    parsed.scheme,
                    parsed.port,
                    parsed.encodedPath.takeIf { it != "/" }
                ).size
                return BaseUrlSource(
                    scheme = parsed.scheme,
                    host = parsed.url.host,
                    port = parsed.port,
                    encodedPathPrefix = normalizeEncodedPath(parsed.encodedPath),
                    explicitPartCount = explicitPartCount
                )
            }
        }
    }

    private data class BaseUrlTarget(
        val scheme: String?,
        val host: String,
        val port: Int?,
        val encodedPathPrefix: String
    ) {
        fun describe(): String {
            return buildString {
                if (scheme != null) append(scheme).append("://")
                append(host)
                if (port != null) append(":").append(port)
                if (encodedPathPrefix != "/") append(encodedPathPrefix)
            }
        }

        companion object {
            fun parse(value: String): BaseUrlTarget? {
                val parsed = parseBaseUrl(value) ?: return null
                return BaseUrlTarget(
                    scheme = parsed.scheme,
                    host = parsed.url.host,
                    port = parsed.port,
                    encodedPathPrefix = normalizeEncodedPath(parsed.encodedPath)
                )
            }
        }
    }

    private data class ParsedBaseUrl(
        val url: HttpUrl,
        val scheme: String?,
        val port: Int?,
        val encodedPath: String
    )

    private fun controlLog(message: String) {
        Log.d(TAG, message)
    }

    private fun flowLog(message: String) {
        if (flowLogEnabled) {
            Log.d(TAG, message)
        }
    }

    private companion object {
        private const val TAG = "DebugNetwork-Rewrite"

        fun parseBaseUrl(value: String): ParsedBaseUrl? {
            val normalizedValue = value.trim().takeIf { it.isNotEmpty() } ?: return null
            if (normalizedValue.contains('?') || normalizedValue.contains('#')) return null

            val hasScheme = normalizedValue.contains("://")
            val parseValue = if (hasScheme) normalizedValue else "https://$normalizedValue"
            val url = parseValue.toHttpUrlOrNull() ?: return null

            return ParsedBaseUrl(
                url = url,
                scheme = url.scheme.takeIf { hasScheme },
                port = url.port.takeIf { hasExplicitPort(normalizedValue, hasScheme) },
                encodedPath = url.encodedPath
            )
        }

        fun hasExplicitPort(value: String, hasScheme: Boolean): Boolean {
            val withoutScheme = if (hasScheme) value.substringAfter("://") else value
            val authority = withoutScheme.substringBefore('/').substringBefore('?').substringBefore('#')
            val portText = authority.substringAfterLast(':', missingDelimiterValue = "")
            return portText.isNotEmpty() && portText.all { it.isDigit() }
        }

        fun normalizeEncodedPath(path: String): String {
            if (path.isEmpty() || path == "/") return "/"
            return path.trimEnd('/')
        }

        fun isPathPrefixMatch(sourcePath: String, requestPath: String): Boolean {
            if (sourcePath == "/") return true
            if (requestPath == sourcePath) return true
            return requestPath.startsWith("$sourcePath/")
        }

        fun mergeEncodedPath(targetPath: String, remainPath: String): String {
            val normalizedRemain = remainPath.takeIf { it.isNotEmpty() } ?: return targetPath
            if (targetPath == "/") return normalizedRemain
            return "$targetPath$normalizedRemain"
        }
    }
}
