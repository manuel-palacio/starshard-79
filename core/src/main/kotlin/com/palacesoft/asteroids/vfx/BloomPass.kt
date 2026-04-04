package com.palacesoft.asteroids.vfx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import com.palacesoft.asteroids.util.Settings

/**
 * Threshold + Kawase bloom.
 *
 * Pipeline (all intermediate work at quarter-resolution):
 *  1. [beginCapture / endCapture] Caller draws bright geometry into fboCapture (half-res).
 *  2. Threshold pass   : fboCapture  → fboA (quarter-res)  extract lum > 0.55
 *  3. Kawase pass 0    : fboA        → fboB (quarter-res)  offset 0.5
 *  4. Kawase pass 1    : fboB        → fboA (quarter-res)  offset 1.5
 *  5. Additive blit    : fboA        → screen              upscale + ONE/ONE blend
 *
 * Why Kawase over Gaussian:
 *  - 4 samples per pass vs. 18 for the old 9-tap separable Gaussian.
 *  - Quarter-resolution multiplies the savings further.
 *  - Two passes at offsets 0.5 and 1.5 produce a smooth, wide glow that suits
 *    vector line art without haloing dark geometry.
 */
class BloomPass(private val batch: SpriteBatch) : Disposable {

    // Half-res capture target (geometry source)
    private val cW = (Gdx.graphics.width  / 2).coerceAtLeast(1)
    private val cH = (Gdx.graphics.height / 2).coerceAtLeast(1)

    // Quarter-res blur targets
    private val bW = (Gdx.graphics.width  / 4).coerceAtLeast(1)
    private val bH = (Gdx.graphics.height / 4).coerceAtLeast(1)

    private val fboCapture = FrameBuffer(Pixmap.Format.RGBA8888, cW, cH, false)
    private val fboA       = FrameBuffer(Pixmap.Format.RGBA8888, bW, bH, false)
    private val fboB       = FrameBuffer(Pixmap.Format.RGBA8888, bW, bH, false)

    private val shaderThreshold = loadShader("shaders/bloom.vert", "shaders/threshold.frag")
    private val shaderKawase    = loadShader("shaders/bloom.vert", "shaders/kawase.frag")

    init {
        ShaderProgram.pedantic = false
    }

    // ── Capture API (called by PostProcessingPipeline / GameRenderer) ──────────

    fun beginCapture() {
        fboCapture.begin()
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    }

    fun endCapture() = fboCapture.end()

    // ── Render: threshold → Kawase ×2 → additive blit ─────────────────────────

    fun render() {
        if (!Settings.bloomEnabled) return

        val savedMatrix = batch.projectionMatrix.cpy()

        // ── Pass 1: Threshold (half-res → quarter-res) ────────────────────────
        blit(
            src    = fboCapture,
            dst    = fboA,
            shader = shaderThreshold,
            uniforms = {}
        )

        // ── Pass 2: Kawase offset 0.5 (quarter-res → quarter-res) ─────────────
        blit(
            src    = fboA,
            dst    = fboB,
            shader = shaderKawase,
            uniforms = {
                shaderKawase.setUniformf("u_pixelSize", 1f / bW, 1f / bH)
                shaderKawase.setUniformf("u_offset", 0.5f)
            }
        )

        // ── Pass 3: Kawase offset 1.5 (quarter-res → quarter-res) ─────────────
        blit(
            src    = fboB,
            dst    = fboA,
            shader = shaderKawase,
            uniforms = {
                shaderKawase.setUniformf("u_pixelSize", 1f / bW, 1f / bH)
                shaderKawase.setUniformf("u_offset", 1.5f)
            }
        )

        // ── Pass 4: Additive composite to screen ──────────────────────────────
        batch.projectionMatrix = savedMatrix
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE)
        batch.shader = null
        batch.begin()
        val tex = fboA.colorBufferTexture
        batch.draw(
            tex, 0f, 0f, Settings.WORLD_WIDTH, Settings.WORLD_HEIGHT,
            0, 0, tex.width, tex.height, false, true
        )
        batch.end()
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun blit(
        src: FrameBuffer,
        dst: FrameBuffer,
        shader: ShaderProgram,
        uniforms: () -> Unit
    ) {
        dst.begin()
        Gdx.gl.glViewport(0, 0, dst.width, dst.height)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        val ortho = Matrix4().setToOrtho2D(0f, 0f, dst.width.toFloat(), dst.height.toFloat())
        batch.projectionMatrix = ortho
        batch.shader = shader
        batch.begin()
        uniforms()
        val tex = src.colorBufferTexture
        batch.draw(tex, 0f, 0f, dst.width.toFloat(), dst.height.toFloat(),
                   0, 0, tex.width, tex.height, false, true)
        batch.end()
        dst.end()
    }

    private fun loadShader(vert: String, frag: String): ShaderProgram {
        val prog = ShaderProgram(
            Gdx.files.internal(vert).readString(),
            Gdx.files.internal(frag).readString()
        )
        if (!prog.isCompiled) Gdx.app.error("BloomPass", "$frag error: ${prog.log}")
        return prog
    }

    override fun dispose() {
        fboCapture.dispose(); fboA.dispose(); fboB.dispose()
        shaderThreshold.dispose(); shaderKawase.dispose()
    }
}
