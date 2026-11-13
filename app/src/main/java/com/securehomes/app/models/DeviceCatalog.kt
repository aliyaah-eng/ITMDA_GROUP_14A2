package com.securehomes.app.models

import androidx.annotation.DrawableRes
import com.securehomes.app.R

/** The consumer device types in the study's scope, with the models the radar can "find". */
enum class DeviceCatalog(
    val category: String,
    val emoji: String,
    @DrawableRes val iconRes: Int,
    val defaultName: String,
    val models: List<String>,
    val macPrefix: String
) {
    CAMERA("Smart Camera", "📹", R.drawable.camera, "Living Room Camera", listOf("SecureEye Ultra HD 4K", "SecureEye Mini"), "3C:84:6A"),
    LOCK("Smart Lock", "🔐", R.drawable.lock, "Front Door Lock", listOf("FortressLock Pro Biometric", "FortressLock Lite"), "A4:C3:F0"),
    PLUG("Smart Plug", "🔌", R.drawable.plug, "Kitchen Plug", listOf("EcoPower Smart Outlet", "EcoPower Duo"), "24:6F:28"),
    LIGHT("Smart Light", "💡", R.drawable.bulb, "Bedroom Light", listOf("LuminaHue RGB Smart Bulb", "LuminaHue White"), "48:3F:DA"),
    THERMOSTAT("Smart Thermostat", "🌡️", R.drawable.thermostat, "Hallway Thermostat", listOf("ClimaSense Smart Thermostat", "ClimaSense Go"), "68:C6:3A");

    companion object {
        fun forCategory(category: String): DeviceCatalog = values().firstOrNull { it.category == category } ?: CAMERA
    }
}
