package com.palacesoft.asteroids.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.asteroids.util.Settings

class InputHandler(private val gameInput: GameInput) {

    private val touchButtons  = TouchControls()
    private val touchJoystick = JoystickControls()

    fun poll() {
        gameInput.rotateLeft  = Gdx.input.isKeyPressed(Keys.LEFT)  || Gdx.input.isKeyPressed(Keys.A)
        gameInput.rotateRight = Gdx.input.isKeyPressed(Keys.RIGHT) || Gdx.input.isKeyPressed(Keys.D)
        gameInput.thrust      = Gdx.input.isKeyPressed(Keys.UP)    || Gdx.input.isKeyPressed(Keys.W)
        gameInput.fire        = Gdx.input.isKeyPressed(Keys.SPACE)
        gameInput.hyperspace  = Gdx.input.isKeyJustPressed(Keys.Z) || Gdx.input.isKeyJustPressed(Keys.SHIFT_LEFT)

        when (Settings.touchScheme) {
            TouchScheme.BUTTONS  -> touchButtons.poll(gameInput)
            TouchScheme.JOYSTICK -> touchJoystick.poll(gameInput)
        }
    }

    /**
     * Renders the active scheme's touch overlay.
     * Must be called with the HUD FitViewport camera active (for BUTTONS)
     * or independently (for JOYSTICK which uses screen-pixel coordinates).
     */
    fun renderTouchOverlay(sr: ShapeRenderer, batch: SpriteBatch) {
        when (Settings.touchScheme) {
            TouchScheme.BUTTONS  -> touchButtons.renderOverlay(sr, batch)
            TouchScheme.JOYSTICK -> touchJoystick.renderOverlay(sr)
        }
    }

    fun dispose() { touchButtons.dispose() }
}
