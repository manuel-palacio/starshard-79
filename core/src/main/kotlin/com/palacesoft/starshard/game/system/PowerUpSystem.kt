package com.palacesoft.starshard.game.system

import com.palacesoft.starshard.events.GameEvent
import com.palacesoft.starshard.events.GameEventBus
import com.palacesoft.starshard.game.World
import com.palacesoft.starshard.game.entity.PowerUp
import com.palacesoft.starshard.game.entity.PowerUpType
import com.palacesoft.starshard.util.Settings
import kotlin.random.Random

class PowerUpSystem(private val world: World) {

    private var spawnTimer = 0f
    private var nextSpawnInterval = randomInterval()

    // Active weapon power-up (only one at a time)
    var activeType: PowerUpType? = null
        private set
    var activeTimer = 0f
        private set
    val activeDuration = 10f

    // Shield is independent of weapon power-ups
    var shieldActive = false
        private set

    fun update(delta: Float) {
        // Update floating powerups
        for (p in world.powerUps) {
            if (!p.alive) continue
            p.age += delta
            p.x += p.velX * delta
            p.y += p.velY * delta
            p.rotation += 90f * delta
            if (p.age >= p.maxAge) p.alive = false
        }
        world.powerUps.removeAll { !it.alive }

        // Spawn timer (starts at wave 3)
        if (world.wave >= 3) {
            spawnTimer += delta
            if (spawnTimer >= nextSpawnInterval && world.powerUps.none { it.alive }) {
                spawnTimer = 0f
                nextSpawnInterval = randomInterval()
                spawn()
            }
        }

        // Count down active weapon power-up
        if (activeType != null) {
            activeTimer -= delta
            if (activeTimer <= 0f) {
                val expired = activeType!!
                activeType = null
                activeTimer = 0f
                GameEventBus.emit(GameEvent.PowerUpExpired(expired))
            }
        }
    }

    fun collect(type: PowerUpType, x: Float, y: Float) {
        if (type == PowerUpType.SHIELD) {
            shieldActive = true
        } else {
            activeType = type
            activeTimer = activeDuration
        }
        GameEventBus.emit(GameEvent.PowerUpCollected(x, y, type))
    }

    fun breakShield(x: Float, y: Float) {
        shieldActive = false
        GameEventBus.emit(GameEvent.ShieldBroken(x, y))
    }

    private fun spawn() {
        val type = PowerUpType.entries[Random.nextInt(PowerUpType.entries.size)]
        val powerUp = PowerUp(type)
        powerUp.x = Settings.WORLD_WIDTH * (0.1f + Random.nextFloat() * 0.8f)
        powerUp.y = Settings.WORLD_HEIGHT * (0.1f + Random.nextFloat() * 0.8f)
        powerUp.velX = (Random.nextFloat() - 0.5f) * 30f
        powerUp.velY = (Random.nextFloat() - 0.5f) * 30f
        powerUp.alive = true
        powerUp.age = 0f
        world.powerUps.add(powerUp)
        GameEventBus.emit(GameEvent.PowerUpSpawned(powerUp.x, powerUp.y, type))
    }

    private fun randomInterval() = 20f + Random.nextFloat() * 10f
}
