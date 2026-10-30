package com.vibedev.app.journal

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.edit

/** Remembers which folder the journal is saved in. */
interface JournalLocation {
    var folder: Uri?
}

class PrefsJournalLocation(private val context: Context) : JournalLocation {
    private val prefs = context.getSharedPreferences("journal", Context.MODE_PRIVATE)

    override var folder: Uri?
        get() = prefs.getString(KEY, null)?.let(Uri::parse)
        set(value) {
            if (value != null) {
                // Keep access to the folder after the phone restarts.
                context.contentResolver.takePersistableUriPermission(
                    value,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            prefs.edit { putString(KEY, value?.toString()) }
        }

    private companion object { const val KEY = "folder" }
}
