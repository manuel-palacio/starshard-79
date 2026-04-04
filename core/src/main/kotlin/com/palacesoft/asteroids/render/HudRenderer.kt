package com.palacesoft.asteroids.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
import com.palacesoft.asteroids.game.World
import com.palacesoft.asteroids.util.Settings

class HudRenderer(batch: SpriteBatch, camera: OrthographicCamera) {
    private val viewport = FitViewport(Settings.WORLD_WIDTH, Settings.WORLD_HEIGHT, camera)
    val stage = Stage(viewport, batch)

    private val font      = BitmapFont().apply { data.setScale(2f); color = Color.CYAN }
    private val waveFont  = BitmapFont().apply { data.setScale(1.8f) }
    private val popupFont = BitmapFont().apply { data.setScale(1.5f) }

    private val scoreStyle  = Label.LabelStyle(font, Color.CYAN)
    private val waveStyle   = Label.LabelStyle(waveFont, Color.WHITE)
    private val popupStyle  = Label.LabelStyle(popupFont, Color.YELLOW)

    private val scoreLabel = Label("0", scoreStyle).apply {
        setPosition(20f, Settings.WORLD_HEIGHT - 40f)
    }
    private val livesLabel = Label("", scoreStyle).apply {
        setPosition(Settings.WORLD_WIDTH - 200f, Settings.WORLD_HEIGHT - 40f)
    }
    private val waveLabel = Label("", waveStyle).apply {
        setWidth(Settings.WORLD_WIDTH)
        setPosition(0f, Settings.WORLD_HEIGHT / 2f + 60f)
        setAlignment(Align.center)
        color.a = 0f
    }

    private var lastScore = -1
    private var lastWave  = -1
    private var lastLives = -1

    // ── Tutorial hints ────────────────────────────────────────────────────────
    private val tutorialHintStyle = Label.LabelStyle(BitmapFont().apply { data.setScale(1.6f) }, Color.WHITE)
    private val hintLabel = Label("", tutorialHintStyle).apply {
        setWidth(Settings.WORLD_WIDTH)
        setPosition(0f, Settings.WORLD_HEIGHT * 0.18f)
        setAlignment(Align.center)
        color.a = 0f
    }
    private var hintShown = false

    init {
        stage.addActor(scoreLabel)
        stage.addActor(livesLabel)
        stage.addActor(waveLabel)
        stage.addActor(hintLabel)
    }

    fun render(world: World) {
        // Score
        if (world.score != lastScore) {
            val delta = world.score - lastScore
            lastScore = world.score
            scoreLabel.setText(world.score.toString())
            if (delta > 0) spawnScorePopup(delta)
        }
        // Lives
        if (world.lives != lastLives) {
            lastLives = world.lives
            livesLabel.setText("△ ".repeat(world.lives.coerceAtLeast(0)))
        }
        // Wave banner
        if (world.wave != lastWave && world.wave > 0) {
            lastWave = world.wave
            waveLabel.setText("WAVE ${world.wave}")
            waveLabel.clearActions()
            waveLabel.color.a = 0f
            waveLabel.addAction(Actions.sequence(
                Actions.fadeIn(0.4f),
                Actions.delay(2f),
                Actions.fadeOut(0.6f)
            ))
        }

        // Tutorial hint: controls reminder on first wave only
        if (!Settings.tutorialCompleted && world.wave == 1 && !hintShown) {
            hintShown = true
            hintLabel.setText("◁ ROTATE  ▲ THRUST  ▷ ROTATE     FIRE     ★ HYPERSPACE")
            hintLabel.clearActions()
            hintLabel.color.a = 0f
            hintLabel.addAction(Actions.sequence(
                Actions.delay(1.0f),
                Actions.fadeIn(0.5f),
                Actions.delay(4.0f),
                Actions.fadeOut(0.8f)
            ))
        }

        stage.act()
        stage.draw()
    }

    private fun spawnScorePopup(delta: Int) {
        val popup = Label("+$delta", popupStyle)
        popup.setPosition(scoreLabel.x + 120f, scoreLabel.y)
        popup.addAction(Actions.sequence(
            Actions.parallel(
                Actions.moveBy(0f, 60f, 1f),
                Actions.fadeOut(1f)
            ),
            Actions.removeActor()
        ))
        stage.addActor(popup)
    }

    fun resize(width: Int, height: Int) = viewport.update(width, height, true)

    fun dispose() {
        stage.dispose()
        font.dispose()
        waveFont.dispose()
        popupFont.dispose()
        tutorialHintStyle.font.dispose()
    }
}
