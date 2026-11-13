package com.securehomes.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.securehomes.app.database.DatabaseHelper
import com.securehomes.app.databinding.ActivityLoginBinding
import com.securehomes.app.models.User
import com.securehomes.app.security.SessionManager
import com.securehomes.app.ui.dashboard.MainActivity
import com.securehomes.app.util.Validators

/** Sign in. If a valid session already exists, goes straight to the dashboard. */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = DatabaseHelper.getInstance(this)
        session = SessionManager(this)
        if (session.isLoggedIn()) {
            openDashboard()
            return
        }
        session.clear()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPresetAdmin.setOnClickListener {
            binding.etEmail.setText(DatabaseHelper.SEED_ADMIN_EMAIL)
            binding.etPassword.setText(DatabaseHelper.SEED_ADMIN_PASSWORD)
        }
        binding.btnPresetUser.setOnClickListener {
            binding.etEmail.setText(DatabaseHelper.SEED_USER_EMAIL)
            binding.etPassword.setText(DatabaseHelper.SEED_USER_PASSWORD)
        }
        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        if (!Validators.isValidEmail(email)) {
            binding.etEmail.error = "Enter a valid email address"
            return
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "Enter your password"
            return
        }
        binding.btnLogin.isEnabled = false
        // Password hashing is deliberately slow, so it runs off the UI thread.
        Thread {
            val user = db.loginUser(email, password)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                binding.btnLogin.isEnabled = true
                if (user == null) {
                    binding.etPassword.error = "Incorrect email or password"
                } else {
                    onLoggedIn(user)
                }
            }
        }.start()
    }

    private fun onLoggedIn(user: User) {
        session.start(user)
        db.log(user.id, "USER_LOGIN", user.studyGroup ?: "", "role=${user.role}")
        Toast.makeText(this, "Welcome back, ${user.fullName.split(" ").first()}!", Toast.LENGTH_SHORT).show()
        openDashboard()
    }

    private fun openDashboard() {
        startActivity(Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
}
