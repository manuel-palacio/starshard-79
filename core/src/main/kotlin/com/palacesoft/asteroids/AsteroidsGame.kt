package com.palacesoft.asteroids

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.asteroids.audio.SoundManager
import com.palacesoft.asteroids.screen.MenuScreen
import com.palacesoft.asteroids.util.Settings

class AsteroidsGame : Game() {
    lateinit var batch: SpriteBatch
    lateinit var sr: ShapeRenderer
    lateinit var camera: OrthographicCamera
    lateinit var sounds: SoundManager

    override fun create() {
        batch  = SpriteBatch()
        sr     = ShapeRenderer()
        camera = OrthographicCamera(Settings.WORLD_WIDTH, Settings.WORLD_HEIGHT)
        camera.position.set(Settings.WORLD_WIDTH / 2f, Settings.WORLD_HEIGHT / 2f, 0f)
        camera.update()
        sounds = SoundManager()   // synthesise once; GameScreen receives game.sounds
        setScreen(MenuScreen(this))
    }

    override fun dispose() {
        batch.dispose()
        sr.dispose()
        screen?.dispose()
        sounds.dispose()
    }
}
