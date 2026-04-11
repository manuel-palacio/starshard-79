package com.palacesoft.starshard.effects.text

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable

/**
 * Manages pool of floating score labels.
 *
 * Rendered with SpriteBatch in world space, AFTER bloom and particles,
 * so text is always crisp (no glow blur).
 *
 * Pool size is set at construction from FxSettings.maxScorePopups.
 * Pre-cached label strings avoid String.format allocations during gameplay.
 */
class FloatingTextSystem(poolSize: Int = 12) : Disposable {

    private val pool = Array(poolSize) { FloatingTextEffect() }

    private val font = BitmapFont().apply {
        data.setScale(1.6f)
        color = Color(0.9f, 0.9f, 0.3f, 1f)   // warm yellow
    }

    // Pre-allocated label strings for common score values
    private val labelCache = mapOf(
        20   to "+20",
        50   to "+50",
        100  to "+100",
        200  to "+200",
        1000 to "+1000"
    )

    fun spawn(x: Float, y: Float, amount: Int, duration: Float = 1.0f) {
        val effect = pool.firstOrNull { !it.alive } ?: return
        val label  = labelCache[amount] ?: "+$amount"
        effect.spawn(x, y, label, duration)
    }

    fun update(delta: Float) {
        for (e in pool) e.update(delta)
    }

    /**
     * Must be called between batch.begin() and batch.end() with world-space camera.
     * Do NOT call inside bloom FBO capture.
     */
    fun render(batch: SpriteBatch) {
        for (e in pool) {
            if (!e.alive) continue
            val alpha = (e.life / e.maxLife).coerceIn(0f, 1f)
            font.color.a = alpha
            font.draw(batch, e.text, e.x, e.y)
        }
    }

    override fun dispose() { font.dispose() }
}
