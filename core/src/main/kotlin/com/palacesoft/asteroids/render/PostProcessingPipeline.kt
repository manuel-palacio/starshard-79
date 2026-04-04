package com.palacesoft.asteroids.render

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import com.palacesoft.asteroids.util.Settings
import com.palacesoft.asteroids.vfx.BloomPass

/**
 * Thin wrapper around BloomPass with quality flag.
 *
 * Usage in GameRenderer.render():
 *   pipeline.beginCapture()
 *   //   draw ship, asteroids, saucer, bullets, muzzle flashes, glow streaks
 *   pipeline.endCapture()
 *   pipeline.render()          // additive bloom composite — no-op if bloom disabled
 *
 * Bloom is enabled only at HIGH quality (Settings.fxSettings.enableBloom).
 * On MEDIUM and LOW it is fully bypassed with zero cost.
 *
 * BloomPass uses thresholded Kawase blur (quarter-res, 2 passes, luminance
 * threshold 0.55) for clean glow on vector lines with low mobile overhead.
 */
class PostProcessingPipeline(batch: SpriteBatch) : Disposable {

    private val bloom   = BloomPass(batch)
    private val enabled get() = Settings.fxSettings.enableBloom

    fun beginCapture() { if (enabled) bloom.beginCapture() }
    fun endCapture()   { if (enabled) bloom.endCapture() }
    fun render()       { bloom.render() }   // BloomPass.render() checks its own flag

    override fun dispose() { bloom.dispose() }
}
