package com.securehomes.app

import com.securehomes.app.security.PasswordHasher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun verifiesCorrectPassword() = assertTrue(PasswordHasher.verify("Admin@1234", PasswordHasher.hash("Admin@1234")))

    @Test
    fun rejectsWrongPassword() = assertFalse(PasswordHasher.verify("admin@1234", PasswordHasher.hash("Admin@1234")))

    @Test
    fun samePasswordGetsDifferentSalts() = assertNotEquals(PasswordHasher.hash("User@1234"), PasswordHasher.hash("User@1234"))

    @Test
    fun neverStoresPlainText() = assertFalse(PasswordHasher.hash("User@1234").contains("User@1234"))

    @Test
    fun rejectsMalformedHashes() {
        assertFalse(PasswordHasher.verify("x", "not-a-hash"))
        assertFalse(PasswordHasher.verify("x", "60000:zz:zz"))
    }
}
