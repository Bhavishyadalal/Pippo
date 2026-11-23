package com.mochyy.mochymusic.ui.screens.settings.storage

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import com.mochyy.mochymusic.ui.screens.audiolab.AudioLabPage
import com.mochyy.mochymusic.ui.screens.audiolab.DownloadQualityGroup
import com.mochyy.mochymusic.ui.screens.audiolab.LabColors
import com.mochyy.mochymusic.ui.screens.audiolab.LabGroup
import com.mochyy.mochymusic.ui.screens.audiolab.LabSelect
import com.mochyy.mochymusic.ui.screens.audiolab.LabSheetTitle
import com.mochyy.mochymusic.ui.screens.audiolab.LabTextAction
import com.mochyy.mochymusic.ui.screens.audiolab.LabTitle
import com.mochyy.mochymusic.ui.theme.InterFontFamily
import com.mochyy.mochymusic.utils.rememberEnumPreference
import com.mochyy.mochymusic.utils.rememberPreference
import com.mochyy.mochymusic.viewmodels.ModulesViewModel
import com.mochyy.mochymusic.viewmodels.StorageViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

const val DownloadSettingsRoute = "download_settings"

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
        }
            .forEach { add(it.id) }
    }
    val selectionUnavailable = provider !in eligible
    val providers = if (selectionUnavailable) eligible + provider else eligible
    val selectedIndex = providers.indexOf(provider).coerceAtLeast(0)
    val providerLabels = providers.map { id ->
        when {
            id.isEmpty() -> automatic
            id !in eligible -> stringResource(R.string.download_settings_provider_unavailable, modules.nameOf(id))
            else -> modules.nameOf(id)
        }
    }

    val tree = remember(folder) { folder.takeIf(String::isNotBlank)?.toUri() }
    val folderName by produceState<String?>(null, tree) {
        value = tree?.let { withContext(Dispatchers.IO) { readFolderName(resolver, it) } }
    }
    val work by remember(context, folder) {
        if (folder.isEmpty()) flowOf(emptyList<WorkInfo>())
        else WorkManager.getInstance(context).getWorkInfosByTagFlow(DownloadFolderWorker.tag(folder))
    }.collectAsState(initial = emptyList())
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
            }.isFailure
        }
    }

    AudioLabPage(
        title = LabTitle(stringResource(R.string.download_settings_title), "", firstSize = 40.sp),
        subtitle = providerLabels[selectedIndex],
        onBack = { navController.navigateUp() },
    ) {
        LabSheetTitle(stringResource(R.string.download_settings_provider))
        LabGroup {
            LabSelect(
                options = providerLabels,
                selectedIndex = selectedIndex,
                onSelect = { index -> providers.getOrNull(index)?.takeIf { it in eligible }?.let(setProvider) },
                contentDescription = stringResource(R.string.download_settings_provider),
            )
            DownloadSettingNote(
                stringResource(
                    when {
                        selectionUnavailable -> R.string.download_settings_source_unavailable
                        provider.isEmpty() -> R.string.download_settings_search_order
                        else -> R.string.download_settings_provider_note
                    },
                ),
            )
            if (provider.isNotEmpty() && !selectionUnavailable && providerFailures.isNotEmpty()) {
                DownloadSettingNote(stringResource(R.string.download_settings_provider_failed))
            }
        }

        Spacer(Modifier.height(22.dp))
        LabSheetTitle(stringResource(R.string.download_settings_quality))
        DownloadQualityGroup(quality, setQuality)
        DownloadSettingNote(stringResource(R.string.download_settings_quality_note))

        Spacer(Modifier.height(22.dp))
        LabSheetTitle(stringResource(R.string.download_settings_folder))
        LabGroup {
            Text(
                text = if (tree == null) stringResource(R.string.download_settings_app_storage)
                else folderName ?: stringResource(R.string.download_settings_picked_folder),
                modifier = Modifier.padding(horizontal = 6.dp),
                color = LabColors.Primary,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            DownloadSettingNote(
                stringResource(if (tree == null) R.string.download_settings_default_folder_note else R.string.download_settings_folder_note),
            )
            LabTextAction(
                stringResource(if (tree == null) R.string.download_settings_choose_folder else R.string.download_settings_change_folder),
                R.drawable.storage,
            ) { runCatching { picker.launch(tree) }.onFailure { pickerFailed = true } }

            if (tree != null) {
                LabTextAction(
                    stringResource(if (copying) R.string.download_settings_copy_progress else R.string.download_settings_copy_existing),
                    R.drawable.download,
                    enabled = !saved.isNullOrEmpty(),
                    busy = copying,
                ) { storage.copyDownloadsToFolder(folder) }
                LabTextAction(stringResource(R.string.download_settings_use_app_storage)) {
                    pickerFailed = false
                    setFolder("")
                }
            }
            if (pickerFailed) DownloadSettingNote(stringResource(R.string.download_settings_folder_error))
            if (copyFailed) DownloadSettingNote(stringResource(R.string.download_settings_copy_error))
        }

        Spacer(Modifier.height(20.dp))
        LabTextAction(stringResource(R.string.storage_manage_downloads), R.drawable.navigate_next) {
            menu.showManageDownloadsSheet(storage)
        }
    }
}

@Composable
private fun DownloadSettingNote(text: String) {
    Text(
        text,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
        color = LabColors.Secondary,
        fontFamily = InterFontFamily,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    )
}

private fun readFolderName(resolver: ContentResolver, tree: Uri): String? = runCatching {
    val document = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
    resolver.query(document, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getString(0) else null
    }
}.getOrNull()
