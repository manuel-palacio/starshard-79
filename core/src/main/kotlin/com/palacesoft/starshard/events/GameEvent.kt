package com.palacesoft.starshard.events

import com.palacesoft.starshard.game.entity.AsteroidSize
import com.palacesoft.starshard.game.entity.SaucerSize

/**
 * All game events. Use class (not data class) to avoid toString overhead.
 * Instances are created at emit sites — keeping them tiny (a few floats)
 * means they land in Eden space and are collected cheaply between frames.
 */
sealed class GameEvent {
    class BulletFired(val x: Float, val y: Float, val angle: Float) : GameEvent()
    class AsteroidHit(val x: Float, val y: Float, val size: AsteroidSize) : GameEvent()
    class AsteroidDestroyed(val x: Float, val y: Float, val size: AsteroidSize) : GameEvent()
    class PlayerHit(val x: Float, val y: Float) : GameEvent()
    class PlayerRespawned(val x: Float, val y: Float) : GameEvent()
    class SaucerSpawned(val x: Float, val y: Float) : GameEvent()
    class SaucerDestroyed(val x: Float, val y: Float) : GameEvent()
    class WaveStarted(val wave: Int) : GameEvent()
    class ScoreAwarded(val x: Float, val y: Float, val amount: Int) : GameEvent()
}
