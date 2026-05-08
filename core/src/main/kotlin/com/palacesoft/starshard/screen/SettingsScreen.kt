package com.palacesoft.starshard.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.palacesoft.starshard.AsteroidsGame
import com.palacesoft.starshard.effects.EffectQuality
import com.palacesoft.starshard.input.TouchScheme
import com.palacesoft.starshard.render.Starfield
import com.palacesoft.starshard.util.Settings

class SettingsScreen(private val game: AsteroidsGame) : Screen {

    private val starfield = Starfield()
    private val titleFont = BitmapFont().apply { data.setScale(2.5f); color = Color.CYAN }
    private val labelFont = BitmapFont().apply { data.setScale(1.5f); color = Color.WHITE }
    private val valueFont = BitmapFont().apply { data.setScale(1.5f); color = Color.YELLOW }
    private val hintFont  = BitmapFont().apply { data.setScale(1.2f); color = Color.GRAY }
    private val layout = GlyphLayout()

    private var selected = 0
    private val itemCount = 4  // SFX, Haptics, Touch Scheme, FX Quality

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0.03f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        game.sr.projectionMatrix = game.camera.combined
        starfield.update(delta, 0f, 0f)
        starfield.render(game.sr)

        handleInput()

        val topY = Settings.WORLD_HEIGHT / 2f + 160f

        game.batch.projectionMatrix = game.camera.combined
        game.batch.begin()

        drawCentered(titleFont, "SETTINGS", topY)

        val startY = topY - 90f
        val spacing = 60f

        // SFX
        drawRow(0, "SOUND", if (Settings.sfxEnabled) "ON" else "OFF", startY)

        // Haptics
        drawRow(1, "HAPTICS", if (Settings.hapticEnabled) "ON" else "OFF", startY - spacing)

        // Touch Scheme
        drawRow(2, "TOUCH CONTROLS", Settings.touchScheme.name, startY - spacing * 2)

        // FX Quality
        drawRow(3, "FX QUALITY", Settings.fxQuality.name, startY - spacing * 3)

        drawCentered(hintFont, "UP/DOWN TO SELECT    LEFT/RIGHT TO CHANGE    ESC/BACK TO RETURN",
                     startY - spacing * 4 - 40f)

        game.batch.end()
    }

    private fun drawRow(index: Int, label: String, value: String, y: Float) {
        val color = if (index == selected) Color.CYAN else Color.WHITE
        labelFont.color = color
        val arrowColor = if (index == selected) Color.YELLOW else Color.DARK_GRAY
        valueFont.color = arrowColor

        layout.setText(labelFont, label)
        labelFont.draw(game.batch, label,
            Settings.WORLD_WIDTH / 2f - 250f, y)

        val displayValue = if (index == selected) "< $value >" else value
        layout.setText(valueFont, displayValue)
        valueFont.draw(game.batch, displayValue,
            Settings.WORLD_WIDTH / 2f + 100f, y)
    }

    private fun handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selected = (selected - 1 + itemCount) % itemCount
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selected = (selected + 1) % itemCount
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            when (selected) {
                0 -> {
                    Settings.sfxEnabled = !Settings.sfxEnabled
                    Settings.saveSfxEnabled()
                }
                1 -> {
                    Settings.hapticEnabled = !Settings.hapticEnabled
                    Settings.saveHapticEnabled()
                }
                2 -> {
                    Settings.touchScheme = if (Settings.touchScheme == TouchScheme.BUTTONS)
                        TouchScheme.JOYSTICK else TouchScheme.BUTTONS
                    Settings.saveTouchScheme()
                }
                3 -> {
                    val qualities = EffectQuality.entries
                    val dir = if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) 1 else -1
                    val idx = (qualities.indexOf(Settings.fxQuality) + dir + qualities.size) % qualities.size
                    Settings.fxQuality = qualities[idx]
                    Settings.saveFxQuality()
                }
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            game.setScreen(MenuScreen(game))
        }

        // Touch: tap left/right half to change, tap top area to go back
        if (Gdx.input.justTouched()) {
            val normX = Gdx.input.x.toFloat() / Gdx.graphics.width
            val normY = Gdx.input.y.toFloat() / Gdx.graphics.height
            if (normY < 0.15f) {
                game.setScreen(MenuScreen(game))
            } else {
                // Cycle through the selected setting
                if (normX < 0.5f) selected = (selected - 1 + itemCount) % itemCount
                else selected = (selected + 1) % itemCount
            }
        }
    }

    private fun drawCentered(f: BitmapFont, text: String, y: Float) {
        layout.setText(f, text)
        f.draw(game.batch, text, (Settings.WORLD_WIDTH - layout.width) / 2f, y)
    }

    override fun resize(w: Int, h: Int) {}
    override fun show() {}
    override fun hide() {}
    override fun pause() {}
    override fun resume() {}
    override fun dispose() {
        titleFont.dispose(); labelFont.dispose()
        valueFont.dispose(); hintFont.dispose()
    }
}
