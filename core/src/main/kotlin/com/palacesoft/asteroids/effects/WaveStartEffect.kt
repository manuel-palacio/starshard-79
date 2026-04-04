package com.palacesoft.asteroids.effects

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.asteroids.util.Settings

/**
 * Single large expanding ring that crosses the entire screen over 1.2s.
 * Triggered once per WaveStarted event. Pool capacity: 1.
 * Rendered OUTSIDE bloom (it's a large transparent shape — bloom would muddy it).
 */
class WaveStartEffect : PooledEffect() {

    companion object {
        const val DURATION   = 1.2f
        const val MAX_RADIUS = 1100f   // exceeds screen diagonal at 1600×900
    }

    private var life = 0f

    fun spawn() {
        alive = true
        life  = DURATION
    }

    override fun update(delta: Float) {
        life -= delta
        if (life <= 0f) reset()
    }

    override fun render(sr: ShapeRenderer) {
        val t        = (life / DURATION).coerceIn(0f, 1f)
        val progress = 1f - t
        val radius   = progress * MAX_RADIUS
        val alpha    = t * 0.35f
        sr.setColor(0.4f, 0.7f, 1f, alpha)   // cool blue
        sr.circle(Settings.WORLD_WIDTH / 2f, Settings.WORLD_HEIGHT / 2f, radius, 48)
    }

    override fun reset() { alive = false; life = 0f }
}
