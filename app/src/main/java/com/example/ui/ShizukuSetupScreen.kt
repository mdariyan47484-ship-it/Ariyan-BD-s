package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shizuku.ShellManager
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuState
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
import kotlinx.coroutines.launch

@Composable
fun ShizukuSetupScreen(
    shizukuManager: ShizukuManager,
    shellManager: ShellManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current

    val shizukuState by shizukuManager.shizukuState.collectAsState()
    val statusMessage by shizukuManager.statusMessage.collectAsState()
    val shizukuUid by shizukuManager.shizukuUid.collectAsState()

    val isShellExecuting by shellManager.isExecuting.collectAsState()
    val lastShellResult by shellManager.lastResult.collectAsState()

    var customCommand by remember { mutableStateOf("dumpsys audio | grep -E 'Devices:|Mode:'") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Screen Header
        Text(
            text = "SHIZUKU INTEGRATION",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.1.sp
        )
        Text(
            text = "Elevated ADB Shell & Permission System",
            color = CyanAccent,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        // Shizuku Connection Status Dashboard Card
        GlassmorphicCard(
            borderGlow = (shizukuState == ShizukuState.AUTHORIZED_ACTIVE)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                when (shizukuState) {
                                    ShizukuState.AUTHORIZED_ACTIVE -> StatusGreen
                                    ShizukuState.RUNNING_NOT_AUTHORIZED -> StatusYellow
                                    else -> StatusRed
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "STATUS: ${shizukuState.name}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { shizukuManager.refreshStatus() },
                    modifier = Modifier.testTag("refresh_shizuku_status")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Status",
                        tint = CyanAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = statusMessage,
                color = TextSecondary,
                fontSize = 12.sp
            )

            if (shizukuUid != -1) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Active Process UID: $shizukuUid (${if (shizukuUid == 0) "ROOT ACCESS" else "ADB SHELL PRIVILEGE"})",
                    color = CyanAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action buttons according to state
            when (shizukuState) {
                ShizukuState.NOT_INSTALLED -> {
                    GlowButton(
                        text = "Open Shizuku Official Website",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/"))
                            context.startActivity(intent)
                        },
                        icon = Icons.Default.OpenInNew,
                        style = ButtonStyle.CYAN_ACCENT
                    )
                }
                ShizukuState.INSTALLED_NOT_RUNNING -> {
                    GlowButton(
                        text = "Open Shizuku App",
                        onClick = {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(ShizukuManager.SHIZUKU_PACKAGE_NAME)
                            if (launchIntent != null) {
                                context.startActivity(launchIntent)
                            } else {
                                Toast.makeText(context, "Cannot open Shizuku", Toast.LENGTH_SHORT).show()
                            }
                        },
                        icon = Icons.Default.PlayArrow,
                        style = ButtonStyle.PRIMARY_NEON
                    )
                }
                ShizukuState.RUNNING_NOT_AUTHORIZED -> {
                    GlowButton(
                        text = "Request Shizuku Permission",
                        onClick = { shizukuManager.requestPermission() },
                        icon = Icons.Default.Security,
                        style = ButtonStyle.CYAN_ACCENT,
                        modifier = Modifier.testTag("request_shizuku_permission")
                    )
                }
                ShizukuState.AUTHORIZED_ACTIVE -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StatusGreen.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, StatusGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Binder Connected & Authorized. Ready for Shell Diagnostics.",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                ShizukuState.BINDER_DEAD -> {
                    GlowButton(
                        text = "Reconnect / Refresh Shizuku",
                        onClick = { shizukuManager.refreshStatus() },
                        icon = Icons.Default.Refresh,
                        style = ButtonStyle.DANGER
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step-by-Step Wireless Debugging & Pairing Guide Card
        GlassmorphicCard {
            Text(
                text = "Interactive Shizuku Setup Guide",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Complete 5-step non-root activation via Android Wireless Debugging",
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            SetupStepItem(
                stepNum = 1,
                title = "Install Shizuku",
                desc = "Download Shizuku from Google Play Store or GitHub (rikka.app).",
                isCompleted = shizukuManager.isInstalled(),
                actionButtonText = "Install Link",
                onAction = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/"))
                    context.startActivity(intent)
                }
            )

            SetupStepItem(
                stepNum = 2,
                title = "Enable Developer Options",
                desc = "Go to Settings > About Phone > Tap 'Build Number' 7 times until Developer Mode is unlocked.",
                isCompleted = false,
                actionButtonText = "Open About Phone",
                onAction = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
                    } catch (_: Exception) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                }
            )

            SetupStepItem(
                stepNum = 3,
                title = "Enable Wireless Debugging",
                desc = "Connect phone to Wi-Fi. In Developer Options, enable 'Wireless Debugging' and allow permissions.",
                isCompleted = false,
                actionButtonText = "Open Dev Options",
                onAction = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                    } catch (_: Exception) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                }
            )

            SetupStepItem(
                stepNum = 4,
                title = "Pair & Start in Shizuku",
                desc = "Open Shizuku app > Choose 'Pairing' > Tap 'Pair device with pairing code' in Developer Options > Enter code in Shizuku notification > Tap 'Start'.",
                isCompleted = (shizukuState == ShizukuState.RUNNING_NOT_AUTHORIZED || shizukuState == ShizukuState.AUTHORIZED_ACTIVE),
                actionButtonText = "Open Shizuku",
                onAction = {
                    val intent = context.packageManager.getLaunchIntentForPackage(ShizukuManager.SHIZUKU_PACKAGE_NAME)
                    if (intent != null) context.startActivity(intent)
                    else Toast.makeText(context, "Shizuku not installed", Toast.LENGTH_SHORT).show()
                }
            )

            SetupStepItem(
                stepNum = 5,
                title = "Grant VOICE FLOAT Permission",
                desc = "Return here and authorize ARIYAN A9X to access the Shizuku Binder interface.",
                isCompleted = (shizukuState == ShizukuState.AUTHORIZED_ACTIVE),
                actionButtonText = "Grant Permission",
                onAction = { shizukuManager.requestPermission() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Shell Process Diagnostics Runner Card
        GlassmorphicCard(
            borderColor = ElectricBlue
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Terminal, contentDescription = null, tint = CyanAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Elevated Shell Diagnostics",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Execute authorized audio inspect commands using Shizuku Remote Process",
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Diagnostic Buttons
            Text(
                text = "Preset Diagnostic Commands:",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PresetCommandChip(
                    label = "Audio State",
                    cmd = "dumpsys audio | grep -E 'Devices:|Mode:'",
                    onSelect = { customCommand = it }
                )
                PresetCommandChip(
                    label = "Volume Levels",
                    cmd = "media volume --get",
                    onSelect = { customCommand = it }
                )
                PresetCommandChip(
                    label = "Device Info",
                    cmd = "getprop ro.product.model",
                    onSelect = { customCommand = it }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = customCommand,
                onValueChange = { customCommand = it },
                label = { Text("Shell Command") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("shell_command_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanAccent,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            GlowButton(
                text = if (isShellExecuting) "Running..." else "Execute Shell Command",
                onClick = {
                    scope.launch {
                        shellManager.executeCommand(customCommand)
                    }
                },
                enabled = !isShellExecuting && customCommand.isNotBlank(),
                icon = Icons.Default.Code,
                style = ButtonStyle.PRIMARY_NEON,
                modifier = Modifier.testTag("execute_shell_button")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Console output display
            Text(
                text = "Terminal Output Log:",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF040308),
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (isShellExecuting) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = CyanAccent,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Running process on device...", color = TextSecondary, fontSize = 12.sp)
                        }
                    } else if (lastShellResult != null) {
                        val res = lastShellResult!!
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Exit Code: ${res.exitCode} (${if (res.isSuccess) "SUCCESS" else "FAILED"})",
                                color = if (res.isSuccess) StatusGreen else StatusRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${res.executionTimeMs}ms • ${if (res.usedShizuku) "Shizuku Shell" else "Local Shell"}",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        val outputText = res.stdout.ifBlank { res.stderr.ifBlank { "(No output)" } }
                        Text(
                            text = outputText,
                            color = if (res.isSuccess) Color(0xFF80FFA0) else Color(0xFFFF8080),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 15.sp
                        )
                    } else {
                        Text(
                            text = "$ Shizuku Shell ready. Tap 'Execute Shell Command' to test.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupStepItem(
    stepNum: Int,
    title: String,
    desc: String,
    isCompleted: Boolean,
    actionButtonText: String,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurfaceVariant,
        border = BorderStroke(1.dp, if (isCompleted) StatusGreen.copy(alpha = 0.5f) else DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isCompleted) StatusGreen else NeonPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$stepNum",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                if (isCompleted) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = StatusGreen.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "DONE",
                            color = StatusGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NeonPurple.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, NeonPurple),
                        modifier = Modifier.clickable { onAction() }
                    ) {
                        Text(
                            text = actionButtonText,
                            color = CyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(start = 34.dp)
            )
        }
    }
}

@Composable
private fun PresetCommandChip(
    label: String,
    cmd: String,
    onSelect: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DarkSurfaceVariant,
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.clickable { onSelect(cmd) }
    ) {
        Text(
            text = label,
            color = CyanAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
