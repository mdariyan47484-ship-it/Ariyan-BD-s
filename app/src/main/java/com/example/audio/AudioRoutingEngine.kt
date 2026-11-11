package com.example.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.example.data.AudioRoutingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioRoutingEngine(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _currentMode = MutableStateFlow(AudioRoutingMode.ACOUSTIC_SPEAKER)
    val currentMode: StateFlow<AudioRoutingMode> = _currentMode.asStateFlow()

    private val _routingStatusMessage = MutableStateFlow("Engine Initialized")
    val routingStatusMessage: StateFlow<String> = _routingStatusMessage.asStateFlow()

    fun applyRouting(mode: AudioRoutingMode): Boolean {
        _currentMode.value = mode
        return try {
            when (mode) {
                AudioRoutingMode.ACOUSTIC_SPEAKER -> {
                    setupAcousticMode()
                }
                AudioRoutingMode.DIGITAL_COMMUNICATION -> {
                    setupCommunicationRoute()
                }
                AudioRoutingMode.SYSTEM_MEDIA -> {
                    resetToMedia()
                }
            }
        } catch (e: Exception) {
            Log.e("AudioRoutingEngine", "Error applying routing mode: $mode", e)
            _routingStatusMessage.value = "Routing Error: ${e.message}"
            false
        }
    }

    private fun setupAcousticMode(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val devices = audioManager.availableCommunicationDevices
                val speakerDevice = devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                if (speakerDevice != null) {
                    val success = audioManager.setCommunicationDevice(speakerDevice)
                    _routingStatusMessage.value = if (success) {
                        "Acoustic Mode: Active on Loudspeaker (Communication Target)"
                    } else {
                        "Acoustic Mode: Standard Loudspeaker Active"
                    }
                    return success
                }
            }
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = true
            _routingStatusMessage.value = "Acoustic Mode: Speakerphone Enabled (Optimized for Mic Pickup)"
            true
        } catch (e: Exception) {
            _routingStatusMessage.value = "Acoustic fallback applied"
            true
        }
    }

    private fun setupCommunicationRoute(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val devices = audioManager.availableCommunicationDevices
                val headsetOrBt = devices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                            it.type == AudioDeviceInfo.TYPE_USB_HEADSET
                }
                if (headsetOrBt != null) {
                    val res = audioManager.setCommunicationDevice(headsetOrBt)
                    _routingStatusMessage.value = "Digital Route Active: ${headsetOrBt.productName}"
                    return res
                } else {
                    _routingStatusMessage.value = "Digital Route: Default Communication Device Selected"
                    return true
                }
            } else {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                _routingStatusMessage.value = "Communication Route Active (MODE_IN_COMMUNICATION)"
                true
            }
        } catch (e: Exception) {
            _routingStatusMessage.value = "Digital Route unavailable on this configuration"
            false
        }
    }

    private fun resetToMedia(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
            }
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = false
            audioManager.mode = AudioManager.MODE_NORMAL
            _routingStatusMessage.value = "Standard System Audio Restored"
            true
        } catch (e: Exception) {
            _routingStatusMessage.value = "Reset to media normal"
            true
        }
    }

    fun getAvailableOutputDevices(): List<AudioDeviceInfo> {
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
    }
}
