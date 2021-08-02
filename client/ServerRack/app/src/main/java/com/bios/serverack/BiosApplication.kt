package com.bios.serverack

import android.app.Application

class BiosApplication : Application() {
    lateinit var instance: Application

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}