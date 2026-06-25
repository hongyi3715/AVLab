package com.lq.avlab.ui

import android.app.Application
import com.lq.core.HRouter
import com.lq.webrtc.WebRtcUtil
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App: Application() {

    companion object{
        lateinit var instance: App
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        WebRtcUtil.init(this)
        HRouter.init(this)
        HRouter.debug(true)
    }
}
