package com.krishisakhi.farmassistant

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.krishisakhi.farmassistant.adapter.MarketPriceAdapter
import com.krishisakhi.farmassistant.data.MarketPrice
import com.krishisakhi.farmassistant.db.AppDatabase
import com.krishisakhi.farmassistant.network.NetworkModule
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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
        //enableEdgeToEdge()
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
        loadingIndicator = findViewById(R.id.loadingIndicator)
        recyclerView = findViewById(R.id.recyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        emptyStateText = findViewById(R.id.emptyStateText)
        errorMessageText = findViewById(R.id.errorMessageText)

        headerIcon.setImageResource(R.drawable.ic_market)
        headerTitle.text = "Market Prices"
        emptyStateText.text = "No market prices available"

        // When user submits a search, query using that commodity and the saved state (if any)
//        searchInput.setOnEditorActionListener { v, actionId, _ ->
//            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
//                val query = v.text?.toString()?.trim()
//                loadData(query)
//                true
//            } else false
//        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
                adapter = MarketPriceAdapter(dataList)
                recyclerView.adapter = adapter
    }

    /**
     * Load market prices.
     * If commodity is null/blank, we will try to read the saved FarmerProfile from Room
     * and use the first primary crop and the saved state as defaults.
     */
    private fun loadData(commodity: String? = null, state: String? = null) {
        showLoading()

        val handler = CoroutineExceptionHandler { _, throwable ->
            throwable.printStackTrace()
            runOnUiThread {
                showError()
                Toast.makeText(this, "Failed to load market prices: ${'$'}{throwable.message}", Toast.LENGTH_LONG).show()
            }
        }

        CoroutineScope(Dispatchers.Main + handler).launch {
            var useCommodity: String? = commodity
            var useState: String? = state

            // If no explicit commodity/state provided, try reading from local DB
            if (useCommodity.isNullOrBlank() || useState.isNullOrBlank()) {
                try {
                    withContext(Dispatchers.IO) {
                        val db = AppDatabase.getDatabase(applicationContext)
                        val profiles = db.farmerProfileDao().getAllProfiles()
                        if (profiles.isNotEmpty()) {
                            val profile = profiles.first()
                            if (useCommodity.isNullOrBlank()) {
                                useCommodity = profile.primaryCrops.firstOrNull()
                            }
                            if (useState.isNullOrBlank()) {
                                useState = profile.state.takeIf { it.isNotBlank() }
                            }
                        }
                    }
                } catch (dbEx: Exception) {
                    // DB read failed — continue and attempt network call without profile defaults
                    dbEx.printStackTrace()
                }
            }

            // Update header to show active filters (helps debugging/will show values from DB)
            val headerSuffix = when {
                !useCommodity.isNullOrBlank() && !useState.isNullOrBlank() -> " — ${'$'}useCommodity (${useState})"
                !useCommodity.isNullOrBlank() -> " — ${'$'}useCommodity"
                !useState.isNullOrBlank() -> " — ${'$'}useState"
                else -> ""
            }
            //headerTitle.text = "Market Prices${headerSuffix}"

            try {
                val response = NetworkModule.fetchMarketPrices(useCommodity, useState)
                dataList.clear()
                dataList.addAll(response)
                showData()

                if (response.isEmpty()) {
                    Toast.makeText(this@MarketPricesActivity, "No market prices returned from server.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showError()
                Toast.makeText(this@MarketPricesActivity, "Could not fetch market prices: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
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