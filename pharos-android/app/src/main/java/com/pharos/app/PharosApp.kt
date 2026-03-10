package com.pharos.app

import android.app.Application
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class PharosApp : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        try {
            PDFBoxResourceLoader.init(this)
        } catch (e: Exception) {
            Log.w("PharosApp", "PDFBox init failed - PDF support may be unavailable", e)
        }

        appContainer = AppContainer(this)
    }

    companion object {
        lateinit var instance: PharosApp
            private set
    }
}
