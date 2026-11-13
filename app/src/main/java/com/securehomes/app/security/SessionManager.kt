package com.securehomes.app.security

import android.content.Context
import com.securehomes.app.models.User

/**
 * Remembers who is signed in. Only IDs and display data are stored (never passwords),
 * and a session expires after 30 minutes without activity.
 */
class SessionManager(context: Context) {

    companion object {
        const val TIMEOUT_MS = 30 * 60 * 1000L
        private const val PREFS = "securehomes_session"
    }

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun start(user: User) {
        prefs.edit()
            .putString("user_id", user.id)
            .putString("full_name", user.fullName)
            .putString("role", user.role)
            .putString("study_group", user.studyGroup)
            .putLong("last_active", System.currentTimeMillis())
            .apply()
    }

    val userId: String? get() = prefs.getString("user_id", null)
    val fullName: String get() = prefs.getString("full_name", "") ?: ""
    val role: String get() = prefs.getString("role", "") ?: ""
    val studyGroup: String? get() = prefs.getString("study_group", null)

    fun hasStoredUser(): Boolean = userId != null

    fun isLoggedIn(): Boolean =
        hasStoredUser() && System.currentTimeMillis() - prefs.getLong("last_active", 0) < TIMEOUT_MS

    fun touch() {
        prefs.edit().putLong("last_active", System.currentTimeMillis()).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
