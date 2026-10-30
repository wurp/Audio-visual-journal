package com.vibedev.app.journal

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EntryListScreen(
    feed: JournalFeed,
    loader: MediaLoader,
    listState: LazyListState,
    onOpen: (StoredEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Text(
            "Journal",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 8.dp),
        )
        if (feed.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        when {
            !feed.hasFolder -> Message("Choose a folder on the New post tab to start your journal.")
            feed.error != null -> Message("Couldn't read the journal: ${feed.error}")
            feed.entries.isEmpty() && !feed.loading -> Message("No entries yet. Make one on the New post tab.")
        }
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(feed.entries, key = { _, e -> e.id }) { i, entry ->
                val previous = feed.entries.getOrNull(i - 1)
                if (previous == null || !EntryDates.sameDay(previous.timestamp, entry.timestamp)) {
                    Text(
                        EntryDates.dayHeading(entry.timestamp),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = if (i == 0) 4.dp else 14.dp, start = 4.dp),
                    )
                }
                EntryRow(entry, loader, onClick = { onOpen(entry) })
            }
        }
    }
}

@Composable
private fun Message(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(20.dp),
    )
}

@Composable
private fun EntryRow(entry: StoredEntry, loader: MediaLoader, onClick: () -> Unit) {
    val note by produceState("", entry.note) { entry.note?.let { value = loader.noteText(it) } }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Thumbnail(entry, loader)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    EntryDates.time(entry.timestamp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    note.trim().ifEmpty { kindTitle(entry.media?.kind) },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (note.isBlank()) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun Thumbnail(entry: StoredEntry, loader: MediaLoader) {
    val sizePx = with(LocalDensity.current) { 72.dp.roundToPx() }
    val image by produceState<ImageBitmap?>(null, entry.media) {
        entry.media?.let { value = loader.thumbnail(it, sizePx) }
    }
    Box(
        Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        val img = image
        if (img != null) {
            Image(img, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text(kindGlyph(entry.media?.kind), fontSize = 28.sp)
        }
        if (entry.media?.kind == MediaKind.VIDEO && img != null) {
            Text("▶", color = androidx.compose.ui.graphics.Color.White, fontSize = 22.sp)
        }
    }
}

fun kindGlyph(kind: MediaKind?) = when (kind) {
    MediaKind.PHOTO -> "📷"
    MediaKind.VIDEO -> "🎬"
    MediaKind.AUDIO -> "🎙"
    null -> "📝"
}

fun kindTitle(kind: MediaKind?) = when (kind) {
    MediaKind.PHOTO -> "Photo"
    MediaKind.VIDEO -> "Video"
    MediaKind.AUDIO -> "Voice recording"
    null -> "Note"
}
