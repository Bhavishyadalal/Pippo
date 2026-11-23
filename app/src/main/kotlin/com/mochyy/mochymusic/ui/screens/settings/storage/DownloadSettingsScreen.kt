package com.mochyy.mochymusic.ui.screens.settings.storage

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.navigation.NavController
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.constants.DownloadFolderKey
import com.mochyy.mochymusic.constants.DownloadProviderKey
import com.mochyy.mochymusic.constants.DownloadQuality
import com.mochyy.mochymusic.constants.DownloadQualityKey
import com.mochyy.mochymusic.module.AccountState
import com.mochyy.mochymusic.module.ModuleFeature
import com.mochyy.mochymusic.modules.SourceModules
import com.mochyy.mochymusic.playback.DownloadFolderWorker
import com.mochyy.mochymusic.ui.component.LocalMenuState
import com.mochyy.mochymusic.ui.component.RemoteFocus
import com.mochyy.mochymusic.ui.component.pressBounce
import com.mochyy.mochymusic.ui.screens.audiolab.AudioLabPage
import com.mochyy.mochymusic.ui.screens.audiolab.LabCard
import com.mochyy.mochymusic.ui.screens.audiolab.LabColors
import com.mochyy.mochymusic.ui.screens.audiolab.LabSelect
import com.mochyy.mochymusic.ui.screens.audiolab.LabTitle
import com.mochyy.mochymusic.ui.screens.audiolab.downloadDetailRes
import com.mochyy.mochymusic.ui.screens.audiolab.downloadTitleRes
import com.mochyy.mochymusic.ui.theme.InterFontFamily
import com.mochyy.mochymusic.ui.theme.PippoColors
import com.mochyy.mochymusic.utils.rememberEnumPreference
import com.mochyy.mochymusic.utils.rememberPreference
import com.mochyy.mochymusic.viewmodels.ModulesViewModel
import com.mochyy.mochymusic.viewmodels.StorageViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val DownloadSettingsRoute = "download_settings"

private data class FolderDetails(val uri: Uri, val name: String?, val writable: Boolean)

@Composable
fun DownloadSettingsScreen(
    navController: NavController,
    modules: ModulesViewModel = hiltViewModel(),
    storage: StorageViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val resolver = context.contentResolver
    val (quality, setQuality) = rememberEnumPreference(DownloadQualityKey, DownloadQuality.HIGH)
    val (provider, setProvider) = rememberPreference(DownloadProviderKey, "")
    val (folder, setFolder) = rememberPreference(DownloadFolderKey, "")
    val entries by modules.entries.collectAsState()
    val saved by storage.downloads.collectAsState()
    val providerFailures by storage.downloadProviderFailures.collectAsState()
    val menu = LocalMenuState.current
    val automatic = stringResource(R.string.download_settings_automatic)

    val eligible = buildList {
        add("")
        if (quality != DownloadQuality.LOSSLESS) add(SourceModules.YouTubeId)
        entries.filter { entry ->
            entry.enabled && entry.adds(ModuleFeature.AudioSource) &&
                (entry.module.signIn == null || entry.account is AccountState.SignedIn)
        }.forEach { add(it.id) }
    }
    val selectionUnavailable = provider !in eligible
    val providers = if (selectionUnavailable) eligible + provider else eligible
    val providerLabels = providers.map { id ->
        when {
            id.isEmpty() -> automatic
            id !in eligible -> stringResource(R.string.download_settings_provider_unavailable, modules.nameOf(id))
            else -> modules.nameOf(id)
        }
    }
    val currentProviderFailed = provider.isNotEmpty() && (provider to quality) in providerFailures.values

    val tree = remember(folder) { folder.takeIf(String::isNotBlank)?.toUri() }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    var permissionRefresh by remember { mutableIntStateOf(0) }
    val details by produceState<FolderDetails?>(null, tree, lifecycle, permissionRefresh) {
        value = tree?.let { selected -> withContext(Dispatchers.IO) { readFolderDetails(resolver, selected) } }
    }
    val selectedFolder = details?.takeIf { it.uri == tree }
    val work = key(folder) {
        if (tree == null) emptyList<WorkInfo>()
        else {
            val flow = remember(context, folder) {
                WorkManager.getInstance(context).getWorkInfosByTagFlow(DownloadFolderWorker.tag(folder))
            }
            flow.collectAsState(initial = emptyList()).value
        }
    }
    val copying = work.any { !it.state.isFinished }
    val copyFailed = work.any { it.state == WorkInfo.State.FAILED }
    var pickerFailed by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { result ->
        if (result != null) {
            pickerFailed = runCatching {
                resolver.takePersistableUriPermission(
                    result, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                setFolder(result.toString())
                permissionRefresh++
            }.isFailure
        }
    }

    AudioLabPage(
        title = LabTitle(stringResource(R.string.download_settings_title), "", firstSize = 40.sp),
        onBack = { navController.navigateUp() },
    ) {
        LabCard(padding = 16.dp) {
            DownloadCardHeader(R.drawable.download, stringResource(R.string.download_settings_provider))
            Spacer(Modifier.height(14.dp))
            LabSelect(
                options = providerLabels,
                selectedIndex = providers.indexOf(provider).coerceAtLeast(0),
                onSelect = { index -> providers.getOrNull(index)?.takeIf { it in eligible }?.let(setProvider) },
                contentDescription = stringResource(R.string.download_settings_provider),
                mutedIndex = -1,
            )
            Spacer(Modifier.height(10.dp))
            DownloadSettingNote(
                stringResource(
                    when {
                        selectionUnavailable -> R.string.download_settings_source_unavailable
                        currentProviderFailed -> R.string.download_settings_provider_failed
                        provider.isEmpty() -> R.string.download_settings_search_order
                        else -> R.string.download_settings_provider_note
                    },
                ),
                warning = selectionUnavailable || currentProviderFailed,
            )
            if (selectionUnavailable || currentProviderFailed) {
                Spacer(Modifier.height(10.dp))
                DownloadAction(stringResource(R.string.download_settings_use_automatic), R.drawable.refresh) { setProvider("") }
            }
        }

        Spacer(Modifier.height(12.dp))
        LabCard(padding = 16.dp) {
            DownloadCardHeader(R.drawable.equalizer, stringResource(R.string.download_settings_quality))
            Spacer(Modifier.height(14.dp))
            DownloadQualityChoices(quality, setQuality)
            Spacer(Modifier.height(10.dp))
            DownloadSettingNote(stringResource(R.string.download_settings_quality_note))
        }

        Spacer(Modifier.height(12.dp))
        LabCard(padding = 16.dp) {
            DownloadCardHeader(
                R.drawable.storage,
                stringResource(R.string.download_settings_folder),
                when {
                    tree == null -> stringResource(R.string.download_settings_app_storage)
                    else -> selectedFolder?.name ?: stringResource(R.string.download_settings_picked_folder)
                },
            )
            Spacer(Modifier.height(10.dp))
            DownloadSettingNote(
                stringResource(
                    if (tree == null) R.string.download_settings_default_folder_note else R.string.download_settings_folder_note,
                ),
            )
            if (pickerFailed || selectedFolder?.writable == false) {
                Spacer(Modifier.height(8.dp))
                DownloadSettingNote(stringResource(R.string.download_settings_folder_error), warning = true)
            }
            Spacer(Modifier.height(14.dp))
            DownloadAction(
                title = stringResource(
                    if (tree == null) R.string.download_settings_choose_folder else R.string.download_settings_change_folder,
                ),
                icon = R.drawable.storage,
                primary = true,
            ) { runCatching { picker.launch(tree) }.onFailure { pickerFailed = true } }

            if (tree != null) {
                Spacer(Modifier.height(8.dp))
                DownloadAction(
                    title = stringResource(
                        if (copying) R.string.download_settings_copy_progress else R.string.download_settings_copy_existing,
                    ),
                    icon = R.drawable.download,
                    enabled = selectedFolder?.writable == true && !saved.isNullOrEmpty(),
                    busy = copying,
                ) { storage.copyDownloadsToFolder(folder) }
                Spacer(Modifier.height(8.dp))
                DownloadAction(stringResource(R.string.download_settings_use_app_storage), R.drawable.close) {
                    pickerFailed = false
                    setFolder("")
                }
            }
            if (copyFailed && !copying && selectedFolder?.writable == true) {
                Spacer(Modifier.height(8.dp))
                DownloadSettingNote(stringResource(R.string.download_settings_copy_error), warning = true)
            }
        }

        Spacer(Modifier.height(12.dp))
        DownloadAction(
            stringResource(R.string.storage_manage_downloads),
            R.drawable.queue_music,
            navigate = true,
        ) { menu.showManageDownloadsSheet(storage) }
    }
}

@Composable
private fun DownloadCardHeader(icon: Int, title: String, detail: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(LabColors.Row),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), null, tint = LabColors.Primary, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = LabColors.Primary, fontFamily = InterFontFamily, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            if (detail != null) {
                Text(
                    detail, color = LabColors.Secondary, fontFamily = InterFontFamily, fontSize = 13.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DownloadQualityChoices(quality: DownloadQuality, onPick: (DownloadQuality) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().selectableGroup()) {
        val columns = if (maxWidth >= 280.dp && LocalDensity.current.fontScale <= 1.25f) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DownloadQuality.entries.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { option ->
                        val selected = option == quality
                        val press = remember { MutableInteractionSource() }
                        val shape = RoundedCornerShape(16.dp)
                        val fill by animateColorAsState(if (selected) LabColors.RowSelected else LabColors.Stage, label = "download quality")
                        Column(
                            modifier = Modifier.weight(1f).fillMaxHeight()
                                .pressBounce(press, pressedScale = 0.97f)
                                .clip(shape)
                                .background(fill)
                                .border(1.dp, if (selected) Color.White.copy(alpha = 0.28f) else LabColors.StageLine, shape)
                                .selectable(
                                    selected = selected,
                                    interactionSource = press,
                                    indication = RemoteFocus,
                                    role = Role.RadioButton,
                                ) { onPick(option) }
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    stringResource(option.downloadTitleRes), color = LabColors.Primary, fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f),
                                )
                                if (selected) {
                                    Icon(painterResource(R.drawable.check), null, tint = LabColors.Primary, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                stringResource(option.downloadDetailRes), color = LabColors.Secondary, fontFamily = InterFontFamily,
                                fontSize = 11.sp, lineHeight = 15.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadAction(
    title: String,
    icon: Int,
    primary: Boolean = false,
    enabled: Boolean = true,
    busy: Boolean = false,
    navigate: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val press = remember { MutableInteractionSource() }
    val contentColor = when {
        primary -> LabColors.Stage
        enabled || busy -> LabColors.Primary
        else -> LabColors.Secondary
    }
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .pressBounce(press, pressedScale = 0.98f)
            .clip(shape)
            .background(if (primary) Color(0xFFDCDCE3) else LabColors.Row)
            .clickable(press, RemoteFocus, enabled = enabled && !busy, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (busy) {
            CircularProgressIndicator(color = contentColor, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else {
            Icon(painterResource(icon), null, tint = contentColor, modifier = Modifier.size(18.dp))
        }
        Text(
            title, color = contentColor, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
        if (navigate) {
            Icon(painterResource(R.drawable.navigate_next), null, tint = contentColor, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun DownloadSettingNote(text: String, warning: Boolean = false) {
    Text(
        text,
        color = if (warning) PippoColors.Error else LabColors.Secondary,
        fontFamily = InterFontFamily,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )
}

private fun readFolderDetails(resolver: ContentResolver, tree: Uri): FolderDetails {
    val writable = runCatching {
        resolver.persistedUriPermissions.any { it.uri == tree && it.isReadPermission && it.isWritePermission }
    }.getOrDefault(false)
    val name = if (writable) runCatching {
        val document = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        resolver.query(document, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }.getOrNull() else null
    return FolderDetails(tree, name, writable)
}
