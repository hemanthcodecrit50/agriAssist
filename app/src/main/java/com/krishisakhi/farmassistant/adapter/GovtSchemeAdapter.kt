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
        val scheme = items[position]
        val ctx = holder.itemView.context

        // Set title and subtitle using string resources
        holder.itemTitle.text = scheme.schemeName
        holder.itemSubtitle.text = ctx.getString(R.string.scheme_subtitle_format, scheme.authority, scheme.description)
        holder.itemTimestamp.text = ctx.getString(R.string.deadline_format, scheme.deadline)

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

        // Click listener on chevron -> show details dialog with ivory background
        holder.chevronIcon.setOnClickListener {
            val eligibilityText = scheme.eligibility.joinToString("\n• ", "• ")
            val message = ctx.getString(R.string.scheme_detail_format, scheme.benefits, eligibilityText)
            val builder = androidx.appcompat.app.AlertDialog.Builder(ctx)
                .setTitle(scheme.schemeName)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)

            val dialog = builder.create()
            dialog.show()
            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_bg_ivory)
        }
    }

    override fun getItemCount() = items.size
}

/* NOTE: color reference comments removed to avoid duplication; use res/values/colors.xml */
