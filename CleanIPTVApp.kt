package com.myiptv.clean

import android.app.Application
import android.content.Context

class CleanIPTVApp : Application() {
    
    companion object {
        lateinit var instance: CleanIPTVApp
            private set
        
        fun getAppContext(): Context {
            return instance.applicationContext
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
