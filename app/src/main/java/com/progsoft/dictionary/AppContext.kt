package com.progsoft.dictionary

import android.app.Application
import android.content.Context
import com.progsoft.dictionary.util.FontStrokeUtil

class AppContext : Application() {
    override fun onCreate() {
        super.onCreate()
        context = this
        FontStrokeUtil.getInstance().init();
    }

    companion object {
        lateinit var context: Context
    }
}