package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.example.data.AppSettings
import com.example.data.AudioFile
import com.example.data.AudioRoutingMode
import com.example.data.PresetSoundType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

class AudioPlayerManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    val focusManager = AudioFocusManager(context)
    val routingEngine = AudioRoutingEngine(context)
    val micTester = MicrophoneTester(context)
    val appSettings = AppSettings.getInstance(context)

    private val _playlist = MutableStateFlow<List<AudioFile>>(emptyList())
    val playlist: StateFlow<List<AudioFile>> = _playlist.asStateFlow()

    private val _currentTrack = MutableStateFlow<AudioFile?>(null)
    val currentTrack: StateFlow<AudioFile?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _volume = MutableStateFlow(appSettings.defaultVolume.value / 100f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isRepeating = MutableStateFlow(false)
    val isRepeating: StateFlow<Boolean> = _isRepeating.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        initializeSampleAudioFiles()
        routingEngine.applyRouting(appSettings.routingMode.value)
    }

    private fun initializeSampleAudioFiles() {
        scope.launch(Dispatchers.IO) {
            val sampleTracks = mutableListOf<AudioFile>()
            try {
                // Generate 6 soundboard sample presets
                val file1 = generateSynthWav("a9x_cyber_pulse.wav", SynthStyle.CYBER_PULSE, durationSeconds = 6)
                val file2 = generateSynthWav("a9x_voice_test.wav", SynthStyle.VOICE_CHORD, durationSeconds = 5)
                val file3 = generateSynthWav("a9x_bass_drop.wav", SynthStyle.BASS_PULSE, durationSeconds = 8)
                val file4 = generateSynthWav("a9x_victory.wav", SynthStyle.VICTORY_HORN, durationSeconds = 5)
                val file5 = generateSynthWav("a9x_laser.wav", SynthStyle.LASER_STRIKE, durationSeconds = 4)
                val file6 = generateSynthWav("a9x_glitch.wav", SynthStyle.GLITCH_ALERT, durationSeconds = 5)

                val cover1 = CoverArtManager.extractOrGenerateCover(context, "preset_1", "Cyber Pulse", Uri.fromFile(file1), "#00F0FF", "SONG 01", isPreset = true)
                val cover2 = CoverArtManager.extractOrGenerateCover(context, "preset_2", "Tactical Tone", Uri.fromFile(file2), "#0077FE", "SONG 02", isPreset = true)
                val cover3 = CoverArtManager.extractOrGenerateCover(context, "preset_3", "Heavy Bass", Uri.fromFile(file3), "#B026FF", "SONG 03", isPreset = true)
                val cover4 = CoverArtManager.extractOrGenerateCover(context, "preset_4", "Victory Horn", Uri.fromFile(file4), "#FFB300", "SONG 04", isPreset = true)
                val cover5 = CoverArtManager.extractOrGenerateCover(context, "preset_5", "Laser Strike", Uri.fromFile(file5), "#00E676", "SONG 05", isPreset = true)
                val cover6 = CoverArtManager.extractOrGenerateCover(context, "preset_6", "Glitch Alert", Uri.fromFile(file6), "#FF1744", "SONG 06", isPreset = true)

                sampleTracks.add(
                    AudioFile(
                        id = "preset_1",
                        title = "Song 01 - Cyber Sonic Pulse",
                        artist = "Ariyan Soundboard",
                        uri = Uri.fromFile(file1),
                        durationMs = 6000L,
                        sizeBytes = file1.length(),
                        format = "WAV",
                        isPreset = true,
                        presetType = PresetSoundType.CYBER_BEEP,
                        coverArtPath = cover1,
                        themeColorHex = "#00F0FF"
                    )
                )
                sampleTracks.add(
                    AudioFile(
                        id = "preset_2",
                        title = "Song 02 - Tactical Voice Tone",
                        artist = "Ariyan Voice Injector",
                        uri = Uri.fromFile(file2),
                        durationMs = 5000L,
                        sizeBytes = file2.length(),
                        format = "WAV",
                        isPreset = true,
                        presetType = PresetSoundType.VOICE_TEST,
                        coverArtPath = cover2,
                        themeColorHex = "#0077FE"
                    )
                )
                sampleTracks.add(
                    AudioFile(
                        id = "preset_3",
                        title = "Song 03 - Heavy Bass Drop",
                        artist = "Ariyan Sound Lab",
                        uri = Uri.fromFile(file3),
                        durationMs = 8000L,
                        sizeBytes = file3.length(),
                        format = "WAV",
                        isPreset = true,
                        presetType = PresetSoundType.BASS_PULSE,
                        coverArtPath = cover3,
                        themeColorHex = "#B026FF"
                    )
                )
                sampleTracks.add(
                    AudioFile(
                        id = "preset_4",
                        title = "Song 04 - Victory Horn Alert",
                        artist = "Gaming Soundboard",
                        uri = Uri.fromFile(file4),
                        durationMs = 5000L,
                        sizeBytes = file4.length(),
                        format = "WAV",
                        isPreset = true,
                        presetType = PresetSoundType.VICTORY_HORN,
                        coverArtPath = cover4,
                        themeColorHex = "#FFB300"
                    )
                )
                sampleTracks.add(
                    AudioFile(
                        id = "preset_5",
                        title = "Song 05 - Laser Strike FX",
                        artist = "Ariyan Battle SFX",
                        uri = Uri.fromFile(file5),
                        durationMs = 4000L,
                        sizeBytes = file5.length(),
                        format = "WAV",
                        isPreset = true,
                        presetType = PresetSoundType.LASER_STRIKE,
                        coverArtPath = cover5,
                        themeColorHex = "#00E676"
                    )
                )
                sampleTracks.add(
                    AudioFile(
                        id = "preset_6",
                        title = "Song 06 - Game Over Glitch",
                        artist = "Ariyan Cyber FX",
                        uri = Uri.fromFile(file6),
                        durationMs = 5000L,
                        sizeBytes = file6.length(),
                        format = "WAV",
                        isPreset = true,
                        presetType = PresetSoundType.GAME_ALERT,
                        coverArtPath = cover6,
                        themeColorHex = "#FF1744"
                    )
                )

                _playlist.value = sampleTracks
                if (_currentTrack.value == null) {
                    _currentTrack.value = sampleTracks.firstOrNull()
                    _durationMs.value = sampleTracks.firstOrNull()?.durationMs ?: 0L
                }
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "Failed to generate preset audio", e)
            }
        }
    }

    fun addTrack(audioFile: AudioFile) {
        val current = _playlist.value.toMutableList()
        current.removeAll { it.id == audioFile.id || (audioFile.uri != null && it.uri == audioFile.uri) }
        current.add(0, audioFile)
        _playlist.value = current
        _currentTrack.value = audioFile
        _durationMs.value = audioFile.durationMs
        playTrack(audioFile)
    }

    fun addTracks(audioFiles: List<AudioFile>) {
        val current = _playlist.value.toMutableList()
        for (file in audioFiles) {
            current.removeAll { it.id == file.id || (file.uri != null && it.uri == file.uri) }
            current.add(0, file)
        }
        _playlist.value = current
        audioFiles.firstOrNull()?.let {
            _currentTrack.value = it
            _durationMs.value = it.durationMs
            playTrack(it)
        }
    }

    fun removeTrack(id: String) {
        val current = _playlist.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            val removingCurrent = current[index].id == _currentTrack.value?.id
            current.removeAt(index)
            _playlist.value = current
            if (removingCurrent) {
                stop()
                _currentTrack.value = current.firstOrNull()
                _durationMs.value = _currentTrack.value?.durationMs ?: 0L
            }
        }
    }

    fun updateTrackCover(trackId: String, newCoverPath: String) {
        val list = _playlist.value.map {
            if (it.id == trackId) it.copy(coverArtPath = newCoverPath) else it
        }
        _playlist.value = list
        if (_currentTrack.value?.id == trackId) {
            _currentTrack.value = _currentTrack.value?.copy(coverArtPath = newCoverPath)
        }
    }

    fun playOrToggleTrack(track: AudioFile) {
        if (_currentTrack.value?.id == track.id) {
            togglePlayPause()
        } else {
            playTrack(track)
        }
    }

    fun playTrack(track: AudioFile) {
        _errorMessage.value = null
        try {
            stop()
            _currentTrack.value = track

            val player = MediaPlayer().apply {
                if (track.uri != null) {
                    setDataSource(context, track.uri)
                } else {
                    throw IllegalStateException("Track URI is null")
                }

                setOnPreparedListener { mp ->
                    _durationMs.value = mp.duration.toLong()
                    focusManager.requestFocus {
                        pause()
                    }
                    mp.setVolume(_volume.value, _volume.value)
                    mp.isLooping = _isRepeating.value
                    mp.start()
                    _isPlaying.value = true
                    startProgressTracker()
                }

                setOnCompletionListener {
                    if (!_isRepeating.value) {
                        _isPlaying.value = false
                        _currentPositionMs.value = 0L
                        stopProgressTracker()
                        next()
                    }
                }

                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlayerManager", "MediaPlayer error: what=$what extra=$extra")
                    _errorMessage.value = "Playback error ($what, $extra)"
                    _isPlaying.value = false
                    stopProgressTracker()
                    true
                }

                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error preparing track", e)
            _errorMessage.value = "Failed to open audio: ${e.localizedMessage}"
            _isPlaying.value = false
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer
        if (player != null) {
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
                stopProgressTracker()
            } else {
                focusManager.requestFocus {
                    pause()
                }
                player.start()
                _isPlaying.value = true
                startProgressTracker()
            }
        } else {
            _currentTrack.value?.let { playTrack(it) } ?: run {
                _playlist.value.firstOrNull()?.let { playTrack(it) }
            }
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
                stopProgressTracker()
            }
        }
    }

    fun stop() {
        stopProgressTracker()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _isPlaying.value = false
        _currentPositionMs.value = 0L
        focusManager.abandonFocus()
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let {
            val target = positionMs.coerceIn(0L, it.duration.toLong()).toInt()
            it.seekTo(target)
            _currentPositionMs.value = target.toLong()
        } ?: run {
            _currentPositionMs.value = positionMs
        }
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        mediaPlayer?.setVolume(clamped, clamped)
        appSettings.setDefaultVolume((clamped * 100).toInt())
    }

    fun toggleRepeat() {
        val newRepeat = !_isRepeating.value
        _isRepeating.value = newRepeat
        mediaPlayer?.isLooping = newRepeat
    }

    fun next() {
        val list = _playlist.value
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.id == _currentTrack.value?.id }
        val nextIndex = if (currentIndex != -1 && currentIndex < list.size - 1) {
            currentIndex + 1
        } else {
            0
        }
        playTrack(list[nextIndex])
    }

    fun previous() {
        val list = _playlist.value
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.id == _currentTrack.value?.id }
        val prevIndex = if (currentIndex > 0) {
            currentIndex - 1
        } else {
            list.size - 1
        }
        playTrack(list[prevIndex])
    }

    fun setRoutingMode(mode: AudioRoutingMode) {
        routingEngine.applyRouting(mode)
        appSettings.setRoutingMode(mode)
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        _currentPositionMs.value = it.currentPosition.toLong()
                        _durationMs.value = it.duration.toLong()
                    }
                }
                delay(200)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private enum class SynthStyle {
        CYBER_PULSE,
        VOICE_CHORD,
        BASS_PULSE,
        VICTORY_HORN,
        LASER_STRIKE,
        GLITCH_ALERT
    }

    private fun generateSynthWav(fileName: String, style: SynthStyle, durationSeconds: Int): File {
        val sampleRate = 44100
        val numSamples = durationSeconds * sampleRate
        val file = File(context.cacheDir, fileName)
        if (file.exists() && file.length() > 1000) return file

        val pcm = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val sample = when (style) {
                SynthStyle.CYBER_PULSE -> {
                    val freq = 440.0 * (1.0 + ((i / (sampleRate / 4)) % 4) * 0.25)
                    val envelope = (1.0 - (t % 0.25) * 3.5).coerceIn(0.1, 1.0)
                    sin(2.0 * Math.PI * freq * t) * envelope * 24000.0
                }
                SynthStyle.VOICE_CHORD -> {
                    val f1 = sin(2.0 * Math.PI * 523.25 * t)
                    val f2 = sin(2.0 * Math.PI * 659.25 * t)
                    val f3 = sin(2.0 * Math.PI * 783.99 * t)
                    ((f1 + f2 + f3) / 3.0) * 26000.0
                }
                SynthStyle.BASS_PULSE -> {
                    val sweepFreq = 180.0 - (t * 15.0).coerceAtMost(110.0)
                    val pulse = 0.5 + 0.5 * sin(2.0 * Math.PI * 2.0 * t)
                    sin(2.0 * Math.PI * sweepFreq * t) * pulse * 28000.0
                }
                SynthStyle.VICTORY_HORN -> {
                    val step = ((t * 4).toInt() % 4)
                    val chordFreq = when (step) {
                        0 -> 440.0
                        1 -> 554.37
                        2 -> 659.25
                        else -> 880.0
                    }
                    sin(2.0 * Math.PI * chordFreq * t) * 25000.0
                }
                SynthStyle.LASER_STRIKE -> {
                    val laserFreq = 1200.0 * (1.0 - (t % 0.4) * 2.0).coerceAtLeast(0.2)
                    sin(2.0 * Math.PI * laserFreq * t) * 24000.0
                }
                SynthStyle.GLITCH_ALERT -> {
                    val mod = if ((i / 400) % 2 == 0) 800.0 else 300.0
                    sin(2.0 * Math.PI * mod * t) * 26000.0
                }
            }
            pcm[i] = sample.toInt().coerceIn(-32767, 32767).toShort()
        }

        FileOutputStream(file).use { out ->
            writeWavHeader(out, sampleRate, 1, 16, numSamples)
            val byteBuffer = ByteBuffer.allocate(pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (s in pcm) byteBuffer.putShort(s)
            out.write(byteBuffer.array())
        }
        return file
    }

    private fun writeWavHeader(
        out: FileOutputStream,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
        numSamples: Int
    ) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val dataSize = numSamples * channels * bitsPerSample / 8
        val chunkSize = 36 + dataSize

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (chunkSize and 0xff).toByte()
        header[5] = ((chunkSize shr 8) and 0xff).toByte()
        header[6] = ((chunkSize shr 16) and 0xff).toByte()
        header[7] = ((chunkSize shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
        header[20] = 1; header[21] = 0
        header[22] = channels.toByte(); header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = ((channels * bitsPerSample) / 8).toByte(); header[33] = 0
        header[34] = bitsPerSample.toByte(); header[35] = 0
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (dataSize and 0xff).toByte()
        header[41] = ((dataSize shr 8) and 0xff).toByte()
        header[42] = ((dataSize shr 16) and 0xff).toByte()
        header[43] = ((dataSize shr 24) and 0xff).toByte()

        out.write(header)
    }

    companion object {
        @Volatile
        private var instance: AudioPlayerManager? = null

        fun getInstance(context: Context): AudioPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: AudioPlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
