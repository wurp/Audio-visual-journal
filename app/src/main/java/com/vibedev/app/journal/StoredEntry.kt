package com.vibedev.app.journal

import android.net.Uri

/** A media file already saved in the journal folder. */
data class StoredMedia(val uri: Uri, val kind: MediaKind, val mime: String)

/** An entry read back from the journal folder. */
data class StoredEntry(
    val id: String,
    val timestamp: Long,
    val media: StoredMedia?,
    val note: Uri?,
)
