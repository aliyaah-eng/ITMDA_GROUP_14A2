package com.securehomes.app.models

data class SecurityEvaluation(
    val score: Int,
    val tier: String, // "SECURE (HIGH)", "ACCEPTABLE (MODERATE)", "VULNERABLE (LOW)"
    val vulnerabilities: List<String>,
    val recommendations: List<String>
)
