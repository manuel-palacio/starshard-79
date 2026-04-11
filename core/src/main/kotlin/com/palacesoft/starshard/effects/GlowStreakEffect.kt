package com.palacesoft.starshard.effects

import com.badlogic.gdx.graphics.glutils.ShapeRenderer

/**
 * Short fading white streak behind each bullet (trail effect).
 * Duration 0.10s. Pool capacity: 16.
 * Rendered inside bloom capture for the neon-tube look.
 */
class GlowStreakEffect : PooledEffect() {

    companion object {
        const val DURATION = 0.10f
    }

    private var x0 = 0f; private var y0 = 0f
    private var x1 = 0f; private var y1 = 0f
    private var life = 0f

    fun spawn(x0: Float, y0: Float, x1: Float, y1: Float) {
        alive    = true
        this.x0  = x0; this.y0 = y0
        this.x1  = x1; this.y1 = y1
        life     = DURATION
    }

    override fun update(delta: Float) {
        life -= delta
        if (life <= 0f) reset()
    }

    override fun render(sr: ShapeRenderer) {
        val t = (life / DURATION).coerceIn(0f, 1f)
        sr.setColor(1f, 1f, 1f, t * 0.6f)
        sr.line(x0, y0, x1, y1)
    }

    override fun reset() { alive = false; life = 0f }
}
