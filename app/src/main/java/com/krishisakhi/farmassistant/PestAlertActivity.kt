package com.krishisakhi.farmassistant

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.krishisakhi.farmassistant.data.PestAlert

class PestAlertActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pest_alert)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            emptyStateText.text = "No pest alerts in your region"

            // Line 88: Initialize adapter
            private lateinit var adapter: PestAlertAdapter

            // Line 92: Data list
            private val dataList = mutableListOf<PestAlert>()

            // Line 97: Setup RecyclerView
            adapter = PestAlertAdapter(dataList)
            recyclerView.adapter = adapter
            insets
        }
    }
}