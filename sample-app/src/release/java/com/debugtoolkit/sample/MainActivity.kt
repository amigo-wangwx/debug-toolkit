package com.debugtoolkit.sample

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(TextView(this).apply {
            text = "Release-safe sample. DebugToolkit is only available in debug builds."
            textSize = 16f
            setPadding(32, 32, 32, 32)
        })
    }
}
