package com.palacesoft.starshard.game.system

import com.palacesoft.starshard.events.GameEvent
import com.palacesoft.starshard.events.GameEventBus
import com.palacesoft.starshard.game.entity.AsteroidSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StreakSystemTest {

    private lateinit var streak: StreakSystem
    private var capturedMultiplier = 1

    @BeforeEach fun setup() {
        GameEventBus.clear()
        streak = StreakSystem { mult -> capturedMultiplier = mult }
        streak.subscribe()
    }

    @AfterEach fun teardown() {
        GameEventBus.clear()
    }

    @Test fun `initial multiplier is 1`() {
        assertEquals(1, streak.multiplier)
    }

    @Test fun `first kill does not increase multiplier`() {
        GameEventBus.emit(GameEvent.AsteroidDestroyed(0f, 0f, AsteroidSize.LARGE))
        assertEquals(1, streak.multiplier)
    }

    @Test fun `two rapid kills give 2x`() {
        GameEventBus.emit(GameEvent.AsteroidDestroyed(0f, 0f, AsteroidSize.LARGE))
        GameEventBus.emit(GameEvent.AsteroidDestroyed(0f, 0f, AsteroidSize.LARGE))
        assertEquals(2, streak.multiplier)
        assertEquals(2, capturedMultiplier)
    }

    @Test fun `saucer kill counts toward streak`() {
        GameEventBus.emit(GameEvent.AsteroidDestroyed(0f, 0f, AsteroidSize.LARGE))
        GameEventBus.emit(GameEvent.SaucerDestroyed(0f, 0f))
        assertEquals(2, streak.multiplier)
    }

    @Test fun `multiplier caps at 4`() {
        repeat(10) { GameEventBus.emit(GameEvent.AsteroidDestroyed(0f, 0f, AsteroidSize.LARGE)) }
        assertEquals(4, streak.multiplier)
    }

    @Test fun `PlayerHit resets multiplier to 1`() {
        repeat(3) { GameEventBus.emit(GameEvent.AsteroidDestroyed(0f, 0f, AsteroidSize.LARGE)) }
        GameEventBus.emit(GameEvent.PlayerHit(0f, 0f))
        assertEquals(1, streak.multiplier)
        assertEquals(1, capturedMultiplier)
    }

    @Test fun `streak timer expiry resets multiplier`() {
        GameEventBus.emit(GameEvent.AsteroidDestroyed(0f, 0f, AsteroidSize.LARGE))
        GameEventBus.emit(GameEvent.AsteroidDestroyed(0f, 0f, AsteroidSize.LARGE))
        assertEquals(2, streak.multiplier)
        streak.update(2.0f)
        assertEquals(1, streak.multiplier)
    }

    @Test fun `kill within window resets timer`() {
        GameEventBus.emit(GameEvent.AsteroidDestroyed(0f, 0f, AsteroidSize.LARGE))
        streak.update(1.0f)
        GameEventBus.emit(GameEvent.AsteroidDestroyed(0f, 0f, AsteroidSize.LARGE))
        streak.update(1.0f)
        assertEquals(2, streak.multiplier)
    }
}
