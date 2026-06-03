package com.lwtor.xhunter

import android.app.Application
import com.lwtor.xhunter.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class XHunterApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(level = Level.INFO)
            androidContext(this@XHunterApplication)
            modules(sharedModule)
        }
    }

}