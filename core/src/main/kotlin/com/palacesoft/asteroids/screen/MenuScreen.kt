package com.palacesoft.asteroids.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.palacesoft.asteroids.AsteroidsGame
import com.palacesoft.asteroids.render.Starfield
import com.palacesoft.asteroids.util.Settings

class MenuScreen(private val game: AsteroidsGame) : Screen {
    private val starfield = Starfield()
    private val font    = BitmapFont().apply { data.setScale(3f); color = com.badlogic.gdx.graphics.Color.CYAN }
    private val subFont = BitmapFont().apply { data.setScale(1.5f); color = com.badlogic.gdx.graphics.Color.WHITE }
    private var time = 0f

    override fun render(delta: Float) {
        time += delta
        Gdx.gl.glClearColor(0f, 0f, 0.03f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        game.sr.projectionMatrix = game.camera.combined
        starfield.update(delta, 0f, 0f)
        starfield.render(game.sr)
        val scale = 3f + 0.15f * kotlin.math.sin(time * 2f).toFloat()
        font.data.setScale(scale)
        game.batch.projectionMatrix = game.camera.combined
        game.batch.begin()
        font.draw(game.batch, "ASTEROIDS",
                  Settings.WORLD_WIDTH / 2f - 180f, Settings.WORLD_HEIGHT / 2f + 60f)
        subFont.draw(game.batch, "PRESS SPACE OR TAP TO START",
                     Settings.WORLD_WIDTH / 2f - 200f, Settings.WORLD_HEIGHT / 2f - 20f)
        subFont.draw(game.batch, "LEFT / RIGHT  -  ROTATE     UP / DRAG  -  THRUST",
                     Settings.WORLD_WIDTH / 2f - 260f, Settings.WORLD_HEIGHT / 2f - 80f)
        subFont.draw(game.batch, "SPACE  -  FIRE     Z / SHIFT  -  HYPERSPACE (random warp)",
                     Settings.WORLD_WIDTH / 2f - 260f, Settings.WORLD_HEIGHT / 2f - 130f)
        game.batch.end()
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE) || Gdx.input.justTouched())
            game.setScreen(GameScreen(game))
    }

    override fun resize(w: Int, h: Int) {}
    override fun show()   {}
    override fun hide()   {}
    override fun pause()  {}
    override fun resume() {}
    override fun dispose() { font.dispose(); subFont.dispose() }
}
