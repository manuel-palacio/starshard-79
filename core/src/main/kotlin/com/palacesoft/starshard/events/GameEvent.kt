package com.palacesoft.starshard.events

import com.palacesoft.starshard.game.entity.AsteroidSize
import com.palacesoft.starshard.game.entity.PowerUpType
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
    class SaucerDestroyed(val x: Float, val y: Float, val r: Float = 1f, val g: Float = 1f, val b: Float = 1f) : GameEvent()
    class Hyperspace(val fromX: Float, val fromY: Float, val toX: Float, val toY: Float) : GameEvent()
    class WaveStarted(val wave: Int) : GameEvent()
    class ScoreAwarded(val x: Float, val y: Float, val amount: Int) : GameEvent()
    class PowerUpSpawned(val x: Float, val y: Float, val type: PowerUpType) : GameEvent()
    class PowerUpCollected(val x: Float, val y: Float, val type: PowerUpType) : GameEvent()
    class PowerUpExpired(val type: PowerUpType) : GameEvent()
    class ShieldBroken(val x: Float, val y: Float) : GameEvent()
}
