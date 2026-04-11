package com.palacesoft.starshard.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4

/**
 * Scheme B — floating joystick + fire/hyperspace.
 *
 * Screen is divided vertically at x = 0.50.
 *
 * Left half — floating joystick:
 *   First touch sets the anchor. Drag from anchor:
 *     Horizontal offset ≥ ROTATE_DEAD_PX  → rotate left or right
 *     Upward offset     ≥ THRUST_DEAD_PX  → thrust
 *   No lock-out: diagonal drag = rotate + thrust simultaneously.
 *   Releasing the finger resets the anchor on the next touch.
 *
 * Right half — fire + hyperspace:
 *   Any touch held = continuous fire.
 *   Swiping the right-half finger upward ≥ HYPER_SWIPE_PX = one-shot hyperspace.
 *   Swipe state resets when the finger is released.
 *
 * Visual overlay (renderOverlay):
 *   When a left joystick touch is active, draws an outer ring at the anchor and
 *   an inner dot at the current thumb position in screen-pixel coordinates.
 */
class JoystickControls {

    companion object {
        const val LEFT_ZONE_END   = 0.50f   // normalised x split
        const val ROTATE_DEAD_PX  = 24f     // horizontal pixel dead zone
        const val THRUST_DEAD_PX  = 24f     // vertical pixel dead zone (upward drag)
        const val HYPER_SWIPE_PX  = 80f     // right-half upward swipe threshold
        const val OUTER_RADIUS    = 72f     // visual ring radius (px)
        const val INNER_RADIUS    = 22f     // visual thumb dot radius (px)
    }

    // ── Left-half joystick state ────────────────────────────────────────────

    private var joyPointer = -1
    var joyAnchorX  = 0f; private set   // screen-pixel anchor (origin at top-left)
    var joyAnchorY  = 0f; private set
    var joyCurrentX = 0f; private set   // current thumb screen position
    var joyCurrentY = 0f; private set
    val joyActive   get() = joyPointer >= 0

    // ── Right-half fire/hyperspace state ────────────────────────────────────

    private var firePointer     = -1
    private var fireAnchorY     = 0f    // screen-pixel y where fire touch began
    private var hyperspaceArmed = false // one-shot flag per right-zone touch-down

    // ── Public API ──────────────────────────────────────────────────────────

    fun poll(input: GameInput) {
        val sw = Gdx.graphics.width.toFloat()

        // Release tracking: clear pointers whose fingers lifted
        if (joyPointer  >= 0 && !Gdx.input.isTouched(joyPointer))  releaseJoy()
        if (firePointer >= 0 && !Gdx.input.isTouched(firePointer)) releaseFire()

        for (i in 0..4) {
            if (!Gdx.input.isTouched(i)) continue
            val nx = Gdx.input.getX(i).toFloat() / sw

            if (nx < LEFT_ZONE_END) {
                // ── Left zone — joystick ────────────────────────────────────
                if (joyPointer < 0) {
                    joyPointer  = i
                    joyAnchorX  = Gdx.input.getX(i).toFloat()
                    joyAnchorY  = Gdx.input.getY(i).toFloat()
                    joyCurrentX = joyAnchorX
                    joyCurrentY = joyAnchorY
                }
                if (i == joyPointer) {
                    joyCurrentX = Gdx.input.getX(i).toFloat()
                    joyCurrentY = Gdx.input.getY(i).toFloat()
                    val dx = joyCurrentX - joyAnchorX
                    val dy = joyCurrentY - joyAnchorY  // positive = downward in screen space
                    if (dx < -ROTATE_DEAD_PX) input.rotateLeft  = true
                    if (dx >  ROTATE_DEAD_PX) input.rotateRight = true
                    if (dy < -THRUST_DEAD_PX) input.thrust      = true  // drag up = thrust
                }
            } else {
                // ── Right zone — fire + swipe-up hyperspace ─────────────────
                if (firePointer < 0) {
                    firePointer     = i
                    fireAnchorY     = Gdx.input.getY(i).toFloat()
                    hyperspaceArmed = false
                }
                if (i == firePointer) {
                    input.fire = true
                    val dy = Gdx.input.getY(i) - fireAnchorY   // negative = swipe upward
                    if (dy < -HYPER_SWIPE_PX && !hyperspaceArmed) {
                        input.hyperspace = true
                        hyperspaceArmed  = true
                    }
                }
            }
        }
    }

    /**
     * Draw the joystick anchor ring and thumb dot in screen-pixel space.
     * Must be called outside any active SpriteBatch or ShapeRenderer begin/end pair.
     */
    fun renderOverlay(sr: ShapeRenderer) {
        if (!joyActive) return

        val sw = Gdx.graphics.width.toFloat()
        val sh = Gdx.graphics.height.toFloat()
        // libGDX y=0 is BOTTOM; input y=0 is TOP — flip for rendering
        val anchorRY  = sh - joyAnchorY
        val thumbRY   = sh - joyCurrentY

        val savedMatrix = sr.projectionMatrix.cpy()
        sr.projectionMatrix = Matrix4().setToOrtho2D(0f, 0f, sw, sh)

        sr.begin(ShapeRenderer.ShapeType.Line)

        // Outer ring — faint, marks the anchor position
        sr.color = Color(1f, 1f, 1f, 0.25f)
        sr.circle(joyAnchorX, anchorRY, OUTER_RADIUS, 28)

        // Thumb dot outline — brighter, tracks the finger
        sr.color = Color(1f, 1f, 1f, 0.60f)
        sr.circle(joyCurrentX, thumbRY, INNER_RADIUS, 16)

        // Line connecting anchor to thumb
        sr.color = Color(1f, 1f, 1f, 0.40f)
        sr.line(joyAnchorX, anchorRY, joyCurrentX, thumbRY)

        sr.end()

        sr.projectionMatrix = savedMatrix
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private fun releaseJoy() {
        joyPointer  = -1
        joyCurrentX = joyAnchorX
        joyCurrentY = joyAnchorY
    }

    private fun releaseFire() {
        firePointer     = -1
        hyperspaceArmed = false
    }
}
