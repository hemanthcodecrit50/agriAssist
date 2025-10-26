package com.krishisakhi.farmassistant

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.krishisakhi.farmassistant.adapter.PestAlertAdapter
import com.krishisakhi.farmassistant.data.PestAlert
class PestAlertActivity : AppCompatActivity() {

    // UI Components
    private lateinit var headerIcon: ImageView
    private lateinit var headerTitle: TextView
    private lateinit var filterCard: MaterialCardView
    private lateinit var searchInput: EditText
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var emptyStateText: TextView
    private lateinit var errorMessageText: TextView
    private lateinit var adapter: PestAlertAdapter

    private val dataList = mutableListOf<PestAlert>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pest_alert)

        // Enable back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Pest Alerts"

        initializeViews()
        setupRecyclerView()
        loadData()
    }

    private fun initializeViews() {
        headerIcon = findViewById(R.id.headerIcon)
        headerTitle = findViewById(R.id.headerTitle)
        filterCard = findViewById(R.id.filterCard)
        searchInput = findViewById(R.id.searchInput)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        recyclerView = findViewById(R.id.recyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        emptyStateText = findViewById(R.id.emptyStateText)
        errorMessageText = findViewById(R.id.errorMessageText)

        headerIcon.setImageResource(R.drawable.ic_pest_alert)
        headerTitle.text = "Pest Alerts"
        emptyStateText.text = "No pest alerts in your region"
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = PestAlertAdapter(dataList)
        recyclerView.adapter = adapter
    }

    private fun loadData() {
        showLoading()
        loadSampleData()
        showData()
    }

    private fun loadSampleData() {
        dataList.clear()
        dataList.addAll(listOf(
            PestAlert(
                pestName = "Aphids (चेपा/माहू)",
                severity = "HIGH",
                cropType = "Cotton, Wheat",
                description = "Small sap-sucking insects that cause leaf curl, stunted growth, and transmit viral diseases",
                symptoms = "Yellowing leaves, sticky honeydew on plants, curled leaves, sooty mold growth",
                treatment = "Spray neem oil (5ml/liter) or dimethoate insecticide. Use yellow sticky traps. Remove infected plants.",
                dateReported = "2 hours ago",
                region = "Karnataka, Punjab"
            ),
            PestAlert(
                pestName = "Stem Borer (तना छेदक)",
                severity = "HIGH",
                cropType = "Rice, Sugarcane",
                description = "Caterpillar larvae that bore into plant stems causing dead hearts and white ear heads",
                symptoms = "Dead hearts in vegetative stage, white ear heads, drying of central shoot, exit holes in stem",
                treatment = "Use pheromone traps (8-10/acre). Apply cartap hydrochloride or chlorantraniliprole. Practice crop rotation.",
                dateReported = "5 hours ago",
                region = "West Bengal, Tamil Nadu"
            ),
            PestAlert(
                pestName = "Bollworm (गुलाबी सुंडी)",
                severity = "MEDIUM",
                cropType = "Cotton",
                description = "Pink bollworm larvae that damage cotton bolls by feeding inside, reducing yield and quality",
                symptoms = "Rosette flowers, small entry holes in green bolls, locule damage, pink lint inside bolls",
                treatment = "Plant Bt cotton varieties. Use pheromone traps. Spray profenofos or emamectin benzoate during flowering.",
                dateReported = "1 day ago",
                region = "Gujarat, Maharashtra"
            ),
            PestAlert(
                pestName = "Fall Armyworm (फॉल आर्मीवर्म)",
                severity = "HIGH",
                cropType = "Maize, Sorghum",
                description = "Highly destructive caterpillar that feeds on leaves creating large holes and can defoliate entire crops",
                symptoms = "Shot holes in leaves, frass on whorl, window-pane feeding pattern, defoliation, damaged cobs",
                treatment = "Early detection critical. Spray chlorantraniliprole or spinetoram. Use egg parasitoid Telenomus remus.",
                dateReported = "6 hours ago",
                region = "Karnataka, Telangana"
            ),
            PestAlert(
                pestName = "Whitefly (सफेद मक्खी)",
                severity = "MEDIUM",
                cropType = "Tomato, Cotton, Chili",
                description = "Tiny white flying insects that suck sap and transmit viral diseases causing severe crop losses",
                symptoms = "Yellowing leaves, sooty mold, leaf curling, reduced plant vigor, viral disease symptoms",
                treatment = "Use yellow sticky traps. Spray imidacloprid or thiamethoxam. Maintain field sanitation.",
                dateReported = "12 hours ago",
                region = "Haryana, Rajasthan"
            ),
            PestAlert(
                pestName = "Fruit Borer (फल छेदक)",
                severity = "MEDIUM",
                cropType = "Tomato, Brinjal",
                description = "Caterpillar that bores into fruits making them unmarketable and causing significant economic losses",
                symptoms = "Entry holes in fruits, frass near holes, fruit drop, internal damage visible when cut",
                treatment = "Install pheromone traps. Handpick and destroy affected fruits. Spray spinosad or indoxacarb.",
                dateReported = "8 hours ago",
                region = "Uttar Pradesh, Bihar"
            )
        ))
    }

    // UI State Management
    private fun showLoading() {
        loadingIndicator.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.GONE
        errorMessageText.visibility = View.GONE
    }

    private fun showData() {
        loadingIndicator.visibility = View.GONE

        if (dataList.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
            adapter.notifyDataSetChanged()
        }

        errorMessageText.visibility = View.GONE
    }

    private fun showError() {
        loadingIndicator.visibility = View.GONE
        recyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.GONE
        errorMessageText.visibility = View.VISIBLE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}