package com.sase.roomwifilogger

import android.app.Application
import com.sase.roomwifilogger.data.AppContainer

class RoomWifiLoggerApp : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
