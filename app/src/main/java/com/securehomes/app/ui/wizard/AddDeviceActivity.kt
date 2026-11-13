package com.securehomes.app.ui.wizard

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import com.securehomes.app.databinding.ActivityAddDeviceBinding
import com.securehomes.app.models.DeviceCatalog
import com.securehomes.app.study.StudyConfig
import com.securehomes.app.ui.common.BaseActivity
import kotlin.random.Random

/**
 * Add device: pick a type, a simulated Wi-Fi / Bluetooth scan "finds" a device, and the
 * participant continues to the setup version they were assigned (A or B). No real
 * hardware or network is touched.
 */
class AddDeviceActivity : BaseActivity() {

    override val requiredRole: String? = StudyConfig.ROLE_USER

    private lateinit var binding: ActivityAddDeviceBinding
    private val handler = Handler(Looper.getMainLooper())
    private var selected: DeviceCatalog? = null
    private var foundModel = ""
    private var foundMac = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing) return
        binding = ActivityAddDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cardCategoryCamera.setOnClickListener { startRadarScan(DeviceCatalog.CAMERA) }
        binding.cardCategoryLock.setOnClickListener { startRadarScan(DeviceCatalog.LOCK) }
        binding.cardCategoryPlug.setOnClickListener { startRadarScan(DeviceCatalog.PLUG) }
        binding.cardCategoryLight.setOnClickListener { startRadarScan(DeviceCatalog.LIGHT) }
        binding.cardCategoryThermostat.setOnClickListener { startRadarScan(DeviceCatalog.THERMOSTAT) }

        binding.btnContinueSetup.setOnClickListener {
            val type = selected ?: return@setOnClickListener
            db.log(userId, "DEVICE_SELECTED", session.studyGroup ?: "", "category=${type.category};mac=$foundMac")
            startActivity(SetupRouter.newDevice(this, session.studyGroup, type.category, foundModel, foundMac))
            finish()
        }
    }

    private fun startRadarScan(type: DeviceCatalog) {
        selected = type
        db.log(userId, "DEVICE_SCAN_STARTED", session.studyGroup ?: "", "category=${type.category}")

        binding.gridCategories.visibility = View.GONE
        binding.cardRadar.visibility = View.VISIBLE
        binding.cardDiscoveredResult.visibility = View.GONE
        binding.tvRadarStatus.text = "Scanning Wi-Fi & Bluetooth for ${type.category}..."

        handler.postDelayed({
            foundModel = type.models.random()
            foundMac = type.macPrefix + ":" + List(3) { "%02X".format(Random.nextInt(256)) }.joinToString(":")
            val signal = -38 - Random.nextInt(40)
            val quality = when {
                signal > -55 -> "Strong"
                signal > -67 -> "Good"
                else -> "Fair"
            }
            binding.cardRadar.visibility = View.GONE
            binding.cardDiscoveredResult.visibility = View.VISIBLE
            binding.tvDiscoveredModel.text = foundModel
            binding.tvDiscoveredSignal.text = "Signal: $signal dBm ($quality) • MAC: $foundMac"
        }, 1500)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
