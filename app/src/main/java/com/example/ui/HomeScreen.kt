package com.example.ui

import android.content.Context
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.AudioPlayerManager
import com.example.audio.AudioPlayerService
import com.example.data.AudioRoutingMode
import com.example.overlay.FloatingWindowService
import com.example.permissions.PermissionManager
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuState
import com.example.ui.components.AudioVisualizerWave
import com.example.ui.components.ButtonStyle
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.GlowButton
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    playerManager: AudioPlayerManager,
    shizukuManager: ShizukuManager,
    permissionManager: PermissionManager,
    onNavigateToShizuku: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToDiagnostics: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val currentTrack by playerManager.currentTrack.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val currentPositionMs by playerManager.currentPositionMs.collectAsState()
    val durationMs by playerManager.durationMs.collectAsState()
    val volume by playerManager.volume.collectAsState()
    val isRepeating by playerManager.isRepeating.collectAsState()
    val routingMode by playerManager.routingEngine.currentMode.collectAsState()
    val shizukuState by shizukuManager.shizukuState.collectAsState()
    val errorMessage by playerManager.errorMessage.collectAsState()

    var isFloatingActive by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header with Gaming Logo & Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, CyanAccent, CircleShape)
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = "Ariyan A9X Logo",
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ARIYAN A9X BD",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Voice Float Injector & Audio Routing",
                    color = CyanAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isPlaying) StatusGreen.copy(alpha = 0.2f) else DarkSurfaceVariant,
                border = BorderStroke(1.dp, if (isPlaying) StatusGreen else DarkBorder)
            ) {
                Text(
                    text = if (isPlaying) "INJECTING" else "IDLE",
                    color = if (isPlaying) StatusGreen else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // System Status Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Shizuku Status Chip
            StatusChip(
                modifier = Modifier.weight(1f),
                title = "SHIZUKU",
                status = when (shizukuState) {
                    ShizukuState.AUTHORIZED_ACTIVE -> "Active"
                    ShizukuState.RUNNING_NOT_AUTHORIZED -> "Auth Req"
                    ShizukuState.INSTALLED_NOT_RUNNING -> "Stopped"
                    ShizukuState.NOT_INSTALLED -> "Missing"
                    ShizukuState.BINDER_DEAD -> "Dead"
                },
                color = when (shizukuState) {
                    ShizukuState.AUTHORIZED_ACTIVE -> StatusGreen
                    ShizukuState.RUNNING_NOT_AUTHORIZED -> StatusYellow
                    else -> StatusRed
                },
                onClick = onNavigateToShizuku
            )

            // Overlay Status Chip
            val canOverlay = permissionManager.canDrawOverlays()
            StatusChip(
                modifier = Modifier.weight(1f),
                title = "OVERLAY",
                status = if (canOverlay) "Granted" else "Needed",
                color = if (canOverlay) StatusGreen else StatusYellow,
                onClick = {
                    if (!canOverlay) {
                        permissionManager.getPermissionsList().firstOrNull { it.id == "overlay" }
                            ?.openSettingsAction?.invoke(context)
                    } else {
                        Toast.makeText(context, "Overlay Permission Active", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // Routing Engine Status Chip
            StatusChip(
                modifier = Modifier.weight(1f),
                title = "ROUTING",
                status = when (routingMode) {
                    AudioRoutingMode.ACOUSTIC_SPEAKER -> "Acoustic"
                    AudioRoutingMode.DIGITAL_COMMUNICATION -> "Digital"
                    AudioRoutingMode.SYSTEM_MEDIA -> "System"
                },
                color = CyanAccent,
                onClick = onNavigateToDiagnostics
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Error Banner if any
        AnimatedVisibility(visible = errorMessage != null) {
            errorMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StatusRed.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, StatusRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = StatusRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = msg, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        // Now Playing / Current Audio Controller Card
        GlassmorphicCard(
            borderGlow = isPlaying,
            modifier = Modifier.testTag("now_playing_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Square thumbnail preview
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.5.dp, if (isPlaying) CyanAccent else DarkBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentTrack?.coverArtPath != null) {
                        AsyncImage(
                            model = File(currentTrack!!.coverArtPath!!),
                            contentDescription = currentTrack?.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentTrack?.title ?: "No Audio Track Selected",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentTrack?.artist ?: "Tap 'Add Audio' to pick local file",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceVariant,
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Text(
                        text = currentTrack?.format ?: "WAV",
                        color = CyanAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Animated Visualizer
            AudioVisualizerWave(isPlaying = isPlaying)

            Spacer(modifier = Modifier.height(10.dp))

            // Seekbar and Time Display
            val totalSeconds = (durationMs / 1000).coerceAtLeast(1L)
            val currentSeconds = (currentPositionMs / 1000).coerceAtLeast(0L)
            val progressFraction = (currentPositionMs.toFloat() / durationMs.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)

            Slider(
                value = progressFraction,
                onValueChange = { frac ->
                    playerManager.seekTo((frac * durationMs).toLong())
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = CyanAccent,
                    activeTrackColor = NeonPurple,
                    inactiveTrackColor = DarkBorder
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(currentPositionMs),
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Text(
                    text = formatDuration(durationMs),
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Repeat toggle
                IconButton(
                    onClick = { playerManager.toggleRepeat() },
                    modifier = Modifier.testTag("repeat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (isRepeating) CyanAccent else TextMuted
                    )
                }

                // Previous
                IconButton(
                    onClick = { playerManager.previous() },
                    modifier = Modifier.testTag("previous_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Big Glowing Play / Pause
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(NeonPurple, Color(0xFF6B11B0))
                            )
                        )
                        .border(2.dp, CyanAccent, CircleShape)
                        .clickable {
                            AudioPlayerService.start(context)
                            playerManager.togglePlayPause()
                        }
                        .testTag("play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Next
                IconButton(
                    onClick = { playerManager.next() },
                    modifier = Modifier.testTag("next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Stop
                IconButton(
                    onClick = {
                        playerManager.stop()
                        AudioPlayerService.stop(context)
                    },
                    modifier = Modifier.testTag("stop_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = StatusRed,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Volume Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeDown,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )

                Slider(
                    value = volume,
                    onValueChange = { playerManager.setVolume(it) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .testTag("volume_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricBlue,
                        activeTrackColor = CyanAccent,
                        inactiveTrackColor = DarkBorder
                    )
                )

                Text(
                    text = "${(volume * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.End
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Floating Overlay Controller Card
        GlassmorphicCard(
            borderColor = NeonPurple
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = CyanAccent
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Floating Audio Panel",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Control audio over games and other apps",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Switch(
                    checked = isFloatingActive,
                    onCheckedChange = { enable ->
                        if (enable) {
                            if (!permissionManager.canDrawOverlays()) {
                                Toast.makeText(
                                    context,
                                    "Please grant 'Display over other apps' permission",
                                    Toast.LENGTH_LONG
                                ).show()
                                permissionManager.getPermissionsList().firstOrNull { it.id == "overlay" }
                                    ?.openSettingsAction?.invoke(context)
                            } else {
                                FloatingWindowService.start(context)
                                isFloatingActive = true
                                Toast.makeText(context, "Floating Bubble Activated", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            FloatingWindowService.stop(context)
                            isFloatingActive = false
                            Toast.makeText(context, "Floating Bubble Stopped", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NeonPurple,
                        uncheckedTrackColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.testTag("floating_switch")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            GlowButton(
                text = if (isFloatingActive) "Close Floating Panel" else "Launch Floating Panel",
                onClick = {
                    if (isFloatingActive) {
                        FloatingWindowService.stop(context)
                        isFloatingActive = false
                    } else {
                        if (!permissionManager.canDrawOverlays()) {
                            permissionManager.getPermissionsList().firstOrNull { it.id == "overlay" }
                                ?.openSettingsAction?.invoke(context)
                        } else {
                            FloatingWindowService.start(context)
                            isFloatingActive = true
                        }
                    }
                },
                style = if (isFloatingActive) ButtonStyle.DANGER else ButtonStyle.PRIMARY_NEON,
                icon = Icons.Default.Layers,
                modifier = Modifier.testTag("launch_floating_button")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Audio Routing Mode Selection Card
        GlassmorphicCard {
            Text(
                text = "Audio Routing Engine Mode",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Optimized injection configuration for device audio hardware",
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            AudioRoutingMode.entries.forEach { mode ->
                val isSelected = (mode == routingMode)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) NeonPurple.copy(alpha = 0.22f) else DarkSurfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) CyanAccent else DarkBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            playerManager.setRoutingMode(mode)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Cast,
                            contentDescription = null,
                            tint = if (isSelected) CyanAccent else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.title,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = mode.subtitle,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Manage Audio Library Quick Button
        GlowButton(
            text = "Open Audio Library (${playerManager.playlist.collectAsState().value.size} tracks)",
            onClick = onNavigateToLibrary,
            style = ButtonStyle.OUTLINE
        )
    }
}

@Composable
private fun StatusChip(
    title: String,
    status: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurfaceVariant,
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = status,
                    color = color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
