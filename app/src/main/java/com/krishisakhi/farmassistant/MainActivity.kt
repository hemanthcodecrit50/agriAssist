package com.krishisakhi.farmassistant

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.krishisakhi.farmassistant.data.NotificationItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var notificationRecyclerView: RecyclerView
    
    // AI Services
    private var speechToTextConverter: SpeechToTextConverter? = null
    private var geminiAIService: GeminiAIService? = null
    private var textToSpeechPlayer: TextToSpeechPlayer? = null
    private var isProcessingAudio = false
    private lateinit var notificationAdapter: NotificationAdapter
    private var audioRecorder: AudioRecorder? = null
    private var currentDialog: Dialog? = null

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check authentication status
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            // User not authenticated, redirect to login
            startActivity(Intent(this, PhoneAuthActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        audioRecorder = AudioRecorder(this)

        // Initialize AI services
        speechToTextConverter = SpeechToTextConverter(this)
        geminiAIService = GeminiAIService(BuildConfig.GEMINI_API_KEY)
        textToSpeechPlayer = TextToSpeechPlayer(this)
        textToSpeechPlayer?.initialize {
            Log.d("MainActivity", "Text-to-Speech initialized")
        }

        setupNotifications()
        setupQuickAccessButtons()
    }

    private fun setupNotifications() {
        notificationRecyclerView = findViewById(R.id.notificationsRecyclerView)
        notificationRecyclerView.layoutManager = LinearLayoutManager(this)

        // Sample notifications data
        val notifications = listOf(
            NotificationItem("Heavy rainfall expected", "Next 3 days", "2h ago"),
            NotificationItem("Market price update", "Wheat ₹2100/quintal", "5h ago"),
            NotificationItem("New pest alert", "Aphids in your region", "1d ago")
        )

        notificationAdapter = NotificationAdapter(notifications)
        notificationRecyclerView.adapter = notificationAdapter
    }

    private fun setupQuickAccessButtons() {
        // Profile button
        findViewById<ImageButton>(R.id.profileButton).setOnClickListener {
            showLogoutDialog()
        }

        // Ask Question button
        findViewById<Button>(R.id.askQuestionButton).setOnClickListener {
            showAskQuestionDialog()
        }

        // Quick access cards
        findViewById<MaterialCardView>(R.id.weatherCard).setOnClickListener {
            val intent = Intent(this, WeatherActivity::class.java)
            startActivity(intent)
        }

        findViewById<MaterialCardView>(R.id.pestAlertCard).setOnClickListener {
            val intent = Intent(this, PestAlertActivity::class.java)
            startActivity(intent)
        }

        findViewById<MaterialCardView>(R.id.marketPriceCard).setOnClickListener {
            val intent = Intent(this, MarketPricesActivity::class.java)
            startActivity(intent)
        }

        findViewById<MaterialCardView>(R.id.govtSchemeCard).setOnClickListener {
            val intent = Intent(this, GovtSchemeActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showAskQuestionDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_ask_question)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setDimAmount(0.9f)

        currentDialog = dialog

        val micButton = dialog.findViewById<ImageButton>(R.id.micButton)
        val tapToSpeakText = dialog.findViewById<TextView>(R.id.tapToSpeakText)
        val questionEditText = dialog.findViewById<EditText>(R.id.questionEditText)
        val sendButton = dialog.findViewById<Button>(R.id.sendButton)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)

        var isListening = false

        micButton.setOnClickListener {
            if (isProcessingAudio) {
                Toast.makeText(this, "Please wait, processing previous request...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isListening) {
                // Start listening - begin speech recognition immediately
                if (checkAudioPermission()) {
                    isListening = true
                    micButton.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_red_dark))
                    tapToSpeakText.text = "🎤 Listening... Speak now!"
                    questionEditText.setText("Listening for your question...")
                    Toast.makeText(this, "Speak your question now...", Toast.LENGTH_SHORT).show()

                    // Start speech recognition immediately
                    startSpeechRecognition(micButton, tapToSpeakText, questionEditText)
                } else {
                    requestAudioPermission()
                }
            } else {
                // Stop listening
                isListening = false
                speechToTextConverter?.stopListening()
                micButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary_green))
                tapToSpeakText.text = "Processing..."
                Toast.makeText(this, "Processing your speech...", Toast.LENGTH_SHORT).show()
            }
        }

        sendButton.setOnClickListener {
            if (isProcessingAudio) {
                Toast.makeText(this, "Please wait, processing...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val question = questionEditText.text.toString()
            if (question.isNotEmpty() && !question.startsWith("Listening") && !question.startsWith("You asked") && !question.startsWith("AI:")) {
                // If user typed a question, send it directly to Gemini AI
                sendTypedQuestion(question, questionEditText, tapToSpeakText)
            } else {
                Toast.makeText(this, "Please ask a question or use the microphone", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            if (isProcessingAudio) {
                speechToTextConverter?.stopListening()
                textToSpeechPlayer?.stop()
                isProcessingAudio = false
            }
            if (isListening) {
                speechToTextConverter?.stopListening()
            }
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            if (isProcessingAudio) {
                speechToTextConverter?.stopListening()
                textToSpeechPlayer?.stop()
                isProcessingAudio = false
            }
            currentDialog = null
        }

        dialog.show()
    }

    private fun startSpeechRecognition(micButton: ImageButton, tapToSpeakText: TextView, questionEditText: EditText) {
        if (isProcessingAudio) {
            Toast.makeText(this, "Already processing a query. Please wait.", Toast.LENGTH_SHORT).show()
            return
        }

        isProcessingAudio = true

        // Initialize AI services if not already done
        if (speechToTextConverter == null) {
            speechToTextConverter = SpeechToTextConverter(this)
        }
        if (geminiAIService == null) {
            geminiAIService = GeminiAIService(BuildConfig.GEMINI_API_KEY)
        }
        if (textToSpeechPlayer == null) {
            textToSpeechPlayer = TextToSpeechPlayer(this)
            textToSpeechPlayer?.initialize {
                android.util.Log.d("MainActivity", "Text-to-Speech initialized")
            }
        }

        // Start live speech recognition immediately
        speechToTextConverter?.startLiveSpeechRecognition(
            object : SpeechToTextConverter.SpeechCallback {
                override fun onSuccess(transcribedText: String) {
                    runOnUiThread {
                        micButton.setBackgroundTintList(ContextCompat.getColorStateList(this@MainActivity, R.color.primary_green))
                        questionEditText.setText("You asked: $transcribedText")
                        tapToSpeakText.text = "🤖 Getting AI response..."
                    }

                    // Send to Gemini AI
                    sendToGeminiAndRespond(transcribedText, questionEditText, tapToSpeakText)
                }

                override fun onError(error: String) {
                    runOnUiThread {
                        micButton.setBackgroundTintList(ContextCompat.getColorStateList(this@MainActivity, R.color.primary_green))
                        questionEditText.setText("Speech recognition error: $error")
                        tapToSpeakText.text = getString(R.string.tap_to_speak)
                        Toast.makeText(
                            this@MainActivity,
                            "Speech error: $error. Try speaking louder or check your internet connection.",
                            Toast.LENGTH_LONG
                        ).show()
                        isProcessingAudio = false
                    }
                }
            }
        )
    }

    private fun processLiveSpeechQuery(questionEditText: EditText, tapToSpeakText: TextView) {
        // This method is no longer needed but kept for compatibility
        startSpeechRecognition(
            currentDialog?.findViewById(R.id.micButton) ?: return,
            tapToSpeakText,
            questionEditText
        )
    }

    private fun sendTypedQuestion(question: String, questionEditText: EditText, tapToSpeakText: TextView) {
        if (isProcessingAudio) {
            Toast.makeText(this, "Already processing a query. Please wait.", Toast.LENGTH_SHORT).show()
            return
        }

        isProcessingAudio = true
        questionEditText.setText("You asked: $question")
        tapToSpeakText.text = "🤖 Getting AI response..."

        // Send directly to Gemini AI
        sendToGeminiAndRespond(question, questionEditText, tapToSpeakText)
    }

    private fun sendToGeminiAndRespond(query: String, questionEditText: EditText, tapToSpeakText: TextView) {
        CoroutineScope(Dispatchers.Main).launch {
            geminiAIService?.generateResponse(
                query,
                object : GeminiAIService.AICallback {
                    override fun onSuccess(response: String) {
                        runOnUiThread {
                            // Close the dialog
                            currentDialog?.dismiss()

                            // Launch AIResponseActivity to show formatted response
                            val intent = Intent(this@MainActivity, AIResponseActivity::class.java)
                            intent.putExtra(AIResponseActivity.EXTRA_USER_QUERY, query)
                            intent.putExtra(AIResponseActivity.EXTRA_AI_RESPONSE, response)
                            startActivity(intent)

                            // Play text-to-speech in background
                            textToSpeechPlayer?.speak(response, object : TextToSpeechPlayer.TTSCallback {
                                override fun onStart() {
                                    Log.d("MainActivity", "TTS Started")
                                }

                                override fun onDone() {
                                    Log.d("MainActivity", "TTS Completed")
                                }

                                override fun onError(error: String) {
                                    Log.e("MainActivity", "TTS Error: $error")
                                }
                            })

                            isProcessingAudio = false
                        }
                    }

                    override fun onError(error: String) {
                        runOnUiThread {
                            questionEditText.setText("Error: $error")
                            tapToSpeakText.text = getString(R.string.tap_to_speak)
                            Toast.makeText(
                                this@MainActivity,
                                "AI Error: $error",
                                Toast.LENGTH_LONG
                            ).show()
                            isProcessingAudio = false
                        }
                    }
                }
            )
        }
    }

    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAudioPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted. Please try again.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission denied. Cannot record audio.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showLogoutDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Logout")
        builder.setMessage("Are you sure you want to logout?")
        builder.setPositiveButton("Logout") { dialog, _ ->
            auth.signOut()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, PhoneAuthActivity::class.java))
            finish()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (audioRecorder?.isRecording == true) {
            audioRecorder?.cancelRecording()
        }
        textToSpeechPlayer?.shutdown()
    }
}
