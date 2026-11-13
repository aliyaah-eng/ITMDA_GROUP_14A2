package com.securehomes.app.ui.wizard

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import com.securehomes.app.R
import com.securehomes.app.databinding.ActivityGuidedWizardBinding
import com.securehomes.app.security.SecurityEvaluator
import com.securehomes.app.study.StudyConfig
import com.securehomes.app.util.Validators

/**
 * Version A (guided): a 4-step wizard with progressive disclosure, plain-English tips,
 * a live password strength rating and a live security score on the review step.
 * It starts from the device's real current settings (factory state for new devices),
 * so participants must make every security choice themselves.
 */
class GuidedWizardActivity : SetupBaseActivity() {

    override val mode = StudyConfig.GROUP_GUIDED

    private lateinit var binding: ActivityGuidedWizardBinding

    /** Password state before this visit, restored if the participant clears the field. */
    private var originalPasswordChanged = false
    private var originalPasswordStrength = 0

    /** Spinner order, strongest first. */
    private val ciphers = listOf("WPA3_AES256", "WPA2_AES", "WPA2_TKIP", "OPEN")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing) return
        binding = ActivityGuidedWizardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (!initAttempt(savedInstanceState)) return
        originalPasswordChanged = device.defaultPasswordChanged
        originalPasswordStrength = device.passwordStrength

        binding.spWizEncryption.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            listOf(
                "WPA3-SAE (AES-256) - Highest Security",
                "WPA2-PSK (AES) - Standard Compatible",
                "WPA2-TKIP - Legacy / Weak",
                "OPEN - No Encryption (Vulnerable)"
            )
        )
        if (savedInstanceState == null) fillFromDevice()

        binding.etWizDevicePassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = updateStrengthLabel()
        })
        updateStrengthLabel()

        binding.btnWizardNext.setOnClickListener {
            if (!validateStep(currentStep)) return@setOnClickListener
            if (currentStep < 4) showStep(currentStep + 1) else saveDevice()
        }
        binding.btnWizardBack.setOnClickListener { onBackRequested() }

        db.log(userId, "GUIDE_STARTED", mode, "device=${device.id}")
        showStep(currentStep)
    }

    /** Current settings of the device: the insecure factory state for a new device. */
    private fun fillFromDevice() {
        binding.etWizDeviceName.setText(device.name)
        binding.switchWizGuestIso.isChecked = device.guestNetworkIsolated
        binding.switchWiz2FA.isChecked = device.twoFactorEnabled
        binding.switchWizAutoUpdate.isChecked = device.autoUpdate
        binding.spWizEncryption.setSelection(ciphers.indexOf(device.encryption).coerceAtLeast(0))
        binding.etWizDevicePassword.setText("")
    }

    override fun onBackRequested() {
        if (currentStep > 1) showStep(currentStep - 1) else confirmLeave()
    }

    private fun showStep(step: Int) {
        currentStep = step
        binding.pbWizard.max = 4
        binding.pbWizard.progress = step
        binding.step1Layout.visibility = if (step == 1) View.VISIBLE else View.GONE
        binding.step2Layout.visibility = if (step == 2) View.VISIBLE else View.GONE
        binding.step3Layout.visibility = if (step == 3) View.VISIBLE else View.GONE
        binding.step4Layout.visibility = if (step == 4) View.VISIBLE else View.GONE
        binding.btnWizardBack.visibility = View.VISIBLE
        binding.btnWizardBack.text = if (step == 1) "Cancel" else "Back"
        binding.btnWizardNext.text = if (step == 4) "Apply & Complete Configuration ✓" else "Next Step →"
        binding.tvWizardStepHeader.text = when (step) {
            1 -> "Step 1 of 4: Home Network Pairing"
            2 -> "Step 2 of 4: Device Access & 2FA"
            3 -> "Step 3 of 4: Encryption & Patching Policies"
            else -> "Step 4 of 4: Security Posture Review"
        }
        if (step == 4) showReview()
        db.log(userId, "STEP_ENTERED", mode, "step=$step")
    }

    /** Checks the current step; every failed check is counted as an error. */
    private fun validateStep(step: Int): Boolean {
        when (step) {
            1 -> {
                if (!Validators.isValidName(binding.etWizDeviceName.text.toString())) {
                    binding.etWizDeviceName.error = "Use between 2 and 40 characters"
                    recordError("device_name")
                    return false
                }
            }
            2 -> {
                val password = binding.etWizDevicePassword.text.toString()
                if (password == StudyConfig.DEFAULT_DEVICE_PASSWORD) {
                    binding.etWizDevicePassword.error = "That's the factory password. Choose a new one."
                    recordError("password_is_default")
                    return false
                }
                if (password.isNotEmpty() && password.length < Validators.MIN_PASSWORD_LENGTH) {
                    binding.etWizDevicePassword.error = "Use at least ${Validators.MIN_PASSWORD_LENGTH} characters"
                    recordError("password_too_short")
                    return false
                }
            }
        }
        db.log(userId, "STEP_COMPLETED", mode, "step=$step")
        return true
    }

    private fun updateStrengthLabel() {
        val password = binding.etWizDevicePassword.text.toString()
        val (text, color) = when {
            password.isEmpty() && !originalPasswordChanged ->
                "⚠ Still using the factory password (${StudyConfig.DEFAULT_DEVICE_PASSWORD}). Type a new one." to R.color.danger
            password.isEmpty() -> "Password unchanged (leave blank to keep the current one)" to R.color.text_secondary
            else -> {
                val strength = Validators.passwordStrength(password)
                "Password strength: ${Validators.strengthLabel(strength)}" to when {
                    strength >= 3 -> R.color.success
                    strength == 2 -> R.color.warning
                    else -> R.color.danger
                }
            }
        }
        binding.tvWizPwdEntropy.text = text
        binding.tvWizPwdEntropy.setTextColor(ContextCompat.getColor(this, color))
    }

    /** Copies the wizard choices into the device (without saving). */
    private fun applyChoices() {
        device.name = binding.etWizDeviceName.text.toString().trim()
        val password = binding.etWizDevicePassword.text.toString()
        if (password.isNotEmpty()) {
            device.defaultPasswordChanged = true
            device.passwordStrength = Validators.passwordStrength(password)
        } else {
            device.defaultPasswordChanged = originalPasswordChanged
            device.passwordStrength = originalPasswordStrength
        }
        device.encryption = ciphers[binding.spWizEncryption.selectedItemPosition.coerceIn(0, ciphers.lastIndex)]
        device.twoFactorEnabled = binding.switchWiz2FA.isChecked
        device.guestNetworkIsolated = binding.switchWizGuestIso.isChecked
        device.autoUpdate = binding.switchWizAutoUpdate.isChecked
        // Part of the guided design: UPnP is switched off automatically as a secure default.
        device.upnpEnabled = false
        if (device.guestNetworkIsolated) {
            device.gateway = "192.168.20.1"
            device.ipAddress = "192.168.20." + device.ipAddress.substringAfterLast('.')
        } else {
            device.gateway = "192.168.1.1"
            device.ipAddress = "192.168.1." + device.ipAddress.substringAfterLast('.')
        }
    }

    /** Step 4: live score and an honest checklist of each scored item. */
    private fun showReview() {
        applyChoices()
        val eval = device.evaluate()
        binding.tvReviewScore.text = "${eval.score}%"
        binding.tvReviewTier.text = eval.tier
        val color = when {
            eval.score >= SecurityEvaluator.HARDENED_THRESHOLD -> R.color.success
            eval.score >= SecurityEvaluator.MODERATE_THRESHOLD -> R.color.warning
            else -> R.color.danger
        }
        binding.tvReviewTier.setTextColor(ContextCompat.getColor(this, color))
        val pwdPoints = SecurityEvaluator.passwordPoints(device.defaultPasswordChanged, device.passwordStrength)
        fun line(ok: Boolean, text: String) = (if (ok) "✓ " else "⚠ ") + text
        binding.tvReviewChecklist.text = listOf(
            line(pwdPoints == 25, if (!device.defaultPasswordChanged) "Factory password still in use (0/25)"
                else "Device password: ${Validators.strengthLabel(device.passwordStrength)} ($pwdPoints/25)"),
            line(device.encryption.startsWith("WPA3"), "Encryption: ${device.encryption} (${SecurityEvaluator.encryptionPoints(device.encryption)}/25)"),
            line(device.twoFactorEnabled, "Two-Factor Auth: ${if (device.twoFactorEnabled) "On (20/20)" else "Off (0/20)"}"),
            line(device.guestNetworkIsolated, "IoT network isolation: ${if (device.guestNetworkIsolated) "On (15/15)" else "Off (0/15)"}"),
            line(device.autoUpdate, "Automatic patching: ${if (device.autoUpdate) "On (15/15)" else "Off (0/15)"}"),
            "✓ UPnP port mapping turned off automatically"
        ).joinToString("\n")
    }

    private fun saveDevice() {
        applyChoices()
        db.log(userId, "GUIDE_COMPLETED", mode, "device=${device.id}")
        submit()
    }
}
