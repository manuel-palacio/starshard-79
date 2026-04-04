package com.palacesoft.asteroids.input

import com.badlogic.gdx.Gdx
import com.palacesoft.asteroids.util.Settings

class TouchControls(private val gameInput: GameInput) {
    private val midX get() = Settings.WORLD_WIDTH / 2f
    private val midY get() = Settings.WORLD_HEIGHT / 2f

    var joystickAnchorX = 0f
    var joystickAnchorY = 0f
    var joystickActive  = false
    var joystickCurrX   = 0f
    var joystickCurrY   = 0f
    private var hyperspacePointer = -1

    fun poll(input: GameInput) {
        if (!Gdx.input.isTouched()) {
            joystickActive = false
            hyperspacePointer = -1
            return
        }

        val scaleX = Settings.WORLD_WIDTH  / Gdx.graphics.width.toFloat()
        val scaleY = Settings.WORLD_HEIGHT / Gdx.graphics.height.toFloat()

        for (i in 0..4) {
            if (!Gdx.input.isTouched(i)) continue
            val tx = Gdx.input.getX(i) * scaleX
            val ty = (Gdx.graphics.height - Gdx.input.getY(i)) * scaleY

            if (tx < midX) {
                if (!joystickActive) {
                    joystickAnchorX = tx; joystickAnchorY = ty
                    joystickActive = true
                }
                joystickCurrX = tx; joystickCurrY = ty
                val dx = joystickCurrX - joystickAnchorX
                val dy = joystickCurrY - joystickAnchorY
                val deadzone = 20f
                if (dx < -deadzone) input.rotateLeft  = true
                if (dx >  deadzone) input.rotateRight = true
                if (dy >  deadzone) input.thrust      = true
            } else {
                if (ty < midY) {
                    input.fire = true
                } else {
                    if (hyperspacePointer == -1) {
                        hyperspacePointer = i
                        input.hyperspace = true
                    }
                }
            }
        }
    }
}
