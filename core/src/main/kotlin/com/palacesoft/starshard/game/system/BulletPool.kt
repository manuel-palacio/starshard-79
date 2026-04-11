package com.palacesoft.starshard.game.system

import com.palacesoft.starshard.game.entity.Bullet
import com.palacesoft.starshard.game.entity.Saucer
import com.palacesoft.starshard.game.entity.SaucerType
import com.palacesoft.starshard.game.entity.Ship
import kotlin.math.cos
import kotlin.math.sin

class BulletPool {
    private val pool = Array(30) { Bullet() }
    private val maxActive = 4
    private val maxActiveRapid = 6

    /** Wing tip offset angle from nose (matches ShipRenderer wing geometry). */
    private val WING_ANGLE = 2.5f
    private val WING_SCALE = 0.85f

    fun acquire(ship: Ship, bullets: MutableList<Bullet>) {
        val active = bullets.count { it.alive }
        if (active >= maxActive) return

        val bullet = pool.firstOrNull { !it.alive } ?: return
        val rad = Math.toRadians(ship.rotation.toDouble()).toFloat()

        // Alternate between left and right wing barrel
        val barrelAngle = if (ship.lastBarrel) rad - WING_ANGLE else rad + WING_ANGLE
        ship.lastBarrel = !ship.lastBarrel

        val spawnX = ship.x + cos(barrelAngle) * ship.radius * WING_SCALE
        val spawnY = ship.y + sin(barrelAngle) * ship.radius * WING_SCALE
        bullet.x = spawnX
        bullet.y = spawnY
        bullet.velX = ship.velX + cos(rad) * Bullet.SPEED
        bullet.velY = ship.velY + sin(rad) * Bullet.SPEED
        bullet.alive = true
        bullet.distanceTravelled = 0f
        bullet.fromPlayer = true
        bullet.colorR = 0.6f; bullet.colorG = 0.9f; bullet.colorB = 1f  // cyan tint for player
        if (!bullets.contains(bullet)) bullets.add(bullet)
    }

    /** Spread shot: fires 3 bullets in a 30° fan from alternating wing. */
    fun acquireSpread(ship: Ship, bullets: MutableList<Bullet>) {
        val rad = Math.toRadians(ship.rotation.toDouble()).toFloat()
        val barrelAngle = if (ship.lastBarrel) rad - WING_ANGLE else rad + WING_ANGLE
        ship.lastBarrel = !ship.lastBarrel
        val spawnX = ship.x + cos(barrelAngle) * ship.radius * WING_SCALE
        val spawnY = ship.y + sin(barrelAngle) * ship.radius * WING_SCALE

        for (offset in floatArrayOf(-0.26f, 0f, 0.26f)) { // ~15° spread
            val bullet = pool.firstOrNull { !it.alive } ?: return
            bullet.x = spawnX; bullet.y = spawnY
            bullet.velX = ship.velX + cos(rad + offset) * Bullet.SPEED
            bullet.velY = ship.velY + sin(rad + offset) * Bullet.SPEED
            bullet.alive = true
            bullet.distanceTravelled = 0f
            bullet.fromPlayer = true
            bullet.colorR = 0.3f; bullet.colorG = 0.6f; bullet.colorB = 1f // blue
            if (!bullets.contains(bullet)) bullets.add(bullet)
        }
    }

    /** Nova burst: fires 8 bullets in all directions from ship center. */
    fun acquireNova(ship: Ship, bullets: MutableList<Bullet>) {
        val baseRad = Math.toRadians(ship.rotation.toDouble()).toFloat()
        for (i in 0 until 8) {
            val bullet = pool.firstOrNull { !it.alive } ?: return
            val angle = baseRad + i * (Math.PI.toFloat() * 2f / 8f)
            bullet.x = ship.x + cos(angle) * ship.radius * 0.5f
            bullet.y = ship.y + sin(angle) * ship.radius * 0.5f
            bullet.velX = ship.velX + cos(angle) * Bullet.SPEED
            bullet.velY = ship.velY + sin(angle) * Bullet.SPEED
            bullet.alive = true
            bullet.distanceTravelled = 0f
            bullet.fromPlayer = true
            bullet.colorR = 1f; bullet.colorG = 0.5f; bullet.colorB = 0.1f // orange
            if (!bullets.contains(bullet)) bullets.add(bullet)
        }
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
