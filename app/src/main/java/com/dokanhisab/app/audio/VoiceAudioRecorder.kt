package com.dokanhisab.app.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

private const val TAG = "VoiceAudioRecorder"

class VoiceAudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(): Result<File> {
        return runCatching {
            stopInternal(deleteFile = true)

            val voiceDir = File(context.cacheDir, "voice").apply { mkdirs() }
            val file = File(voiceDir, "voice_${System.currentTimeMillis()}.m4a")

            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1)
                setAudioSamplingRate(16_000)
                setAudioEncodingBitRate(64_000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recorder = newRecorder
            outputFile = file
            file
        }.onFailure { err ->
            Log.e(TAG, "Failed to start audio recording", err)
            stopInternal(deleteFile = true)
        }
    }

    fun stopRecording(): File? {
        val file = outputFile
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            outputFile = null
            file?.takeIf { it.exists() && it.length() > 0L }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            stopInternal(deleteFile = true)
            null
        }
    }

    fun cancelRecording() {
        stopInternal(deleteFile = true)
    }

    private fun stopInternal(deleteFile: Boolean) {
        try {
            recorder?.reset()
            recorder?.release()
        } catch (_: Exception) {
        } finally {
            recorder = null
        }
        if (deleteFile) {
            outputFile?.let { runCatching { it.delete() } }
        }
        outputFile = null
    }
}
