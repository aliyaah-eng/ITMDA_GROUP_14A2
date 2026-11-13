package com.securehomes.app.models

import com.securehomes.app.security.SecurityEvaluator

/** A smart home device owned by a participant. */
data class Device(
    val id: String,
    val userId: String,
    var name: String,
    val category: String, // "Smart Camera", "Smart Lock", "Smart Plug", "Smart Light", "Smart Thermostat"
    val model: String,
    var ipAddress: String,
    var subnetMask: String = "255.255.255.0",
    var gateway: String = "192.168.1.1",
    val macAddress: String,
    var status: String = "online", // "online" or "offline"
    var securityScore: Int = 0,
    var firmwareVersion: String,
    var autoUpdate: Boolean,
    var encryption: String,
    var twoFactorEnabled: Boolean,
    var guestNetworkIsolated: Boolean,
    var defaultPasswordChanged: Boolean,
    var passwordStrength: Int,
    var upnpEnabled: Boolean,
    var modeConfigured: String, // "guided" or "unguided": the version used for the last setup
    var lastUpdated: String
) {
    /** Score, tier and vulnerabilities, always recalculated from the current settings. */
    fun evaluate(): SecurityEvaluation = SecurityEvaluator.evaluate(
        defaultPasswordChanged = defaultPasswordChanged,
        passwordStrength = passwordStrength,
        encryption = encryption,
        twoFactorEnabled = twoFactorEnabled,
        guestNetworkIsolated = guestNetworkIsolated,
        autoUpdate = autoUpdate,
        upnpEnabled = upnpEnabled,
        firmwareVersion = firmwareVersion
    )
}
