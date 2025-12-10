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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.krishisakhi.farmassistant.data.NotificationItem
import com.krishisakhi.farmassistant.rag.EnhancedAIService
import com.krishisakhi.farmassistant.classifier.QueryIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var notificationRecyclerView: RecyclerView
    
    // AI Services
    private var speechToTextConverter: SpeechToTextConverter? = null
    private var enhancedAIService: EnhancedAIService? = null
    private var textToSpeechPlayer: TextToSpeechPlayer? = null
    private var isProcessingAudio = false
    private lateinit var notificationAdapter: NotificationAdapter
    private var audioRecorder: AudioRecorder? = null
    private var currentDialog: Dialog? = null

    private val db by lazy { com.krishisakhi.farmassistant.db.AppDatabase.getDatabase(this) }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val TAG = "MainActivity"
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

        // Populate header with saved profile or fallback
        loadAndShowProfile()

        // Initialize AI services
        speechToTextConverter = SpeechToTextConverter(this)
        enhancedAIService = EnhancedAIService(this, BuildConfig.GEMINI_API_KEY)
        textToSpeechPlayer = TextToSpeechPlayer(this)
        KrishiSakhiApplication.textToSpeechPlayer = textToSpeechPlayer
        textToSpeechPlayer?.initialize {
            Log.d(TAG, "Text-to-Speech initialized")
        }

        // Initialize knowledge base in background
        CoroutineScope(Dispatchers.IO).launch {
            val success = enhancedAIService?.initialize() ?: false
            withContext(Dispatchers.Main) {
                if (success) {
                    Log.d(TAG, "Knowledge base initialized successfully")
                } else {
                    Log.e(TAG, "Failed to initialize knowledge base")
                }
            }
        }

        setupNotifications()
        setupQuickAccessButtons()
    }

    override fun onResume() {
        super.onResume()
        // Refresh header/profile in case user just saved their profile
        loadAndShowProfile()
    }

    private fun loadAndShowProfile() {
        val tvUserName = findViewById<TextView?>(R.id.tvUserName)
        tvUserName?.text = getString(R.string.welcome_message)

        val uid = auth.currentUser?.uid
        Log.d(TAG, "User UID in MainActivity: $uid")

        if (uid.isNullOrEmpty()) {
            Log.w(TAG, "No authenticated UID available for current user")
            return
        }

        lifecycleScope.launch {
            val profile = withContext(Dispatchers.IO) {
                try {
                    db.farmerProfileDao().getProfileByUid(uid)
                } catch (e: Exception) {
                    Log.e(TAG, "DB query failed: ${e.message}")
                    null
                }
            }

            if (profile != null) {
                Log.d(TAG, "Found profile for uid $uid: ${profile.name}")
                tvUserName?.text = profile.name
            } else {
                Log.d(TAG, "No profile found for uid $uid")
                // Try to sync from Firestore if local profile not found
                val syncManager = com.krishisakhi.farmassistant.sync.SyncManager.getInstance(this@MainActivity)
                val syncedProfile = syncManager.syncOnLogin(uid)
                if (syncedProfile != null) {
                    Log.d(TAG, "Profile synced from Firestore: ${syncedProfile.name}")
                    tvUserName?.text = syncedProfile.name
                } else {
                    tvUserName?.text = "Guest User"
                }
            }
        }
    }

    private fun normalizePhoneToDigits(phone: String?): String? {
        if (phone == null) return null
        val digits = phone.filter { it.isDigit() }
        if (digits.length == 10) return "91$digits"
        if (digits.length >= 11) return digits
        return null
    }

    private fun setupNotifications() {
        notificationRecyclerView = findViewById(R.id.notificationsRecyclerView)
        notificationRecyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize adapter with empty list
        notificationAdapter = NotificationAdapter(mutableListOf())
        notificationRecyclerView.adapter = notificationAdapter

        // Get empty state view
        val emptyView = findViewById<android.view.View>(R.id.notificationsEmptyView)

        // Fetch notifications from backend
        lifecycleScope.launch {
            try {
                val notifications = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.krishisakhi.farmassistant.network.NetworkModule.fetchNotifications()
                }

                if (notifications.isEmpty()) {
                    // Show empty state
                    emptyView.visibility = android.view.View.VISIBLE
                    notificationRecyclerView.visibility = android.view.View.GONE
                } else {
                    // Show notifications
                    emptyView.visibility = android.view.View.GONE
                    notificationRecyclerView.visibility = android.view.View.VISIBLE
                    notificationAdapter.setNotifications(notifications)
                }
            } catch (e: Exception) {
                // On error, show empty state and log
                emptyView.visibility = android.view.View.VISIBLE
                notificationRecyclerView.visibility = android.view.View.GONE
                Log.e(TAG, "Error fetching notifications", e)
                Toast.makeText(this@MainActivity, "Failed to load notifications", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupQuickAccessButtons() {
        // Profile button
        findViewById<ImageButton>(R.id.profileButton).setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
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

        // Farmer Profile button
        findViewById<MaterialCardView>(R.id.farmerProfileCard).setOnClickListener {
            val intent = Intent(this, FarmerProfileEditorActivity::class.java)
            startActivity(intent)
        }

        // Farmer Insights button
        findViewById<MaterialCardView>(R.id.farmerInsightsCard).setOnClickListener {
            showFarmerInsightsDialog()
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
        if (enhancedAIService == null) {
            enhancedAIService = EnhancedAIService(this, BuildConfig.GEMINI_API_KEY)
        }
        if (textToSpeechPlayer == null) {
            textToSpeechPlayer = TextToSpeechPlayer(this)
            KrishiSakhiApplication.textToSpeechPlayer = textToSpeechPlayer
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
            enhancedAIService?.generateEnhancedResponse(
                query,
                object : EnhancedAIService.EnhancedAICallback {
                    override fun onSuccess(response: String, intent: QueryIntent, sources: List<String>) {
                        runOnUiThread {
                            // Close the dialog
                            currentDialog?.dismiss()

                            // Launch AIResponseActivity to show formatted response
                            val activityIntent = Intent(this@MainActivity, AIResponseActivity::class.java)
                            activityIntent.putExtra(AIResponseActivity.EXTRA_USER_QUERY, query)
                            activityIntent.putExtra(AIResponseActivity.EXTRA_AI_RESPONSE, response)
                            activityIntent.putExtra("INTENT_TYPE", intent.name)
                            activityIntent.putStringArrayListExtra("SOURCES", ArrayList(sources))
                            startActivity(activityIntent)

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

    private fun showFarmerInsightsDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_farmer_insights)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setDimAmount(0.7f)

        val insightsTextView = dialog.findViewById<TextView>(R.id.insightsTextView)
        val closeButton = dialog.findViewById<Button>(R.id.closeButton)

        // Load insights from InsightsFileManager
        lifecycleScope.launch {
            try {
                val insightsManager = enhancedAIService?.getInsightsFileManager()
                val insights = withContext(Dispatchers.IO) {
                    insightsManager?.readInsights() ?: ""
                }

                if (insights.isEmpty()) {
                    insightsTextView.text = "No insights yet.\n\nKeep asking questions and the AI will learn about your farming practices and generate personalized insights!"
                } else {
                    insightsTextView.text = insights
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading insights", e)
                insightsTextView.text = "Error loading insights. Please try again later."
            }
        }

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (audioRecorder?.isRecording == true) {
            audioRecorder?.cancelRecording()
        }
        textToSpeechPlayer?.shutdown()
    }
}
