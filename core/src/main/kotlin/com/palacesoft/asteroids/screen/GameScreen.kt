package com.palacesoft.asteroids.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.palacesoft.asteroids.AsteroidsGame
import com.palacesoft.asteroids.game.World
import com.palacesoft.asteroids.input.InputHandler
import com.palacesoft.asteroids.render.GameRenderer
import com.palacesoft.asteroids.render.HudRenderer
import com.palacesoft.asteroids.audio.SoundManager
import com.palacesoft.asteroids.vfx.BloomPass
import com.palacesoft.asteroids.vfx.VfxManager

class GameScreen(private val game: AsteroidsGame) : Screen {
    private val world        = World()
    private val inputHandler = InputHandler(world.input)
    private val vfx          = VfxManager(game.sr)
    private val sounds       = SoundManager()
    private val renderer     = GameRenderer(game.camera, game.batch, game.sr)
    private val bloom        = BloomPass(game.batch)

    init {
        renderer.hudRenderer = HudRenderer(game.batch, game.camera)
        renderer.vfx = vfx
        renderer.bloomPass = bloom
        world.vfx = vfx
        world.sounds = sounds
        world.start()
        Gdx.input.isCatchBackKey = true
    }

    override fun render(delta: Float) {
        val dt = delta.coerceAtMost(0.05f)
        inputHandler.poll()
        world.update(dt)
        vfx.update(dt)
        vfx.updateThrust(dt, world.ship)
        sounds.update(dt, world.asteroids.size)
        renderer.update(dt, world)
        renderer.render(world, vfx.offsetX, vfx.offsetY)
        if (world.gameOver) game.setScreen(GameOverScreen(game, world.score))
    }

    override fun resize(width: Int, height: Int) {
        renderer.hudRenderer?.resize(width, height)
    }

    override fun show()   { Gdx.input.isCatchBackKey = true }
    override fun hide()   {}
    override fun pause()  {}
    override fun resume() {}
    override fun dispose() { renderer.hudRenderer?.dispose(); bloom.dispose(); sounds.dispose() }
}
