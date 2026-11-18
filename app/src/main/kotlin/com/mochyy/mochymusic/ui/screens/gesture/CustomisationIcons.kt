package com.mochyy.mochymusic.ui.screens.gesture

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.ui.component.TopToaster
import com.mochyy.mochymusic.ui.screens.audiolab.LabColors
import com.mochyy.mochymusic.utils.AppIcon
import com.mochyy.mochymusic.utils.AppIconFamily
import com.mochyy.mochymusic.utils.AppIcons
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun IconsSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var picked by remember { mutableStateOf(AppIcons.current(context)) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var applying by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(22.dp)

    fun pick(icon: AppIcon) {
        if (applying || icon == picked) return
        applying = true
        scope.launch {
            try {
                withContext(Dispatchers.IO) { AppIcons.apply(context.applicationContext, icon) }
                picked = icon
                AppIcons.syncSplash(context)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                TopToaster.error(context.getString(R.string.pippo_icon_apply_failed))
            } finally {
                applying = false
            }
        }
    }

    Column(Modifier.fillMaxWidth().clip(shape).background(LabColors.Card).padding(20.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = picked.foreground, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(68.dp).clip(RoundedCornerShape(17.dp)),
            )
            Column {
                Text(stringResource(R.string.customise_icons), color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(picked.label), color = Color.White.copy(alpha = .65f), style = MaterialTheme.typography.bodySmall)
                Text(if (expanded) "−" else "+", color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
        }
        AnimatedVisibility(expanded) {
            Column {
                AppIconFamily.entries.forEach { family ->
                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(family.title), color = Color.White.copy(alpha = .7f), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(AppIcon.entries.filter { it.family == family }, key = { it.name }) { icon ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AsyncImage(
                                    model = icon.foreground, contentDescription = stringResource(icon.label),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(18.dp))
                                        .border(if (icon == picked) 2.dp else 1.dp, Color.White.copy(alpha = if (icon == picked) 1f else .2f), RoundedCornerShape(18.dp))
                                        .semantics { selected = icon == picked }
                                        .clickable(enabled = !applying) { pick(icon) },
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(stringResource(icon.label).substringBefore(" ·"), color = Color.White, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
