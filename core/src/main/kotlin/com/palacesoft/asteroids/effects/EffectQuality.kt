package com.palacesoft.asteroids.effects

/**
 * Visual quality ladder for FX systems.
 *
 * Design intent
 * ─────────────
 * Every quality level preserves the classic vector silhouette.
 * Procedural ShapeRenderer effects (debris lines, flashes, sparks) are
 * the primary visual identity. Particle effects and bloom add shimmer
 * on top — they never replace the line art.
 *
 * Defaults
 * ─────────
 * MEDIUM — Android / balanced (particles for major events only, no bloom)
 * HIGH   — Desktop / premium (particles + optional bloom)
 * LOW    — Weak devices / power-save mode (procedural only, no particles)
 */
enum class EffectQuality { LOW, MEDIUM, HIGH }

/**
 * Per-quality configuration. Derived once from [EffectQuality]; never
 * changed at runtime (no mutable state, no allocations after init).
 *
 * @param enableParticles      Allow libGDX ParticleEffect overlays.
 * @param enableBloom          Allow BloomPass post-processing.
 * @param enableTrails         Allow GlowStreakEffect on bullets.
 * @param maxTransientEffects  Hard cap on simultaneously alive pooled effects.
 * @param maxScorePopups       Hard cap on simultaneously alive score labels.
 * @param shakeMultiplier      Scale applied to all CameraShakeController triggers.
 * @param explosionDebrisCount Target debris line count per explosion (approximate).
 * @param sparkCount           Target spark count per HitSparkEffect spawn.
 * @param popupLifetime        Score popup duration in seconds.
 */
data class FxSettings(
    val enableParticles: Boolean,
    val enableBloom: Boolean,
    val enableTrails: Boolean,
    val maxTransientEffects: Int,
    val maxScorePopups: Int,
    val shakeMultiplier: Float,
    val explosionDebrisCount: Int,
    val sparkCount: Int,
    val popupLifetime: Float
)

/**
 * Returns the [FxSettings] for [quality].
 *
 * Tuning notes (adjust in the Particle Editor or here):
 *  - explosionDebrisCount and sparkCount are hints; actual counts may vary
 *    by ±30% due to random jitter in the effect spawn functions.
 *  - shakeMultiplier of 0.45 on LOW keeps shake present but unobtrusive.
 *  - popupLifetime < 0.5s on LOW prevents popup accumulation during fast waves.
 */
fun fxSettingsFor(quality: EffectQuality): FxSettings = when (quality) {
    EffectQuality.LOW -> FxSettings(
        enableParticles        = false,
        enableBloom            = false,
        enableTrails           = true,
        maxTransientEffects    = 24,
        maxScorePopups         = 6,
        shakeMultiplier        = 0.45f,
        explosionDebrisCount   = 6,
        sparkCount             = 4,
        popupLifetime          = 0.45f
    )
    EffectQuality.MEDIUM -> FxSettings(
        enableParticles        = true,
        enableBloom            = false,
        enableTrails           = true,
        maxTransientEffects    = 40,
        maxScorePopups         = 10,
        shakeMultiplier        = 0.75f,
        explosionDebrisCount   = 10,
        sparkCount             = 8,
        popupLifetime          = 0.60f
    )
    EffectQuality.HIGH -> FxSettings(
        enableParticles        = true,
        enableBloom            = true,
        enableTrails           = true,
        maxTransientEffects    = 72,
        maxScorePopups         = 16,
        shakeMultiplier        = 1.0f,
        explosionDebrisCount   = 16,
        sparkCount             = 14,
        popupLifetime          = 0.75f
    )
}
