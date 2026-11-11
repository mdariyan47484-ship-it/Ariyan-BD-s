package com.example

import android.app.Application
import com.example.audio.AudioPlayerManager
import com.example.data.AppSettings
import com.example.shizuku.ShizukuManager

class AriyanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppSettings.getInstance(this)
        ShizukuManager.getInstance(this)
        AudioPlayerManager.getInstance(this)
    }
}
