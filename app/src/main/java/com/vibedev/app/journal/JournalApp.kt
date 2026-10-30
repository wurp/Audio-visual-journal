package com.vibedev.app.journal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private enum class Tab(val label: String, val glyph: String) { NEW("New post", "✎"), JOURNAL("Journal", "📖") }

/** The app's two tabs: making a new entry, and reading past ones. */
@Composable
fun JournalApp() {
    val context = LocalContext.current.applicationContext
    val location = remember { PrefsJournalLocation(context) }
    val loader = remember { MediaLoader(context) }
    val feed = remember {
        JournalFeed { location.folder?.let { FolderJournalReader(DocumentTree(context.contentResolver, it)) } }
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val tabs = rememberSaveableStateHolder()
    var tab by rememberSaveable { mutableStateOf(Tab.NEW) }
    var opened by remember { mutableStateOf<StoredEntry?>(null) }

    LaunchedEffect(tab) { if (tab == Tab.JOURNAL) feed.refresh() }
    BackHandler(enabled = opened != null) { opened = null }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t; opened = null },
                        icon = { Text(t.glyph, fontSize = 20.sp) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        val inner = Modifier.padding(padding)
        tabs.SaveableStateProvider(tab) {
            when (tab) {
                Tab.NEW -> JournalScreen(inner)
                Tab.JOURNAL -> opened?.let { EntryDetailScreen(it, loader, onBack = { opened = null }, modifier = inner) }
                    ?: EntryListScreen(feed, loader, listState, onOpen = { opened = it },
                        onRefresh = { scope.launch { feed.refresh() } }, modifier = inner)
            }
        }
    }
}
