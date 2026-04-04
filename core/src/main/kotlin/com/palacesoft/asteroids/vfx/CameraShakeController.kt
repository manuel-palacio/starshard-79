// core/src/main/kotlin/com/palacesoft/asteroids/vfx/CameraShakeController.kt
package com.palacesoft.asteroids.vfx

import kotlin.math.sin

/**
 * Trauma-based camera shake.
 *
 * How it works
 * ────────────
 * "Trauma" is a value in [0, 1] that accumulates on impact.
 * The actual shake magnitude is trauma² — this gives a punchy onset
 * that softens as trauma decays, which feels more natural than linear.
 * Two sine oscillators at incommensurable frequencies produce a
 * non-repeating Lissajous-style motion without any noise library.
 *
 * Usage
 * ────────────
 *   // On explosion:
 *   shake.trigger(0.6f)
 *
 *   // In GameRenderer.render():
 *   camera.position.set(
 *       WORLD_WIDTH / 2f + shake.offsetX,
 *       WORLD_HEIGHT / 2f + shake.offsetY, 0f
 *   )
 *   // HUD camera: do NOT add offsetX/Y.
 *
 * Tuning
 * ────────────
 * Adjust MAX_OFFSET for physical screen size. On a phone in landscape,
 * 12–16 world units is enough to feel punchy without disorienting.
 * On desktop at 1600×900, 18 is comfortable.
 */
class CameraShakeController {

    // ── Tuning constants ─────────────────────────────────────────────────────
    private val MAX_OFFSET = 18f    // world units at trauma = 1.0
    private val DECAY_RATE = 1.8f   // trauma/second — how fast it settles
    private val FREQ_X     = 37f    // Hz — must be incommensurable with FREQ_Y
    private val FREQ_Y     = 41f    //       to avoid repeating patterns

    // ── Runtime state ────────────────────────────────────────────────────────
    private var trauma = 0f
    private var time   = 0f

    /** Apply this to the game world camera's X position each frame. */
    var offsetX = 0f
        private set

    /** Apply this to the game world camera's Y position each frame. */
    var offsetY = 0f
        private set

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Trigger shake.
     * @param intensity  0 = nothing, 1 = maximum. Values accumulate (capped at 1).
     *                   Typical values: bullet impact 0.0, small bang 0.15,
     *                   medium bang 0.35, large bang 0.60, ship death 0.80.
     */
    fun trigger(intensity: Float) {
        trauma = (trauma + intensity).coerceAtMost(1f)
    }

    fun update(delta: Float) {
        if (trauma <= 0f) {
            offsetX = 0f; offsetY = 0f
            return
        }
        time   += delta
        trauma  = (trauma - DECAY_RATE * delta).coerceAtLeast(0f)

        val mag = trauma * trauma    // trauma² = punchy onset, smooth tail
        offsetX = sin(time * FREQ_X) * MAX_OFFSET * mag
        offsetY = sin(time * FREQ_Y) * MAX_OFFSET * mag
    }

    fun isShaking() = trauma > 0.01f

    /** Hard-reset. Use when transitioning screens so shake doesn't carry over. */
    fun reset() {
        trauma  = 0f
        time    = 0f
        offsetX = 0f
        offsetY = 0f
    }
}
