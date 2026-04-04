package com.palacesoft.asteroids.game.system

import com.palacesoft.asteroids.game.World
import com.palacesoft.asteroids.game.entity.AsteroidFactory
import com.palacesoft.asteroids.game.entity.AsteroidSize
import com.palacesoft.asteroids.game.entity.Saucer
import com.palacesoft.asteroids.game.entity.SaucerSize
import com.palacesoft.asteroids.util.Settings
import kotlin.math.sin
import kotlin.random.Random

class WaveSystem(private val world: World) {
    private var betweenWaveTimer = 0f
    private var saucerTimer      = 0f
    private val SAUCER_INTERVAL  = 25f
    private val BETWEEN_WAVE_GAP = 3f
    private var respawnTimer     = 0f

    fun update(delta: Float) {
        if (!world.ship.alive && world.lives > 0) {
            handleRespawn(delta)
            return
        }
        val liveAsteroids = world.asteroids.count { it.alive }
        val liveSaucers   = world.saucers.count { it.alive }

        if (liveAsteroids == 0 && liveSaucers == 0) {
            betweenWaveTimer += delta
            if (betweenWaveTimer >= BETWEEN_WAVE_GAP) {
                betweenWaveTimer = 0f
                world.wave++
                spawnWave()
            }
        }

        saucerTimer += delta
        if (saucerTimer >= SAUCER_INTERVAL) {
            saucerTimer = 0f
            spawnSaucer()
        }

        updateSaucers(delta)
    }

    fun spawnWave() {
        val count = world.wave * 2 + 2
        repeat(count) {
            val (x, y) = randomEdgePosition()
            world.asteroids.add(AsteroidFactory.createRandom(x, y, AsteroidSize.LARGE))
        }
        world.waveMaxAsteroids = count.coerceAtLeast(1)
    }

    private fun randomEdgePosition(): Pair<Float, Float> = when (Random.nextInt(4)) {
        0    -> Random.nextFloat() * Settings.WORLD_WIDTH to 0f
        1    -> Random.nextFloat() * Settings.WORLD_WIDTH to Settings.WORLD_HEIGHT
        2    -> 0f to Random.nextFloat() * Settings.WORLD_HEIGHT
        else -> Settings.WORLD_WIDTH to Random.nextFloat() * Settings.WORLD_HEIGHT
    }

    private fun spawnSaucer() {
        val saucer = world.saucers.firstOrNull { !it.alive } ?: return
        val fromLeft = Random.nextBoolean()
        saucer.x = if (fromLeft) 0f else Settings.WORLD_WIDTH
        saucer.y = Random.nextFloat() * Settings.WORLD_HEIGHT
        saucer.velX = (if (fromLeft) 1f else -1f) * Saucer.SPEED
        saucer.velY = 0f
        saucer.alive = true
        saucer.shootTimer = 0f
    }

    private fun updateSaucers(delta: Float) {
        for (saucer in world.saucers) {
            if (!saucer.alive) continue
            saucer.x += saucer.velX * delta
            saucer.y += saucer.velY * delta
            saucer.velY = sin(saucer.x * 0.005f) * 60f

            saucer.shootTimer += delta
            if (saucer.shootTimer >= Saucer.SHOOT_INTERVAL) {
                saucer.shootTimer = 0f
                val spread = if (saucer.size == SaucerSize.LARGE) 360f else 15f
                world.bulletPool.acquireForSaucer(
                    saucer, world.ship.x, world.ship.y, world.bullets, spread
                )
                world.sounds?.playSaucerFire()
            }
            if (saucer.x < -60f || saucer.x > Settings.WORLD_WIDTH + 60f) saucer.alive = false
        }
    }

    private fun handleRespawn(delta: Float) {
        respawnTimer += delta
        if (respawnTimer >= 2f) {
            respawnTimer = 0f
            world.lives--
            world.ship.reset()
        }
    }

    fun start() {
        world.wave = 1
        spawnWave()
        world.ship.reset()
        repeat(2) { world.saucers.add(Saucer(SaucerSize.LARGE)) }
        saucerTimer = 0f
        betweenWaveTimer = 0f
        respawnTimer = 0f
    }
}
