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
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.audio.AudioPlayerManager
import com.example.audio.AudioPlayerService
import com.example.data.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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

    // Views inside panel for updating
    private var tvPanelTrackTitle: TextView? = null
    private var tvPanelRoutingMode: TextView? = null
    private var btnPanelPlayPause: ImageView? = null
    private var panelVolumeSeekBar: SeekBar? = null
    private var visualizerEqualizerView: EqualizerAnimationView? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        playerManager = AudioPlayerManager.getInstance(this)
        appSettings = AppSettings.getInstance(this)

        createNotificationChannel()
        startServiceForeground()

        initBubbleView()
        initPanelView()
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
            .setContentTitle("ARIYAN A9X Floating Window Active")
            .setContentText("Overlay controller running over apps")
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
                description = "Shows when floating overlay controller is active"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initBubbleView() {
        val sizeDp = appSettings.bubbleSize.value
        val sizePx = (sizeDp * resources.displayMetrics.density).toInt()

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

        // Circular neon background
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
                        // Single tap -> expand to Panel
                        expandPanel()
                    } else {
                        // Snap to nearest edge (left or right)
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
        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
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

    private fun initPanelView() {
        val widthPx = (320 * resources.displayMetrics.density).toInt()

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
            y = 200
        }

        val root = FrameLayout(this)

        // Glassmorphism cyberpunk panel container
        val contentCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dpToPx(16f)
            setPadding(pad, pad, pad, pad)

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(18f).toFloat()
                setColor(Color.parseColor("#E60D0A1C")) // 90% opacity deep dark purple
                setStroke(dpToPx(1.5f), Color.parseColor("#B026FF"))
            }
            background = bg
        }

        // Header Row (Logo + Title + Minimize + Close)
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val logoSmall = ImageView(this).apply {
            setImageResource(R.drawable.ic_app_logo)
            val size = dpToPx(28f)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dpToPx(8f)
            }
        }
        headerRow.addView(logoSmall)

        val headerText = TextView(this).apply {
            text = "ARIYAN A9X BD"
            setTextColor(Color.parseColor("#00F0FF"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerRow.addView(headerText)

        // Minimize Button
        val btnMinimize = TextView(this).apply {
            text = "—"
            setTextColor(Color.parseColor("#A5A2BF"))
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            val pad = dpToPx(6f)
            setPadding(pad, 0, pad, 0)
            setOnClickListener { collapsePanel() }
        }
        headerRow.addView(btnMinimize)

        // Close Button
        val btnClose = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#FF1744"))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            val pad = dpToPx(6f)
            setPadding(pad, 0, pad, 0)
            setOnClickListener { stopSelf() }
        }
        headerRow.addView(btnClose)

        contentCard.addView(headerRow)

        // Equalizer animation
        visualizerEqualizerView = EqualizerAnimationView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(28f)
            ).apply {
                topMargin = dpToPx(10f)
                bottomMargin = dpToPx(6f)
            }
        }
        contentCard.addView(visualizerEqualizerView)

        // Track title
        tvPanelTrackTitle = TextView(this).apply {
            text = "Select Audio Track"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        contentCard.addView(tvPanelTrackTitle)

        // Routing mode indicator
        tvPanelRoutingMode = TextView(this).apply {
            text = "Mode: Acoustic Loudspeaker"
            setTextColor(Color.parseColor("#00E676"))
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(10f)
            }
        }
        contentCard.addView(tvPanelRoutingMode)

        // Playback Controls Row (Previous, Play/Pause, Next, Stop)
        val controlsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(10f)
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

        // Volume row
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
    }

    private fun createControlButton(iconRes: Int, isAccent: Boolean = false, onClick: () -> Unit): ImageView {
        val sizePx = dpToPx(if (isAccent) 44f else 36f)
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
                playerManager.currentTrack.collectLatest { track ->
                    tvPanelTrackTitle?.text = track?.title ?: "Select Audio Track"
                }
            }
            launch {
                playerManager.isPlaying.collectLatest { isPlaying ->
                    btnPanelPlayPause?.setImageResource(
                        if (isPlaying) android.R.drawable.ic_media_pause
                        else android.R.drawable.ic_media_play
                    )
                    visualizerEqualizerView?.setPlaying(isPlaying)
                }
            }
            launch {
                playerManager.volume.collectLatest { vol ->
                    panelVolumeSeekBar?.progress = (vol * 100).toInt()
                }
            }
            launch {
                playerManager.routingEngine.currentMode.collectLatest { mode ->
                    tvPanelRoutingMode?.text = "Mode: ${mode.title}"
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

        fun isRunning(context: Context): Boolean {
            // Helper check
            return true
        }
    }

    // Custom lightweight animated equalizer view for the floating panel
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
                    val variation = kotlin.math.abs(kotlin.math.sin((frameCount + i * 4) * 0.15f))
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
