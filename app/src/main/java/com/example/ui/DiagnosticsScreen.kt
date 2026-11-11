package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.AudioDiagnostics
import com.example.audio.AudioPlayerManager
import com.example.audio.DeviceAudioInfo
import com.example.shizuku.ShizukuManager
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

@Composable
fun DiagnosticsScreen(
    playerManager: AudioPlayerManager,
    shizukuManager: ShizukuManager
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var deviceInfo by remember { mutableStateOf(AudioDiagnostics.inspect(context)) }
    val micState by playerManager.micTester.meterState.collectAsState()
    val routingMode by playerManager.routingEngine.currentMode.collectAsState()
    val routingStatus by playerManager.routingEngine.routingStatusMessage.collectAsState()
    val shizukuState by shizukuManager.shizukuState.collectAsState()

    // Permission launcher for RECORD_AUDIO
    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            playerManager.micTester.startTesting()
        } else {
            Toast.makeText(context, "Microphone permission required for acoustic test", Toast.LENGTH_SHORT).show()
        }
    }

    val animatedAmplitude by animateFloatAsState(
        targetValue = micState.amplitudePercent,
        label = "amplitude"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AUDIO DIAGNOSTICS",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.1.sp
                )
                Text(
                    text = "Acoustic Loopback & Hardware Inspection",
                    color = CyanAccent,
                    fontSize = 13.sp
                )
            }

            IconButton(
                onClick = { deviceInfo = AudioDiagnostics.inspect(context) },
                modifier = Modifier.testTag("refresh_diagnostics")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = CyanAccent)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Acoustic Microphone Loopback Tester Card
        GlassmorphicCard(
            borderGlow = micState.isRecording,
            borderColor = if (micState.isRecording) CyanAccent else DarkBorder
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (micState.isRecording) StatusGreen else TextMuted)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Acoustic Microphone Tester",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (micState.isRecording) StatusGreen.copy(alpha = 0.2f) else DarkBorder
                ) {
                    Text(
                        text = if (micState.isRecording) "LIVE RECORDING" else "STANDBY",
                        color = if (micState.isRecording) StatusGreen else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tests whether audio played through the loudspeaker is picked up by the phone's microphone for in-game voice chat.",
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Real-time Audio Level Bar
            Text(
                text = "Live Microphone Level: ${(animatedAmplitude * 100).toInt()}% (${micState.decibels.toInt()} dB)",
                color = if (animatedAmplitude > 0.1f) CyanAccent else TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { animatedAmplitude },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp)),
                color = if (animatedAmplitude > 0.4f) NeonPurple else CyanAccent,
                trackColor = DarkSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = micState.statusText,
                color = if (micState.amplitudePercent > 0.08f) StatusGreen else TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Start / Stop Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlowButton(
                    text = if (micState.isRecording) "Stop Mic Test" else "Start Mic Test",
                    onClick = {
                        if (micState.isRecording) {
                            playerManager.micTester.stopTesting()
                        } else {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                playerManager.micTester.startTesting()
                            } else {
                                recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    icon = if (micState.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    style = if (micState.isRecording) ButtonStyle.DANGER else ButtonStyle.CYAN_ACCENT,
                    modifier = Modifier.weight(1f).testTag("mic_test_button")
                )

                GlowButton(
                    text = "Play Test Tone",
                    onClick = {
                        playerManager.playlist.value.firstOrNull()?.let {
                            playerManager.playTrack(it)
                        }
                    },
                    icon = Icons.Default.PlayArrow,
                    style = ButtonStyle.OUTLINE,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Active Routing Status Card
        GlassmorphicCard {
            Text(
                text = "Active Audio Routing",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Mode: ${routingMode.title}",
                color = CyanAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = routingStatus,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Hardware Inspection Card
        GlassmorphicCard {
            Text(
                text = "Device Hardware Capabilities",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            DiagnosticInfoRow(label = "Device Model", value = "${deviceInfo.manufacturer} ${deviceInfo.model}")
            DiagnosticInfoRow(label = "Android Version", value = "Android ${deviceInfo.androidVersion} (API ${deviceInfo.apiLevel})")
            DiagnosticInfoRow(label = "Built-in Speaker", value = if (deviceInfo.hasSpeaker) "Available (Optimal)" else "Not Detected", isSuccess = deviceInfo.hasSpeaker)
            DiagnosticInfoRow(label = "Built-in Microphone", value = if (deviceInfo.hasMicrophone) "Available" else "Not Detected", isSuccess = deviceInfo.hasMicrophone)
            DiagnosticInfoRow(label = "Active Comm Route", value = deviceInfo.currentCommunicationDevice ?: "Standard Media Output")
            DiagnosticInfoRow(label = "Playback Capture API", value = if (deviceInfo.isPlaybackCaptureSupported) "Supported (API 29+)" else "Unavailable", isSuccess = deviceInfo.isPlaybackCaptureSupported)
            DiagnosticInfoRow(label = "Shizuku Status", value = shizukuState.name, isSuccess = (shizukuState == com.example.shizuku.ShizukuState.AUTHORIZED_ACTIVE))

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Detected Output Endpoints (${deviceInfo.outputDevices.size}):",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            deviceInfo.outputDevices.forEach { dev ->
                Text(
                    text = "• $dev",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Detected Input Endpoints (${deviceInfo.inputDevices.size}):",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            deviceInfo.inputDevices.forEach { dev ->
                Text(
                    text = "• $dev",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Technical Notice / Android Audio Framework Limitations
        GlassmorphicCard(
            borderColor = DarkBorder
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = CyanAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Audio Architecture & Technical Insights",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "• Direct internal digital injection into another 3rd-party game/app's private AudioRecord stream is restricted by Android OS SELinux policy (requires privileged system signature CAPTURE_AUDIO_OUTPUT).\n\n" +
                        "• ARIYAN A9X Acoustic Mode bypasses this limitation cleanly: high-fidelity audio is played through the device's loudspeaker, allowing the physical microphone to naturally capture and transmit it in real time during games (Free Fire, PUBG, Discord) with zero risk of bans.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun DiagnosticInfoRow(
    label: String,
    value: String,
    isSuccess: Boolean? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextMuted, fontSize = 12.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                color = when (isSuccess) {
                    true -> StatusGreen
                    false -> StatusRed
                    null -> Color.White
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            if (isSuccess != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (isSuccess) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (isSuccess) StatusGreen else StatusRed,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
