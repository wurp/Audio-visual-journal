package com.vibedev.app.journal

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.vibedev.app.BuildConfig
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun JournalScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val location = remember { PrefsJournalLocation(context.applicationContext) }
    val files = remember { CaptureFiles(context.applicationContext) }
    val recorder = remember { AudioRecorder(context.applicationContext) }

    var folder by remember { mutableStateOf(location.folder) }
    var text by rememberSaveable { mutableStateOf("") }
    // Pending media, kept as plain values so it survives the camera app taking over.
    var mediaUri by rememberSaveable { mutableStateOf<String?>(null) }
    var mediaKind by rememberSaveable { mutableStateOf<String?>(null) }
    var mediaTime by rememberSaveable { mutableStateOf(0L) }
    var cameraTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var saving by remember { mutableStateOf(false) }
    var lastSaved by rememberSaveable { mutableStateOf<String?>(null) }

    fun setMedia(uri: Uri, kind: MediaKind) {
        mediaUri = uri.toString(); mediaKind = kind.name; mediaTime = System.currentTimeMillis()
    }
    fun clearEntry() {
        mediaUri = null; mediaKind = null; text = ""; files.clear()
    }
    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    DisposableEffect(Unit) { onDispose { recorder.stop() } }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) runCatching { location.folder = uri; folder = uri }
            .onFailure { toast("Couldn't use that folder: ${it.message}") }
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        cameraTarget?.let { if (ok) setMedia(Uri.parse(it), MediaKind.PHOTO) }
        cameraTarget = null
    }
    val takeVideo = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { ok ->
        cameraTarget?.let { if (ok) setMedia(Uri.parse(it), MediaKind.VIDEO) }
        cameraTarget = null
    }
    val pickFromGallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val isVideo = context.contentResolver.getType(uri)?.startsWith("video") == true
            setMedia(uri, if (isVideo) MediaKind.VIDEO else MediaKind.PHOTO)
        }
    }

    fun startRecording() {
        val file = files.newFile("m4a")
        runCatching { recorder.start(file); recordingFile = file }
            .onFailure { toast("Couldn't start recording: ${it.message}") }
    }
    val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else toast("Audio entries need microphone permission")
    }

    fun launchCamera(extension: String, launch: (Uri) -> Unit) {
        val uri = files.shareableUri(files.newFile(extension))
        cameraTarget = uri.toString()
        runCatching { launch(uri) }.onFailure { toast("No camera app found") }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("New post", style = MaterialTheme.typography.headlineSmall)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Saving to", style = MaterialTheme.typography.labelLarge)
                Text(folder?.let(::folderLabel) ?: "No folder chosen yet")
                TextButton(onClick = { pickFolder.launch(folder) }) {
                    Text(if (folder == null) "Choose folder" else "Change folder")
                }
            }
        }

        val isRecording = recordingFile != null
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(
                onClick = { launchCamera("jpg") { takePhoto.launch(it) } },
                enabled = !isRecording, modifier = Modifier.weight(1f),
            ) { Text("Photo") }
            FilledTonalButton(
                onClick = { launchCamera("mp4") { takeVideo.launch(it) } },
                enabled = !isRecording, modifier = Modifier.weight(1f),
            ) { Text("Video") }
            FilledTonalButton(
                onClick = {
                    if (isRecording) {
                        recorder.stop()
                        recordingFile?.let { setMedia(files.shareableUri(it), MediaKind.AUDIO) }
                        recordingFile = null
                    } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) {
                        startRecording()
                    } else {
                        askMic.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text(if (isRecording) "Stop" else "Audio") }
        }
        OutlinedButton(
            onClick = {
                pickFromGallery.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
            },
            enabled = !isRecording, modifier = Modifier.fillMaxWidth(),
        ) { Text("Pick photo or video from gallery") }

        when {
            isRecording -> Text("● Recording audio…", color = MaterialTheme.colorScheme.error)
            mediaKind != null -> Row {
                Text(
                    "Attached: ${MediaKind.valueOf(mediaKind!!).label}",
                    modifier = Modifier.weight(1f).padding(top = 12.dp),
                )
                TextButton(onClick = { mediaUri = null; mediaKind = null }) { Text("Remove") }
            }
        }

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Text (optional)") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
        )

        Button(
            onClick = {
                val root = folder ?: return@Button toast("Choose a folder first")
                val media = mediaUri?.let {
                    CapturedMedia(Uri.parse(it), MediaKind.valueOf(mediaKind!!), mediaTime)
                }
                val entry = JournalEntry(media, text)
                if (entry.isEmpty) return@Button toast("Add a photo, video, audio, or some text")
                saving = true
                scope.launch {
                    runCatching { FolderEntryStore(context.applicationContext, root).save(entry) }
                        .onSuccess { lastSaved = "Saved in $it"; clearEntry() }
                        .onFailure { toast("Couldn't save: ${it.message}") }
                    saving = false
                }
            },
            enabled = !saving && !isRecording,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (saving) "Saving…" else "Save entry") }

        lastSaved?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        Text(
            "Test build ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Turns a picked folder into something readable, like "Drive: Journal". */
private fun folderLabel(uri: Uri): String {
    val id = runCatching { android.provider.DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
        ?: return uri.toString()
    val path = Uri.decode(id).substringAfter(':', Uri.decode(id))
    val place = when {
        uri.authority?.contains("google.android.apps.docs") == true -> "Google Drive"
        uri.authority == "com.android.externalstorage.documents" -> "Phone storage"
        else -> uri.authority ?: ""
    }
    return if (path.isBlank()) place else "$place: $path"
}
