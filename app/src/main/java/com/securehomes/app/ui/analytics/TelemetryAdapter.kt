package com.securehomes.app.ui.analytics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.securehomes.app.databinding.ItemTelemetryBinding
import com.securehomes.app.models.TelemetryEvent

class TelemetryAdapter(
    private var events: List<TelemetryEvent>
) : RecyclerView.Adapter<TelemetryAdapter.TelemetryViewHolder>() {

    fun updateData(newEvents: List<TelemetryEvent>) {
        events = newEvents
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TelemetryViewHolder {
        val binding = ItemTelemetryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TelemetryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TelemetryViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount(): Int = events.size

    inner class TelemetryViewHolder(private val binding: ItemTelemetryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(event: TelemetryEvent) {
            binding.tvTelEventType.text = event.eventType
            binding.tvTelTimestamp.text = event.timestamp
            val parts = mutableListOf("Mode: ${event.setupMode.ifEmpty { "-" }}", "User: ${event.userId.takeLast(8)}")
            if (event.durationMs > 0) parts.add("${event.durationMs / 1000}s")
            if (event.errorCount > 0) parts.add("${event.errorCount} errors")
            event.securityScore?.let { parts.add("score $it%") }
            if (event.details.isNotEmpty()) parts.add(event.details)
            binding.tvTelDetails.text = parts.joinToString(" • ")
        }
    }
}
