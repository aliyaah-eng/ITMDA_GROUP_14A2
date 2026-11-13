package com.securehomes.app.models

data class TelemetryEvent(
    val id: String,
    val userId: String,
    val eventType: String,
    val setupMode: String,
    val durationMs: Long = 0,
    val errorCount: Int = 0,
    /** Device security score when a setup is submitted, otherwise null. */
    val securityScore: Int? = null,
    val timestamp: String,
    val details: String = ""
)
