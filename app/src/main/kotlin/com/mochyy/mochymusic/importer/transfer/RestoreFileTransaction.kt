package com.mochyy.mochymusic.importer.transfer

import android.content.Context
import android.system.Os
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/** Startup-only transaction: prepare copies first, then retain originals until commit. */
internal class RestoreFileTransaction(context: Context) {
    private val dataDir = context.dataDir.canonicalFile
    private val folder = File(dataDir, "transfer_rollback")
    private val manifest = File(folder, "journal.json")
    private val committed = File(folder, "committed")
    private data class Entry(val target: File, val existed: Boolean)
    private val entries = mutableListOf<Entry>()

    val hasStarted: Boolean get() = manifest.exists()
    val hasCommitted: Boolean get() = committed.exists()

    /** Runs before preferences or databases open, including after an interrupted rollback. */
    fun recover(staging: File) {
        if (!folder.exists()) return
        if (hasCommitted) {
            // Consume staging before deleting the commit marker, so it cannot be applied twice.
            check(staging.deleteRecursively()) { "Couldn't remove completed import staging" }
            clear()
            return
        }
        if (hasStarted) {
            markFailed(staging)
            rollback()
        }
        clear()
    }

    fun replace(target: File, source: File?, linkStagedFiles: Boolean = true) {
        check(!hasStarted)
        val canonical = target.canonicalFile
        require(canonical.path.startsWith(dataDir.path + File.separator))
        require(entries.none { it.target == canonical })
        check(folder.isDirectory || folder.mkdirs()) { "Couldn't prepare restore journal" }
        val index = entries.size
        if (source != null) {
            check(source.exists()) { "Missing import file: ${source.name}" }
            prepareTree(source, File(folder, "$index.new"), linkStagedFiles)
        }
        entries += Entry(canonical, canonical.exists())
    }

    /** Preferences are merged through their APIs; protect the underlying files as well. */
    fun preserve(target: File) = replace(target, target.takeIf { it.exists() }, linkStagedFiles = false)

    private fun prepareTree(source: File, target: File, linkStagedFiles: Boolean) {
        if (source.isDirectory) {
            check(target.isDirectory || target.mkdirs())
            val children = source.listFiles() ?: throw IOException("Couldn't read ${source.name}")
            children.forEach { prepareTree(it, File(target, it.name), linkStagedFiles) }
        } else {
            // Staged audio and databases stay untouched until commit. Hard links retain staging
            // without copying gigabytes at startup; original preferences always get real copies.
            if (linkStagedFiles) {
                try {
                    Os.link(source.path, target.path)
                    return
                } catch (_: Exception) {
                    // A filesystem without hard links can still complete using a full copy.
                }
            }
            source.copyTo(target)
        }
    }

    fun install() {
        val journal = JSONArray()
        entries.forEach { entry ->
            journal.put(JSONObject()
                .put("path", entry.target.relativeTo(dataDir).path)
                .put("existed", entry.existed))
        }
        writeDurably(manifest, journal.toString())
        entries.forEachIndexed { index, entry ->
            val target = entry.target
            target.parentFile?.let { check(it.isDirectory || it.mkdirs()) }
            if (entry.existed && !target.renameTo(File(folder, "$index.old"))) {
                throw IOException("Couldn't retain original ${target.name}")
            }
            val prepared = File(folder, "$index.new")
            if (prepared.exists() && !prepared.renameTo(target)) {
                throw IOException("Couldn't install ${target.name}")
            }
        }
    }

    fun commit() = writeDurably(committed, "complete")

    fun rollback() {
        val journal = JSONArray(manifest.readText())
        for (index in journal.length() - 1 downTo 0) {
            val entry = journal.getJSONObject(index)
            val target = File(dataDir, entry.getString("path")).canonicalFile
            require(target.path.startsWith(dataDir.path + File.separator))
            val original = File(folder, "$index.old")
            if (original.exists()) {
                check(target.deleteRecursively()) { "Couldn't remove incomplete ${target.name}" }
                target.parentFile?.let { check(it.isDirectory || it.mkdirs()) }
                check(original.renameTo(target)) { "Couldn't restore original ${target.name}" }
            } else if (!entry.getBoolean("existed")) {
                check(target.deleteRecursively()) { "Couldn't undo ${target.name}" }
            }
            // An existing original without a backup was either never touched, or already
            // rolled back before the process stopped. Leave it alone in both cases.
        }
    }

    fun clear() {
        // Once rollback/commit is complete, remove the journal before deleting backups. A stop
        // during cleanup must not make a committed restore look like an unfinished transaction.
        check(!manifest.exists() || manifest.delete()) { "Couldn't retire restore journal" }
        check(!committed.exists() || committed.delete()) { "Couldn't retire restore commit marker" }
        check(folder.deleteRecursively()) { "Couldn't remove restore journal" }
    }

    companion object {
        const val FailedMarker = "failed.txt"

        fun markFailed(staging: File) {
            check(staging.isDirectory || staging.mkdirs())
            writeDurably(File(staging, FailedMarker), "Import failed; the staged files are retained for recovery.")
        }

        private fun writeDurably(file: File, text: String) {
            val temporary = File(file.path + ".tmp")
            FileOutputStream(temporary).use { output ->
                output.write(text.toByteArray(Charsets.UTF_8))
                output.fd.sync()
            }
            check(temporary.renameTo(file)) { "Couldn't save ${file.name}" }
        }
    }
}
