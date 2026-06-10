package com.debugtoolkit.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import okio.GzipSource
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class DetailLogInterceptor : Interceptor {
    private val UTF8 = Charset.forName("UTF-8")
    private val TAG = "Network-Log"

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.nanoTime()

        // 打印请求信息
        log("================================================================")
        log("[REQUEST] ${request.method} ${request.url}")
        log("--- Headers ---")
        request.headers.forEach { (name, value) -> log("  $name: $value") }

        val requestBody = request.body
        if (requestBody != null) {
            val buffer = Buffer()
            requestBody.writeTo(buffer)
            var charset = UTF8
            val contentType = requestBody.contentType()
            if (contentType != null) {
                charset = contentType.charset(UTF8) ?: UTF8
            }
            log("--- Body ---")
            if (isPlaintext(buffer)) {
                log(formatJson(buffer.readString(charset)))
            } else {
                log("[binary ${requestBody.contentLength()}-byte body]")
            }
        }
        log("================================================================")

        try {
            val response = chain.proceed(request)
            val endTime = System.nanoTime()
            val duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime)

            // 处理响应体（解压Gzip）
            val responseBody = response.body
            if (responseBody == null) {
                log("================================================================")
                log("[RESPONSE] ${request.method} ${request.url} (${response.code} in ${duration}ms)")
                log("--- Headers ---")
                response.headers.forEach { (name, value) -> log("  $name: $value") }
                log("--- Body ---")
                log("[empty body]")
                log("================================================================")
                return response
            }
            val source = responseBody.source()
            source.request(Long.MAX_VALUE)
            var buffer = source.buffer

            if ("gzip".equals(response.headers["Content-Encoding"], ignoreCase = true)) {
                GzipSource(buffer.clone()).use { gzippedResponseBody ->
                    buffer = Buffer()
                    buffer.writeAll(gzippedResponseBody)
                }
            }

            var charset = UTF8
            val contentType = responseBody.contentType()
            if (contentType != null) {
                charset = contentType.charset(UTF8) ?: UTF8
            }

            log("================================================================")
            log("[RESPONSE] ${request.method} ${request.url} (${response.code} in ${duration}ms)")
            log("--- Headers ---")
            response.headers.forEach { (name, value) -> log("  $name: $value") }
            log("--- Body ---")
            if (isPlaintext(buffer)) {
                log(formatJson(buffer.clone().readString(charset)))
            } else {
                log("[binary ${buffer.size}-byte body]")
            }
            log("================================================================")

            return response
        } catch (e: Exception) {
            Log.e(TAG, "[ERROR] ${request.method} ${request.url}: ${e.message}", e)
            throw e
        }
    }

    // 判断是否为文本内容（避免打印二进制数据）
    private fun isPlaintext(buffer: Buffer): Boolean {
        try {
            val prefix = Buffer()
            val byteCount = if (buffer.size < 64) buffer.size else 64
            buffer.copyTo(prefix, 0, byteCount)
            for (i in 0..15) {
                if (prefix.exhausted()) break
                val codePoint = prefix.readUtf8CodePoint()
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) return false
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun formatJson(json: String): String {
        return try {
            if (json.startsWith("{")) JSONObject(json).toString(2)
            else if (json.startsWith("[")) JSONArray(json).toString(2)
            else json
        } catch (e: Exception) {
            json
        }
    }

    private fun log(message: String) {
        Log.d(TAG, message)
    }
}
