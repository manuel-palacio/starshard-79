package com.palacesoft.starshard.effects.particle

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.ParticleEffect
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable

/**
 * Manages libGDX ParticleEffect instances for premium FX overlays.
 *
 * Design
 * ──────
 * Particle effects are additive overlays on top of procedural ShapeRenderer
 * effects. They are loaded once at startup and pooled via [ParticleEffectPool].
 * If [enabled] is false (LOW quality / no particle assets), all spawn calls
 * are silent no-ops — the procedural effects remain the only visual.
 *
 * Asset structure
 * ───────────────
 * .p files live in:  assets/particles/
 * Texture atlas at:  assets/particles/textures/
 *
 * Usage
 * ─────
 *   // In VfxManager, after quality check:
 *   particleManager.spawnExplosionGlow(x, y)
 *
 *   // Each frame (after bloom, before HUD):
 *   batch.begin()
 *   particleManager.render(batch, delta)
 *   batch.end()
 *
 *   // On dispose:
 *   particleManager.dispose()
 *
 */
class ParticleEffectManager(private val enabled: Boolean) : Disposable {

    // ── Pool sizes ────────────────────────────────────────────────────────────
    // Sized for worst-case simultaneous effects at MEDIUM/HIGH quality.
    private val EXPLOSION_POOL_SIZE = 16
    private val SPARK_POOL_SIZE     = 24
    private val MUZZLE_POOL_SIZE    =  8
    private val DEATH_POOL_SIZE     =  2

    // ── Particle pools — null if not enabled or assets missing ────────────────
    private var explosionGlowPool: ParticleEffectPool? = null
    private var impactSparksPool:  ParticleEffectPool? = null
    private var muzzleGlowPool:    ParticleEffectPool? = null
    private var shipDeathPool:     ParticleEffectPool? = null

    // ── Prototype effects (loaded once, copied by pool) ───────────────────────
    private val prototypes = ArrayList<ParticleEffect>()

    // ── Active effects returned from pools ────────────────────────────────────
    // GDX Array avoids iterator allocation on Android.
    private val active = Array<ActiveEffect>(false, 64)

    private data class ActiveEffect(
        val pool: ParticleEffectPool,
        val effect: ParticleEffectPool.PooledEffect
    )

    // ── Initialization ────────────────────────────────────────────────────────

    init {
        if (enabled) {
            explosionGlowPool = loadPool("particles/explosion_glow.p", EXPLOSION_POOL_SIZE)
            impactSparksPool  = loadPool("particles/impact_sparks.p",  SPARK_POOL_SIZE)
            muzzleGlowPool    = loadPool("particles/muzzle_glow.p",    MUZZLE_POOL_SIZE)
            shipDeathPool     = loadPool("particles/ship_death.p",     DEATH_POOL_SIZE)
        }
    }

    private fun loadPool(path: String, capacity: Int): ParticleEffectPool? {
        return try {
            val proto = ParticleEffect()
            proto.load(
                Gdx.files.internal(path),
                Gdx.files.internal("particles/textures")
            )
            proto.scaleEffect(1f)
            prototypes.add(proto)
            ParticleEffectPool(proto, capacity / 2, capacity)
        } catch (e: Exception) {
            Gdx.app.log("ParticleEffectManager", "Could not load $path: ${e.message}")
            null   // Graceful degradation — procedural effects remain
        }
    }

    // ── Public spawn API ──────────────────────────────────────────────────────

    /** Golden glow burst over asteroid destruction. MEDIUM+ only. */
    fun spawnExplosionGlow(x: Float, y: Float) = spawn(explosionGlowPool, x, y)

    /** White-yellow sparks on bullet impact. MEDIUM+ only. */
    fun spawnImpactSparks(x: Float, y: Float) = spawn(impactSparksPool, x, y)

    /** Brief additive flash at muzzle. HIGH only (very subtle). */
    fun spawnMuzzleGlow(x: Float, y: Float) = spawn(muzzleGlowPool, x, y)

    /** Cyan burst for player death. MEDIUM+ only. */
    fun spawnShipDeath(x: Float, y: Float) = spawn(shipDeathPool, x, y)

    private fun spawn(pool: ParticleEffectPool?, x: Float, y: Float) {
        if (!enabled || pool == null) return
        val effect = pool.obtain() ?: return
        effect.setPosition(x, y)
        effect.start()
        active.add(ActiveEffect(pool, effect))
    }

    // ── Per-frame update + render ─────────────────────────────────────────────

    /**
     * Call between batch.begin() and batch.end().
     * Uses additive blending (set in the .p files via [Options] additive=true).
     * Renders AFTER bloom composite and procedural effects, BEFORE score popups.
     */
    fun render(batch: SpriteBatch, delta: Float) {
        if (!enabled) return
        var i = 0
        while (i < active.size) {
            val entry = active[i]
            entry.effect.update(delta)
            entry.effect.draw(batch)
            if (entry.effect.isComplete) {
                entry.pool.free(entry.effect)
                active.removeIndex(i)
                // don't increment i — next element shifted into this slot
            } else {
                i++
            }
        }
    }

    // ── Disposal ──────────────────────────────────────────────────────────────

    override fun dispose() {
        // Free all active effects back to their pools before disposing
        for (entry in active) entry.pool.free(entry.effect)
        active.clear()
        for (p in prototypes) p.dispose()
        prototypes.clear()
    }
}
