package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonPurple
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun AudioVisualizerWave(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 24
) {
    val transition = rememberInfiniteTransition(label = "wave_anim")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(NeonPurple, CyanAccent, ElectricBlue, NeonPurple)
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        val totalWidth = size.width
        val canvasHeight = size.height
        val barWidth = (totalWidth / barCount) * 0.55f
        val spacing = (totalWidth / barCount) * 0.45f

        for (i in 0 until barCount) {
            val normalizedI = i.toFloat() / barCount
            val waveHeightFactor = if (isPlaying) {
                val wave1 = abs(sin(phase + normalizedI * 4.0)).toFloat()
                val wave2 = abs(sin(phase * 1.5 + normalizedI * 7.0)).toFloat()
                0.2f + 0.8f * ((wave1 * 0.6f) + (wave2 * 0.4f))
            } else {
                0.15f
            }

            val barHeight = (canvasHeight * waveHeightFactor).coerceIn(4.dp.toPx(), canvasHeight)
            val left = i * (barWidth + spacing)
            val top = (canvasHeight - barHeight) / 2f

            drawRoundRect(
                brush = gradientBrush,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
