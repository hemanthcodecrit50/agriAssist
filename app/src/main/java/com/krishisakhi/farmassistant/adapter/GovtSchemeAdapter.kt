package com.krishisakhi.farmassistant.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.krishisakhi.farmassistant.R
import com.krishisakhi.farmassistant.data.GovtScheme

class GovtSchemeAdapter(private val items: List<GovtScheme>) : RecyclerView.Adapter<GovtSchemeAdapter.ViewHolder>() {

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
            .inflate(R.layout.item_govtscheme_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val scheme = items[position] as GovtScheme

        // Set title and subtitle
        holder.itemTitle.text = scheme.schemeName
        holder.itemSubtitle.text = "${scheme.authority}\n${scheme.description}"
        holder.itemTimestamp.text = "Deadline: ${scheme.deadline}"

        // Show status badge
        holder.itemBadge.visibility = View.VISIBLE
        holder.itemBadge.text = scheme.status

        // Color code status
        when (scheme.status) {
            "ACTIVE" -> {
                holder.itemBadge.setTextColor(Color.parseColor("#00D66C"))
                holder.statusIndicator.setBackgroundColor(Color.parseColor("#00D66C"))
                holder.statusIndicator.visibility = View.VISIBLE
            }
            "UPCOMING" -> {
                holder.itemBadge.setTextColor(Color.parseColor("#F97316"))
                holder.statusIndicator.setBackgroundColor(Color.parseColor("#F97316"))
                holder.statusIndicator.visibility = View.VISIBLE
            }
            "CLOSED" -> {
                holder.itemBadge.setTextColor(Color.parseColor("#6B7280"))
                holder.statusIndicator.setBackgroundColor(Color.parseColor("#6B7280"))
                holder.statusIndicator.visibility = View.VISIBLE
            }
        }

        // Show government icon
        holder.itemIcon.visibility = View.VISIBLE
        holder.itemIcon.setImageResource(R.drawable.ic_govt)

        // Hide value
        holder.itemValue.visibility = View.GONE

        // Click listener
        holder.itemView.setOnClickListener {
            // TODO: Navigate to scheme detail activity
            // Example:
            // val context = holder.itemView.context
            // val intent = Intent(context, SchemeDetailActivity::class.java)
            // intent.putExtra("SCHEME_NAME", scheme.schemeName)
            // intent.putExtra("SCHEME_DESCRIPTION", scheme.description)
            // intent.putExtra("SCHEME_BENEFITS", scheme.benefits)
            // intent.putExtra("SCHEME_ELIGIBILITY", scheme.eligibility.joinToString("\n• "))
            // context.startActivity(intent)

            // OR show a dialog with details
            val eligibilityText = scheme.eligibility.joinToString("\n• ", "• ")
            android.widget.Toast.makeText(
                holder.itemView.context,
                "Benefits: ${scheme.benefits}\n\nEligibility:\n$eligibilityText",
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