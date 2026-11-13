package com.securehomes.app.models

data class SurveyResult(
    val id: String,
    val userId: String,
    val setupMode: String,
    val susScore: Double,
    val nasaTlxWorkload: Double,
    /** Raw answers, e.g. "sus=[4, 2, ...];tlx=[30, ...]". */
    val answers: String,
    val feedback: String,
    val submittedAt: String
)
