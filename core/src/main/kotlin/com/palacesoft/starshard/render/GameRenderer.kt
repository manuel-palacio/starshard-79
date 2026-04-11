package com.palacesoft.starshard.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.starshard.game.World
import com.palacesoft.starshard.input.InputHandler
import com.palacesoft.starshard.util.Settings
import com.palacesoft.starshard.vfx.VfxManager

class GameRenderer(
    private val camera: OrthographicCamera,
    private val batch: SpriteBatch,
    private val sr: ShapeRenderer
) {
    private val starfield    = Starfield()
    private val shipRenderer = ShipRenderer()
    private val astRenderer  = AsteroidRenderer()
    private val saucerRend   = SaucerRenderer()
    private val powerUpRend  = PowerUpRenderer()
    private val bulletColor  = com.badlogic.gdx.graphics.Color()
    var hudRenderer: HudRenderer? = null
    var inputHandler: InputHandler? = null
    var vfx: VfxManager? = null
    var pipeline: PostProcessingPipeline? = null

    fun update(delta: Float, world: World) {
        starfield.update(delta, world.ship.velX, world.ship.velY)
        shipRenderer.update(delta)
    }

    fun render(world: World, delta: Float, shakeOffX: Float = 0f, shakeOffY: Float = 0f) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Camera with shake applied to game world passes
        camera.position.set(
            Settings.WORLD_WIDTH / 2f + shakeOffX,
            Settings.WORLD_HEIGHT / 2f + shakeOffY, 0f)
        camera.update()
        sr.projectionMatrix = camera.combined
        batch.projectionMatrix = camera.combined

        // Pass 1: Background starfield (never bloomed)
        starfield.render(sr)

        // Pass 2: Draw emissive geometry to screen at full resolution
        drawEmissive(sr, world)

        // Pass 3: Capture same geometry into bloom FBO, blur, additively composite
        if (Settings.bloomEnabled && pipeline != null) {
            pipeline!!.beginCapture()
            sr.projectionMatrix = camera.combined   // world-space projection into FBO
            drawEmissive(sr, world)
            vfx?.renderBloomedEffects()
            pipeline!!.endCapture()
            sr.projectionMatrix  = camera.combined  // restore after viewport reset
            batch.projectionMatrix = camera.combined
            pipeline!!.render()
        }

        // Pass 4: Unbloomed procedural effects + thrust particles
        vfx?.renderUnbloomedEffects()
        vfx?.renderThrustParticles()

        // Pass 5: Particle overlays (additive, SpriteBatch world space)
        batch.projectionMatrix = camera.combined
        vfx?.renderParticleOverlays(batch, delta)

        // Score popups — always crisp, never bloomed
        batch.projectionMatrix = camera.combined
        vfx?.renderTextEffects()

        // Pass 6: HUD and touch overlay (shake-free camera)
        camera.position.set(Settings.WORLD_WIDTH / 2f, Settings.WORLD_HEIGHT / 2f, 0f)
        camera.update()
        hudRenderer?.render(world)
        sr.projectionMatrix = camera.combined
        hudRenderer?.renderLivesIcons(sr, world.lives)
        hudRenderer?.renderPowerUpBar(sr, world)
        inputHandler?.renderTouchOverlay(sr, batch)
    }

    private fun drawEmissive(sr: ShapeRenderer, world: World) {
        astRenderer.render(sr, world.asteroids)
        saucerRend.render(sr, world.saucers)
        powerUpRend.render(sr, world.powerUps)
        if (world.ship.visible) {
            shipRenderer.render(sr, world.ship)
            if (world.powerUpSystem.shieldActive) powerUpRend.renderShield(sr, world.ship)
        }

        Gdx.gl.glLineWidth(2f)
        sr.begin(ShapeRenderer.ShapeType.Line)
        for (b in world.bullets) {
            if (!b.alive) continue
            sr.color = bulletColor.set(b.colorR, b.colorG, b.colorB, 1f)
            val spd = kotlin.math.sqrt(b.velX * b.velX + b.velY * b.velY).takeIf { it > 0f } ?: 1f
            val dx = b.velX / spd * 9f
            val dy = b.velY / spd * 9f
            sr.line(b.x - dx, b.y - dy, b.x + dx, b.y + dy)
        }
        sr.end()
        Gdx.gl.glLineWidth(1f)
    }
}
