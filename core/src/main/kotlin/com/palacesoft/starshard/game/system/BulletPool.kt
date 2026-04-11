package com.palacesoft.starshard.game.system

import com.palacesoft.starshard.game.entity.Bullet
import com.palacesoft.starshard.game.entity.Saucer
import com.palacesoft.starshard.game.entity.Ship
import kotlin.math.cos
import kotlin.math.sin

class BulletPool {
    private val pool = Array(20) { Bullet() }
    private val maxActive = 4

    fun acquire(ship: Ship, bullets: MutableList<Bullet>) {
        val active = bullets.count { it.alive }
        if (active >= maxActive) return

        val bullet = pool.firstOrNull { !it.alive } ?: return
        val rad = Math.toRadians(ship.rotation.toDouble())
        bullet.x = ship.x + cos(rad).toFloat() * ship.radius
        bullet.y = ship.y + sin(rad).toFloat() * ship.radius
        bullet.velX = ship.velX + cos(rad).toFloat() * Bullet.SPEED
        bullet.velY = ship.velY + sin(rad).toFloat() * Bullet.SPEED
        bullet.alive = true
        bullet.distanceTravelled = 0f
        bullet.fromPlayer = true
        if (!bullets.contains(bullet)) bullets.add(bullet)
    }

    fun acquireForSaucer(
        saucer: Saucer,
        targetX: Float, targetY: Float,
        bullets: MutableList<Bullet>,
        spreadDeg: Float = 0f
    ) {
        val bullet = pool.firstOrNull { !it.alive } ?: return
        val baseAngle = Math.atan2((targetY - saucer.y).toDouble(), (targetX - saucer.x).toDouble()).toFloat()
        val spread = Math.toRadians(((Math.random() - 0.5) * spreadDeg * 2).toDouble()).toFloat()
        val angle = baseAngle + spread
        bullet.x = saucer.x; bullet.y = saucer.y
        bullet.velX = cos(angle) * Bullet.SPEED
        bullet.velY = sin(angle) * Bullet.SPEED
        bullet.alive = true
        bullet.distanceTravelled = 0f
        bullet.fromPlayer = false
        if (!bullets.contains(bullet)) bullets.add(bullet)
    }
}
