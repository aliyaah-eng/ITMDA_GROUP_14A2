package com.securehomes.app.ui.analytics

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.securehomes.app.databinding.ActivityAnalyticsBinding
import com.securehomes.app.models.GroupStats
import com.securehomes.app.study.StudyConfig
import com.securehomes.app.ui.common.BaseActivity
import java.util.Locale

/**
 * Research analytics (researcher only). Every figure is calculated from the telemetry and
 * survey tables; until participants have done the task, the screen shows "no data yet".
 */
class AnalyticsActivity : BaseActivity() {

    override val requiredRole: String? = StudyConfig.ROLE_ADMIN

    private lateinit var binding: ActivityAnalyticsBinding
    private lateinit var adapter: TelemetryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing) return
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.rvTelemetry.layoutManager = LinearLayoutManager(this)
        adapter = TelemetryAdapter(emptyList())
        binding.rvTelemetry.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        if (!isFinishing) loadData()
    }

    private fun loadData() {
        binding.tvGuidedStats.text = format(db.getGroupStats(StudyConfig.GROUP_GUIDED))
        binding.tvUnguidedStats.text = format(db.getGroupStats(StudyConfig.GROUP_UNGUIDED))
        adapter.updateData(db.getTelemetryEvents(50))
    }

    private fun format(s: GroupStats): String {
        fun num(v: Double?, pattern: String) = v?.let { String.format(Locale.getDefault(), pattern, it) } ?: "no data yet"
        fun pct(v: Double?) = v?.let { String.format(Locale.getDefault(), "%.0f%%", it * 100) } ?: "no data yet"
        val time = s.avgDurationMs?.let {
            val total = Math.round(it / 1000)
            String.format(Locale.getDefault(), "%dm %02ds", total / 60, total % 60)
        } ?: "no data yet"
        return listOf(
            "• Participants assigned: ${s.participants}",
            "• Setup attempts: ${s.attempts} (${s.submitted} saved, ${s.abandoned} abandoned)",
            "• Avg setup time: $time",
            "• Avg error count: ${num(s.avgErrors, "%.1f errors")}",
            "• Attained security score: ${num(s.avgScore, "%.0f%%")}",
            "• Hardened devices (85+): ${pct(s.hardenedRate)}",
            "• Task abandonment: ${pct(s.abandonmentRate)}",
            "• System Usability (SUS): ${num(s.avgSus, "%.1f / 100")} (${s.surveys} surveys)",
            "• NASA-TLX workload: ${num(s.avgTlx, "%.1f / 100")}"
        ).joinToString("\n")
    }
}
