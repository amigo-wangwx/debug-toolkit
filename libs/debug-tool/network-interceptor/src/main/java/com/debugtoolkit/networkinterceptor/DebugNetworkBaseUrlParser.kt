package com.debugtoolkit.networkinterceptor

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal object DebugNetworkBaseUrlParser {
    data class ParsedBaseUrl(
        val url: HttpUrl,
        val scheme: String?,
        val port: Int?,
        val encodedPath: String
    )

    fun parse(value: String): ParsedBaseUrl? {
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

    private fun hasExplicitPort(value: String, hasScheme: Boolean): Boolean {
        val withoutScheme = if (hasScheme) value.substringAfter("://") else value
        val authority = withoutScheme.substringBefore('/').substringBefore('?').substringBefore('#')
        val portText = authority.substringAfterLast(':', missingDelimiterValue = "")
        return portText.isNotEmpty() && portText.all { it.isDigit() }
    }
}
