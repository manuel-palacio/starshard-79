package com.palacesoft.asteroids.effects

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import kotlin.math.sin

/**
 * Pulsing aura ring around the ship during invulnerability.
 * Not particle-based — renders a single circle that pulses in alpha.
 * Pool capacity: 1 (one player).
 */
class RespawnFlashEffect : PooledEffect() {

    private var x       = 0f
    private var y       = 0f
    private var timer   = 0f
    private var maxTime = 3f

    fun begin(x: Float, y: Float, duration: Float) {
        alive    = true
        this.x   = x; this.y = y
        timer    = duration
        maxTime  = duration
    }

    fun updatePosition(x: Float, y: Float) {
        this.x = x; this.y = y
    }

    override fun update(delta: Float) {
        timer -= delta
        if (timer <= 0f) reset()
    }

    override fun render(sr: ShapeRenderer) {
        val t     = (timer / maxTime).coerceIn(0f, 1f)
        val pulse = sin(timer * 12f).coerceIn(0f, 1f)
        val alpha = t * pulse * 0.5f
        sr.setColor(0.6f, 0.8f, 1f, alpha)   // cool white-blue
        sr.circle(x, y, 22f, 16)
    }

    override fun reset() { alive = false; timer = 0f }
}
