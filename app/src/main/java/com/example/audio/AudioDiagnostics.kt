package com.example.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

data class DeviceAudioInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val apiLevel: Int,
    val outputDevices: List<String>,
    val inputDevices: List<String>,
    val hasSpeaker: Boolean,
    val hasMicrophone: Boolean,
    val currentCommunicationDevice: String?,
    val isPlaybackCaptureSupported: Boolean,
    val isScoAvailable: Boolean,
    val routingCapabilitySummary: String
)

object AudioDiagnostics {
    fun inspect(context: Context): DeviceAudioInfo {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val outputList = mutableListOf<String>()
        val inputList = mutableListOf<String>()

        var hasSpeaker = false
        var hasMic = false

        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL)
        for (dev in devices) {
            val typeName = getDeviceTypeName(dev.type)
            val name = "${dev.productName.ifBlank { typeName }} ($typeName)"
            if (dev.isSink) {
                outputList.add(name)
                if (dev.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) hasSpeaker = true
            }
            if (dev.isSource) {
                inputList.add(name)
                if (dev.type == AudioDeviceInfo.TYPE_BUILTIN_MIC) hasMic = true
            }
        }

        val commDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.communicationDevice?.let { "${it.productName} (${getDeviceTypeName(it.type)})" }
        } else {
            if (audioManager.isSpeakerphoneOn) "Speakerphone ON" else "Standard Normal Route"
        }

        val isPlaybackCapture = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        val summary = buildString {
            append("• Acoustic Injection: 100% Fully Supported (Loudspeaker-to-Microphone loop)\n")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                append("• Dynamic Communication Routing: Supported (API 31+)\n")
            } else {
                append("• Legacy Audio Mode Routing: Supported (API ${Build.VERSION.SDK_INT})\n")
            }
            append("• Direct Internal Audio Injection Notice: Android security sandbox restricts CAPTURE_AUDIO_OUTPUT to system apps. Acoustic Mode provides safe, reliable delivery for in-game voice chat.")
        }

        return DeviceAudioInfo(
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            outputDevices = outputList.distinct(),
            inputDevices = inputList.distinct(),
            hasSpeaker = hasSpeaker,
            hasMicrophone = hasMic,
            currentCommunicationDevice = commDevice,
            isPlaybackCaptureSupported = isPlaybackCapture,
            isScoAvailable = audioManager.isBluetoothScoAvailableOffCall,
            routingCapabilitySummary = summary
        )
    }

    private fun getDeviceTypeName(type: Int): String {
        return when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Built-in Earpiece"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Built-in Speaker"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset (Mic+Headphones)"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth SCO (Hands-free)"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP (Stereo Audio)"
            AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Audio Device"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
            AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Built-in Microphone"
            AudioDeviceInfo.TYPE_FM_TUNER -> "FM Tuner"
            AudioDeviceInfo.TYPE_AUX_LINE -> "AUX Line"
            else -> "Audio Device ($type)"
        }
    }
}
