// DocumentTree.kt

package com.mochyy.mochymusic.utils

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

// characters most file systems refuse in a name, plus anything invisible
private val BadNameChars = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")

// a folder the user picked in the system picker, filled with plain documentscontract calls; each folder is listed once and remembered
class DocumentTree(private val resolver: ContentResolver, private val tree: Uri) {
    val rootId: String = DocumentsContract.getTreeDocumentId(tree)
    private val listed = HashMap<String, MutableMap<String, String>>()

    // the folder called [name] inside [parentId], made if it isn't there yet
    fun folder(parentId: String, name: String): String {
        val children = childrenOf(parentId)
        children[name]?.let { return it }
        val uri = DocumentsContract.createDocument(resolver, documentUri(parentId), DocumentsContract.Document.MIME_TYPE_DIR, name)
            ?: throw IOException("couldn't make the folder $name")
        val id = DocumentsContract.getDocumentId(uri)
        children[name] = id
        return id
    }

    // writes a file called [name] inside [parentId], replacing one that's already there so an export run twice stays tidy
    fun write(parentId: String, name: String, block: (OutputStream) -> Unit) {
        val children = childrenOf(parentId)
        val old = children[name]?.let(::documentUri)
        val temporaryName = if (old == null) name else ".export-${UUID.randomUUID()}"
        // a generic type, so the provider never tacks a second extension onto the name
        var uri = DocumentsContract.createDocument(resolver, documentUri(parentId), "application/octet-stream", temporaryName)
            ?: throw IOException("couldn't make the file $name")
        var backup: Uri? = null
        var promoted = false
        try {
            val stream = resolver.openOutputStream(uri, "w") ?: throw IOException("couldn't open $name")
            stream.buffered(256 * 1024).use(block)
            if (old != null) {
                // Only rename the original after the complete replacement has been closed.
                // Providers without rename support fail here with the original still intact.
                backup = DocumentsContract.renameDocument(resolver, old, ".export-backup-${UUID.randomUUID()}")
                    ?: throw IOException("this folder cannot safely replace $name")
                uri = DocumentsContract.renameDocument(resolver, uri, name)
                    ?: throw IOException("couldn't finish replacing $name")
            }
            promoted = true
            children[name] = DocumentsContract.getDocumentId(uri)
            backup?.let { runCatching { DocumentsContract.deleteDocument(resolver, it) } }
        } catch (error: Throwable) {
            if (!promoted) {
                runCatching { DocumentsContract.deleteDocument(resolver, uri) }
                backup?.let {
                    // If rollback is refused, keep the original bytes under the backup name.
                    runCatching { DocumentsContract.renameDocument(resolver, it, name) }
                }
            }
            // A provider can change document IDs on rename; list again before another write.
            listed.remove(parentId)
            throw error
        }
    }

    private fun documentUri(id: String): Uri = DocumentsContract.buildDocumentUriUsingTree(tree, id)

    private fun childrenOf(parentId: String): MutableMap<String, String> = listed.getOrPut(parentId) {
        val out = HashMap<String, String>()
        val query = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        runCatching {
            resolver.query(query, columns, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0) ?: continue
                    val name = cursor.getString(1) ?: continue
                    out[name] = id
                }
            }
        }
        out
    }

    companion object {
        // a name any drive takes: no slashes or colons, no trailing dots, not too long
        fun safeName(raw: String, fallback: String): String {
            val cleaned = raw.replace(BadNameChars, " ").replace(Regex("\\s+"), " ").trim().trimEnd('.', ' ')
            return cleaned.take(120).trim().ifEmpty { fallback }
        }
    }
}
