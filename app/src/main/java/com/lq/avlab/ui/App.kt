package com.lq.avlab.ui

import android.app.Application
import com.lq.core.HRouter
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App: Application() {

    override fun onCreate() {
        super.onCreate()
        HRouter.init(this)
        HRouter.debug(true)
    }
}
