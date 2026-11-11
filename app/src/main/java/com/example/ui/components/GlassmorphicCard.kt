package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonPurple

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    borderColor: Color = DarkBorder,
    borderGlow: Boolean = false,
    cornerRadius: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderBrush = if (borderGlow) {
        Brush.linearGradient(
            colors = listOf(
                NeonPurple,
                Color(0xFF00F0FF),
                Color(0xFF0077FE)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                borderColor,
                borderColor.copy(alpha = 0.5f)
            )
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = DarkSurface.copy(alpha = 0.88f),
        border = BorderStroke(1.2.dp, borderBrush),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}
