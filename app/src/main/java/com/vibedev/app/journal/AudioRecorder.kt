package com.vibedev.app.journal

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** Records microphone audio to an .m4a file. */
class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null

    val isRecording: Boolean get() = recorder != null

    fun start(output: File) {
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
                else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioEncodingBitRate(128_000)
        r.setAudioSamplingRate(44_100)
        r.setOutputFile(output.absolutePath)
        r.prepare()
        r.start()
        recorder = r
    }

    fun stop() {
        recorder?.let {
            runCatching { it.stop() }
            it.release()
        }
        recorder = null
    }
}
