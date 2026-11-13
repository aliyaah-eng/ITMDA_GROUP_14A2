package com.securehomes.app

import com.securehomes.app.util.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    @Test
    fun emails() {
        assertTrue(Validators.isValidEmail("thina@example.com"))
        assertFalse(Validators.isValidEmail("no-at-sign.com"))
    }

    @Test
    fun accountPasswords() {
        assertTrue(Validators.isValidAccountPassword("secure123"))
        assertFalse(Validators.isValidAccountPassword("short1"))
        assertFalse(Validators.isValidAccountPassword("lettersonly"))
    }

    @Test
    fun ipv4AndSubnets() {
        assertTrue(Validators.isValidIpv4("192.168.1.20"))
        assertFalse(Validators.isValidIpv4("192.168.1.256"))
        assertTrue(Validators.isValidSubnetMask("255.255.255.0"))
        assertFalse(Validators.isValidSubnetMask("255.0.255.0"))
    }

    @Test
    fun passwordStrengthRises() {
        assertEquals(0, Validators.passwordStrength(""))
        assertEquals(1, Validators.passwordStrength("abc"))
        assertEquals(2, Validators.passwordStrength("admin123"))
        assertEquals(3, Validators.passwordStrength("Admin1234"))
        assertEquals(4, Validators.passwordStrength("Cam!Secure#2026"))
    }
}
