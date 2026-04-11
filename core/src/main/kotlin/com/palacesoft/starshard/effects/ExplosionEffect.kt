// core/src/main/kotlin/com/palacesoft/starshard/effects/ExplosionEffect.kt
package com.palacesoft.starshard.effects

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.starshard.game.entity.AsteroidSize
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Pooled explosion — asteroid and saucer destructions.
 *
 * Visual design
 * ─────────────
 * - Outward debris lines: each is a velocity vector rendered as a
 *   short line segment, fading white → gold → transparent.
 * - Expanding ring (LARGE and MEDIUM only): gold circle that grows
 *   to [ringMaxR] over the effect duration, fading out.
 *
 * Pool sizing: wave N spawns up to N*2+2 large asteroids. Each splits
 * to 2 medium and 4 small. Worst-case simultaneous explosions on a
 * wave-clear: ~10 large. Use pool capacity ≥ 24 for ExplosionEffect.
 *
 * Performance
 * ───────────
 * - Zero heap allocation in update() and render().
 * - All state in primitive FloatArrays and Boolean flags.
 * - ShapeRenderer.setColor() is called per debris line — batch with
 *   other effects in a single sr.begin(Line)/sr.end() block.
 */
class ExplosionEffect : PooledEffect() {

    companion object {
        private const val MAX_DEBRIS = 16
    }

    // Spawn position (fixed for the effect's lifetime)
    private var originX = 0f
    private var originY = 0f

    // Debris: positions relative to origin, velocities in world units/s
    private val debrisPx  = FloatArray(MAX_DEBRIS)
    private val debrisPy  = FloatArray(MAX_DEBRIS)
    private val debrisVx  = FloatArray(MAX_DEBRIS)
    private val debrisVy  = FloatArray(MAX_DEBRIS)
    private val debrisLen = FloatArray(MAX_DEBRIS)   // half-length of each line
    private var debrisCount = 0

    // Ring
    private var ringRadius  = 0f
    private var ringMaxR    = 0f
    private var ringEnabled = false

    // Timing
    private var life    = 0f
    private var maxLife = 0f

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Activate this effect. Must be called after acquire() from the pool.
     */
    fun spawn(x: Float, y: Float, size: AsteroidSize, rng: Random = Random) {
        alive   = true
        originX = x
        originY = y

        // Size-specific configuration
        when (size) {
            AsteroidSize.LARGE  -> { debrisCount = 12 + rng.nextInt(5); maxLife = 0.70f; ringMaxR = 80f;  ringEnabled = true  }
            AsteroidSize.MEDIUM -> { debrisCount =  7 + rng.nextInt(4); maxLife = 0.50f; ringMaxR = 44f;  ringEnabled = true  }
            AsteroidSize.SMALL  -> { debrisCount =  4 + rng.nextInt(3); maxLife = 0.30f; ringMaxR =  0f;  ringEnabled = false }
        }
        life = maxLife

        val speed = when (size) {
            AsteroidSize.LARGE  -> 220f
            AsteroidSize.MEDIUM -> 180f
            AsteroidSize.SMALL  -> 140f
        }

        // Distribute debris evenly with per-line angle/speed jitter
        for (i in 0 until debrisCount) {
            val baseAngle = (i.toFloat() / debrisCount) * 2f * PI.toFloat()
            val angle     = baseAngle + (rng.nextFloat() - 0.5f) * 0.45f   // ±25° jitter
            val spd       = speed * (0.55f + rng.nextFloat() * 0.90f)
            debrisPx[i]   = 0f
            debrisPy[i]   = 0f
            debrisVx[i]   = cos(angle) * spd
            debrisVy[i]   = sin(angle) * spd
            debrisLen[i]  = 5f + rng.nextFloat() * 9f
        }
        ringRadius = 0f
    }

    // ── PooledEffect overrides ───────────────────────────────────────────────

    override fun update(delta: Float) {
        life -= delta
        if (life <= 0f) { reset(); return }

        val drag = 1f - delta * 2.2f   // exponential velocity decay
        for (i in 0 until debrisCount) {
            debrisPx[i] += debrisVx[i] * delta
            debrisPy[i] += debrisVy[i] * delta
            debrisVx[i] *= drag
            debrisVy[i] *= drag
        }
        if (ringEnabled) {
            ringRadius = (1f - life / maxLife) * ringMaxR
        }
    }

    override fun render(sr: ShapeRenderer) {
        val t = (life / maxLife).coerceIn(0f, 1f)   // 1 = fresh, 0 = done

        // Debris: white → gold → transparent
        for (i in 0 until debrisCount) {
            val wx   = originX + debrisPx[i]
            val wy   = originY + debrisPy[i]
            val vx   = debrisVx[i]
            val vy   = debrisVy[i]
            val norm = sqrt(vx * vx + vy * vy).coerceAtLeast(0.001f)
            val len  = debrisLen[i] * t
            val ex   = wx + (vx / norm) * len
            val ey   = wy + (vy / norm) * len

            // Color: full white at t=1, gold at t=0.5, fades at t=0
            sr.setColor(1f, 0.55f + t * 0.45f, t * 0.18f, t)
            sr.line(wx, wy, ex, ey)
        }

        // Ring: expanding warm-gold halo, fades out
        if (ringEnabled && ringRadius > 0f) {
            val ringAlpha = t * 0.55f
            sr.setColor(1f, 0.78f, 0.22f, ringAlpha)
            sr.circle(originX, originY, ringRadius, 24)
        }
    }

    override fun reset() {
        alive       = false
        debrisCount = 0
        ringEnabled = false
        life        = 0f
    }
}
