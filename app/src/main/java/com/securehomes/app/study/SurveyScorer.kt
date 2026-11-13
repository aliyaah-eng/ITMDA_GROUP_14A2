package com.securehomes.app.study

/** Scoring for the post-task questionnaires. */
object SurveyScorer {

    const val SUS_ITEMS = 5
    const val TLX_ITEMS = 5

    val SUS_STATEMENTS = listOf(
        "I think that I would like to use this setup screen frequently.",
        "I thought the setup screen was easy to use.",
        "I found the various functions in this setup screen were well integrated.",
        "I thought there was too much inconsistency in this setup screen.",
        "I found the setup screen very cumbersome to use."
    )

    val TLX_DIMENSIONS = listOf(
        "Mental demand: how mentally demanding was the task?",
        "Temporal demand: how hurried or rushed was the pace?",
        "Performance: how unsuccessful were you? (0 = perfect, 100 = failure)",
        "Effort: how hard did you have to work?",
        "Frustration: how insecure, irritated or stressed were you?"
    )

    /**
     * System Usability Scale (Brooke, 1996). Answers are 1-5. Odd items score (answer - 1),
     * even items score (5 - answer), and the total is multiplied by 2.5 to give 0-100.
     */
    fun susScore(answers: List<Int>): Double {
        require(answers.size == SUS_ITEMS) { "SUS needs exactly $SUS_ITEMS answers" }
        require(answers.all { it in 1..5 }) { "SUS answers must be 1-5" }
        val total = answers.withIndex().sumOf { (i, a) -> if (i % 2 == 0) a - 1 else 5 - a }
        return total * 2.5
    }

    /** Raw (unweighted) NASA-TLX: the mean of the 0-100 ratings. */
    fun rawTlx(ratings: List<Int>): Double {
        require(ratings.size == TLX_ITEMS) { "NASA-TLX needs exactly $TLX_ITEMS ratings" }
        require(ratings.all { it in 0..100 }) { "NASA-TLX ratings must be 0-100" }
        return ratings.average()
    }
}
