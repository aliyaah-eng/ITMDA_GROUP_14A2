package com.securehomes.app.security

import com.securehomes.app.models.SecurityEvaluation
import com.securehomes.app.study.StudyConfig

/**
 * The 100-point security scoring engine.
 *
 *  - Password (25): 0 while the factory default is in use, otherwise scaled by strength (0-4)
 *  - Encryption (25): WPA3 25, WPA2-AES 18, WPA2-TKIP 5, open 0
 *  - Two-factor authentication (20)
 *  - Network isolation / IoT VLAN (15)
 *  - Automatic security updates (15)
 *
 * 85 or more counts as hardened. UPnP and outdated firmware are reported as advisories
 * but do not change the score.
 */
object SecurityEvaluator {

    const val HARDENED_THRESHOLD = 85
    const val MODERATE_THRESHOLD = 50

    fun passwordPoints(defaultPasswordChanged: Boolean, passwordStrength: Int): Int {
        if (!defaultPasswordChanged) return 0
        return Math.round(25f * passwordStrength.coerceIn(0, 4) / 4f)
    }

    fun encryptionPoints(encryption: String): Int = when (encryption) {
        "WPA3_AES256", "WPA3_SAE" -> 25
        "WPA2_AES", "WPA2_PSK" -> 18
        "WPA2_TKIP" -> 5
        else -> 0
    }

    fun evaluate(
        defaultPasswordChanged: Boolean,
        passwordStrength: Int,
        encryption: String,
        twoFactorEnabled: Boolean,
        guestNetworkIsolated: Boolean,
        autoUpdate: Boolean,
        upnpEnabled: Boolean = false,
        firmwareVersion: String = StudyConfig.LATEST_FIRMWARE
    ): SecurityEvaluation {
        val vulnerabilities = mutableListOf<String>()
        val recommendations = mutableListOf<String>()

        val pwdPoints = passwordPoints(defaultPasswordChanged, passwordStrength)
        if (!defaultPasswordChanged) {
            vulnerabilities.add("CRITICAL: Default factory password remains active. Susceptible to Mirai botnet exploits.")
            recommendations.add("Change the default factory password to a unique strong password.")
        } else if (passwordStrength <= 2) {
            vulnerabilities.add("MODERATE: Device password is weak.")
            recommendations.add("Use 12+ characters mixing upper and lower case, numbers and symbols.")
        }

        val encPoints = encryptionPoints(encryption)
        when (encPoints) {
            25 -> Unit
            18 -> recommendations.add("Upgrade to WPA3 encryption for stronger brute-force resistance.")
            5 -> {
                vulnerabilities.add("WARNING: Deprecated WPA2-TKIP cipher detected (vulnerable to key reinstallation attacks).")
                recommendations.add("Switch the wireless cipher from TKIP to AES or WPA3.")
            }
            else -> {
                vulnerabilities.add("CRITICAL: Unencrypted communication channel (OPEN Wi-Fi / plaintext).")
                recommendations.add("Enable WPA3-SAE or WPA2-AES wireless security.")
            }
        }

        if (!twoFactorEnabled) {
            vulnerabilities.add("MODERATE: Two-Factor Authentication is disabled.")
            recommendations.add("Enable Two-Factor Authentication (2FA) for management access.")
        }
        if (!guestNetworkIsolated) {
            vulnerabilities.add("WARNING: Device resides on the primary LAN with access to personal PCs.")
            recommendations.add("Isolate the device on a dedicated IoT VLAN or guest Wi-Fi network.")
        }
        if (!autoUpdate) {
            vulnerabilities.add("WARNING: Automatic security patching is disabled.")
            recommendations.add("Enable automatic background firmware updates.")
        }
        if (upnpEnabled) {
            vulnerabilities.add("ADVISORY: UPnP port mapping is enabled (not scored).")
            recommendations.add("Disable UPnP unless an app specifically needs it.")
        }
        if (firmwareVersion != StudyConfig.LATEST_FIRMWARE) {
            vulnerabilities.add("ADVISORY: Firmware $firmwareVersion is outdated (latest ${StudyConfig.LATEST_FIRMWARE}).")
            recommendations.add("Install the latest firmware update.")
        }

        val score = (pwdPoints + encPoints +
            (if (twoFactorEnabled) 20 else 0) +
            (if (guestNetworkIsolated) 15 else 0) +
            (if (autoUpdate) 15 else 0)).coerceIn(0, 100)

        return SecurityEvaluation(score, tierFor(score), vulnerabilities, recommendations)
    }

    fun tierFor(score: Int): String = when {
        score >= HARDENED_THRESHOLD -> "SECURE (HIGH)"
        score >= MODERATE_THRESHOLD -> "ACCEPTABLE (MODERATE)"
        else -> "VULNERABLE (LOW)"
    }
}
