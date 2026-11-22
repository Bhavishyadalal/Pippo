package com.mochyy.mochymusic.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ReleaseChannelBadge(
    label: String,
    color: Color = Color(0xFFF5E27A),
) {
    Text(
        label,
        color = color,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp,
        modifier = Modifier
            .border(1.dp, color.copy(alpha = 0.8f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 3.dp),
    )
}
