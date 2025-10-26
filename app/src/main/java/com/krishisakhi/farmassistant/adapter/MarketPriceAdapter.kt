package com.krishisakhi.farmassistant.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.krishisakhi.farmassistant.R
import com.krishisakhi.farmassistant.data.MarketPrice

class MarketPriceAdapter(private val items: MutableList<MarketPrice>) : RecyclerView.Adapter<MarketPriceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val statusIndicator: View = view.findViewById(R.id.statusIndicator)
        val itemIcon: ImageView = view.findViewById(R.id.itemIcon)
        val itemTitle: TextView = view.findViewById(R.id.itemTitle)
        val itemSubtitle: TextView = view.findViewById(R.id.itemSubtitle)
        val itemBadge: TextView = view.findViewById(R.id.itemBadge)
        val itemTimestamp: TextView = view.findViewById(R.id.itemTimestamp)
        val itemValue: TextView = view.findViewById(R.id.itemValue)
        val chevronIcon: ImageView = view.findViewById(R.id.chevronIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_marketprices_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val marketPrice = items[position] as MarketPrice

        // Set title and subtitle
        holder.itemTitle.text = marketPrice.commodityName
        holder.itemSubtitle.text = "${marketPrice.marketName} • ${marketPrice.category}"
        holder.itemTimestamp.text = marketPrice.lastUpdated

        // Show current price
        holder.itemValue.visibility = View.VISIBLE
        holder.itemValue.text = "₹${marketPrice.currentPrice.toInt()}/${marketPrice.unit}"

        // Calculate and show price change badge
        holder.itemBadge.visibility = View.VISIBLE
        val changePercentage = (marketPrice.currentPrice-marketPrice.previousPrice)/marketPrice.previousPrice*100

        if (changePercentage > 0) {
            holder.itemBadge.text = "↑ +${String.format("%.1f", changePercentage)}%"
            holder.itemBadge.setTextColor(Color.parseColor("#00D66C"))
        } else if (changePercentage < 0) {
            holder.itemBadge.text = "↓ ${String.format("%.1f", changePercentage)}%"
            holder.itemBadge.setTextColor(Color.parseColor("#EF4444"))
        } else {
            holder.itemBadge.text = "─ 0.0%"
            holder.itemBadge.setTextColor(Color.parseColor("#6B7280"))
        }

        // Hide status indicator and icon
        holder.statusIndicator.visibility = View.GONE
        holder.itemIcon.visibility = View.GONE

        // Click listener
        holder.itemView.setOnClickListener {
            // TODO: Show price trend or more details
            android.widget.Toast.makeText(
                holder.itemView.context,
                "Previous: ₹${marketPrice.previousPrice.toInt()}\nCurrent: ₹${marketPrice.currentPrice.toInt()}\nChange: ₹${changePercentage.toInt()}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
    override fun getItemCount() = items.size
}

// ============================================================================
// QUICK REFERENCE FOR COLORS
// ============================================================================
/*
PRIMARY COLORS:
- Primary Green: #00D66C
- Dark Background: #0F0F0F
- Card Background: #1A1A1A

TEXT COLORS:
- Text Primary: #FFFFFF
- Text Secondary: #9CA3AF
- Text Tertiary: #6B7280

STATUS/SEVERITY COLORS:
- Red (High/Error): #EF4444
- Orange (Medium/Warning): #F97316
- Yellow (Low/Info): #FDE047
- Gray (Closed/Inactive): #6B7280
- Green (Active/Success): #00D66C
*/