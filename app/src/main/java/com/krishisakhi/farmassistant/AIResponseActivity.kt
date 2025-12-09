package com.krishisakhi.farmassistant

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.noties.markwon.Markwon

class AIResponseActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_USER_QUERY = "user_query"
        const val EXTRA_AI_RESPONSE = "ai_response"
    }

    private var stopAudioButton: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_response)

        // Get data from intent
        val userQuery = intent.getStringExtra(EXTRA_USER_QUERY) ?: "No question"
        val aiResponse = intent.getStringExtra(EXTRA_AI_RESPONSE) ?: "No response"

        // Initialize views
        val backButton = findViewById<ImageButton>(R.id.backButton)
        stopAudioButton = findViewById<ImageButton>(R.id.stopAudioButton)
        val userQuestionText = findViewById<TextView>(R.id.userQuestionText)
        val aiResponseText = findViewById<TextView>(R.id.aiResponseText)

        // Set user question
        userQuestionText.text = userQuery

        // Render markdown response
        val markwon = Markwon.create(this)
        markwon.setMarkdown(aiResponseText, aiResponse)

        // Back button listener
        backButton.setOnClickListener {
            finish()
        }

        // Stop audio button listener
        stopAudioButton?.setOnClickListener {
            stopAudio()
        }

        // Show stop button if audio is playing
        updateStopButtonVisibility()

        // Handle system back button
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun stopAudio() {
        KrishiSakhiApplication.textToSpeechPlayer?.stop()
        stopAudioButton?.visibility = android.view.View.GONE
    }

    private fun updateStopButtonVisibility() {
        val isSpeaking = KrishiSakhiApplication.textToSpeechPlayer?.isSpeaking() ?: false
        stopAudioButton?.visibility = if (isSpeaking) android.view.View.VISIBLE else android.view.View.GONE
    }

    override fun onResume() {
        super.onResume()
        updateStopButtonVisibility()
    }
}

