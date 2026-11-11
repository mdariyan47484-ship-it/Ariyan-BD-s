package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ariyan_a9x_prefs", Context.MODE_PRIVATE)

    private val _bubbleSize = MutableStateFlow(prefs.getInt("bubble_size", 58))
    val bubbleSize: StateFlow<Int> = _bubbleSize.asStateFlow()

    private val _bubbleAlpha = MutableStateFlow(prefs.getFloat("bubble_alpha", 0.95f))
    val bubbleAlpha: StateFlow<Float> = _bubbleAlpha.asStateFlow()

    private val _defaultVolume = MutableStateFlow(prefs.getInt("default_volume", 85))
    val defaultVolume: StateFlow<Int> = _defaultVolume.asStateFlow()

    private val _autoStartFloating = MutableStateFlow(prefs.getBoolean("auto_floating", false))
    val autoStartFloating: StateFlow<Boolean> = _autoStartFloating.asStateFlow()

    private val _routingMode = MutableStateFlow(
        AudioRoutingMode.valueOf(
            prefs.getString("routing_mode", AudioRoutingMode.ACOUSTIC_SPEAKER.name)
                ?: AudioRoutingMode.ACOUSTIC_SPEAKER.name
        )
    )
    val routingMode: StateFlow<AudioRoutingMode> = _routingMode.asStateFlow()

    fun setBubbleSize(size: Int) {
        _bubbleSize.value = size
        prefs.edit().putInt("bubble_size", size).apply()
    }

    fun setBubbleAlpha(alpha: Float) {
        _bubbleAlpha.value = alpha
        prefs.edit().putFloat("bubble_alpha", alpha).apply()
    }

    fun setDefaultVolume(vol: Int) {
        _defaultVolume.value = vol
        prefs.edit().putInt("default_volume", vol).apply()
    }

    fun setAutoStartFloating(enabled: Boolean) {
        _autoStartFloating.value = enabled
        prefs.edit().putBoolean("auto_floating", enabled).apply()
    }

    fun setRoutingMode(mode: AudioRoutingMode) {
        _routingMode.value = mode
        prefs.edit().putString("routing_mode", mode.name).apply()
    }

    companion object {
        @Volatile
        private var instance: AppSettings? = null

        fun getInstance(context: Context): AppSettings {
            return instance ?: synchronized(this) {
                instance ?: AppSettings(context.applicationContext).also { instance = it }
            }
        }
    }
}
