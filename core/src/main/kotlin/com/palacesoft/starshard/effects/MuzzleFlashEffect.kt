package com.palacesoft.starshard.effects

import com.badlogic.gdx.graphics.glutils.ShapeRenderer

/**
 * Very brief white circle flash at the ship's nose when firing.
 * Duration 0.06s. Radius scales 0 → 8 → 0. Pool capacity: 4.
 * Rendered inside bloom capture for glow halo.
 */
class MuzzleFlashEffect : PooledEffect() {

    companion object {
        const val DURATION = 0.06f
    }

    private var x    = 0f
    private var y    = 0f
    private var life = 0f

    fun spawn(x: Float, y: Float) {
        alive  = true
        this.x = x; this.y = y
        life   = DURATION
    }

    override fun update(delta: Float) {
        life -= delta
        if (life <= 0f) reset()
    }

    override fun render(sr: ShapeRenderer) {
        val t = (life / DURATION).coerceIn(0f, 1f)
        // triangle wave: peak at t=0.5 (mid-flash)
        val scale = 1f - kotlin.math.abs(t - 0.5f) * 2f
        sr.setColor(1f, 1f, 1f, t * 0.9f)
        sr.circle(x, y, 8f * scale, 10)
    }

    override fun reset() { alive = false; life = 0f }
}
