package com.krishisakhi.farmassistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class SpeechToTextConverter(private val context: Context) {

    companion object {
        private const val TAG = "SpeechToText"
    }

    interface SpeechCallback {
        fun onSuccess(transcribedText: String)
        fun onError(error: String)
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var currentCallback: SpeechCallback? = null

    fun startLiveSpeechRecognition(callback: SpeechCallback) {
        currentCallback = callback

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            callback.onError("Speech recognition not available on this device")
            return
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

            val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "Ready for speech")
                }

                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "Speech started")
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Audio level changed
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    // Buffer received
                }

                override fun onEndOfSpeech() {
                    Log.d(TAG, "Speech ended")
                }

                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                        else -> "Unknown error"
                    }
                    Log.e(TAG, "Speech recognition error: $errorMessage")
                    currentCallback?.onError("Speech recognition failed: $errorMessage")
                    cleanup()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val recognizedText = matches[0]
                        Log.d(TAG, "Recognized text: $recognizedText")
                        currentCallback?.onSuccess(recognizedText)
                    } else {
                        currentCallback?.onError("No speech detected")
                    }
                    cleanup()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    // Partial results available
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    // Event occurred
                }
            })

            speechRecognizer?.startListening(recognizerIntent)
            Log.d(TAG, "Speech recognition started")

        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            callback.onError("Failed to start speech recognition: ${e.message}")
            cleanup()
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    private fun cleanup() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        currentCallback = null
    }
}

