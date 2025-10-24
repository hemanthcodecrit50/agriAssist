package com.krishisakhi.farmassistant

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Bundle
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

class MainActivity : AppCompatActivity() {

    private lateinit var notificationRecyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter
    private var audioRecorder: AudioRecorder? = null
    private var currentDialog: Dialog? = null

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        audioRecorder = AudioRecorder(this)
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
            Toast.makeText(this, "Profile clicked", Toast.LENGTH_SHORT).show()
        }

        // Ask Question button
        findViewById<Button>(R.id.askQuestionButton).setOnClickListener {
            showAskQuestionDialog()
        }

        // Quick access cards
        findViewById<MaterialCardView>(R.id.weatherCard).setOnClickListener {
            Toast.makeText(this, "Weather Information", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialCardView>(R.id.pestAlertCard).setOnClickListener {
            Toast.makeText(this, "Pest Alerts", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialCardView>(R.id.marketPriceCard).setOnClickListener {
            Toast.makeText(this, "Market Prices", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialCardView>(R.id.govtSchemeCard).setOnClickListener {
            Toast.makeText(this, "Government Schemes", Toast.LENGTH_SHORT).show()
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

        micButton.setOnClickListener {
            if (audioRecorder?.isRecording == true) {
                stopRecording(micButton, tapToSpeakText, questionEditText)
            } else {
                startRecording(micButton, tapToSpeakText)
            }
        }

        sendButton.setOnClickListener {
            if (audioRecorder?.isRecording == true) {
                stopRecording(micButton, tapToSpeakText, questionEditText)
            }

            val question = questionEditText.text.toString()
            if (question.isNotEmpty()) {
                Toast.makeText(this, "Question sent!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please ask a question", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            if (audioRecorder?.isRecording == true) {
                audioRecorder?.cancelRecording()
            }
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            if (audioRecorder?.isRecording == true) {
                audioRecorder?.cancelRecording()
            }
            currentDialog = null
        }

        dialog.show()
    }

    private fun startRecording(micButton: ImageButton, tapToSpeakText: TextView) {
        if (checkAudioPermission()) {
            val filePath = audioRecorder?.startRecording()
            if (filePath != null) {
                micButton.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_red_dark))
                tapToSpeakText.text = "Recording... Tap to stop"
                Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
            }
        } else {
            requestAudioPermission()
        }
    }

    private fun stopRecording(micButton: ImageButton, tapToSpeakText: TextView, questionEditText: EditText) {
        val filePath = audioRecorder?.stopRecording()
        micButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary_green))
        tapToSpeakText.text = getString(R.string.tap_to_speak)

        if (filePath != null) {
            Toast.makeText(this, "Recording saved: ${filePath.substringAfterLast("/")}", Toast.LENGTH_SHORT).show()
            questionEditText.setText("Audio recorded: ${filePath.substringAfterLast("/")}")
        } else {
            Toast.makeText(this, "Failed to save recording", Toast.LENGTH_SHORT).show()
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

    override fun onDestroy() {
        super.onDestroy()
        if (audioRecorder?.isRecording == true) {
            audioRecorder?.cancelRecording()
        }
    }
}

// Data class for notifications
data class NotificationItem(
    val title: String,
    val description: String,
    val time: String
)
