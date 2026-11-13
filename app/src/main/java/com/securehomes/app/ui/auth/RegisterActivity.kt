package com.securehomes.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.securehomes.app.database.DatabaseHelper
import com.securehomes.app.databinding.ActivityRegisterBinding
import com.securehomes.app.security.SessionManager
import com.securehomes.app.ui.dashboard.MainActivity
import com.securehomes.app.util.Validators

/**
 * Participant registration. Each new participant is assigned to Version A or B
 * (balanced random) and signed in straight away. The version is not shown to them.
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = DatabaseHelper.getInstance(this)

        binding.btnRegister.setOnClickListener { attemptRegister() }
        binding.tvBackToLogin.setOnClickListener { finish() }
    }

    private fun attemptRegister() {
        val fullName = binding.etRegFullName.text?.toString()?.trim().orEmpty()
        val email = binding.etRegEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etRegPassword.text?.toString().orEmpty()

        var valid = true
        if (!Validators.isValidName(fullName)) {
            binding.etRegFullName.error = "Enter your name (2-40 characters)"; valid = false
        }
        if (!Validators.isValidEmail(email)) {
            binding.etRegEmail.error = "Enter a valid email address"; valid = false
        }
        if (!Validators.isValidAccountPassword(password)) {
            binding.etRegPassword.error = "At least 8 characters with a letter and a number"; valid = false
        }
        if (!valid) return

        binding.btnRegister.isEnabled = false
        Thread {
            val user = db.registerParticipant(fullName, email, password)
            if (user != null) db.log(user.id, "USER_REGISTERED", user.studyGroup ?: "", "")
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                binding.btnRegister.isEnabled = true
                if (user == null) {
                    binding.etRegEmail.error = "An account with this email already exists"
                    return@runOnUiThread
                }
                SessionManager(this).start(user)
                Toast.makeText(this, "Account created. Welcome, ${user.fullName.split(" ").first()}!", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                finish()
            }
        }.start()
    }
}
