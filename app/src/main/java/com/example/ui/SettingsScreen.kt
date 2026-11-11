package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppSettings
import com.example.permissions.PermissionItem
import com.example.permissions.PermissionManager
import com.example.shizuku.ShizukuManager
import com.example.ui.components.GlassmorphicCard
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    appSettings: AppSettings,
    permissionManager: PermissionManager,
    shizukuManager: ShizukuManager
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val bubbleSize by appSettings.bubbleSize.collectAsState()
    val bubbleAlpha by appSettings.bubbleAlpha.collectAsState()
    val defaultVolume by appSettings.defaultVolume.collectAsState()
    val autoStartFloating by appSettings.autoStartFloating.collectAsState()
    val permissions = remember(permissionManager) { permissionManager.getPermissionsList() }

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
            text = "SETTINGS & SYSTEM",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.1.sp
        )
        Text(
            text = "Overlay Customization & Permissions",
            color = CyanAccent,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        // Floating Overlay Configuration Card
        GlassmorphicCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Widgets, contentDescription = null, tint = CyanAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Floating Overlay Appearance",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bubble Size Slider
            Text(
                text = "Bubble Diameter: ${bubbleSize}dp",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Slider(
                value = bubbleSize.toFloat(),
                onValueChange = { appSettings.setBubbleSize(it.toInt()) },
                valueRange = 46f..74f,
                colors = SliderDefaults.colors(
                    thumbColor = CyanAccent,
                    activeTrackColor = NeonPurple,
                    inactiveTrackColor = DarkBorder
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bubble Transparency Slider
            Text(
                text = "Bubble Opacity: ${(bubbleAlpha * 100).toInt()}%",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Slider(
                value = bubbleAlpha,
                onValueChange = { appSettings.setBubbleAlpha(it) },
                valueRange = 0.5f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = ElectricBlue,
                    activeTrackColor = CyanAccent,
                    inactiveTrackColor = DarkBorder
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Auto-Start Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-Start Floating Bubble",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Automatically open floating controller on app launch",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Switch(
                    checked = autoStartFloating,
                    onCheckedChange = { appSettings.setAutoStartFloating(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NeonPurple,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Default Audio Playback Settings Card
        GlassmorphicCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = CyanAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Default Audio Profile",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Startup Volume Level: $defaultVolume%",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Slider(
                value = defaultVolume.toFloat(),
                onValueChange = { appSettings.setDefaultVolume(it.toInt()) },
                valueRange = 10f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = CyanAccent,
                    activeTrackColor = NeonPurple,
                    inactiveTrackColor = DarkBorder
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Permissions Status Checklist Card
        GlassmorphicCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = CyanAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Android System Permissions",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Tap on any permission to configure in Android system settings",
                color = TextMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            permissions.forEach { item ->
                PermissionItemRow(item = item, onClick = { item.openSettingsAction(context) })
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // About ARIYAN A9X BD OFFICIAL Card
        GlassmorphicCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = CyanAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "About ARIYAN A9X BD OFFICIAL",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Application: ARIYAN A9X BD OFFICIAL\n" +
                        "Version: 1.0 (Production Release)\n" +
                        "Engine: Voice Float Injector & Android Audio Routing System\n" +
                        "Shizuku: Integrated (v13.1.5 Official API)\n" +
                        "Target: Gaming, In-Call Voice Routing, and Floating Audio Control\n" +
                        "Developed for: Advanced Non-Root & ADB Shell Android Environments",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun PermissionItemRow(
    item: PermissionItem,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurfaceVariant,
        border = BorderStroke(1.dp, if (item.isGranted) StatusGreen.copy(alpha = 0.4f) else DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (item.isGranted) StatusGreen else StatusRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isGranted) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.description,
                    color = TextMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (item.isGranted) StatusGreen.copy(alpha = 0.2f) else NeonPurple.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, if (item.isGranted) StatusGreen else NeonPurple)
            ) {
                Text(
                    text = if (item.isGranted) "Granted" else "Open Settings",
                    color = if (item.isGranted) StatusGreen else CyanAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
