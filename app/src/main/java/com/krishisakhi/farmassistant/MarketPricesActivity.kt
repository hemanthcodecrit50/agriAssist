package com.krishisakhi.farmassistant

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.krishisakhi.farmassistant.adapter.MarketPriceAdapter
import com.krishisakhi.farmassistant.data.MarketPrice


class MarketPricesActivity : AppCompatActivity() {

    private lateinit var headerIcon: ImageView
    private lateinit var headerTitle: TextView
    private lateinit var filterCard: MaterialCardView
    private lateinit var searchInput: EditText
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var emptyStateText: TextView
    private lateinit var errorMessageText: TextView

    private lateinit var adapter: MarketPriceAdapter
    private val dataList = mutableListOf<MarketPrice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_market_prices)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Market Prices"

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

        headerIcon.setImageResource(R.drawable.ic_market)
        headerTitle.text = "Market Prices"
        emptyStateText.text = "No market prices available"

    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
                adapter = MarketPriceAdapter(dataList)
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
            MarketPrice(
                commodityName = "Wheat (गेहूं)",
                currentPrice = 2100.0F,
                previousPrice = 2050.0F,
                unit = "quintal",
                marketName = "APMC Bangalore",
                category = "Grains",
                lastUpdated = "2 hours ago"
            ),
            MarketPrice(
                commodityName = "Rice (चावल)",
                currentPrice = 3500.0F,
                previousPrice = 3600.0F,
                unit = "quintal",
                marketName = "APMC Bangalore",
                category = "Grains",
                lastUpdated = "2 hours ago"
            ),
            MarketPrice(
                commodityName = "Tomato (टमाटर)",
                currentPrice = 25.0F,
                previousPrice = 22.0F,
                unit = "kg",
                marketName = "Yeshwanthpur Market",
                category = "Vegetables",
                lastUpdated = "1 hour ago"
            ),
            MarketPrice(
                commodityName = "Onion (प्याज)",
                currentPrice = 40.0F,
                previousPrice = 45.0F,
                unit = "kg",
                marketName = "KR Market Bangalore",
                category = "Vegetables",
                lastUpdated = "4 hours ago"
            ),
            MarketPrice(
                commodityName = "Potato (आलू)",
                currentPrice = 18.0F,
                previousPrice = 20.0F,
                unit = "kg",
                marketName = "Azadpur Mandi Delhi",
                category = "Vegetables",
                lastUpdated = "3 hours ago"
            ),
            MarketPrice(
                commodityName = "Cotton (कपास)",
                currentPrice = 5800.0F,
                previousPrice = 5750.0F,
                unit = "quintal",
                marketName = "APMC Hubli",
                category = "Cash Crops",
                lastUpdated = "5 hours ago"
            ),
            MarketPrice(
                commodityName = "Soybean (सोयाबीन)",
                currentPrice = 4200.0F,
                previousPrice = 4300.0F,
                unit = "quintal",
                marketName = "APMC Indore",
                category = "Pulses",
                lastUpdated = "6 hours ago"
            ),
            MarketPrice(
                commodityName = "Sugarcane (गन्ना)",
                currentPrice = 310.0F,
                previousPrice = 305.0F,
                unit = "quintal",
                marketName = "UP Sugar Mills",
                category = "Cash Crops",
                lastUpdated = "1 day ago"
            ),
            MarketPrice(
                commodityName = "Turmeric (हल्दी)",
                currentPrice = 7500.0F,
                previousPrice = 7200.0F,
                unit = "quintal",
                marketName = "APMC Nizamabad",
                category = "Spices",
                lastUpdated = "4 hours ago"
            ),
            MarketPrice(
                commodityName = "Banana (केला)",
                currentPrice = 35.0F,
                previousPrice = 32.0F,
                unit = "dozen",
                marketName = "Yeshwanthpur Market",
                category = "Fruits",
                lastUpdated = "2 hours ago"
            )
        ))
    }

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