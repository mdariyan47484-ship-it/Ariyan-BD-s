package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonPurple

enum class ButtonStyle {
    PRIMARY_NEON,
    CYAN_ACCENT,
    OUTLINE,
    DANGER
}

@Composable
fun GlowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: ButtonStyle = ButtonStyle.PRIMARY_NEON,
    height: Dp = 50.dp,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(14.dp)

    val backgroundBrush = when (style) {
        ButtonStyle.PRIMARY_NEON -> Brush.horizontalGradient(
            colors = listOf(NeonPurple, Color(0xFF7928CA))
        )
        ButtonStyle.CYAN_ACCENT -> Brush.horizontalGradient(
            colors = listOf(CyanAccent, ElectricBlue)
        )
        ButtonStyle.DANGER -> Brush.horizontalGradient(
            colors = listOf(Color(0xFFFF1744), Color(0xFFD50000))
        )
        ButtonStyle.OUTLINE -> Brush.horizontalGradient(
            colors = listOf(Color(0xFF1B1533), Color(0xFF1B1533))
        )
    }

    val contentColor = when (style) {
        ButtonStyle.CYAN_ACCENT -> Color(0xFF07060E)
        else -> Color.White
    }

    val border = if (style == ButtonStyle.OUTLINE) {
        BorderStroke(1.2.dp, Brush.horizontalGradient(listOf(NeonPurple, CyanAccent)))
    } else {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .clickable(
                enabled = enabled,
                onClick = onClick
            ),
        shape = shape,
        color = Color.Transparent,
        border = border,
        tonalElevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .background(if (enabled) backgroundBrush else Brush.horizontalGradient(listOf(Color.DarkGray, Color.Gray))),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = contentColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
