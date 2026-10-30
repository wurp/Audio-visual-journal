package com.vibedev.app.journal

import android.net.Uri

enum class MediaKind(val label: String) { PHOTO("photo"), VIDEO("video"), AUDIO("audio") }

/** A piece of media waiting to be saved: where it is now, what it is, and when it was taken. */
data class CapturedMedia(val uri: Uri, val kind: MediaKind, val capturedAt: Long)

/** One journal entry: optional media plus optional text. At least one must be present. */
data class JournalEntry(val media: CapturedMedia?, val text: String) {
    val isEmpty: Boolean get() = media == null && text.isBlank()
    val timestamp: Long get() = media?.capturedAt ?: System.currentTimeMillis()
}
