package com.securehomes.app.ui.common

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.securehomes.app.database.DatabaseHelper
import com.securehomes.app.security.SessionManager
import com.securehomes.app.ui.auth.LoginActivity

/**
 * Base class for every screen that needs a signed-in user. Sends the user back to login
 * when there is no session, when it has timed out, or when the screen is meant for another
 * role. Subclasses start onCreate with:
 *
 *     super.onCreate(savedInstanceState)
 *     if (isFinishing) return
 */
abstract class BaseActivity : AppCompatActivity() {

    protected lateinit var session: SessionManager
    protected lateinit var db: DatabaseHelper

    /** Role allowed on this screen ("user" or "admin"), or null for any signed-in user. */
    protected open val requiredRole: String? = null

    /** The signed-in user's ID. Only valid after onCreate has passed the access check. */
    protected val userId: String get() = session.userId ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionManager(this)
        db = DatabaseHelper.getInstance(this)
        ensureAccess()
    }

    override fun onResume() {
        super.onResume()
        if (ensureAccess()) session.touch()
    }

    private fun ensureAccess(): Boolean {
        if (isFinishing) return false
        if (!session.isLoggedIn()) {
            if (session.hasStoredUser()) {
                db.log(userId, "SESSION_ENDED", session.studyGroup ?: "", "timeout")
                session.clear()
                Toast.makeText(this, "Your session expired. Please sign in again.", Toast.LENGTH_LONG).show()
            }
            openLogin()
            return false
        }
        val role = requiredRole
        if (role != null && session.role != role) {
            Toast.makeText(this, "You don't have access to that screen.", Toast.LENGTH_SHORT).show()
            openLogin()
            return false
        }
        return true
    }

    protected fun logout() {
        db.log(userId, "SESSION_ENDED", session.studyGroup ?: "", "logout")
        session.clear()
        Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show()
        openLogin()
    }

    /** Opens login and clears the back stack so Back cannot return to a protected screen. */
    private fun openLogin() {
        startActivity(Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
}
