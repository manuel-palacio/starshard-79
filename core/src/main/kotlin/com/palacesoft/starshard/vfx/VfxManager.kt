package com.palacesoft.starshard.vfx

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.starshard.effects.*
import com.palacesoft.starshard.effects.particle.ParticleEffectManager
import com.palacesoft.starshard.effects.text.FloatingTextSystem
import com.palacesoft.starshard.events.GameEvent
import com.palacesoft.starshard.events.GameEventBus
import com.palacesoft.starshard.game.entity.AsteroidSize
import com.palacesoft.starshard.game.entity.Ship
import com.palacesoft.starshard.util.Settings

/**
 * Single FX facade. All gameplay code talks only to this class via GameEventBus.
 *
 * Quality dispatch
 * ────────────────
 * LOW    → procedural ShapeRenderer effects only; no particles; shake ×0.45
 * MEDIUM → procedural primary + particle overlays for major destruction events
 * HIGH   → procedural + particles everywhere + bloom (via PostProcessingPipeline)
 *
 * Render order (called from GameRenderer)
 * ────────────────────────────────────────
 *  1. [inside bloom capture] renderBloomedEffects()
 *  2. [after bloom]          renderUnbloomedEffects()
 *  3.                        renderThrustParticles()
 *  4. [SpriteBatch world]    renderParticleOverlays(batch, delta)
 *  5. [SpriteBatch world]    renderTextEffects()
 *
 * Pool sizing
 * ───────────
 * Pools are sized to [FxSettings.maxTransientEffects]. On LOW this is 24;
 * on HIGH it is 72. The caps enforce the quality budget.
 */
class VfxManager(private val sr: ShapeRenderer, private val batch: SpriteBatch) {

    private val fx = Settings.fxSettings

    // ── Effect pools ──────────────────────────────────────────────────────────
    // Pool capacity is fixed at HIGH-quality maximum to avoid GC from
    // reallocation if quality changes at runtime. FxSettings.maxTransientEffects
    // is a runtime spawn cap (enforced in acquire() call sites), not a pool size.
    private val explosions  = EffectPool(24)  { ExplosionEffect() }
    private val hitSparks   = EffectPool(24)  { HitSparkEffect() }
    private val flashes     = EffectPool(4)   { MuzzleFlashEffect() }
    private val streaks     = EffectPool(16)  { GlowStreakEffect() }
    private val waveRings   = EffectPool(1)   { WaveStartEffect() }
    private val warnings    = EffectPool(2)   { SpawnWarningEffect() }
    private val respawnAura = RespawnFlashEffect()

    // ── Sub-systems ───────────────────────────────────────────────────────────
    val shake   = CameraShakeController()
    val textFx  = FloatingTextSystem(fx.maxScorePopups)

    // Particle overlays — null when disabled (LOW quality)
    private val particles = ParticleEffectManager(fx.enableParticles)

    // ThrustTrail — wired through ParticlePool
    private val particlePool = ParticlePool(80)
    private val thrust       = ThrustTrail(particlePool)

    val offsetX get() = shake.offsetX
    val offsetY get() = shake.offsetY

    // ── Event subscription ────────────────────────────────────────────────────

    fun subscribeToEvents() {
        GameEventBus.subscribe { event ->
            when (event) {
                is GameEvent.BulletFired       -> onBulletFired(event)
                is GameEvent.AsteroidHit       -> onAsteroidHit(event)
                is GameEvent.AsteroidDestroyed -> onAsteroidDestroyed(event)
                is GameEvent.PlayerHit         -> onPlayerHit(event)
                is GameEvent.PlayerRespawned   -> onPlayerRespawned(event)
                is GameEvent.SaucerSpawned     -> onSaucerSpawned(event)
                is GameEvent.SaucerDestroyed   -> onSaucerDestroyed(event)
                is GameEvent.WaveStarted       -> onWaveStarted(event)
                is GameEvent.ScoreAwarded      -> onScoreAwarded(event)
            }
        }
    }

    // ── Event handlers ────────────────────────────────────────────────────────

    private fun onBulletFired(e: GameEvent.BulletFired) {
        flashes.acquire()?.spawn(e.x, e.y)
        // muzzle_glow is HIGH-only: requires bloom pipeline to look intentional
        if (fx.enableBloom) {
            particles.spawnMuzzleGlow(e.x, e.y)
        }
    }

    private fun onAsteroidHit(e: GameEvent.AsteroidHit) {
        hitSparks.acquire()?.spawn(e.x, e.y)
        if (fx.enableParticles) particles.spawnImpactSparks(e.x, e.y)
    }

    private fun onAsteroidDestroyed(e: GameEvent.AsteroidDestroyed) {
        // Procedural debris — always present (core visual identity)
        explosions.acquire()?.spawn(e.x, e.y, e.size)

        // Shake scaled by quality
        val baseIntensity = when (e.size) {
            AsteroidSize.LARGE  -> 0.60f
            AsteroidSize.MEDIUM -> 0.35f
            AsteroidSize.SMALL  -> 0.15f
        }
        shake.trigger(baseIntensity * fx.shakeMultiplier)

        // MEDIUM+: additive particle glow overlay on top of procedural debris
        if (fx.enableParticles) {
            particles.spawnExplosionGlow(e.x, e.y)
            // AsteroidHit is never emitted (every bullet hit destroys), so sparks fire here
            particles.spawnImpactSparks(e.x, e.y)
        }
    }

    private fun onPlayerHit(e: GameEvent.PlayerHit) {
        // Procedural debris — always visible at all quality levels
        explosions.acquire()?.spawn(e.x, e.y, AsteroidSize.LARGE)
        shake.trigger(0.80f * fx.shakeMultiplier)
        if (fx.enableParticles) particles.spawnShipDeath(e.x, e.y)
    }

    private fun onPlayerRespawned(e: GameEvent.PlayerRespawned) {
        respawnAura.begin(e.x, e.y, 3f)
    }

    private fun onSaucerSpawned(e: GameEvent.SaucerSpawned) {
        warnings.acquire()?.spawn(e.x, e.y)
    }

    private fun onSaucerDestroyed(e: GameEvent.SaucerDestroyed) {
        explosions.acquire()?.spawn(e.x, e.y, AsteroidSize.LARGE)
        shake.trigger(0.50f * fx.shakeMultiplier)
        if (fx.enableParticles) particles.spawnExplosionGlow(e.x, e.y)
    }

    private fun onWaveStarted(@Suppress("UNUSED_PARAMETER") e: GameEvent.WaveStarted) {
        // Theatrical ring — only on HIGH quality (premium polish, not original Asteroids)
        if (fx.enableWaveRing) waveRings.acquire()?.spawn()
    }

    private fun onScoreAwarded(e: GameEvent.ScoreAwarded) {
        // Score popups are HIGH-only; LOW/MEDIUM use HUD-corner score (1979 identity)
        if (fx.maxScorePopups > 0) textFx.spawn(e.x, e.y, e.amount, fx.popupLifetime)
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    fun update(delta: Float) {
        shake.update(delta)
        explosions.update(delta)
        hitSparks.update(delta)
        flashes.update(delta)
        if (fx.enableTrails) streaks.update(delta)
        waveRings.update(delta)
        warnings.update(delta)
        if (respawnAura.alive) respawnAura.update(delta)
        particlePool.update(delta)
        textFx.update(delta)
    }

    fun updateThrust(delta: Float, ship: Ship) {
        thrust.update(delta, ship)
    }

    // ── Render passes ─────────────────────────────────────────────────────────

    /** Effects inside bloom capture — get glow halo. */
    fun renderBloomedEffects() {
        sr.begin(ShapeRenderer.ShapeType.Line)
        flashes.render(sr)
        if (fx.enableTrails) streaks.render(sr)
        if (respawnAura.alive) respawnAura.render(sr)
        sr.end()
    }

    /** Effects outside bloom — crisp, no glow blur. */
    fun renderUnbloomedEffects() {
        sr.begin(ShapeRenderer.ShapeType.Line)
        explosions.render(sr)
        hitSparks.render(sr)
        waveRings.render(sr)
        warnings.render(sr)
        sr.end()
    }

    /** Thrust particles — existing ParticlePool renderer. */
    fun renderThrustParticles() {
        particlePool.render(sr)
    }

    /**
     * Particle overlays (additive, via SpriteBatch).
     * Call between batch.begin() and batch.end() with world camera.
     */
    fun renderParticleOverlays(batch: SpriteBatch, delta: Float) {
        if (fx.enableParticles) particles.render(batch, delta)
    }

    /** Score popups — always last (crisp, no bloom). */
    fun renderTextEffects() {
        batch.begin()
        textFx.render(batch)
        batch.end()
    }

    fun dispose() {
        textFx.dispose()
        particles.dispose()
        shake.reset()
    }
}
