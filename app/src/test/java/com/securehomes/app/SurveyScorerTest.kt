package com.securehomes.app

import com.securehomes.app.study.SurveyScorer
import org.junit.Assert.assertEquals
import org.junit.Test

class SurveyScorerTest {

    @Test
    fun bestSusIs100() = assertEquals(100.0, SurveyScorer.susScore(listOf(5, 1, 5, 1, 5, 1, 5, 1, 5, 1)), 0.001)

    @Test
    fun worstSusIs0() = assertEquals(0.0, SurveyScorer.susScore(listOf(1, 5, 1, 5, 1, 5, 1, 5, 1, 5)), 0.001)

    @Test
    fun neutralSusIs50() = assertEquals(50.0, SurveyScorer.susScore(List(10) { 3 }), 0.001)

    @Test
    fun rawTlxIsTheMean() = assertEquals(30.0, SurveyScorer.rawTlx(listOf(10, 20, 30, 40, 50)), 0.001)

    @Test(expected = IllegalArgumentException::class)
    fun susNeedsAllTenAnswers() {
        SurveyScorer.susScore(listOf(5, 1))
    }

    @Test
    fun questionnairesHaveTheRightLength() {
        assertEquals(10, SurveyScorer.SUS_STATEMENTS.size)
        assertEquals(5, SurveyScorer.TLX_DIMENSIONS.size)
    }
}
