package com.securehomes.app.util

/**
 * Input validation and password strength rules shared by every screen.
 * Pure Kotlin (no Android classes) so the rules are covered by unit tests.
 */
object Validators {

    const val MIN_PASSWORD_LENGTH = 8
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val IPV4 = Regex(
        "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$"
    )

    fun isValidEmail(email: String): Boolean = EMAIL.matches(email.trim())

    /** Names (people and devices) must be 2-40 characters. */
    fun isValidName(name: String): Boolean = name.trim().length in 2..40

    /** Account passwords: at least 8 characters with a letter and a digit. */
    fun isValidAccountPassword(password: String): Boolean =
        password.length >= MIN_PASSWORD_LENGTH &&
            password.any { it.isLetter() } &&
            password.any { it.isDigit() }

    fun isValidIpv4(ip: String): Boolean = IPV4.matches(ip.trim())

    /** A subnet mask is valid when its bits are all ones followed by all zeros. */
    fun isValidSubnetMask(mask: String): Boolean {
        if (!isValidIpv4(mask)) return false
        var bits = 0L
        mask.trim().split(".").forEach { bits = (bits shl 8) or it.toLong() }
        if (bits == 0L) return false
        val inverted = bits.inv() and 0xFFFFFFFFL
        return inverted and (inverted + 1) == 0L
    }

    /**
     * Password strength from 0 (empty) to 4 (strong):
     * 1 = under 8 characters or one character type, rising with variety and length.
     */
    fun passwordStrength(password: String): Int {
        if (password.isEmpty()) return 0
        if (password.length < MIN_PASSWORD_LENGTH) return 1
        var variety = 0
        if (password.any { it.isLowerCase() }) variety++
        if (password.any { it.isUpperCase() }) variety++
        if (password.any { it.isDigit() }) variety++
        if (password.any { !it.isLetterOrDigit() }) variety++
        var score = 1
        if (variety >= 2) score++
        if (variety >= 3) score++
        if (password.length >= 12 && variety >= 3) score++
        return score.coerceAtMost(4)
    }

    fun strengthLabel(score: Int): String = when (score) {
        4 -> "Strong"
        3 -> "Good"
        2 -> "Fair"
        1 -> "Weak"
        else -> "not set"
    }
}
