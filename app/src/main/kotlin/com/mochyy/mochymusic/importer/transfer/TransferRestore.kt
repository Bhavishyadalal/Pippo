// TransferRestore.kt

package com.mochyy.mochymusic.importer.transfer

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.database.sqlite.SQLiteDatabase
import com.mochyy.mochymusic.db.MusicDatabaseVersion
import java.util.zip.ZipInputStream
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import androidx.datastore.preferences.core.PreferencesSerializer
import okio.buffer
import okio.source
import android.os.Build
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.mochyy.mochymusic.MainActivity
import com.mochyy.mochymusic.db.InternalDatabase
import com.mochyy.mochymusic.markOnboardedOnThisInstall
import com.mochyy.mochymusic.playback.MusicService
import com.mochyy.mochymusic.playback.StoragePlaces
import com.mochyy.mochymusic.playback.StoredKind
import com.mochyy.mochymusic.utils.dataStore
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.File
import kotlin.system.exitProcess

/** finishes an import on the first start after it was unpacked, before room, media3 or the settings open anything */
object TransferRestore {
    private const val Tag = "TransferRestore"
    private const val StagingName = "transfer_staging"
    private const val NoteName = "transfer_restored.json"
    internal const val ReadyMarker = "ready.json"
    private const val LegacySettings = "legacy-settings.preferences_pb"

    /** Unpack and validate old two-entry backups without touching live data. */
    internal suspend fun stageLegacy(context: Context, source: Uri, settingsName: String) {
        val staging = stagingDir(context)
        check(!File(staging, ReadyMarker).exists()) { "Another restore is pending" }
        val temporary = File(context.dataDir, "legacy-restore-" + java.util.UUID.randomUUID())
        check(temporary.mkdirs())
        try {
            val library = File(temporary, TransferArchive.LibraryEntry)
            val settings = File(temporary, LegacySettings)
            check(library.parentFile?.mkdirs() == true)
            val raw = requireNotNull(context.contentResolver.openInputStream(source))
            ZipInputStream(raw.buffered()).use { zip ->
                val buffer = ByteArray(128 * 1024)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val entry = zip.nextEntry ?: break
                    val target = when (entry.name) {
                        InternalDatabase.DB_NAME -> library
                        settingsName -> settings
                        else -> null
                    }
                    if (target == null || entry.isDirectory) continue
                    check(!target.exists()) { "Duplicate backup entry" }
                    target.outputStream().use { output ->
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = zip.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                        }
                    }
                    zip.closeEntry()
                }
            }
            check(library.isFile && library.length() > 0L) { "Missing library" }
            SQLiteDatabase.openDatabase(library.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                check(TransferImporter.hasMigrationPath(db.version, MusicDatabaseVersion)) { "Unsupported backup database version" }
                db.rawQuery("PRAGMA quick_check", null).use { cursor ->
                    check(cursor.moveToFirst() && cursor.getString(0).equals("ok", ignoreCase = true)) { "Damaged backup" }
                }
                db.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'song'", null).use {
                    check(it.moveToFirst()) { "Not a Pippo library" }
                }
                foldIntoOneFile(db)
            }
            listOf("-wal", "-shm", "-journal").forEach { File(library.path + it).delete() }
            if (settings.exists()) settings.source().buffer().use { PreferencesSerializer.readFrom(it) }
            currentCoroutineContext().ensureActive()
            File(temporary, ReadyMarker).writeText(JSONObject().put("legacy", true).toString())
            check(!File(staging, ReadyMarker).exists()) { "Another restore is pending" }
            check(!staging.exists() || staging.deleteRecursively())
            check(temporary.renameTo(staging)) { "Couldn't stage backup" }
        } finally {
            temporary.deleteRecursively()
        }
    }

    /** where an unpacked import waits for the next start, next to the data so moving it in is a rename */
    fun stagingDir(context: Context): File = File(context.dataDir, StagingName)

    /** called first thing in App.onCreate, swaps the unpacked import in if one is waiting */
    fun applyPending(app: Application) {
        // the crash screen runs in its own process and must never touch an import
        if (!isMainProcess(app)) return
        val staging = stagingDir(app)
        val transaction = RestoreFileTransaction(app)
        // Recovery runs even if the previous process died after consuming staging.
        transaction.recover(staging)
        if (!staging.exists() || File(staging, RestoreFileTransaction.FailedMarker).exists()) return
        val ready = File(staging, ReadyMarker)
        if (!ready.exists()) {
            staging.deleteRecursively()
            return
        }
        val hasMedia = File(staging, TransferArchive.MediaEntry).exists()
        val oldSongCache = if (hasMedia) StoragePlaces.dir(app, StoredKind.SongCache) else null
        var committed = false
        try {
            val note = JSONObject(ready.readText())
            prepareFiles(app, staging, transaction, legacy = note.optBoolean("legacy"))
            transaction.install()
            restorePrefs(app, File(staging, TransferArchive.PrefsPrefix))
            File(staging, TransferArchive.SettingsEntry).takeIf { it.exists() }?.let { settings ->
                val json = JSONObject(settings.readText())
                runBlocking { app.dataStore.edit { TransferArchive.jsonIntoPreferences(json, it) } }
            }
            if (File(staging, TransferArchive.FilesPrefix + StoredKind.Downloads.folder).exists()) {
                StoragePlaces.resetToPhone(app, StoredKind.Downloads)
            }
            markOnboardedOnThisInstall(app)
            File(app.noBackupFilesDir, NoteName).writeText(note.toString())
            transaction.commit()
            committed = true
            // The cache belongs to the replaced media index. Only discard it after commit.
            oldSongCache?.let { runCatching { it.deleteRecursively() } }
            check(staging.deleteRecursively()) { "Couldn't remove completed import staging" }
            transaction.clear()
        } catch (failure: Exception) {
            Log.e(Tag, "Couldn't finish the import", failure)
            if (committed || transaction.hasCommitted) {
                // Finish retiring this journal before the UI can stage another import.
                throw IllegalStateException("Import completed; restart the app to finish cleanup.", failure)
            }
            RestoreFileTransaction.markFailed(staging)
            if (transaction.hasStarted) {
                transaction.rollback()
                transaction.clear()
                // Preferences may already be cached by their APIs. End this startup rather than
                // opening Room against restored files and imported in-memory preferences.
                throw IllegalStateException("Import failed; previous data restored. Restart the app to continue.", failure)
            }
            transaction.clear() // Preparation failed before any original was changed.
        }
    }

    /** stops playback and starts the app again, so the next start can swap the import in */
    fun restartToFinish(context: Context) {
        context.stopService(Intent(context, MusicService::class.java))
        context.startActivity(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        exitProcess(0)
    }

    /** the welcome line for the first screen after an import, handed out once */
    fun takeRestoredNote(context: Context): String? {
        val staging = stagingDir(context)
        if (File(staging, RestoreFileTransaction.FailedMarker).exists() &&
            runCatching { File(staging, "failure_shown").createNewFile() }.getOrDefault(false)
        ) {
            return "Import couldn't finish. Your previous data was kept. Please import again."
        }
        val file = File(context.noBackupFilesDir, NoteName)
        if (!file.exists()) return null
        val note = runCatching { JSONObject(file.readText()) }.getOrNull()
        file.delete()
        note ?: return null
        val missing = note.optInt("missingLocal")
        return if (missing > 0) {
            "Welcome back! Your library is here, but $missing local ${if (missing == 1) "song wasn't" else "songs weren't"} found on this phone."
        } else {
            "Welcome back! Your whole library is here."
        }
    }

    private fun prepareFiles(context: Context, staging: File, transaction: RestoreFileTransaction, legacy: Boolean) {
        val library = File(staging, TransferArchive.LibraryEntry)
        check(library.isFile && library.length() > 0L) { "The staged library is missing" }
        for (item in File(staging, TransferArchive.FilesPrefix).listFiles().orEmpty()) {
            transaction.replace(File(context.filesDir, item.name), item)
        }
        fun database(name: String, source: File) {
            val target = context.getDatabasePath(name)
            transaction.replace(target, source)
            listOf("-wal", "-shm", "-journal").forEach {
                transaction.replace(File(target.path + it), null)
            }
        }
        database(InternalDatabase.DB_NAME, library)
        File(staging, TransferArchive.MediaEntry).takeIf { it.exists() }?.let {
            database(TransferArchive.MediaDatabaseName, it)
        }
        transaction.preserve(File(context.dataDir, "shared_prefs"))
        val legacySettings = File(staging, LegacySettings)
        if (legacy && legacySettings.isFile) {
            transaction.replace(File(context.filesDir, "datastore/settings.preferences_pb"), legacySettings)
        } else {
            transaction.preserve(File(context.filesDir, "datastore"))
        }
        if (legacy) {
            transaction.replace(File(context.filesDir, MusicService.PERSISTENT_QUEUE_FILE), null)
        }
        transaction.preserve(context.noBackupFilesDir)
    }

    // merged into what's there, so anything set during onboarding that the old app never had stays
    private fun restorePrefs(context: Context, folder: File) {
        for (file in folder.listFiles().orEmpty()) {
            val fileName = TransferArchive.PrefsToCarry[file.name.removeSuffix(".json")] ?: continue
            val editor = context.getSharedPreferences(fileName, Context.MODE_PRIVATE).edit()
            TransferArchive.jsonIntoSharedPreferences(JSONObject(file.readText()), editor)
            check(editor.commit()) { "Couldn't restore preferences: $fileName" }
        }
    }

    internal fun isMainProcess(context: Context): Boolean {
        val name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Application.getProcessName()
        } else {
            runCatching { File("/proc/self/cmdline").readText().substringBefore('\u0000').trim() }.getOrNull()
        }
        return name.isNullOrEmpty() || name == context.packageName
    }
}
