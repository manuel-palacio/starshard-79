package com.palacesoft.starshard

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.starshard.audio.SoundManager
import com.palacesoft.starshard.screen.MenuScreen
import com.palacesoft.starshard.util.Settings

class AsteroidsGame : Game() {
    lateinit var batch: SpriteBatch
    lateinit var sr: ShapeRenderer
    lateinit var camera: OrthographicCamera
    lateinit var sounds: SoundManager
    var gameServices: GameServices? = null

    override fun create() {
        batch  = SpriteBatch()
        sr     = ShapeRenderer()
        camera = OrthographicCamera(Settings.WORLD_WIDTH, Settings.WORLD_HEIGHT)
        camera.position.set(Settings.WORLD_WIDTH / 2f, Settings.WORLD_HEIGHT / 2f, 0f)
        camera.update()
        sounds = SoundManager()   // synthesise once; GameScreen receives game.sounds
        Settings.loadAll()
        setScreen(MenuScreen(this))
    }

    override fun dispose() {
        batch.dispose()
        sr.dispose()
        sounds.dispose()
    }
}
