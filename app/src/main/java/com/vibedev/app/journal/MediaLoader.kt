package com.vibedev.app.journal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Point
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads thumbnails, full images and note text for saved entries, with a small memory cache. */
class MediaLoader(context: Context) {
    private val context = context.applicationContext
    private val resolver = this.context.contentResolver
    private val thumbs = LruCache<Uri, ImageBitmap>(80)
    private val notes = LruCache<Uri, String>(200)

    suspend fun thumbnail(media: StoredMedia, sizePx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        thumbs.get(media.uri)?.let { return@withContext it }
        if (media.kind == MediaKind.AUDIO) return@withContext null
        val bmp = runCatching {
            DocumentsContract.getDocumentThumbnail(resolver, media.uri, Point(sizePx, sizePx), null)
        }.getOrNull() ?: when (media.kind) {
            MediaKind.PHOTO -> decodeImage(media.uri, sizePx)
            MediaKind.VIDEO -> videoFrame(media.uri)
            MediaKind.AUDIO -> null
        }
        bmp?.asImageBitmap()?.also { thumbs.put(media.uri, it) }
    }

    suspend fun fullImage(uri: Uri, maxPx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        decodeImage(uri, maxPx)?.asImageBitmap()
    }

    suspend fun noteText(uri: Uri): String = withContext(Dispatchers.IO) {
        notes.get(uri) ?: runCatching {
            resolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
        }.getOrDefault("").also { notes.put(uri, it) }
    }

    private fun decodeImage(uri: Uri, maxPx: Int): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxPx && bounds.outHeight / (sample * 2) >= maxPx) sample *= 2
        val bmp = resolver.openInputStream(uri)!!.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        rotateUpright(bmp, uri)
    }.getOrNull()

    private fun rotateUpright(bmp: Bitmap, uri: Uri): Bitmap {
        val degrees = runCatching {
            resolver.openInputStream(uri)!!.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            }
        }.getOrDefault(0f)
        if (degrees == 0f) return bmp
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(degrees) }, true)
    }

    private fun videoFrame(uri: Uri): Bitmap? = runCatching {
        MediaMetadataRetriever().run {
            try { setDataSource(context, uri); frameAtTime } finally { release() }
        }
    }.getOrNull()
}
