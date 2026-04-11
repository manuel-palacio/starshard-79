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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

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

    // ThrustTrail + general particle effects — shared pool
    private val particlePool = ParticlePool(600)
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
                is GameEvent.Hyperspace        -> onHyperspace(event)
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

        // Shatter sparks — bright white sparks that scatter outward
        val sparkCount = when (e.size) {
            AsteroidSize.LARGE  -> 18
            AsteroidSize.MEDIUM -> 10
            AsteroidSize.SMALL  -> 5
        }
        emitSparks(e.x, e.y, sparkCount, 1f, 0.9f, 0.7f, 280f, 0.4f)

        // MEDIUM+: additive particle glow overlay on top of procedural debris
        if (fx.enableParticles) {
            particles.spawnExplosionGlow(e.x, e.y)
            particles.spawnImpactSparks(e.x, e.y)
        }
    }

    private fun onPlayerHit(e: GameEvent.PlayerHit) {
        // Procedural debris — always visible at all quality levels
        explosions.acquire()?.spawn(e.x, e.y, AsteroidSize.LARGE)
        shake.trigger(0.80f * fx.shakeMultiplier)

        // Dramatic death: large burst of cyan-white sparks + debris lines
        emitSparks(e.x, e.y, 30, 0.5f, 0.8f, 1f, 350f, 0.7f)
        emitDebrisLines(e.x, e.y, 12, 0.4f, 0.7f, 1f, 250f, 0.8f)

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
        // Electric death sparks in saucer's color
        emitSparks(e.x, e.y, 20, e.r, e.g, e.b, 300f, 0.5f)
        emitDebrisLines(e.x, e.y, 8, e.r, e.g, e.b, 200f, 0.6f)
        if (fx.enableParticles) particles.spawnExplosionGlow(e.x, e.y)
    }

    private fun onHyperspace(e: GameEvent.Hyperspace) {
        // Shimmer ring at departure point
        emitRing(e.fromX, e.fromY, 16, 0.6f, 0.8f, 1f, 150f, 0.4f)
        // Sparkle at arrival point
        emitRing(e.toX, e.toY, 12, 0.8f, 0.9f, 1f, 80f, 0.35f)
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

    /** Emit bullet trail particles for all alive non-player bullets. */
    fun updateBulletTrails(bullets: List<com.palacesoft.starshard.game.entity.Bullet>) {
        for (b in bullets) {
            if (!b.alive || b.fromPlayer) continue
            // ~30% chance per frame to emit a trail particle (avoids flooding)
            if (Random.nextFloat() > 0.30f) continue
            val p = particlePool.acquire() ?: return
            p.x = b.x + (Random.nextFloat() - 0.5f) * 4f
            p.y = b.y + (Random.nextFloat() - 0.5f) * 4f
            p.velX = (Random.nextFloat() - 0.5f) * 15f
            p.velY = (Random.nextFloat() - 0.5f) * 15f
            p.life = 0.15f; p.maxLife = 0.15f
            p.r = b.colorR; p.g = b.colorG; p.b = b.colorB
            p.size = 1.5f
            p.alive = true; p.isLine = false
        }
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

    // ── Particle emitter helpers ──────────────────────────────────────────────

    /** Emit radial burst of dot sparks. */
    private fun emitSparks(x: Float, y: Float, count: Int, r: Float, g: Float, b: Float, speed: Float, life: Float) {
        repeat(count) {
            val p = particlePool.acquire() ?: return
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val spd = speed * (0.4f + Random.nextFloat() * 0.6f)
            p.x = x; p.y = y
            p.velX = cos(angle) * spd; p.velY = sin(angle) * spd
            p.life = life * (0.6f + Random.nextFloat() * 0.4f); p.maxLife = p.life
            p.r = r; p.g = g; p.b = b
            p.size = 1.5f + Random.nextFloat() * 2.5f
            p.alive = true; p.isLine = false
        }
    }

    /** Emit radial burst of spinning line debris. */
    private fun emitDebrisLines(x: Float, y: Float, count: Int, r: Float, g: Float, b: Float, speed: Float, life: Float) {
        repeat(count) {
            val p = particlePool.acquire() ?: return
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val spd = speed * (0.3f + Random.nextFloat() * 0.7f)
            p.x = x; p.y = y
            p.velX = cos(angle) * spd; p.velY = sin(angle) * spd
            p.life = life * (0.5f + Random.nextFloat() * 0.5f); p.maxLife = p.life
            p.r = r; p.g = g; p.b = b
            p.size = 2f + Random.nextFloat() * 3f
            p.alive = true; p.isLine = true
            p.angle = Random.nextFloat() * 360f
            p.rotSpeed = (Random.nextFloat() - 0.5f) * 600f
        }
    }

    /** Emit expanding ring of particles (hyperspace shimmer). */
    private fun emitRing(x: Float, y: Float, count: Int, r: Float, g: Float, b: Float, radius: Float, life: Float) {
        repeat(count) {
            val p = particlePool.acquire() ?: return
            val angle = (it.toFloat() / count) * 2f * Math.PI.toFloat()
            p.x = x; p.y = y
            p.velX = cos(angle) * radius; p.velY = sin(angle) * radius
            p.life = life; p.maxLife = life
            p.r = r; p.g = g; p.b = b
            p.size = 2f + Random.nextFloat() * 1.5f
            p.alive = true; p.isLine = false
        }
    }

    fun dispose() {
        textFx.dispose()
        particles.dispose()
        shake.reset()
    }
}
