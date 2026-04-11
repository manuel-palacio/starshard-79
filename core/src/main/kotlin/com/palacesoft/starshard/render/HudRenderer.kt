package com.palacesoft.starshard.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
import com.palacesoft.starshard.game.World
import com.palacesoft.starshard.game.entity.PowerUpType
import com.palacesoft.starshard.util.Settings

class HudRenderer(batch: SpriteBatch, camera: OrthographicCamera) {
    private val viewport = FitViewport(Settings.WORLD_WIDTH, Settings.WORLD_HEIGHT, camera)
    val stage = Stage(viewport, batch)

    private val font          = BitmapFont().apply { data.setScale(2f);   color = Color.CYAN }
    private val waveFont      = BitmapFont().apply { data.setScale(1.8f) }
    private val popupFont     = BitmapFont().apply { data.setScale(1.5f) }
    private val bestHudFont   = BitmapFont().apply { data.setScale(1.6f); color = Color(0.7f, 0.7f, 0.7f, 1f) }
    private val waveCountFont = BitmapFont().apply { data.setScale(1.4f); color = Color.WHITE }
    private val multFont      = BitmapFont().apply { data.setScale(2.2f); color = Color.YELLOW }
    private val powerUpFont   = BitmapFont().apply { data.setScale(1.6f) }

    private val scoreStyle     = Label.LabelStyle(font, Color.CYAN)
    private val waveStyle      = Label.LabelStyle(waveFont, Color.WHITE)
    private val popupStyle     = Label.LabelStyle(popupFont, Color.YELLOW)
    private val bestHudStyle   = Label.LabelStyle(bestHudFont, Color(0.7f, 0.7f, 0.7f, 1f))
    private val waveCountStyle = Label.LabelStyle(waveCountFont, Color.WHITE)
    private val multStyle      = Label.LabelStyle(multFont, Color.YELLOW)

    private val scoreLabel = Label("0", scoreStyle).apply {
        setPosition(20f, Settings.WORLD_HEIGHT - 40f)
    }
    private val waveLabel = Label("", waveStyle).apply {
        setWidth(Settings.WORLD_WIDTH)
        setPosition(0f, Settings.WORLD_HEIGHT / 2f + 60f)
        setAlignment(Align.center)
        color.a = 0f
    }
    private val bestLabel = Label("", bestHudStyle).apply {
        setWidth(Settings.WORLD_WIDTH)
        setPosition(0f, Settings.WORLD_HEIGHT - 40f)
        setAlignment(Align.center)
    }
    private val waveCountLabel = Label("", waveCountStyle).apply {
        setWidth(Settings.WORLD_WIDTH)
        setPosition(0f, Settings.WORLD_HEIGHT - 78f)
        setAlignment(Align.center)
        color.a = 0.7f
    }
    private val multiplierLabel = Label("", multStyle).apply {
        setPosition(160f, Settings.WORLD_HEIGHT - 40f)
        color.a = 0f
    }

    private val tutorialHintStyle = Label.LabelStyle(BitmapFont().apply { data.setScale(1.6f) }, Color.WHITE)
    private val hintLabel = Label("", tutorialHintStyle).apply {
        setWidth(Settings.WORLD_WIDTH)
        setPosition(0f, Settings.WORLD_HEIGHT * 0.18f)
        setAlignment(Align.center)
        color.a = 0f
    }
    private var hintShown = false

    private var lastScore         = -1
    private var lastWave          = -1
    private var lastHighScore     = -1
    private var lastWaveForCount  = -1
    private var lastAsteroidCount = -1
    private var lastMultiplier    = -1

    init {
        stage.addActor(scoreLabel)
        stage.addActor(waveLabel)
        stage.addActor(bestLabel)
        stage.addActor(waveCountLabel)
        stage.addActor(multiplierLabel)
        stage.addActor(hintLabel)
    }

    fun render(world: World) {
        if (world.score != lastScore) {
            val delta = world.score - lastScore
            lastScore = world.score
            scoreLabel.setText(world.score.toString())
            if (delta > 0) spawnScorePopup(delta)
        }
        val best = Settings.highScore
        if (best != lastHighScore) {
            lastHighScore = best
            if (best > 0) bestLabel.setText("BEST  $best")
        }
        val liveCount = world.asteroids.count { it.alive }
        if (world.wave != lastWaveForCount || liveCount != lastAsteroidCount) {
            lastWaveForCount  = world.wave
            lastAsteroidCount = liveCount
            if (world.wave > 0) waveCountLabel.setText("WAVE ${world.wave}  ·  $liveCount LEFT")
        }
        val mult = world.streakSystem.multiplier
        if (mult != lastMultiplier) {
            lastMultiplier = mult
            multiplierLabel.clearActions()
            if (mult > 1) {
                multiplierLabel.setText("${mult}×")
                multiplierLabel.color.a = 1f
            } else {
                multiplierLabel.addAction(Actions.fadeOut(0.5f))
            }
        }
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
        // Power-up indicator (top-right area)
        renderPowerUpIndicator(world)

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

    private fun renderPowerUpIndicator(world: World) {
        val pus = world.powerUpSystem
        val active = pus.activeType
        val shield = pus.shieldActive

        if (active == null && !shield) return

        val batch = stage.batch
        batch.begin()

        val x = Settings.WORLD_WIDTH - 220f
        var y = Settings.WORLD_HEIGHT - 45f

        if (active != null) {
            val pulse = if (pus.activeTimer <= 3f) ((pus.activeTimer * 6f).toInt() % 2 == 0) else true
            if (pulse) {
                powerUpFont.color = Color(active.r, active.g, active.b, 1f)
                powerUpFont.draw(batch, active.label, x, y)
            }
            y -= 20f
            // Timer bar
            val barWidth = 180f * (pus.activeTimer / pus.activeDuration).coerceIn(0f, 1f)
            // Draw bar using font as a simple rectangle approximation
            powerUpFont.color = Color(active.r, active.g, active.b, 0.6f)
            powerUpFont.draw(batch, "|".repeat((barWidth / 6f).toInt().coerceAtLeast(0)), x, y)
        }

        if (shield) {
            y -= if (active != null) 30f else 0f
            powerUpFont.color = Color(0.3f, 1f, 0.4f, 1f)
            powerUpFont.draw(batch, "SHIELD", x, y)
        }

        batch.end()
    }

    /**
     * Renders the power-up countdown bar using ShapeRenderer.
     * Call after stage.draw() with HUD camera active.
     */
    fun renderPowerUpBar(sr: ShapeRenderer, world: World) {
        val pus = world.powerUpSystem
        val active = pus.activeType ?: return
        val fraction = (pus.activeTimer / pus.activeDuration).coerceIn(0f, 1f)
        val x = Settings.WORLD_WIDTH - 220f
        val y = Settings.WORLD_HEIGHT - 68f
        val barWidth = 180f

        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = Color(active.r, active.g, active.b, 0.5f)
        sr.rect(x, y, barWidth * fraction, 4f)
        sr.end()

        sr.begin(ShapeRenderer.ShapeType.Line)
        sr.color = Color(active.r, active.g, active.b, 0.3f)
        sr.rect(x, y, barWidth, 4f)
        sr.end()
    }

    /**
     * Draws N small ship silhouettes in the top-right corner, one per remaining life.
     * Must be called after the Stage draws and after sr.projectionMatrix is set to the
     * HUD (shake-free) camera. Uses the same 4-line hull geometry as ShipRenderer.
     */
    fun renderLivesIcons(sr: ShapeRenderer, lives: Int) {
        if (lives <= 0) return
        val r      = 11f                         // icon radius in world units
        val cy     = Settings.WORLD_HEIGHT - 28f // vertical centre of icons
        val startX = Settings.WORLD_WIDTH - 28f  // rightmost icon centre x
        val spacing = 36f

        // Precomputed hull offsets for nose-up orientation (rad = π/2).
        // Matches ShipRenderer geometry exactly, scaled to r.
        val nx =  0f;       val ny =  r           // nose
        val lx = -5.95f * r / 10f; val ly = -7.00f * r / 10f  // left wing
        val rx =  5.61f * r / 10f; val ry = -7.40f * r / 10f  // right wing
        val bx =  0f;       val by = -r * 0.35f   // rear notch

        sr.begin(ShapeRenderer.ShapeType.Line)
        sr.setColor(0f, 1f, 1f, 1f)  // CYAN, matching score label
        for (i in 0 until lives) {
            val cx = startX - i * spacing
            sr.line(cx + nx, cy + ny, cx + lx, cy + ly)
            sr.line(cx + lx, cy + ly, cx + bx, cy + by)
            sr.line(cx + bx, cy + by, cx + rx, cy + ry)
            sr.line(cx + rx, cy + ry, cx + nx, cy + ny)
        }
        sr.end()
    }

    fun resize(width: Int, height: Int) = viewport.update(width, height, true)

    fun dispose() {
        stage.dispose()
        font.dispose()
        waveFont.dispose()
        popupFont.dispose()
        bestHudFont.dispose()
        waveCountFont.dispose()
        multFont.dispose()
        tutorialHintStyle.font.dispose()
        powerUpFont.dispose()
    }
}
