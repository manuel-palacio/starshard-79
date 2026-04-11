package com.palacesoft.starshard

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GameServicesTest {

    private class RecordingGameServices : GameServices {
        val submittedScores = mutableListOf<Int>()
        var leaderboardShown = false
        override fun submitScore(score: Int) { submittedScores.add(score) }
        override fun showLeaderboard() { leaderboardShown = true }
    }

    @Test fun `submitScore records the score`() {
        val svc = RecordingGameServices()
        svc.submitScore(1234)
        assertEquals(listOf(1234), svc.submittedScores)
    }

    @Test fun `showLeaderboard sets flag`() {
        val svc = RecordingGameServices()
        svc.showLeaderboard()
        assertTrue(svc.leaderboardShown)
    }
}
