package com.palacesoft.starshard.game.system

import com.palacesoft.starshard.game.entity.Bullet
import com.palacesoft.starshard.game.entity.Saucer
import com.palacesoft.starshard.game.entity.SaucerType
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
        bullet.colorR = 1f; bullet.colorG = 1f; bullet.colorB = 1f
        if (!bullets.contains(bullet)) bullets.add(bullet)
    }

    fun acquireForSaucer(
        saucer: Saucer,
        targetX: Float, targetY: Float,
        bullets: MutableList<Bullet>,
        spreadDeg: Float = 0f
    ) {
        val baseAngle = Math.atan2((targetY - saucer.y).toDouble(), (targetX - saucer.x).toDouble()).toFloat()

        when (saucer.type) {
            SaucerType.CRESCENT -> {
                // Twin shots at ±8° from aim direction
                fireSaucerBullet(saucer, baseAngle + Math.toRadians(8.0).toFloat(), bullets)
                fireSaucerBullet(saucer, baseAngle - Math.toRadians(8.0).toFloat(), bullets)
            }
            else -> {
                // Single shot with spread (CLASSIC uses this, DIAMOND uses it per burst round)
                val spread = Math.toRadians(((Math.random() - 0.5) * spreadDeg * 2).toDouble()).toFloat()
                fireSaucerBullet(saucer, baseAngle + spread, bullets)
            }
        }
    }

    private fun fireSaucerBullet(saucer: Saucer, angle: Float, bullets: MutableList<Bullet>) {
        val bullet = pool.firstOrNull { !it.alive } ?: return
        bullet.x = saucer.x; bullet.y = saucer.y
        bullet.velX = cos(angle) * Bullet.SPEED
        bullet.velY = sin(angle) * Bullet.SPEED
        bullet.alive = true
        bullet.distanceTravelled = 0f
        bullet.fromPlayer = false
        when (saucer.type) {
            SaucerType.CLASSIC  -> { bullet.colorR = 1.0f; bullet.colorG = 0.3f; bullet.colorB = 0.3f } // red
            SaucerType.DIAMOND  -> { bullet.colorR = 1.0f; bullet.colorG = 0.8f; bullet.colorB = 0.2f } // yellow
            SaucerType.CRESCENT -> { bullet.colorR = 0.4f; bullet.colorG = 1.0f; bullet.colorB = 0.4f } // green
        }
        if (!bullets.contains(bullet)) bullets.add(bullet)
    }
}
