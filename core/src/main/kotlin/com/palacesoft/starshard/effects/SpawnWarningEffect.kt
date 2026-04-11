package com.palacesoft.starshard.effects

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import kotlin.math.PI
import kotlin.math.sin

/**
 * Three quick pulses at the saucer's entry point before it appears.
 * Gives the player a moment to react. Duration 0.8s. Pool capacity: 2.
 */
class SpawnWarningEffect : PooledEffect() {

    companion object {
        const val DURATION   = 0.8f
        private const val PULSE_FREQ = 3f / DURATION   // 3 pulses in total
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
        val t       = (life / DURATION).coerceIn(0f, 1f)
        val elapsed = DURATION - life
        val pulse   = sin(elapsed * PULSE_FREQ * 2f * PI.toFloat()).coerceAtLeast(0f)
        val radius  = 24f + pulse * 20f
        val alpha   = t * pulse * 0.9f
        sr.setColor(1f, 0.3f, 0.3f, alpha)   // danger red
        sr.circle(x, y, radius, 20)
    }

    override fun reset() { alive = false; life = 0f }
}
