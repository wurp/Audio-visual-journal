package com.vibedev.app.journal

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract

/** One file or folder inside a picked folder tree. */
data class DocNode(val id: String, val name: String, val mime: String) {
    val isDirectory: Boolean get() = mime == Document.MIME_TYPE_DIR
}

/** Lists a picked folder tree with one query per folder (much faster than DocumentFile on Drive). */
class DocumentTree(private val resolver: ContentResolver, private val treeUri: Uri) {
    val rootId: String = DocumentsContract.getTreeDocumentId(treeUri)

    fun children(parentId: String): List<DocNode> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        val columns = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE)
        return resolver.query(uri, columns, null, null, null)?.use { c ->
            buildList {
                while (c.moveToNext()) add(DocNode(c.getString(0), c.getString(1) ?: "", c.getString(2) ?: ""))
            }
        } ?: emptyList()
    }

    fun uriOf(id: String): Uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
}
