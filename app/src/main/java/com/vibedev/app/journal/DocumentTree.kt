package com.vibedev.app.journal

import android.content.ContentResolver
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** One file or folder inside a picked folder tree. */
data class DocNode(val id: String, val name: String, val mime: String) {
    val isDirectory: Boolean get() = mime == Document.MIME_TYPE_DIR
}

/** Lists a picked folder tree with one query per folder (much faster than DocumentFile on Drive). */
class DocumentTree(private val resolver: ContentResolver, private val treeUri: Uri) {
    val rootId: String = DocumentsContract.getTreeDocumentId(treeUri)

    /** Call off the main thread: may wait a few seconds for Drive to fetch a fresh listing. */
    fun children(parentId: String): List<DocNode> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        repeat(MAX_WAITS) {
            val result = query(uri) ?: return emptyList()
            // Drive may first answer from its cache and say it is still loading.
            if (!result.loading) return result.nodes
            if (!awaitChange(result.notifyUri)) return result.nodes
        }
        return query(uri)?.nodes ?: emptyList()
    }

    fun uriOf(id: String): Uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)

    private class Listing(val nodes: List<DocNode>, val loading: Boolean, val notifyUri: Uri?)

    private fun query(uri: Uri): Listing? {
        val columns = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE)
        return resolver.query(uri, columns, null, null, null)?.use { c ->
            Listing(readNodes(c), c.extras?.getBoolean(DocumentsContract.EXTRA_LOADING) == true, c.notificationUri)
        }
    }

    private fun readNodes(c: Cursor) = buildList {
        while (c.moveToNext()) add(DocNode(c.getString(0), c.getString(1) ?: "", c.getString(2) ?: ""))
    }

    private fun awaitChange(notifyUri: Uri?): Boolean {
        if (notifyUri == null) return false
        val changed = CountDownLatch(1)
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) = changed.countDown()
        }
        resolver.registerContentObserver(notifyUri, true, observer)
        return try { changed.await(WAIT_SECONDS, TimeUnit.SECONDS) } finally {
            resolver.unregisterContentObserver(observer)
        }
    }

    private companion object {
        const val MAX_WAITS = 3
        const val WAIT_SECONDS = 8L
    }
}
