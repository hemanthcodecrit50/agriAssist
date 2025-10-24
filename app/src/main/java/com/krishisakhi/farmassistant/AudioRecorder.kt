package com.krishisakhi.farmassistant

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

class AudioRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    var isRecording = false
        private set

    fun startRecording(): String? {
        return try {
            // Create audio file
            val fileName = "audio_${System.currentTimeMillis()}.m4a"
            audioFile = File(context.cacheDir, fileName)

            // Initialize MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFile?.absolutePath)

                prepare()
                start()
                isRecording = true
            }

            audioFile?.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            stopRecording()
            null
        }
    }

    fun stopRecording(): String? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            audioFile?.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            null
        }
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            audioFile?.delete()
            audioFile = null
            isRecording = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

