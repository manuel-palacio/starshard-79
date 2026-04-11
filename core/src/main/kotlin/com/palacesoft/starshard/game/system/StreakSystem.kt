package com.palacesoft.starshard.game.system

import com.palacesoft.starshard.events.GameEvent
import com.palacesoft.starshard.events.GameEventBus

/**
 * Tracks rapid-kill streaks and notifies caller of the current multiplier (1–4).
 * Multiplier increases with each kill within [STREAK_WINDOW] seconds.
 * Resets to 1 on PlayerHit or timeout.
 *
 * @param onMultiplierChanged called whenever the multiplier value changes.
 */
class StreakSystem(private val onMultiplierChanged: (Int) -> Unit) {

    var multiplier: Int = 1
        private set

    private var streakCount = 0
    private var streakTimer = 0f

    companion object {
        const val STREAK_WINDOW  = 1.5f
        const val MAX_MULTIPLIER = 4
    }

    fun subscribe() {
        GameEventBus.subscribe { event ->
            when (event) {
                is GameEvent.AsteroidDestroyed -> onKill()
                is GameEvent.SaucerDestroyed   -> onKill()
                is GameEvent.PlayerHit         -> reset()
                else -> {}
            }
        }
    }

    fun update(delta: Float) {
        if (streakCount > 0) {
            streakTimer -= delta
            if (streakTimer <= 0f) reset()
        }
    }

    private fun onKill() {
        streakCount++
        streakTimer = STREAK_WINDOW
        val newMult = minOf(streakCount, MAX_MULTIPLIER)
        if (newMult != multiplier) {
            multiplier = newMult
            onMultiplierChanged(multiplier)
        }
    }

    private fun reset() {
        if (multiplier == 1 && streakCount == 0) return
        streakCount = 0
        streakTimer = 0f
        multiplier  = 1
        onMultiplierChanged(1)
    }
}
