package com.palacesoft.asteroids.input

import com.badlogic.gdx.Gdx

/**
 * Scheme A — dual-zone discrete button layout.
 *
 * The screen is divided into a control strip at the bottom [STRIP_HEIGHT] fraction.
 * Above the strip is untouched play area.
 *
 * Left zone (x < 0.50 of screen width):
 *   [◁ ROTATE LEFT] [▲ THRUST] [▷ ROTATE RIGHT]
 *   Three equal horizontal thirds. Holding any fires continuously.
 *
 * Right zone (x >= 0.50 of screen width):
 *   [FIRE]  [HYPERSPACE]
 *   Fire is continuous (hold to fire at rate). Hyperspace is one-shot per touch.
 *
 * Button zone fractions are in normalised screen-width units (0..1).
 */
class TouchControls {

    companion object {
        /** Bottom fraction of screen used as the control strip. */
        const val STRIP_HEIGHT = 0.22f

        // Left zone column boundaries (normalised x, 0..1)
        const val COL_ROT_L_RIGHT  = 0.17f  // rotate-left  : 0.00 → 0.17
        const val COL_THRUST_RIGHT = 0.33f  // thrust       : 0.17 → 0.33
        const val COL_LEFT_END     = 0.50f  // rotate-right : 0.33 → 0.50

        // Right zone column boundaries (normalised x)
        const val COL_FIRE_RIGHT   = 0.75f  // fire         : 0.50 → 0.75
                                            // hyperspace   : 0.75 → 1.00
    }

    // Track which pointer indices are currently held in the hyperspace zone.
    // Hyperspace fires exactly once per new touch-down there.
    private val hyperspaceActive = BooleanArray(5)

    fun poll(input: GameInput) {
        val sw = Gdx.graphics.width.toFloat()
        val sh = Gdx.graphics.height.toFloat()
        // stripTopY: screen y-pixels above which the strip starts (y=0 is screen top)
        val stripTopY = sh * (1f - STRIP_HEIGHT)

        for (i in 0..4) {
            if (!Gdx.input.isTouched(i)) {
                hyperspaceActive[i] = false
                continue
            }

            val sy = Gdx.input.getY(i).toFloat()
            if (sy < stripTopY) {
                // Touch is above the control strip — no button action
                hyperspaceActive[i] = false
                continue
            }

            val sx = Gdx.input.getX(i).toFloat() / sw  // normalised 0..1

            when {
                sx < COL_ROT_L_RIGHT  -> {
                    input.rotateLeft = true
                    hyperspaceActive[i] = false
                }
                sx < COL_THRUST_RIGHT -> {
                    input.thrust = true
                    hyperspaceActive[i] = false
                }
                sx < COL_LEFT_END     -> {
                    input.rotateRight = true
                    hyperspaceActive[i] = false
                }
                sx < COL_FIRE_RIGHT   -> {
                    input.fire = true
                    hyperspaceActive[i] = false
                }
                else -> {
                    // Hyperspace: one-shot per new touch-down
                    if (!hyperspaceActive[i]) {
                        input.hyperspace = true
                        hyperspaceActive[i] = true
                    }
                }
            }
        }
    }
}
