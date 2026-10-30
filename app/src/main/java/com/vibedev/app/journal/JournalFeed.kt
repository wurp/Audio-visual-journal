package com.vibedev.app.journal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The list of saved entries shown on the Journal tab. */
class JournalFeed(private val readerFor: () -> JournalReader?) {
    val entries = mutableStateListOf<StoredEntry>()
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var hasFolder by mutableStateOf(true)
        private set

    suspend fun refresh() {
        val reader = readerFor()
        hasFolder = reader != null
        if (reader == null) { entries.clear(); return }
        loading = true
        error = null
        val fresh = mutableListOf<StoredEntry>()
        try {
            reader.newestFirst().collect { e ->
                fresh += e
                // Update in place so entries already on screen don't flicker.
                val i = fresh.lastIndex
                when {
                    i >= entries.size -> entries += e
                    entries[i] != e -> entries[i] = e
                }
            }
            while (entries.size > fresh.size) entries.removeAt(entries.lastIndex)
        } catch (e: Exception) {
            error = e.message ?: "Couldn't read the journal folder"
        } finally {
            loading = false
        }
    }
}
