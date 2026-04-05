package com.palacesoft.asteroids.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.palacesoft.asteroids.AsteroidsGame
import com.palacesoft.asteroids.render.Starfield
import com.palacesoft.asteroids.util.Settings

class GameOverScreen(private val game: AsteroidsGame, private val finalScore: Int) : Screen {
    private val starfield  = Starfield()
    private val titleFont  = BitmapFont().apply { data.setScale(3.5f); color = com.badlogic.gdx.graphics.Color.RED }
    private val scoreFont  = BitmapFont().apply { data.setScale(2f);   color = com.badlogic.gdx.graphics.Color.WHITE }
    private val subFont    = BitmapFont().apply { data.setScale(1.5f); color = com.badlogic.gdx.graphics.Color.GRAY }
    private val lbFont     = BitmapFont().apply { data.setScale(1.5f); color = com.badlogic.gdx.graphics.Color.CYAN }

    // Normalised screen-y threshold: touch below this fraction triggers leaderboard
    private val LB_ZONE_THRESHOLD = 0.33f

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0.03f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        game.sr.projectionMatrix = game.camera.combined
        starfield.update(delta, 0f, 0f)
        starfield.render(game.sr)
        game.batch.projectionMatrix = game.camera.combined
        game.batch.begin()
        titleFont.draw(game.batch, "GAME OVER",
                       Settings.WORLD_WIDTH / 2f - 200f, Settings.WORLD_HEIGHT / 2f + 80f)
        scoreFont.draw(game.batch, "SCORE  $finalScore",
                       Settings.WORLD_WIDTH / 2f - 100f, Settings.WORLD_HEIGHT / 2f)
        subFont.draw(game.batch, "PRESS SPACE OR TAP TO RETRY",
                     Settings.WORLD_WIDTH / 2f - 200f, Settings.WORLD_HEIGHT / 2f - 80f)
        if (game.gameServices != null) {
            lbFont.draw(game.batch, "LEADERBOARD",
                        Settings.WORLD_WIDTH / 2f - 100f, Settings.WORLD_HEIGHT / 2f - 140f)
        }
        game.batch.end()

        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE)) {
            game.setScreen(GameScreen(game))
            return
        }

        if (Gdx.input.justTouched()) {
            val normY = Gdx.input.y.toFloat() / Gdx.graphics.height.toFloat()
            // normY = 0 at top of screen, 1 at bottom
            if (game.gameServices != null && normY > (1f - LB_ZONE_THRESHOLD)) {
                game.gameServices?.showLeaderboard()
            } else {
                game.setScreen(GameScreen(game))
            }
        }
    }

    override fun resize(w: Int, h: Int) {}
    override fun show()   {}
    override fun hide()   {}
    override fun pause()  {}
    override fun resume() {}
    override fun dispose() {
        titleFont.dispose(); scoreFont.dispose(); subFont.dispose(); lbFont.dispose()
    }
}
