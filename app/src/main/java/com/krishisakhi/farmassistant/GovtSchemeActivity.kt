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
import com.krishisakhi.farmassistant.network.NetworkModule
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

        val handler = CoroutineExceptionHandler { _, throwable ->
            throwable.printStackTrace()
            runOnUiThread {
                showData()
            }
        }

        CoroutineScope(Dispatchers.Main + handler).launch {
            try {
                val response = NetworkModule.fetchGovtSchemes()
                dataList.clear()
                dataList.addAll(response)
                showData()
            } catch (e: Exception) {
                e.printStackTrace()
                showData()
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