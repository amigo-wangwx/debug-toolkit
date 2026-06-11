package com.debugtoolkit.sample

import android.app.Application
import android.util.Log

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("DebugToolkit-Sample", "SampleApp onCreate")
    }
}
