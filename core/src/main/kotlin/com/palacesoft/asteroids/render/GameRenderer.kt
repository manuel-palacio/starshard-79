package com.palacesoft.asteroids.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.asteroids.game.World
import com.palacesoft.asteroids.util.Settings
import com.palacesoft.asteroids.vfx.BloomPass
import com.palacesoft.asteroids.vfx.VfxManager

class GameRenderer(
    private val camera: OrthographicCamera,
    private val batch: SpriteBatch,
    private val sr: ShapeRenderer
) {
    private val starfield    = Starfield()
    private val shipRenderer = ShipRenderer()
    private val astRenderer  = AsteroidRenderer()
    private val saucerRend   = SaucerRenderer()
    var hudRenderer: HudRenderer? = null
    var vfx: VfxManager? = null

    // BloomPass plugged in Task 14
    var bloomPass: BloomPass? = null

    fun update(delta: Float, world: World) {
        starfield.update(delta, world.ship.velX, world.ship.velY)
        shipRenderer.update(delta)
    }

    fun render(world: World, shakeOffX: Float = 0f, shakeOffY: Float = 0f) {
        Gdx.gl.glClearColor(0f, 0f, 0.03f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Camera with shake applied to game world passes
        camera.position.set(
            Settings.WORLD_WIDTH / 2f + shakeOffX,
            Settings.WORLD_HEIGHT / 2f + shakeOffY, 0f)
        camera.update()
        sr.projectionMatrix = camera.combined
        batch.projectionMatrix = camera.combined

        // Pass 1: Background starfield
        starfield.render(sr)

        // Pass 2: Geometry (asteroids, ship)
        astRenderer.render(sr, world.asteroids)
        saucerRend.render(sr, world.saucers)
        if (world.ship.visible) shipRenderer.render(sr, world.ship)

        // Pass 3: Bullets (filled circles)
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = com.badlogic.gdx.graphics.Color(1f, 1f, 0.2f, 1f)
        for (b in world.bullets) {
            if (b.alive) sr.circle(b.x, b.y, b.radius)
        }
        sr.end()
        bloomPass?.render()

        // Pass 3b: Particles (after geometry, before HUD)
        vfx?.renderParticles()

        // Pass 4: HUD (reset camera shake first)
        camera.position.set(Settings.WORLD_WIDTH / 2f, Settings.WORLD_HEIGHT / 2f, 0f)
        camera.update()
        hudRenderer?.render(world)
    }
}
