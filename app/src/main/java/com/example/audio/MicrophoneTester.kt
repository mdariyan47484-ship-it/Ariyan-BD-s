package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt

data class MicMeterState(
    val isRecording: Boolean = false,
    val amplitudePercent: Float = 0f,
    val decibels: Float = 0f,
    val statusText: String = "Tap 'Start Mic Test' to verify speaker-to-mic acoustic pickup"
)

class MicrophoneTester(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var recordingJob: Job? = null
    private var audioRecord: AudioRecord? = null

    private val _meterState = MutableStateFlow(MicMeterState())
    val meterState: StateFlow<MicMeterState> = _meterState.asStateFlow()

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun startTesting() {
        if (!hasPermission()) {
            _meterState.value = MicMeterState(
                isRecording = false,
                statusText = "RECORD_AUDIO permission required for microphone verification"
            )
            return
        }

        if (recordingJob?.isActive == true) return

        recordingJob = scope.launch {
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = maxOf(minBufferSize, 2048)

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    _meterState.value = MicMeterState(
                        isRecording = false,
                        statusText = "Microphone hardware unavailable"
                    )
                    return@launch
                }

                audioRecord?.startRecording()
                _meterState.value = MicMeterState(
                    isRecording = true,
                    statusText = "Listening... Speak or play audio to observe pickup level"
                )

                val buffer = ShortArray(bufferSize / 2)

                while (isActive) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        var sum = 0.0
                        for (i in 0 until read) {
                            sum += buffer[i] * buffer[i]
                        }
                        val rms = sqrt(sum / read)
                        val db = if (rms > 0) (20 * log10(rms / 1.0)).toFloat() else 0f
                        val percent = (rms / 32767.0).toFloat().coerceIn(0f, 1f)

                        _meterState.value = MicMeterState(
                            isRecording = true,
                            amplitudePercent = percent,
                            decibels = db,
                            statusText = if (percent > 0.08f) {
                                "Sound Detected! (Level: ${db.toInt()} dB) - Acoustic loopback working"
                            } else {
                                "Listening... Level: ${db.toInt()} dB"
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                _meterState.value = MicMeterState(
                    isRecording = false,
                    statusText = "Mic Test Error: ${e.message}"
                )
            } finally {
                stopInternal()
            }
        }
    }

    fun stopTesting() {
        recordingJob?.cancel()
        recordingJob = null
        stopInternal()
        _meterState.value = MicMeterState(
            isRecording = false,
            amplitudePercent = 0f,
            decibels = 0f,
            statusText = "Microphone test stopped"
        )
    }

    private fun stopInternal() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }
}
