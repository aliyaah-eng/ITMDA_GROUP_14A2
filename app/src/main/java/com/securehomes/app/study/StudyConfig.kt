package com.securehomes.app.study

import kotlin.random.Random

/**
 * Settings for the A/B experiment.
 * Version A = guided wizard (proposed design), Version B = unguided form (baseline).
 */
object StudyConfig {

    const val GROUP_GUIDED = "guided"
    const val GROUP_UNGUIDED = "unguided"

    const val ROLE_USER = "user"
    const val ROLE_ADMIN = "admin"

    /** Simulated factory state of every new device. */
    const val DEFAULT_DEVICE_PASSWORD = "admin123"
    const val FACTORY_FIRMWARE = "v1.0.3"
    const val LATEST_FIRMWARE = "v2.4.1"
    const val FACTORY_ENCRYPTION = "WPA2_TKIP"

    fun versionLabel(group: String?): String = when (group) {
        GROUP_GUIDED -> "Version A (Guided)"
        GROUP_UNGUIDED -> "Version B (Unguided)"
        else -> "Not assigned"
    }

    /**
     * Balanced random assignment: a new participant joins the smaller group, and ties
     * are broken at random. Keeps both groups close in size for a fair comparison.
     */
    fun pickGroup(guidedCount: Int, unguidedCount: Int, random: Random = Random.Default): String = when {
        guidedCount < unguidedCount -> GROUP_GUIDED
        unguidedCount < guidedCount -> GROUP_UNGUIDED
        random.nextBoolean() -> GROUP_GUIDED
        else -> GROUP_UNGUIDED
    }
}
