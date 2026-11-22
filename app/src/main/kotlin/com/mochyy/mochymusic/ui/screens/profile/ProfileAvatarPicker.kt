package com.mochyy.mochymusic.ui.screens.profile

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mochyy.mochymusic.R

@Composable
internal fun ProfileAvatarPicker(
    photo: Any?,
    onPick: (Uri) -> Unit,
    onUpload: () -> Unit,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val selected = remember(context, photo) { ProfileAvatars.selectedIndex(context, photo) }
    val order = ProfileAvatars.displayOrder
    val firstVisible = selected?.let(order::indexOf)?.let { (it - 1).coerceAtLeast(0) } ?: 0
    val scroll = rememberLazyListState(initialFirstVisibleItemIndex = firstVisible)
    val accent = Color(0xFFF6CBDC)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0xFF29262C)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.pippo_avatar_picker_title),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onUpload, enabled = enabled) {
                Text(stringResource(R.string.pippo_avatar_upload), color = if (enabled) accent else Color(0xFFAF9BA4))
            }
        }
        LazyRow(
            state = scroll,
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(order, key = { it }) { index ->
                val drawable = ProfileAvatars.pictures[index]
                val active = selected == index
                val label = ProfileAvatars.label(index)?.let { stringResource(it) }
                    ?: stringResource(R.string.pippo_avatar_choice, index + 1)
                Box(
                    Modifier.size(62.dp)
                        .clip(CircleShape)
                        .selectable(selected = active, enabled = enabled, role = Role.RadioButton) {
                            onPick(ProfileAvatars.uri(context, index))
                        }
                        .semantics { contentDescription = label }
                        .border(if (active) 2.dp else 1.dp, if (active) accent else Color(0xFF5D525C), CircleShape)
                        .padding(4.dp),
                ) {
                    AsyncImage(
                        model = drawable,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize().clip(CircleShape),
                    )
                    if (active) {
                        Box(
                            Modifier.align(Alignment.BottomEnd).padding(end = 2.dp, bottom = 2.dp).size(19.dp)
                                .background(accent, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(painterResource(R.drawable.check), null, Modifier.size(13.dp), tint = Color(0xFF392731))
                        }
                    }
                }
            }
        }
    }
}
