package com.securehomes.app.models

/** Results for one study version, calculated from the database for the analytics screen. */
data class GroupStats(
    val participants: Int,
    val submitted: Int,
    val abandoned: Int,
    val hardened: Int,
    /** Averages are null when there is no data yet. */
    val avgDurationMs: Double?,
    val avgErrors: Double?,
    val avgScore: Double?,
    val avgSus: Double?,
    val avgTlx: Double?,
    val surveys: Int
) {
    val attempts: Int get() = submitted + abandoned
    val abandonmentRate: Double? get() = if (attempts == 0) null else abandoned.toDouble() / attempts
    val hardenedRate: Double? get() = if (submitted == 0) null else hardened.toDouble() / submitted
}
