package com.vibedev.app.journal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.Calendar

/** Reads saved entries, newest first. */
interface JournalReader {
    fun newestFirst(): Flow<StoredEntry>
}

/** Reads the <year>/<month>/<day>/<hour> layout written by [FolderEntryStore]. */
class FolderJournalReader(private val tree: DocumentTree) : JournalReader {

    override fun newestFirst(): Flow<StoredEntry> = flow {
        for (year in numberedDirs(tree.rootId, 4))
            for (month in numberedDirs(year.id, 2))
                for (day in numberedDirs(month.id, 2))
                    for (hour in numberedDirs(day.id, 2)) {
                        val base = intArrayOf(year.name.toInt(), month.name.toInt(), day.name.toInt(), hour.name.toInt())
                        entriesIn(hour, base).forEach { emit(it) }
                    }
    }.flowOn(Dispatchers.IO)

    private fun numberedDirs(parentId: String, digits: Int): List<DocNode> =
        tree.children(parentId)
            .filter { it.isDirectory && it.name.length == digits && it.name.all(Char::isDigit) }
            .sortedByDescending { it.name }

    private fun entriesIn(hour: DocNode, ymdh: IntArray): List<StoredEntry> =
        tree.children(hour.id)
            .filter { !it.isDirectory && STAMP.containsMatchIn(it.name) }
            .groupBy { it.name.take(8) }
            .toSortedMap(compareByDescending { it })
            .map { (stamp, files) -> toEntry(stamp, files, ymdh) }

    private fun toEntry(stamp: String, files: List<DocNode>, ymdh: IntArray): StoredEntry {
        val (_, min, sec) = stamp.split('-').map(String::toInt)
        val time = Calendar.getInstance().apply {
            clear()
            set(ymdh[0], ymdh[1] - 1, ymdh[2], ymdh[3], min, sec)
        }.timeInMillis
        val note = files.firstOrNull { it.name.endsWith("note.txt") }
        val media = files.firstNotNullOfOrNull { f -> kindOf(f.mime)?.let { StoredMedia(tree.uriOf(f.id), it, f.mime) } }
        return StoredEntry(
            id = files.first().id,
            timestamp = time,
            media = media,
            note = note?.let { tree.uriOf(it.id) },
        )
    }

    private fun kindOf(mime: String): MediaKind? = when {
        mime.startsWith("image/") -> MediaKind.PHOTO
        mime.startsWith("video/") -> MediaKind.VIDEO
        mime.startsWith("audio/") -> MediaKind.AUDIO
        else -> null
    }

    private companion object { val STAMP = Regex("""^\d{2}-\d{2}-\d{2}""") }
}
