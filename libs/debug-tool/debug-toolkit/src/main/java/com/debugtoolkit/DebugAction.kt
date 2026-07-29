package com.debugtoolkit

import android.content.ClipData
import android.content.Context

data class DebugAction(
    val id: String,
    val title: String,
    val iconResId: Int,
    val backgroundColor: String,
    val group: String,
    val visible: (Context) -> Boolean = { true },
    val onClick: () -> Unit
)
