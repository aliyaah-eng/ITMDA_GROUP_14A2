package com.securehomes.app.ui.wizard

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import com.securehomes.app.database.DatabaseHelper
import com.securehomes.app.models.Device
import com.securehomes.app.models.DeviceCatalog
import com.securehomes.app.study.StudyConfig
import com.securehomes.app.ui.common.BaseActivity
import com.securehomes.app.ui.survey.SurveyActivity
import kotlin.random.Random

/**
 * Shared logic for both setup versions, so they are measured in exactly the same way:
 * time on task, real validation error counts, the step where a participant leaves, and the
 * device's security score when saved. Each visit ends with exactly one TASK_SUBMITTED or
 * TASK_ABANDONED event, which the analytics screen uses.
 */
abstract class SetupBaseActivity : BaseActivity() {

    override val requiredRole: String? = StudyConfig.ROLE_USER

    /** "guided" (Version A) or "unguided" (Version B). */
    protected abstract val mode: String

    protected lateinit var device: Device
    protected var isNewDevice = true
    protected var errorCount = 0
    protected var currentStep = 1
    private var startTime = 0L
    private var closed = false
    private var initialised = false

    /**
     * Loads the device and starts (or restores) the attempt.
     * @return false if the screen is closing and the subclass should stop
     */
    protected fun initAttempt(savedInstanceState: Bundle?): Boolean {
        if (session.studyGroup != mode) {
            // Participants must always see the version they were assigned.
            startActivity(Intent(this, SetupRouter.screenFor(session.studyGroup)).putExtras(intent))
            finish()
            return false
        }
        val existingId = intent.getStringExtra(SetupRouter.EXTRA_DEVICE_ID)
        if (existingId != null) {
            val found = db.getDeviceById(existingId)?.takeIf { it.userId == userId }
            if (found == null) {
                Toast.makeText(this, "Device not found", Toast.LENGTH_SHORT).show()
                finish()
                return false
            }
            device = found
            isNewDevice = false
        } else {
            val catalog = DeviceCatalog.forCategory(intent.getStringExtra(SetupRouter.EXTRA_CATEGORY) ?: "")
            device = db.factoryDevice(
                id = savedInstanceState?.getString("new_id") ?: DatabaseHelper.newId("dev"),
                userId = userId,
                name = catalog.defaultName,
                category = catalog.category,
                model = intent.getStringExtra(SetupRouter.EXTRA_MODEL) ?: catalog.models.first(),
                mac = intent.getStringExtra(SetupRouter.EXTRA_MAC) ?: "${catalog.macPrefix}:00:00:01",
                ip = savedInstanceState?.getString("new_ip") ?: "192.168.1.${Random.nextInt(100, 220)}"
            )
        }

        if (savedInstanceState == null) {
            startTime = System.currentTimeMillis()
            db.log(userId, "TASK_STARTED", mode, "device=${device.id};new=$isNewDevice;startScore=${device.securityScore}")
        } else {
            startTime = savedInstanceState.getLong("start_time")
            errorCount = savedInstanceState.getInt("errors")
            currentStep = savedInstanceState.getInt("step", 1)
            closed = savedInstanceState.getBoolean("closed")
        }
        initialised = true

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = onBackRequested()
        })
        return true
    }

    private fun elapsedMs(): Long = System.currentTimeMillis() - startTime

    /** Counts a real validation error and logs which field caused it. */
    protected fun recordError(reason: String) {
        errorCount++
        db.log(userId, "INVALID_SELECTION", mode, "step=$currentStep;reason=$reason")
    }

    /** Back button. The guided wizard overrides this to go to the previous step first. */
    protected open fun onBackRequested() = confirmLeave()

    protected fun confirmLeave() {
        AlertDialog.Builder(this)
            .setTitle("Leave setup?")
            .setMessage("Your changes to this device won't be saved.")
            .setPositiveButton("Leave") { _, _ ->
                abandon()
                finish()
            }
            .setNegativeButton("Keep going", null)
            .show()
    }

    private fun abandon() {
        if (closed || !initialised) return
        closed = true
        db.log(userId, DatabaseHelper.EVENT_ABANDONED, mode, "device=${device.id};step=$currentStep",
            durationMs = elapsedMs(), errors = errorCount)
    }

    /** Saves the device, records the outcome with its real score, then moves on. */
    protected fun submit() {
        if (closed) return
        closed = true
        device.modeConfigured = mode
        val saved = if (isNewDevice) db.insertDevice(device) else db.updateDevice(device)
        if (!saved) {
            closed = false
            Toast.makeText(this, "Could not save the device. Please try again.", Toast.LENGTH_LONG).show()
            return
        }
        val score = device.securityScore
        db.log(userId, DatabaseHelper.EVENT_SUBMITTED, mode, "device=${device.id};new=$isNewDevice",
            durationMs = elapsedMs(), errors = errorCount, score = score)
        Toast.makeText(this, "${device.name} saved. Security score: $score%", Toast.LENGTH_LONG).show()
        if (!db.hasSurvey(userId)) {
            startActivity(Intent(this, SurveyActivity::class.java))
        }
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("start_time", startTime)
        outState.putInt("errors", errorCount)
        outState.putInt("step", currentStep)
        outState.putBoolean("closed", closed)
        if (initialised && isNewDevice) {
            outState.putString("new_id", device.id)
            outState.putString("new_ip", device.ipAddress)
        }
    }

    override fun onDestroy() {
        // Leaving any other way (e.g. the session timed out) still counts as abandoning.
        if (isFinishing) abandon()
        super.onDestroy()
    }
}
