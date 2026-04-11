package com.palacesoft.starshard.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.palacesoft.starshard.AsteroidsGame
import com.palacesoft.starshard.render.Starfield
import com.palacesoft.starshard.util.Settings

class MenuScreen(private val game: AsteroidsGame) : Screen {
    private val starfield = Starfield()
    private val font    = BitmapFont().apply { data.setScale(3f); color = Color.CYAN }
    private val subFont = BitmapFont().apply { data.setScale(1.5f); color = Color.WHITE }
    private val settingsFont = BitmapFont().apply { data.setScale(1.5f); color = Color.GRAY }
    private val layout  = GlyphLayout()
    private var time = 0f

    private fun drawCentered(f: BitmapFont, text: String, y: Float) {
        layout.setText(f, text)
        f.draw(game.batch, text, (Settings.WORLD_WIDTH - layout.width) / 2f, y)
    }

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
        drawCentered(font, "STARSHARD 79", Settings.WORLD_HEIGHT / 2f + 60f)
        drawCentered(subFont, "PRESS SPACE OR TAP TO START", Settings.WORLD_HEIGHT / 2f - 20f)
        drawCentered(subFont, "LEFT / RIGHT  -  ROTATE     UP / DRAG  -  THRUST", Settings.WORLD_HEIGHT / 2f - 80f)
        drawCentered(subFont, "SPACE  -  FIRE     Z / SHIFT  -  HYPERSPACE (random warp)", Settings.WORLD_HEIGHT / 2f - 130f)
        drawCentered(settingsFont, "S  -  SETTINGS", Settings.WORLD_HEIGHT / 2f - 200f)
        game.batch.end()

        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            game.setScreen(SettingsScreen(game))
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.setScreen(GameScreen(game))
        } else if (Gdx.input.justTouched()) {
            val normY = Gdx.input.y.toFloat() / Gdx.graphics.height
            if (normY > 0.75f) {
                game.setScreen(SettingsScreen(game))
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
    override fun dispose() { font.dispose(); subFont.dispose(); settingsFont.dispose() }
}
