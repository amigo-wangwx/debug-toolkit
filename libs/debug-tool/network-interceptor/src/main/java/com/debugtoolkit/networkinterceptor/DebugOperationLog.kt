package com.debugtoolkit.networkinterceptor

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugOperationLog {
    private const val TAG = "DebugToolkit-Operation"
    private const val MAX_ENTRIES = 200
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val entries = ArrayDeque<Entry>()

    data class Entry(
        val timestamp: Long,
        val timeText: String,
        val category: String,
        val action: String,
        val message: String,
        val success: Boolean?
    )

    @Synchronized
    fun record(
        category: String,
        action: String,
        message: String = "",
        success: Boolean? = null
    ) {
        val now = System.currentTimeMillis()
        val entry = Entry(
            timestamp = now,
            timeText = timeFormat.format(Date(now)),
            category = category,
            action = action,
            message = message,
            success = success
        )
        while (entries.size >= MAX_ENTRIES) {
            entries.removeFirst()
        }
        entries.addLast(entry)
        Log.d(TAG, entry.toLogText())
    }

    @Synchronized
    fun getRecentEntries(): List<Entry> = entries.toList()

    @Synchronized
    fun clear() {
        entries.clear()
        Log.d(TAG, "operation log cleared")
    }

    private fun Entry.toLogText(): String {
        val successText = success?.let { " success=$it" }.orEmpty()
        val messageText = message.takeIf { it.isNotBlank() }?.let { " message=$it" }.orEmpty()
        return "[$timeText][$category] action=$action$successText$messageText"
    }
}
