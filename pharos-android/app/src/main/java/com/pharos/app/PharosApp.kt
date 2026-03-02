package com.pharos.app

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class PharosApp : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(this)
        appContainer = AppContainer(this)
        instance = this
    }

    companion object {
        lateinit var instance: PharosApp
            private set
    }
}
