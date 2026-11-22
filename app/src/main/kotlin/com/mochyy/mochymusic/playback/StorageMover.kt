// StorageMover.kt

package com.mochyy.mochymusic.playback

import android.content.Context
import android.os.Environment
import android.util.Log
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.importer.transfer.TransferRestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// how a move is going, for the storage page and the place sheet
sealed interface MoveStep {
    data object Idle : MoveStep
    data class Working(val kind: StoredKind, val progress: Float) : MoveStep
    data class Failed(val kind: StoredKind, val message: String) : MoveStep
}

// moves downloads (copied, every song kept) or the song cache (started fresh) to another drive, then restarts so the player opens the new folder
@Singleton
class StorageMover @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadUtil: DownloadUtil,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _step = MutableStateFlow<MoveStep>(MoveStep.Idle)
    val step: StateFlow<MoveStep> = _step.asStateFlow()

    // what moving [kind] there would need, so the sheet can say it won't fit before anyone taps
    fun bytesToMove(kind: StoredKind): Long =
        if (kind == StoredKind.Downloads) sizeOf(StoragePlaces.dir(context, kind)) else 0L

    fun move(kind: StoredKind, to: StoragePlace) {
        val previous = _step.value
        if (previous is MoveStep.Working || !_step.compareAndSet(previous, MoveStep.Working(kind, 0f))) return
        scope.launch {
            try {
                val from = StoragePlaces.dir(context, kind).canonicalFile
                val target = File(to.root, kind.folder).canonicalFile
                if (from == target) {
                    _step.value = MoveStep.Idle
                    return@launch
                }
                val sourcePath = from.path + File.separator
                val targetPath = target.path + File.separator
                check(!targetPath.startsWith(sourcePath) && !sourcePath.startsWith(targetPath)) {
                    "Storage folders overlap"
                }
                // A picked path can also overlap the other cache. Never copy into or later
                // retire a folder that contains files already used by Media3.
                val other = StoredKind.entries.first { it != kind }
                val otherPath = StoragePlaces.dir(context, other).canonicalPath + File.separator
                check(!targetPath.startsWith(otherPath) && !otherPath.startsWith(targetPath)) {
                    "Storage destination overlaps the other cache"
                }
                if (!to.isPhone) {
                    check(Environment.getExternalStorageState(to.root) == Environment.MEDIA_MOUNTED) {
                        "Storage destination is unavailable"
                    }
                    checkWritable(to.root)
                }
                if (kind == StoredKind.Downloads) {
                    copyDownloads(from, target) { StoragePlaces.choose(context, kind, to, oldDir = from) }
                } else {
                    check(target.isDirectory || target.mkdirs()) { "Couldn't create the song cache folder" }
                    StoragePlaces.choose(context, kind, to, oldDir = from)
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                _step.value = MoveStep.Idle
                throw cancelled
            } catch (error: Exception) {
                Log.e("StorageMover", "Couldn't move ${kind.folder}", error)
                _step.value = MoveStep.Failed(kind, context.getString(R.string.storage_move_failed))
                return@launch
            }
            _step.value = MoveStep.Working(kind, 1f)
            delay(600)
            withContext(Dispatchers.Main) { TransferRestore.restartToFinish(context) }
        }
    }

    fun clearFailure() {
        if (_step.value is MoveStep.Failed) _step.value = MoveStep.Idle
    }

    // copied next to where it goes and renamed in one step, so a move cut short never leaves a half folder in use
    private suspend fun copyDownloads(from: File, target: File, select: () -> Unit) =
        downloadUtil.withDownloadFilesPaused(keepPausedUntilRestart = true) {
            var moved = false
            val parent = checkNotNull(target.parentFile)
            val staging = createTemporaryDirectory(parent, "download-moving")
            var previous: File? = null
            var installed = false
            try {
                val files = from.walkTopDown().onFail { folder, error ->
                    throw IOException("Couldn't read ${folder.name}", error)
                }.filter { it.isFile }.toList()
                val total = files.sumOf { it.length() }
                if (total > parent.usableSpace) throw IOException("not enough room")
                var copied = 0L
                var shown = 0f
                val buffer = ByteArray(256 * 1024)
                for (file in files) {
                    val out = File(staging, file.relativeTo(from).path)
                    out.parentFile?.mkdirs()
                    file.inputStream().use { input ->
                        out.outputStream().use { output ->
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                                copied += read
                                val progress = (copied.toFloat() / total.coerceAtLeast(1L)).coerceAtMost(0.99f)
                                if (progress - shown >= 0.005f) {
                                    shown = progress
                                    _step.value = MoveStep.Working(StoredKind.Downloads, progress)
                                }
                            }
                        }
                    }
                    if (out.length() != file.length()) throw IOException("${file.name} came out a different size")
                }
                if (target.exists()) {
                    val backup = createTemporaryDirectory(parent, "download-backup")
                    val saved = File(backup, target.name)
                    if (!target.renameTo(saved)) {
                        backup.delete()
                        throw IOException("Couldn't retain the previous destination")
                    }
                    previous = saved
                }
                check(staging.renameTo(target)) { "Couldn't install the copied downloads" }
                installed = true
                select()
                moved = true
            } finally {
                if (moved) {
                    previous?.parentFile?.deleteRecursively()
                } else {
                    if (installed) target.deleteRecursively()
                    previous?.let { if (it.renameTo(target)) it.parentFile?.delete() }
                }
                staging.deleteRecursively()
            }
        }

    private fun createTemporaryDirectory(parent: File, label: String): File =
        File(parent, "$label-${java.util.UUID.randomUUID()}").also {
            check(it.mkdirs()) { "Couldn't create temporary storage" }
        }

    // writes and removes a small file first, so a folder android won't let mochy write in fails here instead of after the restart
    private fun checkWritable(root: File) {
        if (!root.exists() && !root.mkdirs()) throw IOException("couldn't make ${root.path}")
        val probe = File.createTempFile("pippo-write-", ".tmp", root)
        if (!probe.delete()) throw IOException("couldn't tidy up in ${root.path}")
    }

    private fun sizeOf(dir: File): Long =
        runCatching { dir.walkTopDown().filter { it.isFile }.sumOf { it.length() } }.getOrDefault(0L)
}
