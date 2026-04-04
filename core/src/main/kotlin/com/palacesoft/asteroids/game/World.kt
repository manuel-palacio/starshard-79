package com.palacesoft.asteroids.game

import com.palacesoft.asteroids.events.GameEvent
import com.palacesoft.asteroids.events.GameEventBus
import com.palacesoft.asteroids.game.entity.*
import com.palacesoft.asteroids.game.system.BulletPool
import com.palacesoft.asteroids.game.system.CollisionSystem
import com.palacesoft.asteroids.game.system.WaveSystem
import com.palacesoft.asteroids.input.GameInput
import com.palacesoft.asteroids.util.Settings
import com.palacesoft.asteroids.util.wrapCoord
import com.palacesoft.asteroids.vfx.VfxManager
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class World {
    val ship        = Ship()
    val asteroids   = mutableListOf<Asteroid>()
    val bullets     = mutableListOf<Bullet>()
    val saucers     = mutableListOf<Saucer>()
    var score            = 0
    var lives            = 3
    var wave             = 0
    var gameOver         = false
    var waveMaxAsteroids = 1   // peak alive count this wave; denominator for danger ratio

    val input            = GameInput()
    val bulletPool       = BulletPool()
    val collisionSystem  = CollisionSystem(this)
    val waveSystem       = WaveSystem(this)
    var vfx: VfxManager? = null
    var sounds: com.palacesoft.asteroids.audio.SoundManager? = null

    private val ROTATE_SPEED = 200f
    private val THRUST_FORCE = 400f
    private val DRAG         = 0.985f
    private val MAX_SPEED    = 500f
    private val FIRE_RATE    = 0.22f
    private var fireCooldown = 0f

    fun start() {
        waveSystem.start()
    }

    fun update(delta: Float) {
        if (gameOver) return
        updateShip(delta)
        updateBullets(delta)
        updateAsteroids(delta)
        asteroids.removeAll { !it.alive }
        bullets.removeAll   { !it.alive }
        collisionSystem.update()
        waveSystem.update(delta)
        // Track peak alive count so the heartbeat danger ratio stays valid across splits
        val alive = asteroids.count { it.alive }
        if (alive > waveMaxAsteroids) waveMaxAsteroids = alive
        if (lives <= 0 && !ship.alive) gameOver = true
    }

    private fun updateShip(delta: Float) {
        if (!ship.alive) { ship.visible = false; return }
        if (ship.invulnerableTimer > 0f) {
            ship.invulnerableTimer -= delta
            ship.flickerAccum += delta
            ship.visible = (ship.flickerAccum % 0.2f) < 0.1f
        } else {
            ship.visible = true
            ship.flickerAccum = 0f
        }
        if (input.rotateLeft)  ship.rotation += ROTATE_SPEED * delta
        if (input.rotateRight) ship.rotation -= ROTATE_SPEED * delta
        ship.thrusting = input.thrust
        if (input.thrust) {
            val rad = Math.toRadians(ship.rotation.toDouble())
            ship.velX += cos(rad).toFloat() * THRUST_FORCE * delta
            ship.velY += sin(rad).toFloat() * THRUST_FORCE * delta
        }
        ship.velX *= DRAG; ship.velY *= DRAG
        val spd = sqrt(ship.velX * ship.velX + ship.velY * ship.velY)
        if (spd > MAX_SPEED) { ship.velX = ship.velX / spd * MAX_SPEED; ship.velY = ship.velY / spd * MAX_SPEED }
        ship.x += ship.velX * delta; ship.y += ship.velY * delta
        ship.x = wrapCoord(ship.x, 0f, Settings.WORLD_WIDTH)
        ship.y = wrapCoord(ship.y, 0f, Settings.WORLD_HEIGHT)
        fireCooldown -= delta
        if (input.fire && fireCooldown <= 0f) {
            bulletPool.acquire(ship, bullets)
            sounds?.playFire()
            GameEventBus.emit(GameEvent.BulletFired(ship.x, ship.y, ship.rotation))
            fireCooldown = FIRE_RATE
        }
        if (input.hyperspace) {
            ship.x = (Math.random() * Settings.WORLD_WIDTH).toFloat()
            ship.y = (Math.random() * Settings.WORLD_HEIGHT).toFloat()
            ship.invulnerableTimer = 1.5f
            input.hyperspace = false
        }
    }

    private fun updateBullets(delta: Float) {
        for (b in bullets) {
            if (!b.alive) continue
            b.x += b.velX * delta; b.y += b.velY * delta
            b.x = wrapCoord(b.x, 0f, Settings.WORLD_WIDTH)
            b.y = wrapCoord(b.y, 0f, Settings.WORLD_HEIGHT)
            val moved = sqrt(b.velX * b.velX + b.velY * b.velY) * delta
            b.distanceTravelled += moved
            if (b.distanceTravelled > Bullet.MAX_DISTANCE) b.alive = false
        }
    }

    private fun updateAsteroids(delta: Float) {
        for (a in asteroids) {
            if (!a.alive) continue
            a.x += a.velX * delta; a.y += a.velY * delta
            a.rotation += a.rotSpeed * delta
            a.x = wrapCoord(a.x, 0f, Settings.WORLD_WIDTH)
            a.y = wrapCoord(a.y, 0f, Settings.WORLD_HEIGHT)
        }
    }
}
