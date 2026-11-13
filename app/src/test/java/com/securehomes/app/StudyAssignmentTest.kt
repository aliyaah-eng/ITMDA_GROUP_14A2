package com.securehomes.app

import com.securehomes.app.study.StudyConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class StudyAssignmentTest {

    @Test
    fun smallerGroupIsChosen() {
        assertEquals(StudyConfig.GROUP_GUIDED, StudyConfig.pickGroup(2, 3))
        assertEquals(StudyConfig.GROUP_UNGUIDED, StudyConfig.pickGroup(5, 4))
    }

    @Test
    fun tiesAreRandomButBothOccur() {
        val random = Random(42)
        val picks = List(200) { StudyConfig.pickGroup(0, 0, random) }.toSet()
        assertEquals(setOf(StudyConfig.GROUP_GUIDED, StudyConfig.GROUP_UNGUIDED), picks)
    }

    @Test
    fun groupsStayBalanced() {
        var guided = 0
        var unguided = 0
        val random = Random(7)
        repeat(101) {
            if (StudyConfig.pickGroup(guided, unguided, random) == StudyConfig.GROUP_GUIDED) guided++ else unguided++
        }
        assertTrue(abs(guided - unguided) <= 1)
    }
}
