package com.palacesoft.starshard.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.palacesoft.starshard.AsteroidsGame
import com.palacesoft.starshard.game.World
import com.palacesoft.starshard.util.Settings
import com.palacesoft.starshard.input.InputHandler
import com.palacesoft.starshard.events.GameEventBus
import com.palacesoft.starshard.render.GameRenderer
import com.palacesoft.starshard.render.HudRenderer
import com.palacesoft.starshard.render.PostProcessingPipeline
import com.palacesoft.starshard.vfx.VfxManager

class GameScreen(private val game: AsteroidsGame) : Screen {
    private val world        = World()
    private val inputHandler = InputHandler(world.input)
    private val vfx          = VfxManager(game.sr, game.batch)
    private val sounds       = game.sounds   // singleton — synthesised once in AsteroidsGame
    private val renderer     = GameRenderer(game.camera, game.batch, game.sr)
    private var pipeline     = PostProcessingPipeline(game.batch)

    private var disposed = false
    private var gameOverHandled = false
    private var paused = false

    private val pauseFont = BitmapFont().apply { data.setScale(3f); color = Color.WHITE }
    private val pauseSubFont = BitmapFont().apply { data.setScale(1.5f); color = Color.GRAY }
    private val pauseLayout = GlyphLayout()

    init {
        renderer.hudRenderer  = HudRenderer(game.batch, game.camera)
        renderer.inputHandler = inputHandler
        renderer.vfx          = vfx
        renderer.pipeline     = pipeline
        world.vfx   = vfx
        world.sounds = sounds
        vfx.subscribeToEvents()
        world.start()
        @Suppress("DEPRECATION")
        Gdx.input.isCatchBackKey = true
    }

    override fun render(delta: Float) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            paused = !paused
            if (paused) sounds.pauseLoops()
        }

        if (paused) {
            renderer.render(world, 0f, vfx.offsetX, vfx.offsetY)
            renderPauseOverlay()
            return
        }

        val dt = delta.coerceAtMost(0.05f)
        inputHandler.poll()
        world.update(dt)
        vfx.update(dt)
        vfx.updateThrust(dt, world.ship)
        sounds.update(dt, world.asteroids.size, world.waveMaxAsteroids, world.score,
              world.ship.thrusting && world.ship.alive,
              world.saucers.any { it.alive })
        renderer.update(dt, world)
        renderer.render(world, dt, vfx.offsetX, vfx.offsetY)
        if (world.gameOver && !gameOverHandled) {
            gameOverHandled = true
            game.gameServices?.submitScore(world.score)
            val previousBest = Settings.highScore
            if (world.score > previousBest) Settings.highScore = world.score
            game.setScreen(GameOverScreen(game, world.score, previousBest))
        }
    }

    private fun renderPauseOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        game.sr.projectionMatrix = game.camera.combined
        game.sr.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled)
        game.sr.color = Color(0f, 0f, 0f, 0.6f)
        game.sr.rect(0f, 0f, Settings.WORLD_WIDTH, Settings.WORLD_HEIGHT)
        game.sr.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        game.batch.projectionMatrix = game.camera.combined
        game.batch.begin()
        drawPauseCentered(pauseFont, "PAUSED", Settings.WORLD_HEIGHT / 2f + 40f)
        drawPauseCentered(pauseSubFont, "PRESS ESC OR BACK TO RESUME", Settings.WORLD_HEIGHT / 2f - 30f)
        drawPauseCentered(pauseSubFont, "PRESS Q TO QUIT", Settings.WORLD_HEIGHT / 2f - 70f)
        game.batch.end()

        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            game.setScreen(MenuScreen(game))
        }
    }

    private fun drawPauseCentered(f: BitmapFont, text: String, y: Float) {
        pauseLayout.setText(f, text)
        f.draw(game.batch, text, (Settings.WORLD_WIDTH - pauseLayout.width) / 2f, y)
    }

    override fun resize(width: Int, height: Int) {
        renderer.hudRenderer?.resize(width, height)
    }

    @Suppress("DEPRECATION")
    override fun show()   { Gdx.input.isCatchBackKey = true }

    // Fix 1: forward hide() → dispose() so that GameEventBus.clear() and resource
    // disposal always happen on screen transitions, not just on app exit.
    override fun hide()   { dispose() }

    // Fix 3: stop looping audio when the app is backgrounded (Android home button).
    override fun pause()  { sounds.pauseLoops() }

    // Fix 3: recreate FrameBuffer objects after GL context is restored (Android resume).
    // BloomPass holds raw GL texture/FBO IDs — they are invalidated by context loss.
    override fun resume() {
        pipeline.dispose()
        pipeline          = PostProcessingPipeline(game.batch)
        renderer.pipeline = pipeline
    }

    // Fix 1: idempotency guard prevents double-disposal when hide() → dispose()
    // is followed by the framework also calling dispose() on app exit.
    override fun dispose() {
        if (disposed) return
        disposed = true
        renderer.hudRenderer?.dispose()
        inputHandler.dispose()
        vfx.dispose()
        pipeline.dispose()
        pauseFont.dispose()
        pauseSubFont.dispose()
        GameEventBus.clear()
        // sounds is owned by AsteroidsGame — do not dispose here
    }
}
