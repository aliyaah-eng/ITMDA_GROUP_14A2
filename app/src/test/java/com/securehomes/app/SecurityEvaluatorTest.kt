package com.securehomes.app

import com.securehomes.app.security.SecurityEvaluator
import com.securehomes.app.study.StudyConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityEvaluatorTest {

    private fun hardened() = SecurityEvaluator.evaluate(
        defaultPasswordChanged = true, passwordStrength = 4, encryption = "WPA3_AES256",
        twoFactorEnabled = true, guestNetworkIsolated = true, autoUpdate = true
    )

    @Test
    fun fullyHardenedDeviceScores100WithNoVulnerabilities() {
        val eval = hardened()
        assertEquals(100, eval.score)
        assertEquals("SECURE (HIGH)", eval.tier)
        assertTrue(eval.vulnerabilities.isEmpty())
    }

    @Test
    fun factoryDeviceScoresFive() {
        val eval = SecurityEvaluator.evaluate(
            defaultPasswordChanged = false, passwordStrength = 2, encryption = StudyConfig.FACTORY_ENCRYPTION,
            twoFactorEnabled = false, guestNetworkIsolated = false, autoUpdate = false,
            upnpEnabled = true, firmwareVersion = StudyConfig.FACTORY_FIRMWARE
        )
        assertEquals(5, eval.score)
        assertEquals("VULNERABLE (LOW)", eval.tier)
        assertEquals(7, eval.vulnerabilities.size)
    }

    @Test
    fun weightsMatchTheDesign() {
        assertEquals(0, SecurityEvaluator.passwordPoints(false, 4))
        assertEquals(25, SecurityEvaluator.passwordPoints(true, 4))
        assertEquals(25, SecurityEvaluator.encryptionPoints("WPA3_AES256"))
        assertEquals(18, SecurityEvaluator.encryptionPoints("WPA2_AES"))
        assertEquals(5, SecurityEvaluator.encryptionPoints("WPA2_TKIP"))
        assertEquals(0, SecurityEvaluator.encryptionPoints("OPEN"))
    }

    @Test
    fun hardenedThresholdIs85() {
        val noIsolation = SecurityEvaluator.evaluate(true, 4, "WPA3_AES256", true, false, true)
        assertEquals(85, noIsolation.score)
        assertEquals("SECURE (HIGH)", noIsolation.tier)
        val no2fa = SecurityEvaluator.evaluate(true, 4, "WPA3_AES256", false, true, true)
        assertEquals(80, no2fa.score)
        assertEquals("ACCEPTABLE (MODERATE)", no2fa.tier)
    }

    @Test
    fun upnpAndFirmwareAreAdvisoriesNotScored() {
        val eval = SecurityEvaluator.evaluate(true, 4, "WPA3_AES256", true, true, true,
            upnpEnabled = true, firmwareVersion = "v1.0.0")
        assertEquals(100, eval.score)
        assertEquals(2, eval.vulnerabilities.size)
    }
}
