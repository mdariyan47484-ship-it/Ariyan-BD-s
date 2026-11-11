package com.example.overlay

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.LruCache
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.audio.AudioPlayerManager
import com.example.data.AppSettings
import com.example.data.AudioFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

class FloatingWindowService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var stateObserverJob: Job? = null

    private lateinit var windowManager: WindowManager
    private lateinit var playerManager: AudioPlayerManager
    private lateinit var appSettings: AppSettings

    private var bubbleContainer: FrameLayout? = null
    private var panelContainer: FrameLayout? = null

    private lateinit var bubbleLayoutParams: WindowManager.LayoutParams
    private lateinit var panelLayoutParams: WindowManager.LayoutParams

    private var isPanelExpanded = false

    // Soundboard UI components
    private var gridLayout: GridLayout? = null
    private var tvPanelTrackTitle: TextView? = null
    private var tvPanelTrackStatus: TextView? = null
    private var btnPanelPlayPause: ImageView? = null
    private var panelVolumeSeekBar: SeekBar? = null
    private var currentPlayingVisualizer: EqualizerAnimationView? = null

    // Bitmap cache for smooth soundboard rendering
    private val bitmapCache = object : LruCache<String, Bitmap>(20 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        playerManager = AudioPlayerManager.getInstance(this)
        appSettings = AppSettings.getInstance(this)

        createNotificationChannel()
        startServiceForeground()

        initBubbleView()
        initSoundboardPanelView()
        observePlayerState()
    }

    private fun startServiceForeground() {
        val notification = buildForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                OVERLAY_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(OVERLAY_NOTIFICATION_ID, notification)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, OVERLAY_CHANNEL_ID)
            .setContentTitle("ARIYAN A9X Soundboard Active")
            .setContentText("Tap floating bubble to access soundboard tiles")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                OVERLAY_CHANNEL_ID,
                "Ariyan Floating Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when floating soundboard is active"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initBubbleView() {
        val sizeDp = appSettings.bubbleSize.value
        val sizePx = dpToPx(sizeDp.toFloat())

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        bubbleLayoutParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 350
        }

        val container = FrameLayout(this).apply {
            alpha = appSettings.bubbleAlpha.value
        }

        // Circular neon background with cyber glow
        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#120E22"))
            setStroke(dpToPx(2.5f), Color.parseColor("#00F0FF"))
        }
        container.background = bgDrawable

        // App Logo inside
        val icon = ImageView(this).apply {
            setImageResource(R.drawable.ic_app_logo)
            val pad = dpToPx(7f)
            setPadding(pad, pad, pad, pad)
        }
        container.addView(icon, FrameLayout.LayoutParams(sizePx, sizePx))

        // Touch listener with dragging and edge snapping
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isMoving = false

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = bubbleLayoutParams.x
                    initialY = bubbleLayoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isMoving = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 12 || abs(dy) > 12) {
                        isMoving = true
                    }
                    bubbleLayoutParams.x = initialX + dx
                    bubbleLayoutParams.y = initialY + dy
                    try {
                        windowManager.updateViewLayout(container, bubbleLayoutParams)
                    } catch (_: Exception) {}
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isMoving) {
                        expandPanel()
                    } else {
                        snapBubbleToEdge()
                    }
                    true
                }
                else -> false
            }
        }

        bubbleContainer = container
        if (Settings.canDrawOverlays(this)) {
            windowManager.addView(container, bubbleLayoutParams)
        }
    }

    private fun snapBubbleToEdge() {
        val screenWidth = resources.displayMetrics.widthPixels
        val currentX = bubbleLayoutParams.x
        val targetX = if (currentX + (bubbleContainer?.width ?: 0) / 2 < screenWidth / 2) {
            20
        } else {
            screenWidth - (bubbleContainer?.width ?: 0) - 20
        }
        bubbleLayoutParams.x = targetX
        bubbleContainer?.let {
            try {
                windowManager.updateViewLayout(it, bubbleLayoutParams)
            } catch (_: Exception) {}
        }
    }

    private fun initSoundboardPanelView() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val widthPx = (screenWidth * 0.90f).toInt().coerceIn(dpToPx(320f), dpToPx(380f))
        val maxGridHeightPx = dpToPx(280f)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        panelLayoutParams = WindowManager.LayoutParams(
            widthPx,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dpToPx(60f)
        }

        val root = FrameLayout(this)

        // Glassmorphism Gaming Panel Container
        val contentCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dpToPx(14f)
            setPadding(pad, pad, pad, pad)

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(20f).toFloat()
                setColor(Color.parseColor("#F0080614")) // 94% opacity deep dark purple
                setStroke(dpToPx(1.8f), Color.parseColor("#B026FF"))
            }
            background = bg
        }

        // 1. Header (Logo + Title + Minimize + Close)
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val logoSmall = ImageView(this).apply {
            setImageResource(R.drawable.ic_app_logo)
            val size = dpToPx(26f)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dpToPx(8f)
            }
        }
        headerRow.addView(logoSmall)

        val headerText = TextView(this).apply {
            text = "ARIYAN A9X SOUNDBOARD"
            setTextColor(Color.parseColor("#00F0FF"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerRow.addView(headerText)

        val btnMinimize = TextView(this).apply {
            text = "—"
            setTextColor(Color.parseColor("#A5A2BF"))
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            val pad = dpToPx(8f)
            setPadding(pad, 0, pad, 0)
            setOnClickListener { collapsePanel() }
        }
        headerRow.addView(btnMinimize)

        val btnClose = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#FF1744"))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            val pad = dpToPx(8f)
            setPadding(pad, 0, pad, 0)
            setOnClickListener { stopSelf() }
        }
        headerRow.addView(btnClose)

        contentCard.addView(headerRow)

        // 2. Soundboard Category Subheader
        val subheaderRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(6f)
                bottomMargin = dpToPx(8f)
            }
        }

        val tvGridLabel = TextView(this).apply {
            text = "SELECT AUDIO TO INJECT / PLAY"
            setTextColor(Color.parseColor("#A5A2BF"))
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        subheaderRow.addView(tvGridLabel)

        contentCard.addView(subheaderRow)

        // 3. Scrollable Soundboard Grid (2 Columns of Individual Audio Tiles)
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                maxGridHeightPx
            )
            isVerticalScrollBarEnabled = true
        }

        gridLayout = GridLayout(this).apply {
            columnCount = 2
            alignmentMode = GridLayout.ALIGN_BOUNDS
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        scrollView.addView(gridLayout)
        contentCard.addView(scrollView)

        // 4. Divider Line
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1f)
            ).apply {
                topMargin = dpToPx(10f)
                bottomMargin = dpToPx(8f)
            }
            setBackgroundColor(Color.parseColor("#261E42"))
        }
        contentCard.addView(divider)

        // 5. Active Playing Info Bar
        val activeTrackRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(8f)
            }
        }

        val trackDetailsCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        tvPanelTrackTitle = TextView(this).apply {
            text = "Song 01 - Cyber Sonic Pulse"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        trackDetailsCol.addView(tvPanelTrackTitle)

        tvPanelTrackStatus = TextView(this).apply {
            text = "STATUS: READY"
            setTextColor(Color.parseColor("#00E676"))
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
        }
        trackDetailsCol.addView(tvPanelTrackStatus)
        activeTrackRow.addView(trackDetailsCol)

        currentPlayingVisualizer = EqualizerAnimationView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(48f), dpToPx(20f))
        }
        activeTrackRow.addView(currentPlayingVisualizer)

        contentCard.addView(activeTrackRow)

        // 6. Playback Controls Row (Prev, Play/Pause, Next, Stop)
        val controlsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(8f)
            }
        }

        val btnPrev = createControlButton(android.R.drawable.ic_media_previous) {
            playerManager.previous()
        }
        controlsRow.addView(btnPrev)

        btnPanelPlayPause = createControlButton(android.R.drawable.ic_media_play, isAccent = true) {
            playerManager.togglePlayPause()
        }
        controlsRow.addView(btnPanelPlayPause)

        val btnNext = createControlButton(android.R.drawable.ic_media_next) {
            playerManager.next()
        }
        controlsRow.addView(btnNext)

        val btnStop = createControlButton(android.R.drawable.ic_menu_close_clear_cancel) {
            playerManager.stop()
        }
        controlsRow.addView(btnStop)

        contentCard.addView(controlsRow)

        // 7. Volume Slider Row
        val volumeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val volumeLabel = TextView(this).apply {
            text = "VOL"
            setTextColor(Color.parseColor("#A5A2BF"))
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
        }
        volumeRow.addView(volumeLabel)

        panelVolumeSeekBar = SeekBar(this).apply {
            max = 100
            progress = (playerManager.volume.value * 100).toInt()
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        playerManager.setVolume(progress / 100f)
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        volumeRow.addView(panelVolumeSeekBar)

        contentCard.addView(volumeRow)

        root.addView(contentCard)
        panelContainer = root

        // Initial render of soundboard grid
        populateSoundboardGrid(playerManager.playlist.value, playerManager.currentTrack.value, playerManager.isPlaying.value)
    }

    private fun populateSoundboardGrid(
        playlist: List<AudioFile>,
        currentTrack: AudioFile?,
        isPlaying: Boolean
    ) {
        val grid = gridLayout ?: return
        grid.removeAllViews()

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val panelWidth = (screenWidth * 0.90f).toInt().coerceIn(dpToPx(320f), dpToPx(380f))
        val tilePadding = dpToPx(14f) * 2
        val tileSpacing = dpToPx(8f)
        val tileWidth = ((panelWidth - tilePadding - tileSpacing) / 2).coerceAtLeast(dpToPx(130f))
        val tileHeight = dpToPx(110f)

        for (track in playlist) {
            val isCurrent = track.id == currentTrack?.id
            val isThisPlaying = isCurrent && isPlaying

            val tileView = createSoundboardTileView(
                track = track,
                widthPx = tileWidth,
                heightPx = tileHeight,
                isCurrent = isCurrent,
                isPlaying = isThisPlaying
            ) {
                // Click To Play: clicking any tile immediately plays that audio file!
                playerManager.playOrToggleTrack(track)
            }

            val params = GridLayout.LayoutParams().apply {
                width = tileWidth
                height = tileHeight
                val margin = dpToPx(4f)
                setMargins(margin, margin, margin, margin)
            }
            grid.addView(tileView, params)
        }
    }

    private fun createSoundboardTileView(
        track: AudioFile,
        widthPx: Int,
        heightPx: Int,
        isCurrent: Boolean,
        isPlaying: Boolean,
        onClick: () -> Unit
    ): View {
        val tileRoot = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(14f).toFloat()
                setColor(Color.parseColor("#17112E"))
                val strokeColor = if (isCurrent) {
                    if (isPlaying) Color.parseColor("#00F0FF") else Color.parseColor("#B026FF")
                } else {
                    Color.parseColor("#2E2355")
                }
                setStroke(dpToPx(if (isCurrent) 2f else 1.2f), strokeColor)
            }
            background = bg
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }

        // Cover Art Image
        val coverImageView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            val bitmap = getTrackBitmap(track)
            if (bitmap != null) {
                setImageBitmap(bitmap)
            } else {
                setImageResource(R.drawable.ic_app_logo)
            }
        }
        tileRoot.addView(coverImageView)

        // Dark gradient scrim overlay so text is crisp
        val scrimView = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            val scrimDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#33000000"), Color.parseColor("#D9070511"))
            )
            background = scrimDrawable
        }
        tileRoot.addView(scrimView)

        // Play/Pause State Badge in Center
        val playStateIcon = ImageView(this).apply {
            val iconSize = dpToPx(28f)
            layoutParams = FrameLayout.LayoutParams(iconSize, iconSize).apply {
                gravity = Gravity.CENTER
            }
            setImageResource(
                if (isPlaying) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play
            )
            val iconBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (isPlaying) {
                    setColor(Color.parseColor("#B026FF"))
                    setStroke(dpToPx(1.5f), Color.parseColor("#00F0FF"))
                } else {
                    setColor(Color.parseColor("#B3000000"))
                }
            }
            background = iconBg
            val pad = dpToPx(6f)
            setPadding(pad, pad, pad, pad)
        }
        tileRoot.addView(playStateIcon)

        // Top-left Track Tag (e.g. SONG 01 / WAV)
        val tagBadge = TextView(this).apply {
            text = if (track.isPreset) track.title.substringBefore(" - ") else track.format
            setTextColor(Color.parseColor("#00F0FF"))
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
            val padH = dpToPx(6f)
            val padV = dpToPx(2f)
            setPadding(padH, padV, padH, padV)
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(6f).toFloat()
                setColor(Color.parseColor("#CC000000"))
            }
            background = bg
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                val m = dpToPx(6f)
                setMargins(m, m, 0, 0)
            }
        }
        tileRoot.addView(tagBadge)

        // Bottom Title Label
        val titleLabel = TextView(this).apply {
            text = track.title.substringAfter(" - ").ifBlank { track.title }
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM
                val m = dpToPx(6f)
                setMargins(m, 0, m, m)
            }
        }
        tileRoot.addView(titleLabel)

        return tileRoot
    }

    private fun getTrackBitmap(track: AudioFile): Bitmap? {
        val path = track.coverArtPath ?: return null
        bitmapCache.get(path)?.let { return it }

        return try {
            val file = File(path)
            if (file.exists()) {
                val bmp = BitmapFactory.decodeFile(file.absolutePath)
                if (bmp != null) {
                    bitmapCache.put(path, bmp)
                }
                bmp
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun createControlButton(iconRes: Int, isAccent: Boolean = false, onClick: () -> Unit): ImageView {
        val sizePx = dpToPx(if (isAccent) 42f else 34f)
        return ImageView(this).apply {
            setImageResource(iconRes)
            val pad = dpToPx(if (isAccent) 8f else 6f)
            setPadding(pad, pad, pad, pad)

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (isAccent) {
                    setColor(Color.parseColor("#B026FF"))
                    setStroke(dpToPx(1.5f), Color.parseColor("#00F0FF"))
                } else {
                    setColor(Color.parseColor("#231C3D"))
                }
            }
            background = bg

            layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                val margin = dpToPx(6f)
                setMargins(margin, 0, margin, 0)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun expandPanel() {
        if (isPanelExpanded) return
        try {
            bubbleContainer?.let { windowManager.removeView(it) }
            panelContainer?.let { windowManager.addView(it, panelLayoutParams) }
            isPanelExpanded = true
            // Refresh tiles with latest states
            populateSoundboardGrid(playerManager.playlist.value, playerManager.currentTrack.value, playerManager.isPlaying.value)
        } catch (_: Exception) {}
    }

    private fun collapsePanel() {
        if (!isPanelExpanded) return
        try {
            panelContainer?.let { windowManager.removeView(it) }
            bubbleContainer?.let { windowManager.addView(it, bubbleLayoutParams) }
            isPanelExpanded = false
        } catch (_: Exception) {}
    }

    private fun observePlayerState() {
        stateObserverJob = scope.launch {
            launch {
                playerManager.playlist.collectLatest { playlist ->
                    populateSoundboardGrid(playlist, playerManager.currentTrack.value, playerManager.isPlaying.value)
                }
            }
            launch {
                playerManager.currentTrack.collectLatest { track ->
                    tvPanelTrackTitle?.text = track?.title ?: "Select Audio Track"
                    populateSoundboardGrid(playerManager.playlist.value, track, playerManager.isPlaying.value)
                }
            }
            launch {
                playerManager.isPlaying.collectLatest { isPlaying ->
                    btnPanelPlayPause?.setImageResource(
                        if (isPlaying) android.R.drawable.ic_media_pause
                        else android.R.drawable.ic_media_play
                    )
                    tvPanelTrackStatus?.text = if (isPlaying) "STATUS: INJECTING..." else "STATUS: PAUSED"
                    tvPanelTrackStatus?.setTextColor(
                        if (isPlaying) Color.parseColor("#00E676") else Color.parseColor("#FFB300")
                    )
                    currentPlayingVisualizer?.setPlaying(isPlaying)
                    populateSoundboardGrid(playerManager.playlist.value, playerManager.currentTrack.value, isPlaying)
                }
            }
            launch {
                playerManager.volume.collectLatest { vol ->
                    panelVolumeSeekBar?.progress = (vol * 100).toInt()
                }
            }
        }
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        stateObserverJob?.cancel()
        try {
            if (isPanelExpanded) {
                panelContainer?.let { windowManager.removeView(it) }
            } else {
                bubbleContainer?.let { windowManager.removeView(it) }
            }
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val OVERLAY_CHANNEL_ID = "ariyan_floating_overlay_channel"
        const val OVERLAY_NOTIFICATION_ID = 902

        fun start(context: Context) {
            val intent = Intent(context, FloatingWindowService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingWindowService::class.java))
        }
    }

    private class EqualizerAnimationView(context: Context) : View(context) {
        private var isPlaying = false
        private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00F0FF")
            style = android.graphics.Paint.Style.FILL
        }
        private val heights = floatArrayOf(0.3f, 0.7f, 0.4f, 0.9f, 0.5f, 0.8f, 0.2f, 0.6f, 1.0f, 0.4f)
        private var frameCount = 0

        fun setPlaying(playing: Boolean) {
            isPlaying = playing
            postInvalidateOnAnimation()
        }

        override fun onDraw(canvas: android.graphics.Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val barCount = heights.size
            val barWidth = (w / barCount) * 0.6f
            val spacing = (w / barCount) * 0.4f

            for (i in 0 until barCount) {
                val currentHeight = if (isPlaying) {
                    val variation = abs(kotlin.math.sin((frameCount + i * 4) * 0.15f))
                    (h * 0.2f) + (h * 0.8f * variation)
                } else {
                    h * 0.2f
                }
                val left = i * (barWidth + spacing)
                val top = h - currentHeight
                val right = left + barWidth
                val bottom = h
                canvas.drawRoundRect(left, top, right, bottom, barWidth / 2, barWidth / 2, paint)
            }

            if (isPlaying) {
                frameCount++
                postInvalidateOnAnimation()
            }
        }
    }
}
