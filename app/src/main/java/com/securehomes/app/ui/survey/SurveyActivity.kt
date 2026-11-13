package com.securehomes.app.ui.survey

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.securehomes.app.R
import com.securehomes.app.database.DatabaseHelper
import com.securehomes.app.databinding.ActivitySurveyBinding
import com.securehomes.app.models.SurveyResult
import com.securehomes.app.study.StudyConfig
import com.securehomes.app.study.SurveyScorer
import com.securehomes.app.ui.common.BaseActivity

/**
 * Post-task survey: the full 10-item System Usability Scale and the 5-item raw NASA-TLX.
 * Nothing is pre-selected, every SUS statement must be answered, and the response is stored
 * under the participant's real study version.
 */
class SurveyActivity : BaseActivity() {

    override val requiredRole: String? = StudyConfig.ROLE_USER

    private lateinit var binding: ActivitySurveyBinding
    private var susAnswers = IntArray(SurveyScorer.SUS_ITEMS) // 0 = not answered
    private var tlxRatings = IntArray(SurveyScorer.TLX_ITEMS) { 50 }
    private val questionViews = mutableListOf<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing) return
        binding = ActivitySurveyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        savedInstanceState?.let {
            susAnswers = it.getIntArray("sus") ?: susAnswers
            tlxRatings = it.getIntArray("tlx") ?: tlxRatings
        }
        buildSusQuestions()
        buildTlxSliders()
        binding.btnSubmitSurvey.setOnClickListener { submitSurvey() }
    }

    private fun buildSusQuestions() {
        SurveyScorer.SUS_STATEMENTS.forEachIndexed { index, statement ->
            val question = TextView(this).apply {
                text = "${index + 1}. $statement"
                setTextColor(ContextCompat.getColor(this@SurveyActivity, R.color.text_primary))
                setPadding(0, 24, 0, 4)
            }
            binding.containerSus.addView(question)
            questionViews.add(question)

            val group = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
            for (value in 1..5) {
                val option = RadioButton(this).apply {
                    id = View.generateViewId()
                    text = value.toString()
                    tag = value
                    isSaveEnabled = false // restored from susAnswers instead
                    isChecked = susAnswers[index] == value
                }
                group.addView(option, RadioGroup.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            }
            group.setOnCheckedChangeListener { g, checkedId ->
                val value = g.findViewById<RadioButton>(checkedId)?.tag as? Int ?: return@setOnCheckedChangeListener
                susAnswers[index] = value
                question.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            }
            binding.containerSus.addView(group)
        }
    }

    private fun buildTlxSliders() {
        SurveyScorer.TLX_DIMENSIONS.forEachIndexed { index, dimension ->
            binding.containerTlx.addView(TextView(this).apply {
                text = dimension
                setTextColor(ContextCompat.getColor(this@SurveyActivity, R.color.text_primary))
                setPadding(0, 24, 0, 4)
            })
            val value = TextView(this).apply {
                text = "${tlxRatings[index]} / 100"
                setTextColor(ContextCompat.getColor(this@SurveyActivity, R.color.primary))
            }
            val slider = SeekBar(this).apply {
                max = 100
                progress = tlxRatings[index]
                isSaveEnabled = false
                contentDescription = dimension
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        tlxRatings[index] = progress
                        value.text = "$progress / 100"
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                    override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
                })
            }
            binding.containerTlx.addView(slider)
            binding.containerTlx.addView(value)
        }
    }

    private fun submitSurvey() {
        val missing = susAnswers.indices.filter { susAnswers[it] == 0 }
        if (missing.isNotEmpty()) {
            missing.forEach { questionViews[it].setTextColor(ContextCompat.getColor(this, R.color.danger)) }
            val first = questionViews[missing.first()]
            first.requestRectangleOnScreen(Rect(0, 0, first.width, first.height))
            Toast.makeText(this, "Please answer all ${SurveyScorer.SUS_ITEMS} statements in Part 1", Toast.LENGTH_LONG).show()
            return
        }
        val sus = SurveyScorer.susScore(susAnswers.toList())
        val tlx = SurveyScorer.rawTlx(tlxRatings.toList())
        val group = session.studyGroup ?: ""
        db.insertSurvey(
            SurveyResult(
                id = DatabaseHelper.newId("srv"),
                userId = userId,
                setupMode = group,
                susScore = sus,
                nasaTlxWorkload = tlx,
                answers = "sus=${susAnswers.toList()};tlx=${tlxRatings.toList()}",
                feedback = binding.etFeedback.text?.toString()?.trim().orEmpty(),
                submittedAt = DatabaseHelper.now()
            )
        )
        db.log(userId, "SURVEY_SUBMITTED", group, "sus=$sus;tlx=$tlx")
        Toast.makeText(this, "Thank you! Your feedback has been saved.", Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntArray("sus", susAnswers)
        outState.putIntArray("tlx", tlxRatings)
    }
}
