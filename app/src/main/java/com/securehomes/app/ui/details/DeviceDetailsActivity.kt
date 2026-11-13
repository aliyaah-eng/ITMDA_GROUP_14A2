package com.securehomes.app.ui.details

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.securehomes.app.R
import com.securehomes.app.databinding.ActivityDeviceDetailsBinding
import com.securehomes.app.models.Device
import com.securehomes.app.security.SecurityEvaluator
import com.securehomes.app.study.StudyConfig
import com.securehomes.app.ui.common.BaseActivity
import com.securehomes.app.ui.wizard.SetupRouter

/**
 * Device audit. Participants can reconfigure (update), update firmware or delete their own
 * devices. The researcher can view any device read-only.
 */
class DeviceDetailsActivity : BaseActivity() {

    private lateinit var binding: ActivityDeviceDetailsBinding
    private var deviceId: String = ""
    private var device: Device? = null

    private val isAdmin: Boolean get() = session.role == StudyConfig.ROLE_ADMIN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing) return
        binding = ActivityDeviceDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        deviceId = intent.getStringExtra("DEVICE_ID").orEmpty()

        binding.btnReconfigure.setOnClickListener {
            startActivity(SetupRouter.existingDevice(this, session.studyGroup, deviceId))
        }
        binding.btnUpdateFirmware.setOnClickListener {
            if (db.updateFirmware(deviceId, userId)) {
                db.log(userId, "FIRMWARE_UPDATED", session.studyGroup ?: "", "device=$deviceId")
                Toast.makeText(this, "Firmware updated to ${StudyConfig.LATEST_FIRMWARE}", Toast.LENGTH_SHORT).show()
            }
            loadDeviceDetails()
        }
        binding.btnDeleteDevice.setOnClickListener { confirmDeleteDevice() }
    }

    override fun onResume() {
        super.onResume()
        if (!isFinishing) loadDeviceDetails()
    }

    private fun loadDeviceDetails() {
        // Participants may only open their own devices.
        val d = db.getDeviceById(deviceId)?.takeIf { isAdmin || it.userId == userId }
        if (d == null) {
            Toast.makeText(this, "Device not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        device = d
        val eval = d.evaluate()

        binding.tvDetailName.text = d.name
        binding.tvDetailCategoryModel.text = "${d.category} • ${d.model}"
        binding.tvDetailScore.text = "${d.securityScore}%"
        val color = when {
            d.securityScore >= SecurityEvaluator.HARDENED_THRESHOLD -> R.color.success
            d.securityScore >= SecurityEvaluator.MODERATE_THRESHOLD -> R.color.warning
            else -> R.color.danger
        }
        binding.tvDetailScore.setTextColor(ContextCompat.getColor(this, color))

        binding.tvDetailParams.text = """
            Security Tier: ${eval.tier}
            MAC Address: ${d.macAddress}
            IP Address: ${d.ipAddress}
            Subnet Mask: ${d.subnetMask}
            Gateway: ${d.gateway}
            Firmware Version: ${d.firmwareVersion} (latest ${StudyConfig.LATEST_FIRMWARE})
            Encryption Cipher: ${d.encryption}
            Device Password: ${if (d.defaultPasswordChanged) "Changed" else "Factory default"}
            Two-Factor Auth: ${if (d.twoFactorEnabled) "Enabled" else "Disabled"}
            IoT VLAN Isolated: ${if (d.guestNetworkIsolated) "Yes" else "No"}
            UPnP Port Mapping: ${if (d.upnpEnabled) "Enabled" else "Disabled"}
            Auto Updates: ${if (d.autoUpdate) "Enabled" else "Disabled"}
            Last Configured With: ${StudyConfig.versionLabel(d.modeConfigured.ifEmpty { null })}
            Last Updated: ${d.lastUpdated}
        """.trimIndent()

        if (eval.vulnerabilities.isNotEmpty()) {
            binding.tvDetailVulns.text = eval.vulnerabilities.joinToString("\n• ", prefix = "• ")
            binding.tvDetailVulns.setTextColor(ContextCompat.getColor(this, R.color.danger))
        } else {
            binding.tvDetailVulns.text = "✓ No known vulnerabilities. This device meets the hardened standard."
            binding.tvDetailVulns.setTextColor(ContextCompat.getColor(this, R.color.success))
        }

        // The researcher's view is read-only.
        val canEdit = !isAdmin
        binding.btnReconfigure.visibility = if (canEdit) View.VISIBLE else View.GONE
        binding.btnDeleteDevice.visibility = if (canEdit) View.VISIBLE else View.GONE
        val outdated = d.firmwareVersion != StudyConfig.LATEST_FIRMWARE
        binding.btnUpdateFirmware.visibility = if (canEdit) View.VISIBLE else View.GONE
        binding.btnUpdateFirmware.isEnabled = outdated
        binding.btnUpdateFirmware.text = if (outdated) "Update Firmware to ${StudyConfig.LATEST_FIRMWARE}" else "Firmware Up to Date"
    }

    private fun confirmDeleteDevice() {
        val d = device ?: return
        AlertDialog.Builder(this)
            .setTitle("Remove Device")
            .setMessage("Permanently disconnect and delete ${d.name} from your network?")
            .setPositiveButton("Remove") { _, _ ->
                if (db.deleteDevice(d.id, userId)) {
                    db.log(userId, "DEVICE_DELETED", session.studyGroup ?: "", "device=${d.id}")
                    Toast.makeText(this, "Device removed from network", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
