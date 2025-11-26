package com.krishisakhi.farmassistant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.krishisakhi.farmassistant.data.NotificationItem

class NotificationAdapter(private val notifications: MutableList<NotificationItem> = mutableListOf()) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.notificationTitle)
        val descriptionText: TextView = view.findViewById(R.id.notificationDescription)
        val timeText: TextView = view.findViewById(R.id.notificationTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.titleText.text = notification.title
        holder.descriptionText.text = notification.description
        holder.timeText.text = notification.time
    }

    override fun getItemCount() = notifications.size

    // Replace current list with a new list and notify adapter
    fun setNotifications(newList: List<NotificationItem>) {
        notifications.clear()
        notifications.addAll(newList)
        notifyDataSetChanged()
    }
}
