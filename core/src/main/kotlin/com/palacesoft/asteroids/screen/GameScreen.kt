package com.palacesoft.asteroids.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.palacesoft.asteroids.AsteroidsGame
import com.palacesoft.asteroids.game.World
import com.palacesoft.asteroids.input.InputHandler
import com.palacesoft.asteroids.events.GameEventBus
import com.palacesoft.asteroids.render.GameRenderer
import com.palacesoft.asteroids.render.HudRenderer
import com.palacesoft.asteroids.render.PostProcessingPipeline
import com.palacesoft.asteroids.vfx.VfxManager

class GameScreen(private val game: AsteroidsGame) : Screen {
    private val world        = World()
    private val inputHandler = InputHandler(world.input)
    private val vfx          = VfxManager(game.sr, game.batch)
    private val sounds       = game.sounds   // singleton — synthesised once in AsteroidsGame
    private val renderer     = GameRenderer(game.camera, game.batch, game.sr)
    private var pipeline     = PostProcessingPipeline(game.batch)

    private var disposed = false

    init {
        renderer.hudRenderer  = HudRenderer(game.batch, game.camera)
        renderer.inputHandler = inputHandler
        renderer.vfx          = vfx
        renderer.pipeline     = pipeline
        world.vfx   = vfx
        world.sounds = sounds
        vfx.subscribeToEvents()
        world.start()
        Gdx.input.isCatchBackKey = true
    }

    override fun render(delta: Float) {
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
        if (world.gameOver) game.setScreen(GameOverScreen(game, world.score))
    }

    override fun resize(width: Int, height: Int) {
        renderer.hudRenderer?.resize(width, height)
    }

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
        GameEventBus.clear()
        // sounds is owned by AsteroidsGame — do not dispose here
    }
}
