package com.example.ui

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioPlayerManager
import com.example.data.AudioFile
import com.example.ui.components.ButtonStyle
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.GlowButton
import com.example.ui.theme.BrightCyan
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
import java.util.UUID

@Composable
fun AudioLibraryScreen(
    playerManager: AudioPlayerManager
) {
    val context = LocalContext.current
    val playlist by playerManager.playlist.collectAsState()
    val currentTrack by playerManager.currentTrack.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()

    // SAF Document Picker for audio files (MP3, WAV, AAC, M4A, OGG)
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}

                val audioFile = resolveAudioFileFromUri(context, uri)
                playerManager.addTrack(audioFile)
                Toast.makeText(context, "Added: ${audioFile.title}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("AudioLibraryScreen", "Failed to resolve selected audio", e)
                Toast.makeText(context, "Could not open audio file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .padding(16.dp)
    ) {
        // Screen Header
        Text(
            text = "AUDIO LIBRARY",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.1.sp
        )
        Text(
            text = "Select MP3, WAV, AAC, M4A, OGG from device storage",
            color = CyanAccent,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Add Local Audio Button (SAF)
        GlowButton(
            text = "Pick Audio File from Device Storage",
            onClick = {
                audioPickerLauncher.launch(
                    arrayOf(
                        "audio/*",
                        "audio/mpeg",
                        "audio/mp3",
                        "audio/wav",
                        "audio/x-wav",
                        "audio/aac",
                        "audio/mp4",
                        "audio/ogg"
                    )
                )
            },
            icon = Icons.Default.FolderOpen,
            style = ButtonStyle.PRIMARY_NEON,
            modifier = Modifier.testTag("pick_audio_button")
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tracks in Playlist (${playlist.size})",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Storage Access Framework",
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Audio Tracks List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(playlist, key = { it.id }) { track ->
                val isCurrent = (track.id == currentTrack?.id)
                val isThisPlaying = (isCurrent && isPlaying)

                AudioTrackItem(
                    track = track,
                    isCurrent = isCurrent,
                    isPlaying = isThisPlaying,
                    onPlayToggle = {
                        if (isCurrent) {
                            playerManager.togglePlayPause()
                        } else {
                            playerManager.playTrack(track)
                        }
                    },
                    onDelete = {
                        playerManager.removeTrack(track.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun AudioTrackItem(
    track: AudioFile,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isCurrent) DarkSurfaceVariant else Color(0xFF0F0B1E),
        border = BorderStroke(
            1.2.dp,
            if (isCurrent) CyanAccent else DarkBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayToggle() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play / Wave Icon Indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) NeonPurple else DarkBorder),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isPlaying -> Icons.Default.Pause
                        isCurrent -> Icons.Default.PlayArrow
                        track.isPreset -> Icons.Default.GraphicEq
                        else -> Icons.Default.Audiotrack
                    },
                    contentDescription = null,
                    tint = if (isPlaying || isCurrent) CyanAccent else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = if (isCurrent) Color.White else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.artist,
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${formatDuration(track.durationMs)}",
                        color = CyanAccent,
                        fontSize = 11.sp
                    )
                    if (track.sizeBytes > 0) {
                        val sizeMb = String.format("%.1f MB", track.sizeBytes / (1024f * 1024f))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• $sizeMb",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Format Badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (track.isPreset) NeonPurple.copy(alpha = 0.2f) else DarkBorder
            ) {
                Text(
                    text = track.format,
                    color = if (track.isPreset) CyanAccent else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Delete Track button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove Track",
                    tint = TextMuted.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun resolveAudioFileFromUri(context: Context, uri: Uri): AudioFile {
    var title = "Audio Track"
    var size = 0L

    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            if (nameIndex != -1) title = cursor.getString(nameIndex) ?: title
            if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
        }
    }

    var durationMs = 0L
    var artist = "Local Device File"
    try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        if (durStr != null) durationMs = durStr.toLongOrNull() ?: 0L
        val artStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        if (artStr != null) artist = artStr
        retriever.release()
    } catch (_: Exception) {}

    val format = title.substringAfterLast('.', "Audio").uppercase()

    return AudioFile(
        id = UUID.randomUUID().toString(),
        title = title.substringBeforeLast('.'),
        artist = artist,
        uri = uri,
        durationMs = durationMs,
        sizeBytes = size,
        format = format,
        isPreset = false
    )
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
