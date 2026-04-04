package com.palacesoft.asteroids.vfx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Disposable
import com.palacesoft.asteroids.util.Settings

class BloomPass(private val batch: SpriteBatch) : Disposable {
    private val w = (Gdx.graphics.width  / 2).coerceAtLeast(1)
    private val h = (Gdx.graphics.height / 2).coerceAtLeast(1)

    private val fbo1 = FrameBuffer(Pixmap.Format.RGBA8888, w, h, false)
    private val fbo2 = FrameBuffer(Pixmap.Format.RGBA8888, w, h, false)

    private val shaderH = ShaderProgram(
        Gdx.files.internal("shaders/bloom.vert").readString(),
        Gdx.files.internal("shaders/bloomH.frag").readString()
    )
    private val shaderV = ShaderProgram(
        Gdx.files.internal("shaders/bloom.vert").readString(),
        Gdx.files.internal("shaders/bloomV.frag").readString()
    )

    init {
        ShaderProgram.pedantic = false
        if (!shaderH.isCompiled) Gdx.app.error("BloomPass", "BloomH error: ${shaderH.log}")
        if (!shaderV.isCompiled) Gdx.app.error("BloomPass", "BloomV error: ${shaderV.log}")
    }

    fun beginCapture() {
        fbo1.begin()
        Gdx.gl.glViewport(0, 0, w, h)
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    }

    fun endCapture() {
        fbo1.end()
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
    }

    fun render() {
        if (!Settings.bloomEnabled) return
        val tex1 = fbo1.colorBufferTexture

        // Save caller's projection; H-pass blits into an FBO using its own pixel-space ortho
        val savedMatrix = batch.projectionMatrix.cpy()
        val fboOrtho = com.badlogic.gdx.math.Matrix4().setToOrtho2D(0f, 0f, w.toFloat(), h.toFloat())

        // Horizontal blur: fbo1 → fbo2
        fbo2.begin()
        Gdx.gl.glViewport(0, 0, w, h)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        batch.projectionMatrix = fboOrtho
        batch.shader = shaderH
        batch.begin()
        shaderH.setUniformf("u_width", w.toFloat())
        batch.draw(tex1, 0f, 0f, w.toFloat(), h.toFloat(),
                   0, 0, tex1.width, tex1.height, false, true)
        batch.end()
        fbo2.end()
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)

        val tex2 = fbo2.colorBufferTexture

        // Restore world-space projection for the final screen blit
        batch.projectionMatrix = savedMatrix

        // Vertical blur + additive blend to screen
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE)  // additive
        batch.shader = shaderV
        batch.begin()
        shaderV.setUniformf("u_height", h.toFloat())
        batch.draw(tex2, 0f, 0f, Settings.WORLD_WIDTH, Settings.WORLD_HEIGHT,
                   0, 0, tex2.width, tex2.height, false, true)
        batch.end()
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)  // restore
        batch.shader = null
    }

    override fun dispose() {
        fbo1.dispose(); fbo2.dispose()
        shaderH.dispose(); shaderV.dispose()
    }
}
