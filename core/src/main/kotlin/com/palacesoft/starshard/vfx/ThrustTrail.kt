package com.palacesoft.starshard.vfx

import com.palacesoft.starshard.game.entity.Ship
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class ThrustTrail(private val pool: ParticlePool) {
    private var accumulator = 0f
    private val EMIT_INTERVAL = 0.03f

    fun update(delta: Float, ship: Ship) {
        if (!ship.thrusting || !ship.alive) { accumulator = 0f; return }
        accumulator += delta
        while (accumulator >= EMIT_INTERVAL) {
            accumulator -= EMIT_INTERVAL
            emit(ship)
        }
    }

    private fun emit(ship: Ship) {
        val p = pool.acquire() ?: return
        val rad = Math.toRadians(ship.rotation.toDouble()).toFloat()
        val spread = (Random.nextFloat() - 0.5f) * 0.6f
        p.x = ship.x - cos(rad) * ship.radius * 0.7f
        p.y = ship.y - sin(rad) * ship.radius * 0.7f
        p.velX = ship.velX - cos(rad + spread) * 120f
        p.velY = ship.velY - sin(rad + spread) * 120f
        p.life = 0.25f; p.maxLife = 0.25f
        p.r = 0.4f; p.g = 0.7f; p.b = 1f
        p.size = 3f
        p.alive = true; p.isLine = false
    }
}
