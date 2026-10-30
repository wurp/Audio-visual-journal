package com.vibedev.app.journal

import android.media.MediaPlayer
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

@Composable
fun EntryDetailScreen(entry: StoredEntry, loader: MediaLoader, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val note by produceState("", entry.note) { entry.note?.let { value = loader.noteText(it) } }
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        TextButton(onClick = onBack) { Text("‹  Back to journal") }
        Column {
            Text(EntryDates.fullDate(entry.timestamp), style = MaterialTheme.typography.headlineSmall)
            Text(
                EntryDates.time(entry.timestamp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        entry.media?.let { media ->
            when (media.kind) {
                MediaKind.PHOTO -> PhotoView(media.uri, loader)
                MediaKind.VIDEO -> VideoPlayer(media.uri)
                MediaKind.AUDIO -> AudioPlayer(media.uri)
            }
        }
        if (note.isNotBlank()) {
            Text(
                note.trim(),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 28.sp),
            )
        }
    }
}

@Composable
private fun PhotoView(uri: Uri, loader: MediaLoader) {
    val maxPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }
    val image by produceState<ImageBitmap?>(null, uri) { value = loader.fullImage(uri, maxPx) }
    val img = image
    if (img == null) {
        Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        Image(
            img,
            contentDescription = "Photo",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
        )
    }
}

@Composable
private fun VideoPlayer(uri: Uri) {
    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                val controls = MediaController(ctx)
                controls.setAnchorView(this)
                setMediaController(controls)
                setVideoURI(uri)
                setOnPreparedListener { start() }
            }
        },
        onRelease = { it.stopPlayback() },
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
    )
}

@Composable
private fun AudioPlayer(uri: Uri) {
    val context = LocalContext.current
    var ready by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var position by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(1f) }
    val player = remember {
        MediaPlayer().apply {
            setOnPreparedListener { ready = true; duration = it.duration.coerceAtLeast(1).toFloat() }
            setOnCompletionListener { playing = false; position = 0f }
            runCatching { setDataSource(context, uri); prepareAsync() }
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(playing) {
        while (playing) { position = player.currentPosition.toFloat(); delay(200) }
    }

    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(
                onClick = {
                    if (playing) player.pause() else player.start()
                    playing = !playing
                },
                enabled = ready,
            ) { Text(if (playing) "❚❚" else "▶") }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Slider(
                    value = position,
                    onValueChange = { position = it; player.seekTo(it.toInt()) },
                    valueRange = 0f..duration,
                    enabled = ready,
                )
                Text(
                    "${clock(position)} / ${clock(duration)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun clock(ms: Float): String {
    val s = (ms / 1000).toInt()
    return "%d:%02d".format(s / 60, s % 60)
}
