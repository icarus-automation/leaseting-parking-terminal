package com.leaseting.parkingterminal

import android.app.Application
import com.leaseting.parkingterminal.di.AppContainer
import com.leaseting.parkingterminal.di.DefaultAppContainer

class ParkingTerminalApplication : Application() {

    /** Process-scoped dependency graph. Read it through [appContainer]. */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
