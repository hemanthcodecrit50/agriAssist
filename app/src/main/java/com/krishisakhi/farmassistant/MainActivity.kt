package com.krishisakhi.farmassistant

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    private lateinit var notificationRecyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
            val intent = Intent(this, WeatherActivity::class.java)
            startActivity(intent)
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

        // Add background blur effect
        dialog.window?.setDimAmount(0.9f)
        dialog.window?.attributes?.windowAnimations = android.R.style.Animation_Dialog

        val micButton = dialog.findViewById<ImageButton>(R.id.micButton)
        val sendButton = dialog.findViewById<Button>(R.id.sendButton)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)

        micButton.setOnClickListener {
            Toast.makeText(this, "Voice recording...", Toast.LENGTH_SHORT).show()
        }

        sendButton.setOnClickListener {
            Toast.makeText(this, "Question sent!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}

// Data class for notifications
data class NotificationItem(
    val title: String,
    val description: String,
    val time: String
)
