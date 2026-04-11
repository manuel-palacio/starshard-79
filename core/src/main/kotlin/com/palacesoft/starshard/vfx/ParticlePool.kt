package com.palacesoft.starshard.vfx

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

class Particle {
    var x = 0f; var y = 0f
    var velX = 0f; var velY = 0f
    var life = 0f; var maxLife = 1f
    var r = 1f; var g = 1f; var b = 1f
    var size = 2f
    var alive = false
    var isLine = false      // if true, drawn as a short line (debris)
    var angle  = 0f         // rotation for line debris
    var rotSpeed = 0f
}

class ParticlePool(capacity: Int = 400) {
    val particles = Array(capacity) { Particle() }
    private var head = 0

    fun acquire(): Particle? {
        var checked = 0
        while (checked < particles.size) {
            val p = particles[head]
            head = (head + 1) % particles.size
            checked++
            if (!p.alive) return p
        }
        return null  // pool exhausted — silently drop
    }

    fun update(delta: Float) {
        particles.forEach { p ->
            if (!p.alive) return@forEach
            p.x    += p.velX * delta
            p.y    += p.velY * delta
            p.life -= delta
            p.angle += p.rotSpeed * delta
            if (p.life <= 0f) p.alive = false
        }
    }

    fun render(sr: ShapeRenderer) {
        sr.begin(ShapeRenderer.ShapeType.Filled)
        particles.forEach { p ->
            if (!p.alive) return@forEach
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            sr.color = Color(p.r, p.g, p.b, alpha)
            if (!p.isLine) {
                sr.circle(p.x, p.y, p.size * alpha)
            }
        }
        sr.end()

        sr.begin(ShapeRenderer.ShapeType.Line)
        particles.forEach { p ->
            if (!p.alive || !p.isLine) return@forEach
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            sr.color = Color(p.r, p.g, p.b, alpha)
            val cos = kotlin.math.cos(Math.toRadians(p.angle.toDouble())).toFloat()
            val sin = kotlin.math.sin(Math.toRadians(p.angle.toDouble())).toFloat()
            val len = p.size * 4f
            sr.line(p.x - cos * len, p.y - sin * len, p.x + cos * len, p.y + sin * len)
        }
        sr.end()
    }
}
