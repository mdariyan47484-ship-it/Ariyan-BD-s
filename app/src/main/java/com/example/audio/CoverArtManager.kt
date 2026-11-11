package com.example.audio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.sin

object CoverArtManager {

    fun extractOrGenerateCover(
        context: Context,
        audioId: String,
        title: String,
        uri: Uri?,
        themeColorHex: String,
        badgeText: String = "AUDIO",
        isPreset: Boolean = false
    ): String {
        val coverFile = File(context.cacheDir, "cover_${audioId}.png")
        if (coverFile.exists() && coverFile.length() > 500) {
            return coverFile.absolutePath
        }

        // Only attempt extraction for user non-preset files and formats that support embedded art (MP3, M4A, FLAC)
        if (!isPreset && uri != null) {
            val uriStr = uri.toString().lowercase()
            val isWav = uriStr.endsWith(".wav") || uri.path?.lowercase()?.endsWith(".wav") == true
            if (!isWav) {
                var retriever: MediaMetadataRetriever? = null
                try {
                    retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, uri)
                    // Check if container has image before querying embeddedPicture
                    val picBytes = retriever.embeddedPicture
                    if (picBytes != null && picBytes.isNotEmpty()) {
                        FileOutputStream(coverFile).use { fos ->
                            fos.write(picBytes)
                        }
                        return coverFile.absolutePath
                    }
                } catch (e: Exception) {
                    Log.d("CoverArtManager", "No embedded art for $title: ${e.message}")
                } finally {
                    try {
                        retriever?.release()
                    } catch (_: Exception) {}
                }
            }
        }

        // Generate a high-resolution gaming soundboard cover art tile
        val generated = generateGamingCoverBitmap(title, themeColorHex, badgeText)
        FileOutputStream(coverFile).use { fos ->
            generated.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        return coverFile.absolutePath
    }

    fun saveCustomCover(context: Context, audioId: String, imageUri: Uri): String? {
        return try {
            val coverFile = File(context.cacheDir, "cover_${audioId}.png")
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                FileOutputStream(coverFile).use { output ->
                    input.copyTo(output)
                }
            }
            coverFile.absolutePath
        } catch (e: Exception) {
            Log.e("CoverArtManager", "Failed to save custom cover", e)
            null
        }
    }

    private fun generateGamingCoverBitmap(
        title: String,
        accentColorHex: String,
        badgeText: String
    ): Bitmap {
        val size = 256
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val accentColor = try {
            Color.parseColor(accentColorHex)
        } catch (_: Exception) {
            Color.parseColor("#B026FF")
        }

        // Dark cyberpunk gradient background
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                Color.parseColor("#150F2C"),
                Color.parseColor("#070511"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        // Neon cyber border
        val borderPaint = Paint().apply {
            color = accentColor
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }
        val rect = RectF(6f, 6f, size - 6f, size - 6f)
        canvas.drawRoundRect(rect, 24f, 24f, borderPaint)

        // Subtle inner glow ring
        val ringPaint = Paint().apply {
            color = Color.parseColor("#33FFFFFF")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, 68f, ringPaint)

        // Draw animated-looking equalizer waveform bars in center
        val barPaint = Paint().apply {
            color = accentColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val bars = 9
        val barWidth = 10f
        val startX = (size - (bars * 18f)) / 2f
        val centerY = size / 2f

        for (i in 0 until bars) {
            val h = 18f + abs(sin(i * 1.1) * 58f).toFloat()
            val left = startX + i * 18f
            val top = centerY - h / 2f
            val right = left + barWidth
            val bottom = centerY + h / 2f
            val barRect = RectF(left, top, right, bottom)
            canvas.drawRoundRect(barRect, 5f, 5f, barPaint)
        }

        // Top badge pill
        val badgeBgPaint = Paint().apply {
            color = Color.parseColor("#E6000000")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val badgeRect = RectF(20f, 18f, 110f, 44f)
        canvas.drawRoundRect(badgeRect, 10f, 10f, badgeBgPaint)

        val badgeBorderPaint = Paint().apply {
            color = accentColor
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRoundRect(badgeRect, 10f, 10f, badgeBorderPaint)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(badgeText, 65f, 35f, textPaint)

        // Bottom title snippet
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val displayTitle = if (title.length > 14) title.take(13) + "…" else title
        canvas.drawText(displayTitle, size / 2f, size - 24f, titlePaint)

        return bitmap
    }
}
