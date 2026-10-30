package com.vibedev.app.journal

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Saves journal entries somewhere. Returns a short description of where. */
interface EntryStore {
    suspend fun save(entry: JournalEntry): String
}

/**
 * Saves entries into a folder the user picked, as
 * <folder>/<year>/<month>/<day>/<hour>/<HH-mm-ss> <kind>.<ext>
 * with any text in a matching "<HH-mm-ss> note.txt".
 */
class FolderEntryStore(
    private val context: Context,
    private val root: Uri,
) : EntryStore {

    override suspend fun save(entry: JournalEntry): String = withContext(Dispatchers.IO) {
        val date = Date(entry.timestamp)
        val folder = hourFolder(date)
        val stamp = format("HH-mm-ss", date)

        entry.media?.let { media ->
            val mime = context.contentResolver.getType(media.uri) ?: defaultMime(media.kind)
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: defaultExt(media.kind)
            write(folder, "$stamp ${media.kind.label}.$ext", mime) { out ->
                context.contentResolver.openInputStream(media.uri)!!.use { it.copyTo(out) }
            }
        }
        if (entry.text.isNotBlank()) {
            write(folder, "$stamp note.txt", "text/plain") { it.write(entry.text.toByteArray()) }
        }
        format("yyyy/MM/dd/HH", date)
    }

    private fun hourFolder(date: Date): DocumentFile {
        var dir = DocumentFile.fromTreeUri(context, root) ?: error("Can't open the journal folder")
        for (pattern in listOf("yyyy", "MM", "dd", "HH")) {
            val name = format(pattern, date)
            dir = dir.findFile(name)?.takeIf { it.isDirectory }
                ?: dir.createDirectory(name)
                ?: error("Can't create folder $name")
        }
        return dir
    }

    private fun write(dir: DocumentFile, name: String, mime: String, body: (java.io.OutputStream) -> Unit) {
        val file = dir.createFile(mime, name) ?: error("Can't create $name")
        context.contentResolver.openOutputStream(file.uri)!!.use(body)
    }

    private fun format(pattern: String, date: Date) = SimpleDateFormat(pattern, Locale.US).format(date)

    private fun defaultMime(kind: MediaKind) = when (kind) {
        MediaKind.PHOTO -> "image/jpeg"
        MediaKind.VIDEO -> "video/mp4"
        MediaKind.AUDIO -> "audio/mp4"
    }

    private fun defaultExt(kind: MediaKind) = when (kind) {
        MediaKind.PHOTO -> "jpg"
        MediaKind.VIDEO -> "mp4"
        MediaKind.AUDIO -> "m4a"
    }
}
