package com.palacesoft.starshard.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.starshard.game.entity.PowerUp
import com.palacesoft.starshard.game.entity.PowerUpType
import com.palacesoft.starshard.game.entity.Ship
import kotlin.math.cos
import kotlin.math.sin

class PowerUpRenderer {
    private val color = Color()

    fun render(sr: ShapeRenderer, powerUps: List<PowerUp>) {
        if (powerUps.none { it.alive }) return
        Gdx.gl.glLineWidth(2f)
        sr.begin(ShapeRenderer.ShapeType.Line)
        for (p in powerUps) {
            if (!p.alive || !p.visible) continue
            color.set(p.type.r, p.type.g, p.type.b, 1f)
            sr.color = color
            when (p.type) {
                PowerUpType.RAPID_FIRE  -> renderBolt(sr, p)
                PowerUpType.SPREAD_SHOT -> renderFan(sr, p)
                PowerUpType.NOVA_BURST  -> renderStar(sr, p)
                PowerUpType.SHIELD      -> renderRing(sr, p)
            }
            // Outer pickup circle
            sr.color = Color(color.r, color.g, color.b, 0.3f)
            sr.circle(p.x, p.y, p.radius, 16)
        }
        sr.end()
        Gdx.gl.glLineWidth(1f)
    }

    /** Render shield ring around ship when shield is active. */
    fun renderShield(sr: ShapeRenderer, ship: Ship) {
        Gdx.gl.glLineWidth(2f)
        sr.begin(ShapeRenderer.ShapeType.Line)
        sr.color = Color(0.3f, 1f, 0.4f, 0.6f)
        sr.circle(ship.x, ship.y, ship.radius + 6f, 24)
        sr.color = Color(0.3f, 1f, 0.4f, 0.25f)
        sr.circle(ship.x, ship.y, ship.radius + 10f, 24)
        sr.end()
        Gdx.gl.glLineWidth(1f)
    }

    /** Yellow zigzag lightning bolt. */
    private fun renderBolt(sr: ShapeRenderer, p: PowerUp) {
        val rad = Math.toRadians(p.rotation.toDouble()).toFloat()
        val s = 10f
        val dx = cos(rad); val dy = sin(rad)
        val px = -dy; val py = dx  // perpendicular

        sr.line(p.x + dx * s, p.y + dy * s, p.x + px * s * 0.4f, p.y + py * s * 0.4f)
        sr.line(p.x + px * s * 0.4f, p.y + py * s * 0.4f, p.x - px * s * 0.3f + dx * s * 0.1f, p.y - py * s * 0.3f + dy * s * 0.1f)
        sr.line(p.x - px * s * 0.3f + dx * s * 0.1f, p.y - py * s * 0.3f + dy * s * 0.1f, p.x - dx * s, p.y - dy * s)
    }

    /** Blue triple-line fan shape. */
    private fun renderFan(sr: ShapeRenderer, p: PowerUp) {
        val rad = Math.toRadians(p.rotation.toDouble()).toFloat()
        val s = 10f
        for (offset in floatArrayOf(-0.4f, 0f, 0.4f)) {
            val a = rad + offset
            sr.line(p.x, p.y, p.x + cos(a) * s, p.y + sin(a) * s)
        }
    }

    /** Orange 4-line asterisk/star shape. */
    private fun renderStar(sr: ShapeRenderer, p: PowerUp) {
        val rad = Math.toRadians(p.rotation.toDouble()).toFloat()
        val s = 10f
        for (i in 0 until 4) {
            val a = rad + i * (Math.PI.toFloat() / 4f)
            sr.line(p.x - cos(a) * s, p.y - sin(a) * s, p.x + cos(a) * s, p.y + sin(a) * s)
        }
    }

    /** Green circle ring. */
    private fun renderRing(sr: ShapeRenderer, p: PowerUp) {
        sr.circle(p.x, p.y, 8f, 16)
        sr.circle(p.x, p.y, 5f, 12)
    }
}
