package com.palacesoft.starshard.game.system

import com.palacesoft.starshard.events.GameEvent
import com.palacesoft.starshard.events.GameEventBus
import com.palacesoft.starshard.game.World
import com.palacesoft.starshard.game.entity.AsteroidFactory
import com.palacesoft.starshard.game.entity.AsteroidSize
import com.palacesoft.starshard.game.entity.Saucer
import com.palacesoft.starshard.game.entity.SaucerSize
import com.palacesoft.starshard.util.Settings
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
                // Tutorial is complete once the player has cleared the first wave
                if (world.wave == 1) Settings.tutorialCompleted = true
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
        if (world.wave == 1 && !Settings.tutorialCompleted) {
            spawnTutorialWave()
        } else {
            spawnNormalWave()
        }
        GameEventBus.emit(GameEvent.WaveStarted(world.wave))
    }

    /**
     * Scripted Wave 1: single LARGE asteroid placed off-centre toward the
     * right edge. Drifts slowly left across the wrap boundary — teaches
     * wraparound without a prompt. Splitting teaches fragment danger.
     */
    private fun spawnTutorialWave() {
        val x = Settings.WORLD_WIDTH * 0.78f
        val y = Settings.WORLD_HEIGHT * 0.55f
        world.asteroids.add(AsteroidFactory.createRandom(x, y, AsteroidSize.LARGE))
        world.waveMaxAsteroids = 1
    }

    private fun spawnNormalWave() {
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
        saucer.sineTimer = 0f
        GameEventBus.emit(GameEvent.SaucerSpawned(saucer.x, saucer.y))
    }

    private fun updateSaucers(delta: Float) {
        for (saucer in world.saucers) {
            if (!saucer.alive) continue
            saucer.sineTimer += delta
            saucer.x += saucer.velX * delta
            saucer.y += saucer.velY * delta
            saucer.velY = sin(saucer.sineTimer * 2.5f) * 60f

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
            world.ship.reset()
            GameEventBus.emit(GameEvent.PlayerRespawned(world.ship.x, world.ship.y))
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
