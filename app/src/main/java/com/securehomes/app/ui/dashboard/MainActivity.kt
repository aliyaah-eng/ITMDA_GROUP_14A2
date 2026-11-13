package com.securehomes.app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.securehomes.app.R
import com.securehomes.app.databinding.ActivityMainBinding
import com.securehomes.app.models.Device
import com.securehomes.app.security.SecurityEvaluator
import com.securehomes.app.study.StudyConfig
import com.securehomes.app.ui.analytics.AnalyticsActivity
import com.securehomes.app.ui.common.BaseActivity
import com.securehomes.app.ui.details.DeviceDetailsActivity
import com.securehomes.app.ui.survey.SurveyActivity
import com.securehomes.app.ui.wizard.AddDeviceActivity

/**
 * Dashboard. Participants see and control their own devices; the researcher (admin)
 * sees every participant's devices read-only and gets the Telemetry tab instead of
 * Add Device and Survey.
 */
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: DeviceAdapter
    private var allDevices = listOf<Device>()
    private var currentFilter = "all"

    private val isAdmin: Boolean get() = session.role == StudyConfig.ROLE_ADMIN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing) return
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val firstName = session.fullName.split(" ").firstOrNull().orEmpty()
        binding.tvUserGreeting.text = if (isAdmin) "Welcome, $firstName (Researcher)" else "Welcome, $firstName"

        setupRecyclerView()
        setupFilterButtons()
        setupBottomNavigation()

        binding.btnQuickAddDevice.visibility = if (isAdmin) View.GONE else View.VISIBLE
        binding.btnQuickAddDevice.setOnClickListener {
            startActivity(Intent(this, AddDeviceActivity::class.java))
        }
        binding.btnLogout.setOnClickListener { logout() }
    }

    override fun onResume() {
        super.onResume()
        if (!isFinishing) loadDevices()
    }

    private fun setupRecyclerView() {
        binding.rvDevices.layoutManager = LinearLayoutManager(this)
        adapter = DeviceAdapter(
            devices = emptyList(),
            canControl = !isAdmin,
            onAuditClick = { device ->
                startActivity(Intent(this, DeviceDetailsActivity::class.java).putExtra("DEVICE_ID", device.id))
            },
            onToggleClick = { device ->
                if (db.toggleDeviceStatus(device.id, userId)) {
                    db.log(userId, "DEVICE_POWER_TOGGLED", session.studyGroup ?: "", "device=${device.id}")
                    Toast.makeText(this, "${device.name} power state toggled", Toast.LENGTH_SHORT).show()
                }
                loadDevices()
            }
        )
        binding.rvDevices.adapter = adapter
    }

    private fun loadDevices() {
        allDevices = db.getDevices(if (isAdmin) null else userId)
        applyFilter(currentFilter)
        updateKPIs()
    }

    private fun updateKPIs() {
        val total = allDevices.size
        val hardened = allDevices.count { it.securityScore >= SecurityEvaluator.HARDENED_THRESHOLD }
        val warnings = allDevices.sumOf { it.evaluate().vulnerabilities.size }

        binding.tvStatTotal.text = total.toString()
        binding.tvStatProtected.text = hardened.toString()
        binding.tvStatWarnings.text = warnings.toString()
        binding.tvActiveDeviceCount.text = "${allDevices.count { it.status == "online" }} Active"

        if (total == 0) {
            binding.tvOverallScore.text = "–"
            binding.tvScoreTier.text = "NO DEVICES YET"
            binding.tvScoreTier.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
            binding.tvScoreDesc.text = if (isAdmin) "No participant devices yet." else "Tap + Add Device to secure your first smart device."
            return
        }
        val avgScore = allDevices.map { it.securityScore }.average().toInt()
        binding.tvOverallScore.text = "$avgScore%"
        when {
            avgScore >= SecurityEvaluator.HARDENED_THRESHOLD -> {
                binding.tvScoreTier.text = "SECURE ENVIRONMENT"
                binding.tvScoreTier.setTextColor(ContextCompat.getColor(this, R.color.success))
                binding.tvScoreDesc.text = "Connected devices meet the hardened security baseline."
            }
            avgScore >= SecurityEvaluator.MODERATE_THRESHOLD -> {
                binding.tvScoreTier.text = "MODERATE EXPOSURE"
                binding.tvScoreTier.setTextColor(ContextCompat.getColor(this, R.color.warning))
                binding.tvScoreDesc.text = "Some devices still have configuration weaknesses. Open an audit to see them."
            }
            else -> {
                binding.tvScoreTier.text = "HIGH RISK VULNERABILITIES"
                binding.tvScoreTier.setTextColor(ContextCompat.getColor(this, R.color.danger))
                binding.tvScoreDesc.text = "Action required: default passwords or weak encryption detected."
            }
        }
    }

    private fun setupFilterButtons() {
        val filterButtons = listOf(
            binding.filterAll to "all",
            binding.filterCameras to "Smart Camera",
            binding.filterLocks to "Smart Lock",
            binding.filterPlugs to "Smart Plug",
            binding.filterLights to "Smart Light",
            binding.filterClimate to "Smart Thermostat"
        )
        filterButtons.forEach { (btn, category) ->
            btn.setOnClickListener {
                currentFilter = category
                updateFilterButtonStyles(btn, filterButtons.map { it.first })
                applyFilter(category)
            }
        }
    }

    private fun updateFilterButtonStyles(selectedBtn: Button, allBtns: List<Button>) {
        allBtns.forEach { b ->
            val selected = b == selectedBtn
            b.setBackgroundResource(if (selected) R.drawable.bg_pill_selected else R.drawable.bg_pill_unselected)
            b.setTextColor(ContextCompat.getColor(this, if (selected) R.color.white else R.color.text_primary))
        }
    }

    private fun applyFilter(category: String) {
        val filtered = if (category == "all") allDevices else allDevices.filter { it.category == category }
        adapter.updateData(filtered)
        binding.tvEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.rvDevices.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun setupBottomNavigation() {
        val menu = binding.bottomNav.menu
        menu.findItem(R.id.nav_add).isVisible = !isAdmin
        menu.findItem(R.id.nav_survey).isVisible = !isAdmin
        menu.findItem(R.id.nav_telemetry).isVisible = isAdmin
        binding.bottomNav.selectedItemId = R.id.nav_home
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_add -> startActivity(Intent(this, AddDeviceActivity::class.java))
                R.id.nav_survey -> startActivity(Intent(this, SurveyActivity::class.java))
                R.id.nav_telemetry -> startActivity(Intent(this, AnalyticsActivity::class.java))
            }
            // Only "Home" stays highlighted; the other tabs open screens on top.
            item.itemId == R.id.nav_home
        }
    }
}
