package com.securehomes.app.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.securehomes.app.R
import com.securehomes.app.databinding.ItemDeviceBinding
import com.securehomes.app.models.Device
import com.securehomes.app.models.DeviceCatalog
import com.securehomes.app.security.SecurityEvaluator

/** Device cards on the dashboard. The power switch is hidden in the researcher's read-only view. */
class DeviceAdapter(
    private var devices: List<Device>,
    private val canControl: Boolean,
    private val onAuditClick: (Device) -> Unit,
    private val onToggleClick: (Device) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    fun updateData(newDevices: List<Device>) {
        devices = newDevices
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder =
        DeviceViewHolder(ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) = holder.bind(devices[position])

    override fun getItemCount(): Int = devices.size

    inner class DeviceViewHolder(private val binding: ItemDeviceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(device: Device) {
            val context = binding.root.context
            binding.tvDeviceName.text = device.name
            binding.tvDeviceMeta.text = "${device.model} • ${device.ipAddress}"
            binding.tvDeviceIcon.setImageResource(DeviceCatalog.forCategory(device.category).iconRes)

            val isOnline = device.status == "online"
            binding.tvDeviceStatus.text = if (isOnline) "Online" else "Offline"
            binding.tvDeviceStatus.setTextColor(ContextCompat.getColor(context,
                if (isOnline) R.color.success else R.color.text_muted))

            // Clear the listener before setting the state, so recycling a view never fires it.
            binding.switchPower.setOnCheckedChangeListener(null)
            binding.switchPower.isChecked = isOnline
            binding.switchPower.isEnabled = canControl
            binding.switchPower.setOnCheckedChangeListener { _, _ -> onToggleClick(device) }

            val color = when {
                device.securityScore >= SecurityEvaluator.HARDENED_THRESHOLD -> R.color.success
                device.securityScore >= SecurityEvaluator.MODERATE_THRESHOLD -> R.color.warning
                else -> R.color.danger
            }
            binding.tvDeviceScore.text = " Score: ${device.securityScore}%"
            binding.tvDeviceScore.setTextColor(ContextCompat.getColor(context, color))
            binding.btnAuditSettings.setOnClickListener { onAuditClick(device) }
        }
    }
}
