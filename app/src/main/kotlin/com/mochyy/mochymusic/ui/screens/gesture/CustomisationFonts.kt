package com.mochyy.mochymusic.ui.screens.gesture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.constants.AppFontKey
import com.mochyy.mochymusic.ui.theme.AppFont
import com.mochyy.mochymusic.utils.rememberEnumPreference

@Composable
internal fun AppFontSection() {
    val (selected, select) = rememberEnumPreference(AppFontKey, AppFont.DEFAULT)
    NavigationCard {
        Text(stringResource(R.string.app_font_title), color = Color.White, style = CardLabelStyle)
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 84.dp)
                .background(
                    Brush.linearGradient(listOf(Color(0xFF33283B), Color(0xFF211F29))),
                    RoundedCornerShape(16.dp),
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.app_font_sample),
                color = Color(0xFFF8EAF4),
                fontFamily = selected.family,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                textAlign = TextAlign.Center,
            )
        }
        NavigationChoices(
            options = AppFont.entries,
            selected = selected,
            label = { it.label },
            swatch = { font ->
                Text(
                    "Aa",
                    modifier = Modifier.width(92.dp),
                    color = Color(0xFFF4DBE9),
                    fontFamily = font.family,
                    fontSize = 25.sp,
                    lineHeight = 31.sp,
                    textAlign = TextAlign.Center,
                )
            },
            onSelect = select,
        )
    }
}
