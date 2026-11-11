package com.example.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuState

data class PermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val isCritical: Boolean,
    val openSettingsAction: (Context) -> Unit
)

class PermissionManager(private val context: Context) {
    private val shizukuManager = ShizukuManager.getInstance(context)

    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasModifyAudioSettingsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getPermissionsList(): List<PermissionItem> {
        return listOf(
            PermissionItem(
                id = "overlay",
                title = "SYSTEM_ALERT_WINDOW (Floating Window)",
                description = "Required to show floating audio controller bubble and panel over games and other apps.",
                isGranted = canDrawOverlays(),
                isCritical = true,
                openSettingsAction = { ctx ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${ctx.packageName}")
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    ctx.startActivity(intent)
                }
            ),
            PermissionItem(
                id = "notification",
                title = "POST_NOTIFICATIONS (Media Notification)",
                description = "Displays background media playback controls and current track status in notification shade.",
                isGranted = hasNotificationPermission(),
                isCritical = true,
                openSettingsAction = { ctx ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        ctx.startActivity(intent)
                    } else {
                        openAppDetails(ctx)
                    }
                }
            ),
            PermissionItem(
                id = "audio_settings",
                title = "MODIFY_AUDIO_SETTINGS (Audio Routing)",
                description = "Enables communication device selection, speakerphone toggling, and audio focus management.",
                isGranted = hasModifyAudioSettingsPermission(),
                isCritical = true,
                openSettingsAction = { ctx -> openAppDetails(ctx) }
            ),
            PermissionItem(
                id = "record_audio",
                title = "RECORD_AUDIO (Mic Acoustic Testing)",
                description = "Used exclusively by the Acoustic Loopback Tester to measure if speaker audio reaches the phone's microphone.",
                isGranted = hasRecordAudioPermission(),
                isCritical = false,
                openSettingsAction = { ctx -> openAppDetails(ctx) }
            ),
            PermissionItem(
                id = "shizuku",
                title = "SHIZUKU BINDER (Elevated ADB Shell)",
                description = "Provides privileged ADB shell diagnostics, audio dump inspections, and background process management.",
                isGranted = shizukuManager.shizukuState.value == ShizukuState.AUTHORIZED_ACTIVE,
                isCritical = false,
                openSettingsAction = {
                    shizukuManager.requestPermission()
                }
            )
        )
    }

    private fun openAppDetails(ctx: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${ctx.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        ctx.startActivity(intent)
    }
}
