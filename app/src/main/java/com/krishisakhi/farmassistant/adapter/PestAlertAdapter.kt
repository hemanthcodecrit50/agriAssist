package com.krishisakhi.farmassistant.adapter

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.krishisakhi.farmassistant.R
import com.krishisakhi.farmassistant.data.PestAlert

class PestAlertAdapter(private val items: MutableList<PestAlert>) : RecyclerView.Adapter<PestAlertAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val statusIndicator: View = view.findViewById(R.id.statusIndicator)
        val itemIcon: ImageView = view.findViewById(R.id.itemIcon)
        val itemTitle: TextView = view.findViewById(R.id.itemTitle)
        val itemSubtitle: TextView = view.findViewById(R.id.itemSubtitle)
        val itemBadge: TextView = view.findViewById(R.id.itemBadge)
        val itemTimestamp: TextView = view.findViewById(R.id.itemTimestamp)
        val itemValue: TextView = view.findViewById(R.id.itemValue)
        // added chevron reference
        val chevronIcon: ImageView = view.findViewById(R.id.chevronIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pestalert_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pestAlert = items[position]
        val ctx = holder.itemView.context

        // Set title and subtitle
        holder.itemTitle.text = pestAlert.pestName
        holder.itemSubtitle.text = ctx.getString(R.string.pest_subtitle_format, pestAlert.cropType, pestAlert.description)
        holder.itemTimestamp.text = pestAlert.dateReported

        // Show severity badge
        holder.itemBadge.text = pestAlert.severity
        holder.itemBadge.visibility = View.VISIBLE

        // Color code severity indicator and badge
        holder.statusIndicator.visibility = View.VISIBLE
        when (pestAlert.severity) {
            "HIGH" -> {
                holder.statusIndicator.setBackgroundColor(Color.parseColor("#EF4444"))
                holder.itemBadge.setTextColor(Color.parseColor("#EF4444"))
            }
            "MEDIUM" -> {
                holder.statusIndicator.setBackgroundColor(Color.parseColor("#F97316"))
                holder.itemBadge.setTextColor(Color.parseColor("#F97316"))
            }
            "LOW" -> {
                holder.statusIndicator.setBackgroundColor(Color.parseColor("#FDE047"))
                holder.itemBadge.setTextColor(Color.parseColor("#FDE047"))
            }
        }

        // Hide icon and value for pest alerts
        holder.itemIcon.visibility = View.GONE
        holder.itemValue.visibility = View.GONE

        // Click listener for chevron (show details in a dialog with ivory background)
        holder.chevronIcon.setOnClickListener {
            val message = ctx.getString(R.string.pest_detail_format, pestAlert.symptoms, pestAlert.treatment)
            val builder = androidx.appcompat.app.AlertDialog.Builder(ctx)
                .setTitle(pestAlert.pestName)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)

            val dialog = builder.create()
            dialog.show()
            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_bg_ivory)
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