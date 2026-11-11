package com.example.data

import android.net.Uri

data class AudioFile(
    val id: String,
    val title: String,
    val artist: String = "Ariyan Audio",
    val uri: Uri? = null,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val format: String = "Audio",
    val isPreset: Boolean = false,
    val presetType: PresetSoundType? = null
)

enum class PresetSoundType {
    CYBER_BEEP,
    VOICE_TEST,
    BASS_PULSE,
    GAME_ALERT
}

enum class AudioRoutingMode(val title: String, val subtitle: String) {
    ACOUSTIC_SPEAKER(
        title = "Acoustic (Speaker-to-Mic)",
        subtitle = "Loudspeaker playback optimized for game voice chat & call microphone pickup"
    ),
    DIGITAL_COMMUNICATION(
        title = "Digital Communication Route",
        subtitle = "Routes to active communication device (Headset, Bluetooth SCO, USB)"
    ),
    SYSTEM_MEDIA(
        title = "Standard System Audio",
        subtitle = "Plays on standard Android media output channel"
    )
}
