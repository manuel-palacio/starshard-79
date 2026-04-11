package com.palacesoft.starshard.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

/**
 * Scheme C — dual-zone gesture controls. Clean screen, no visible buttons.
 *
 * Left half (x < 0.50):
 *   Horizontal drag from anchor ≥ ROTATE_DEAD_PX = rotate left/right.
 *   Drag upward from anchor ≥ THRUST_DEAD_PX = thrust.
 *   Diagonal drag = rotate + thrust simultaneously.
 *
 * Right half (x ≥ 0.50):
 *   Tap / hold = fire (continuous while held).
 *   Swipe downward ≥ HYPER_SWIPE_PX = one-shot hyperspace.
 */
class GestureControls {

    companion object {
        const val LEFT_ZONE_END  = 0.50f
        const val ROTATE_DEAD_PX = 20f
        const val THRUST_DEAD_PX = 20f
        const val HYPER_SWIPE_PX = 80f
    }

    // ── Left-half state ───────────────────────────────────────────────────────

    private var leftPointer = -1
    private var leftAnchorX = 0f
    private var leftAnchorY = 0f
    private var leftCurrentX = 0f
    private var leftCurrentY = 0f

    // ── Right-half state ──────────────────────────────────────────────────────

    private var rightPointer = -1
    private var rightAnchorY = 0f
    private var hyperspaceArmed = false

    // ── Public API ────────────────────────────────────────────────────────────

    fun poll(input: GameInput) {
        val sw = Gdx.graphics.width.toFloat()

        if (leftPointer >= 0 && !Gdx.input.isTouched(leftPointer)) releaseLeft()
        if (rightPointer >= 0 && !Gdx.input.isTouched(rightPointer)) releaseRight()

        for (i in 0..4) {
            if (!Gdx.input.isTouched(i)) continue
            val nx = Gdx.input.getX(i).toFloat() / sw

            if (nx < LEFT_ZONE_END) {
                // ── Left zone — thrust + rotate ───────────────────────────
                if (leftPointer < 0) {
                    leftPointer = i
                    leftAnchorX = Gdx.input.getX(i).toFloat()
                    leftAnchorY = Gdx.input.getY(i).toFloat()
                    leftCurrentX = leftAnchorX
                    leftCurrentY = leftAnchorY
                }
                if (i == leftPointer) {
                    leftCurrentX = Gdx.input.getX(i).toFloat()
                    leftCurrentY = Gdx.input.getY(i).toFloat()
                    val dx = leftCurrentX - leftAnchorX
                    val dy = leftAnchorY - leftCurrentY // positive = drag up (screen y is inverted)
                    if (dx < -ROTATE_DEAD_PX) input.rotateLeft = true
                    if (dx > ROTATE_DEAD_PX) input.rotateRight = true
                    if (dy > THRUST_DEAD_PX) input.thrust = true
                }
            } else {
                // ── Right zone — fire + swipe-down hyperspace ─────────────
                if (rightPointer < 0) {
                    rightPointer = i
                    rightAnchorY = Gdx.input.getY(i).toFloat()
                    hyperspaceArmed = false
                }
                if (i == rightPointer) {
                    input.fire = true
                    val dy = Gdx.input.getY(i) - rightAnchorY // positive = swipe down
                    if (dy > HYPER_SWIPE_PX && !hyperspaceArmed) {
                        input.hyperspace = true
                        hyperspaceArmed = true
                    }
                }
            }
        }
    }

    /** No visible overlay — clean screen. */
    fun renderOverlay(@Suppress("UNUSED_PARAMETER") sr: ShapeRenderer) {
        // Intentionally empty — gesture controls have no on-screen UI
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun releaseLeft() {
        leftPointer = -1
    }

    private fun releaseRight() {
        rightPointer = -1
        hyperspaceArmed = false
    }
}
