package com.palacesoft.starshard.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.palacesoft.starshard.AsteroidsGame
import com.palacesoft.starshard.render.Starfield
import com.palacesoft.starshard.util.Settings

class GameOverScreen(
    private val game: AsteroidsGame,
    private val finalScore: Int,
    private val previousBest: Int = 0
) : Screen {

    private val isNewBest = finalScore > previousBest
    private val bestScore = Settings.highScore

    private val starfield = Starfield()
    private val titleFont = BitmapFont().apply { data.setScale(3.5f); color = Color.RED }
    private val scoreFont = BitmapFont().apply { data.setScale(2f);   color = Color.WHITE }
    private val subFont   = BitmapFont().apply { data.setScale(1.5f); color = Color.GRAY }
    private val lbFont    = BitmapFont().apply { data.setScale(1.5f); color = Color.CYAN }
    private val bestFont  = BitmapFont().apply {
        data.setScale(1.8f)
        color = if (isNewBest) Color.GOLD else Color.LIGHT_GRAY
    }
    private val layout = GlyphLayout()

    private val LB_ZONE_THRESHOLD = 0.33f

    private fun drawCentered(f: BitmapFont, text: String, y: Float) {
        layout.setText(f, text)
        f.draw(game.batch, text, (Settings.WORLD_WIDTH - layout.width) / 2f, y)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0.03f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        game.sr.projectionMatrix = game.camera.combined
        starfield.update(delta, 0f, 0f)
        starfield.render(game.sr)
        game.batch.projectionMatrix = game.camera.combined
        game.batch.begin()

        drawCentered(titleFont, "GAME OVER", Settings.WORLD_HEIGHT / 2f + 80f)
        drawCentered(scoreFont, "SCORE  $finalScore", Settings.WORLD_HEIGHT / 2f)

        val bestText = if (isNewBest) "NEW BEST!  $bestScore" else "BEST  $bestScore"
        drawCentered(bestFont, bestText, Settings.WORLD_HEIGHT / 2f - 60f)

        drawCentered(subFont, "PRESS SPACE OR TAP TO RETRY", Settings.WORLD_HEIGHT / 2f - 120f)
        if (game.gameServices != null) {
            drawCentered(lbFont, "LEADERBOARD", Settings.WORLD_HEIGHT / 2f - 180f)
        }
        game.batch.end()

        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE)) {
            game.setScreen(GameScreen(game))
            return
        }
        if (Gdx.input.justTouched()) {
            val normY = Gdx.input.y.toFloat() / Gdx.graphics.height.toFloat()
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
        titleFont.dispose(); scoreFont.dispose(); subFont.dispose()
        lbFont.dispose(); bestFont.dispose()
    }
}
