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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.audio.AudioPlayerManager
import com.example.audio.CoverArtManager
import com.example.data.AudioFile
import com.example.ui.components.AudioVisualizerWave
import com.example.ui.components.ButtonStyle
import com.example.ui.components.GlowButton
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import java.util.UUID

@Composable
fun AudioLibraryScreen(
    playerManager: AudioPlayerManager
) {
    val context = LocalContext.current
    val playlist by playerManager.playlist.collectAsState()
    val currentTrack by playerManager.currentTrack.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()

    var isGridView by remember { mutableStateOf(true) }
    var selectedTrackForCustomCover by remember { mutableStateOf<String?>(null) }

    // Multi-file picker using SAF OpenMultipleDocuments
    val multiAudioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val resolvedList = mutableListOf<AudioFile>()
            for (uri in uris) {
                try {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: Exception) {}

                    val audioFile = resolveAudioFileWithCover(context, uri)
                    resolvedList.add(audioFile)
                } catch (e: Exception) {
                    Log.e("AudioLibraryScreen", "Error resolving uri $uri", e)
                }
            }
            if (resolvedList.isNotEmpty()) {
                playerManager.addTracks(resolvedList)
                Toast.makeText(context, "Added ${resolvedList.size} tracks to soundboard", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Photo picker for setting custom cover art
    val customCoverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { imageUri: Uri? ->
        val trackId = selectedTrackForCustomCover
        if (imageUri != null && trackId != null) {
            val savedPath = CoverArtManager.saveCustomCover(context, trackId, imageUri)
            if (savedPath != null) {
                playerManager.updateTrackCover(trackId, savedPath)
                Toast.makeText(context, "Cover art updated!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GAMING SOUNDBOARD",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.1.sp
                )
                Text(
                    text = "Audio Library & Thumbnail Grid (${playlist.size} Tracks)",
                    color = CyanAccent,
                    fontSize = 12.sp
                )
            }

            // View toggle (Grid vs List)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkSurfaceVariant,
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Row {
                    IconButton(
                        onClick = { isGridView = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Grid View",
                            tint = if (isGridView) CyanAccent else TextMuted
                        )
                    }
                    IconButton(
                        onClick = { isGridView = false },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewList,
                            contentDescription = "List View",
                            tint = if (!isGridView) CyanAccent else TextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Multiple Audio File Picker Button
        GlowButton(
            text = "Import Audio Files from Storage (Multi-Select)",
            onClick = {
                multiAudioPickerLauncher.launch(
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

        // Active Playing Indicator Banner
        currentTrack?.let { active ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF140F28),
                border = BorderStroke(1.dp, if (isPlaying) CyanAccent else DarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBorder)
                    ) {
                        active.coverArtPath?.let { path ->
                            AsyncImage(
                                model = File(path),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = active.title,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isPlaying) "NOW PLAYING / INJECTING" else "PAUSED",
                            color = if (isPlaying) StatusGreen else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = { playerManager.togglePlayPause() }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Toggle Play",
                            tint = CyanAccent
                        )
                    }
                }
            }
        }

        // Soundboard Grid or List
        if (isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(playlist, key = { it.id }) { track ->
                    val isCurrent = (track.id == currentTrack?.id)
                    val isThisPlaying = (isCurrent && isPlaying)

                    SoundboardGridTile(
                        track = track,
                        isCurrent = isCurrent,
                        isPlaying = isThisPlaying,
                        onClick = { playerManager.playOrToggleTrack(track) },
                        onPickCover = {
                            selectedTrackForCustomCover = track.id
                            customCoverPickerLauncher.launch(arrayOf("image/*"))
                        },
                        onDelete = { playerManager.removeTrack(track.id) }
                    )
                }
            }
        } else {
            // Detailed List View
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(playlist, key = { it.id }) { track ->
                    val isCurrent = (track.id == currentTrack?.id)
                    val isThisPlaying = (isCurrent && isPlaying)

                    SoundboardListItem(
                        track = track,
                        isCurrent = isCurrent,
                        isPlaying = isThisPlaying,
                        onClick = { playerManager.playOrToggleTrack(track) },
                        onPickCover = {
                            selectedTrackForCustomCover = track.id
                            customCoverPickerLauncher.launch(arrayOf("image/*"))
                        },
                        onDelete = { playerManager.removeTrack(track.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SoundboardGridTile(
    track: AudioFile,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPickCover: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = if (isCurrent) {
        if (isPlaying) CyanAccent else NeonPurple
    } else {
        DarkBorder
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF130E26),
        border = BorderStroke(if (isCurrent) 2.dp else 1.2.dp, borderColor),
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column {
            // Square Thumbnail Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(DarkSurfaceVariant)
            ) {
                if (track.coverArtPath != null) {
                    AsyncImage(
                        model = File(track.coverArtPath),
                        contentDescription = track.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Dark gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xCC070511)),
                                startY = 60f
                            )
                        )
                )

                // Playing Pulsing Waveform in Corner
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xE6000000))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "PLAYING",
                            color = StatusGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Center Play/Pause Badge
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPlaying) Brush.radialGradient(listOf(NeonPurple, Color(0xFF6B11B0)))
                            else Brush.radialGradient(listOf(Color(0xCC000000), Color(0x99000000)))
                        )
                        .border(1.5.dp, if (isPlaying) CyanAccent else Color.White.copy(alpha = 0.5f), CircleShape)
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Action Buttons Overlay (Change Cover & Delete)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xCC000000))
                            .clickable { onPickCover() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Change Cover Art",
                            tint = CyanAccent,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xCC000000))
                            .clickable { onDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Track",
                            tint = StatusRed,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Tile Caption
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = track.title,
                    color = if (isCurrent) Color.White else TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = track.format,
                        color = CyanAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatDuration(track.durationMs),
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SoundboardListItem(
    track: AudioFile,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPickCover: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isCurrent) DarkSurfaceVariant else Color(0xFF110C24),
        border = BorderStroke(1.dp, if (isCurrent) CyanAccent else DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkBorder)
            ) {
                if (track.coverArtPath != null) {
                    AsyncImage(
                        model = File(track.coverArtPath),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xB3000000))
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.artist} • ${formatDuration(track.durationMs)} • ${track.format}",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = onPickCover) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "Set Cover Art",
                    tint = CyanAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove Track",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun resolveAudioFileWithCover(context: Context, uri: Uri): AudioFile {
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
    val id = UUID.randomUUID().toString()

    // Randomize vibrant neon color for the tile
    val colors = listOf("#00F0FF", "#0077FE", "#B026FF", "#FFB300", "#00E676", "#FF1744", "#EC4899")
    val chosenColor = colors.random()

    val coverPath = CoverArtManager.extractOrGenerateCover(
        context = context,
        audioId = id,
        title = title.substringBeforeLast('.'),
        uri = uri,
        themeColorHex = chosenColor,
        badgeText = format
    )

    return AudioFile(
        id = id,
        title = title.substringBeforeLast('.'),
        artist = artist,
        uri = uri,
        durationMs = durationMs,
        sizeBytes = size,
        format = format,
        isPreset = false,
        coverArtPath = coverPath,
        themeColorHex = chosenColor
    )
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
