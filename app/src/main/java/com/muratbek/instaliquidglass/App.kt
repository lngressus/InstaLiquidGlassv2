package com.muratbek.instaliquidglass

import android.app.Application
import com.muratbek.instaliquidglass.ipc.PrefsManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        PrefsManager.initApp(this)
    }
}
