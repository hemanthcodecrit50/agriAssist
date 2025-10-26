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
import com.krishisakhi.farmassistant.adapter.GovtSchemeAdapter
import com.krishisakhi.farmassistant.data.GovtScheme

class GovtSchemeActivity : AppCompatActivity() {

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

    private lateinit var adapter: GovtSchemeAdapter

    private val dataList = mutableListOf<GovtScheme>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_govt_scheme)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Govt Schemes"

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


        headerIcon.setImageResource(R.drawable.ic_govt)
        headerTitle.text = "Government Schemes"
        emptyStateText.text = "No schemes available"

    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)


        adapter = GovtSchemeAdapter(dataList)
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
            GovtScheme(
                schemeName = "PM-KISAN (प्रधानमंत्री किसान सम्मान निधि)",
                authority = "Ministry of Agriculture & Farmers Welfare",
                category = "Direct Benefit Transfer",
                description = "Income support of ₹6000 per year to all farmer families holding cultivable land. Amount paid in 3 equal installments of ₹2000 each directly to bank accounts.",
                eligibility = listOf(
                    "All farmer families irrespective of size of land holding",
                    "Land ownership records required",
                    "Aadhaar card mandatory",
                    "Bank account linked with Aadhaar"
                ),
                benefits = "₹6000 per year in 3 installments of ₹2000 each (paid every 4 months)",
                deadline = "Ongoing - Register anytime",
                status = "ACTIVE"
            ),
            GovtScheme(
                schemeName = "Kisan Credit Card (किसान क्रेडिट कार्ड)",
                authority = "NABARD & Ministry of Agriculture",
                category = "Credit/Loan",
                description = "Provides timely access to credit for agriculture and allied activities at subsidized interest rates. Farmers can withdraw money as needed up to sanctioned limit.",
                eligibility = listOf(
                    "All farmers - Owner cultivators and tenant farmers",
                    "Land ownership proof or lease agreement",
                    "Age: 18-75 years",
                    "Good credit history"
                ),
                benefits = "Credit up to ₹3 lakh at 4% interest (with 3% subvention). Crop insurance included.",
                deadline = "Ongoing enrollment",
                status = "ACTIVE"
            ),
            GovtScheme(
                schemeName = "PM Fasal Bima Yojana (प्रधानमंत्री फसल बीमा योजना)",
                authority = "Ministry of Agriculture & Farmers Welfare",
                category = "Insurance",
                description = "Comprehensive crop insurance scheme protecting farmers against crop loss due to natural calamities, pests & diseases. Very low premium with high coverage.",
                eligibility = listOf(
                    "All farmers growing notified crops",
                    "Both loanee and non-loanee farmers eligible",
                    "Enrollment before sowing deadline"
                ),
                benefits = "Insurance coverage: 2% premium for Kharif, 1.5% for Rabi crops. Rest paid by government.",
                deadline = "Season-wise enrollment (Kharif: Jun-Jul, Rabi: Oct-Dec)",
                status = "ACTIVE"
            ),
            GovtScheme(
                schemeName = "Soil Health Card Scheme (मृदा स्वास्थ्य कार्ड योजना)",
                authority = "Department of Agriculture & Farmers Welfare",
                category = "Subsidy",
                description = "Provides soil health cards to farmers every 2 years with recommendations on nutrients and fertilizers required for improving productivity and soil health.",
                eligibility = listOf(
                    "All farmers with agricultural land",
                    "Both irrigated and rainfed areas covered",
                    "No minimum land requirement"
                ),
                benefits = "Free soil testing every 2 years. Customized fertilizer recommendations. Reduces input costs by 15-20%.",
                deadline = "31 December 2025",
                status = "ACTIVE"
            ),
            GovtScheme(
                schemeName = "Sub-Mission on Agricultural Mechanization (कृषि यंत्रीकरण पर उप मिशन)",
                authority = "Ministry of Agriculture & Farmers Welfare",
                category = "Subsidy",
                description = "Financial assistance for purchase of agricultural machinery and equipment. Aims to increase mechanization from current 40% to 50% in next 5 years.",
                eligibility = listOf(
                    "Individual farmers, Self Help Groups, FPOs",
                    "Women farmers get priority",
                    "SC/ST/Small/Marginal farmers eligible"
                ),
                benefits = "40-50% subsidy on farm equipment purchase (up to ₹1 lakh). Custom Hiring Centres supported.",
                deadline = "31 March 2026",
                status = "ACTIVE"
            ),
            GovtScheme(
                schemeName = "National Mission for Sustainable Agriculture (राष्ट्रीय सतत कृषि मिशन)",
                authority = "Ministry of Agriculture & Farmers Welfare",
                category = "Training & Development",
                description = "Promotes sustainable agricultural practices through training, demonstrations, and adoption of climate-resilient technologies and practices.",
                eligibility = listOf(
                    "Farmers in rainfed areas given priority",
                    "Focus on water use efficiency",
                    "Groups/FPOs can apply for demonstrations"
                ),
                benefits = "Free training programs. 50% subsidy on drip/sprinkler systems. Soil conservation support.",
                deadline = "Ongoing - Quarterly intake",
                status = "ACTIVE"
            ),
            GovtScheme(
                schemeName = "Pradhan Mantri Krishi Sinchai Yojana (पीएम कृषि सिंचाई योजना)",
                authority = "Ministry of Jal Shakti & Agriculture",
                category = "Infrastructure",
                description = "Aims to expand cultivated area under irrigation and improve water use efficiency. Focus on 'Per Drop More Crop' initiative.",
                eligibility = listOf(
                    "Individual farmers and groups",
                    "Land ownership required",
                    "Priority to water-stressed areas"
                ),
                benefits = "Subsidy: 55% for small/marginal, 45% for others on micro-irrigation. Max ₹2.5 lakh/farmer.",
                deadline = "Rolling admissions",
                status = "ACTIVE"
            ),
            GovtScheme(
                schemeName = "Kisan Rail Scheme (किसान रेल योजना)",
                authority = "Ministry of Railways & Agriculture",
                category = "Logistics",
                description = "Special parcel trains for transporting perishable agricultural products quickly at subsidized freight rates to reduce post-harvest losses.",
                eligibility = listOf(
                    "Farmers, FPOs, and cooperatives",
                    "Transporting fruits, vegetables, dairy, meat, fish",
                    "Minimum 5 tonnes required for booking"
                ),
                benefits = "50% subsidy on freight charges for fruits & vegetables. Fast delivery with refrigerated coaches.",
                deadline = "Service available year-round",
                status = "ACTIVE"
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