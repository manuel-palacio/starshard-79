package com.palacesoft.starshard.effects

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 3–5 tiny spark lines radiating from a bullet impact point.
 * Triggered on AsteroidHit (bullet hit, no kill) — not on destruction.
 * Pool capacity recommendation: 24 (4 active bullets × ~6 sparks per hit).
 */
class HitSparkEffect : PooledEffect() {

    companion object {
        private const val MAX_SPARKS = 5
        const val DURATION = 0.12f
    }

    private val px   = FloatArray(MAX_SPARKS)
    private val py   = FloatArray(MAX_SPARKS)
    private val vx   = FloatArray(MAX_SPARKS)
    private val vy   = FloatArray(MAX_SPARKS)
    private var count = 0
    private var life  = 0f

    fun spawn(x: Float, y: Float, rng: Random = Random) {
        alive = true
        life  = DURATION
        count = 3 + rng.nextInt(3)   // 3–5
        for (i in 0 until count) {
            val angle = rng.nextFloat() * 2f * PI.toFloat()
            val spd   = 80f + rng.nextFloat() * 80f
            px[i] = x; py[i] = y
            vx[i] = cos(angle) * spd
            vy[i] = sin(angle) * spd
        }
    }

    override fun update(delta: Float) {
        life -= delta
        if (life <= 0f) { reset(); return }
        val drag = 1f - delta * 6f
        for (i in 0 until count) {
            px[i] += vx[i] * delta; py[i] += vy[i] * delta
            vx[i] *= drag;          vy[i] *= drag
        }
    }

    override fun render(sr: ShapeRenderer) {
        val t = (life / DURATION).coerceIn(0f, 1f)
        sr.setColor(1f, 0.9f, 0.5f, t)   // bright yellow, fades out
        for (i in 0 until count) {
            sr.line(px[i], py[i], px[i] + vx[i] * 0.02f, py[i] + vy[i] * 0.02f)
        }
    }

    override fun reset() { alive = false; count = 0; life = 0f }
}
