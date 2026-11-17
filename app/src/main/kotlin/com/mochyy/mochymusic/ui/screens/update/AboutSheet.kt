package com.mochyy.mochymusic.ui.screens.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mochyy.mochymusic.BuildConfig
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.ui.brand.PippoCharacter
import com.mochyy.mochymusic.ui.brand.PippoPose
import com.mochyy.mochymusic.ui.component.PopupSheetState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Offline app information. No remote releases, profiles or donation services. */
fun PopupSheetState.showAboutSheet(onOpenDevSpace: (Int) -> Unit = {}) {
    val popup = this
    show(buttonBar = { SheetPrimaryButton(stringResource(R.string.done), popup::dismiss) }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            PippoCharacter(PippoPose.Thanks, Modifier.fillMaxWidth().height(230.dp))
            Text("Pippo", style = MaterialTheme.typography.headlineLarge)
            Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.pippo_about_description), Modifier.padding(top = 16.dp))
            Text(stringResource(R.string.pippo_manual_updates), Modifier.padding(top = 12.dp))
            TextButton(onClick = { popup.showPippoLicense() }) { Text("GNU GPL v3 · License") }
        }
    }
}

private fun PopupSheetState.showPippoLicense() {
    val popup = this
    show(buttonBar = { SheetPrimaryButton(stringResource(R.string.done), popup::dismiss) }) {
        val context = LocalContext.current
        val license by produceState(initialValue = "", context) {
            value = withContext(Dispatchers.IO) {
                context.assets.open("LICENSE.txt").bufferedReader().use { it.readText() }
            }
        }
        Text(
            text = license,
            modifier = Modifier.fillMaxWidth().height(420.dp)
                .verticalScroll(rememberScrollState()).padding(24.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
