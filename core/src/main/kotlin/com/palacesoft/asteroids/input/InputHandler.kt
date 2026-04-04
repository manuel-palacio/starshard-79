package com.palacesoft.asteroids.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys

class InputHandler(private val gameInput: GameInput) {
    private val touch = TouchControls(gameInput)

    fun poll() {
        gameInput.rotateLeft  = Gdx.input.isKeyPressed(Keys.LEFT)  || Gdx.input.isKeyPressed(Keys.A)
        gameInput.rotateRight = Gdx.input.isKeyPressed(Keys.RIGHT) || Gdx.input.isKeyPressed(Keys.D)
        gameInput.thrust      = Gdx.input.isKeyPressed(Keys.UP)    || Gdx.input.isKeyPressed(Keys.W)
        gameInput.fire        = Gdx.input.isKeyPressed(Keys.SPACE)
        touch.poll(gameInput)
    }
}
