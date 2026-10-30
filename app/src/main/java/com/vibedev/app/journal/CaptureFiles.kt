package com.vibedev.app.journal

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Temporary files for the camera and recorder to write into before an entry is saved. */
class CaptureFiles(private val context: Context) {
    private val dir get() = File(context.cacheDir, "captures").apply { mkdirs() }

    fun newFile(extension: String): File = File(dir, "capture-${System.nanoTime()}.$extension")

    fun shareableUri(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    fun clear() { dir.listFiles()?.forEach { it.delete() } }
}
