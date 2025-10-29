package com.krishisakhi.farmassistant

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.*

class TextToSpeechPlayer(private val context: Context) {

    companion object {
        private const val TAG = "TextToSpeechPlayer"
    }

    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false

    interface TTSCallback {
        fun onStart()
        fun onDone()
        fun onError(error: String)
    }

    fun initialize(callback: () -> Unit) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Language not supported")
                    isInitialized = false
                } else {
                    Log.d(TAG, "TextToSpeech initialized successfully")
                    isInitialized = true
                    callback()
                }
            } else {
                Log.e(TAG, "TextToSpeech initialization failed")
                isInitialized = false
            }
        }
    }

    fun speak(text: String, callback: TTSCallback) {
        if (!isInitialized) {
            callback.onError("Text-to-Speech not initialized")
            return
        }

        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "TTS Started")
                callback.onStart()
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "TTS Completed")
                callback.onDone()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e(TAG, "TTS Error")
                callback.onError("Speech playback error")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e(TAG, "TTS Error: $errorCode")
                callback.onError("Speech playback error: $errorCode")
            }
        })

        val utteranceId = UUID.randomUUID().toString()
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        textToSpeech?.stop()
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
    }

    fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking ?: false
    }
}

