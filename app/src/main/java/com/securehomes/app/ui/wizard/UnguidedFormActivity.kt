package com.securehomes.app.ui.wizard

import android.os.Bundle
import android.widget.ArrayAdapter
import com.securehomes.app.databinding.ActivityUnguidedFormBinding
import com.securehomes.app.study.StudyConfig
import com.securehomes.app.util.Validators

/**
 * Version B (unguided baseline): every raw parameter on one technical form, like a legacy
 * router admin panel. No tips and no live score. Errors are counted from real validation
 * failures, exactly as in Version A.
 */
class UnguidedFormActivity : SetupBaseActivity() {

    override val mode = StudyConfig.GROUP_UNGUIDED

    private lateinit var binding: ActivityUnguidedFormBinding

    /** Spinner order, as legacy panels list them. */
    private val ciphers = listOf("WPA2_TKIP", "WPA2_AES", "WPA3_AES256", "OPEN")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing) return
        binding = ActivityUnguidedFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (!initAttempt(savedInstanceState)) return

        binding.spRawCipher.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            listOf("WPA2_TKIP (Legacy)", "WPA2_AES", "WPA3_AES256", "OPEN")
        )
        if (savedInstanceState == null) fillFromDevice()
        binding.btnSaveRawConfig.setOnClickListener { saveRawDevice() }
    }

    /** Current settings of the device: the insecure factory state for a new device. */
    private fun fillFromDevice() {
        binding.etRawName.setText(device.name)
        binding.etRawIp.setText(device.ipAddress)
        binding.etRawSubnet.setText(device.subnetMask)
        binding.etRawGateway.setText(device.gateway)
        // The factory password is shown, as on real admin panels; blank keeps an already-changed one.
        binding.etRawPassword.setText(if (device.defaultPasswordChanged) "" else StudyConfig.DEFAULT_DEVICE_PASSWORD)
        binding.spRawCipher.setSelection(ciphers.indexOf(device.encryption).coerceAtLeast(0))
        binding.cbRawAutoPatch.isChecked = device.autoUpdate
        binding.cbRawMFA.isChecked = device.twoFactorEnabled
        binding.cbRawVlan.isChecked = device.guestNetworkIsolated
        binding.cbRawUpnp.isChecked = device.upnpEnabled
    }

    /** Validates every field at once, as legacy panels do; each failure is a counted error. */
    private fun saveRawDevice() {
        val name = binding.etRawName.text.toString().trim()
        val ip = binding.etRawIp.text.toString().trim()
        val subnet = binding.etRawSubnet.text.toString().trim()
        val gateway = binding.etRawGateway.text.toString().trim()
        val password = binding.etRawPassword.text.toString()
        var valid = true

        if (!Validators.isValidName(name)) {
            binding.etRawName.error = "ERR: invalid hostname"
            recordError("hostname"); valid = false
        }
        if (!Validators.isValidIpv4(ip)) {
            binding.etRawIp.error = "ERR: invalid IPv4 address"
            recordError("ip"); valid = false
        }
        if (!Validators.isValidSubnetMask(subnet)) {
            binding.etRawSubnet.error = "ERR: invalid subnet mask"
            recordError("subnet"); valid = false
        }
        if (!Validators.isValidIpv4(gateway)) {
            binding.etRawGateway.error = "ERR: invalid gateway"
            recordError("gateway"); valid = false
        }
        val keepsCurrent = password.isEmpty() ||
            (!device.defaultPasswordChanged && password == StudyConfig.DEFAULT_DEVICE_PASSWORD)
        if (!keepsCurrent) {
            if (password == StudyConfig.DEFAULT_DEVICE_PASSWORD) {
                binding.etRawPassword.error = "ERR: default credential not permitted"
                recordError("password_is_default"); valid = false
            } else if (password.length < Validators.MIN_PASSWORD_LENGTH) {
                binding.etRawPassword.error = "ERR: password length < ${Validators.MIN_PASSWORD_LENGTH}"
                recordError("password_too_short"); valid = false
            }
        }
        if (!valid) return

        device.name = name
        device.ipAddress = ip
        device.subnetMask = subnet
        device.gateway = gateway
        if (!keepsCurrent) {
            device.defaultPasswordChanged = true
            device.passwordStrength = Validators.passwordStrength(password)
        }
        device.encryption = ciphers[binding.spRawCipher.selectedItemPosition.coerceIn(0, ciphers.lastIndex)]
        device.autoUpdate = binding.cbRawAutoPatch.isChecked
        device.twoFactorEnabled = binding.cbRawMFA.isChecked
        device.guestNetworkIsolated = binding.cbRawVlan.isChecked
        device.upnpEnabled = binding.cbRawUpnp.isChecked
        submit()
    }
}
