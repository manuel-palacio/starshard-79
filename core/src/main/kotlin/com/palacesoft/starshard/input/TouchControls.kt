package com.palacesoft.starshard.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Disposable
import com.palacesoft.starshard.util.Settings

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
class TouchControls : Disposable {

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

    // Button label font — owned here so disposal is co-located with usage
    private val btnFont = BitmapFont().apply { data.setScale(1.4f) }

    // Pre-computed layouts — labels never change, so allocate once at construction
    private val labelLayouts = listOf("◁", "▲", "▷", "FIRE", "★")
        .map { GlyphLayout(btnFont, it) }

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

    /**
     * Draws button dividers and labels in HUD world coordinates.
     * Uses [batch] for label text and [sr] for divider lines.
     * Must be called while the HUD FitViewport camera is active.
     * Does nothing on non-touch devices.
     */
    fun renderOverlay(sr: ShapeRenderer, batch: SpriteBatch) {
        if (!Gdx.input.isPeripheralAvailable(Input.Peripheral.MultitouchScreen)) return

        val W  = Settings.WORLD_WIDTH
        val H  = Settings.WORLD_HEIGHT
        val bH = H * STRIP_HEIGHT

        val x0 = 0f
        val x1 = W * COL_ROT_L_RIGHT
        val x2 = W * COL_THRUST_RIGHT
        val x3 = W * COL_LEFT_END
        val x4 = W * COL_FIRE_RIGHT
        val x5 = W

        val strokeColor = Color(0.9f, 0.9f, 0.9f, 0.20f)
        val labelColor  = Color(0.9f, 0.9f, 0.9f, 0.35f)

        sr.begin(ShapeRenderer.ShapeType.Line)
        sr.color = strokeColor
        sr.line(x1, 0f, x1, bH)
        sr.line(x2, 0f, x2, bH)
        sr.line(x3, 0f, x3, bH)
        sr.line(x4, 0f, x4, bH)
        sr.line(x0, bH, x5, bH)
        sr.end()

        batch.begin()
        btnFont.color = labelColor
        val centers = listOf((x0+x1)/2f, (x1+x2)/2f, (x2+x3)/2f, (x3+x4)/2f, (x4+x5)/2f)
        val labels  = listOf("◁", "▲", "▷", "FIRE", "★")
        for (i in labels.indices) {
            val gl = labelLayouts[i]
            btnFont.draw(batch, labels[i], centers[i] - gl.width / 2f, bH / 2f + gl.height / 2f)
        }
        batch.end()
    }

    override fun dispose() { btnFont.dispose() }
}
