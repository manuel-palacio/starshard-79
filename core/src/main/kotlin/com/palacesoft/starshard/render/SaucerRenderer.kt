package com.palacesoft.starshard.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.starshard.game.entity.Saucer
import com.palacesoft.starshard.game.entity.SaucerType

class SaucerRenderer {
    private val color = Color()

    fun render(sr: ShapeRenderer, saucers: List<Saucer>) {
        if (saucers.none { it.alive }) return
        Gdx.gl.glLineWidth(2f)
        sr.begin(ShapeRenderer.ShapeType.Line)
        for (s in saucers) {
            if (!s.alive) continue
            when (s.type) {
                SaucerType.CLASSIC  -> renderClassic(sr, s)
                SaucerType.DIAMOND  -> renderDiamond(sr, s)
                SaucerType.CRESCENT -> renderCrescent(sr, s)
            }
        }
        sr.end()
        Gdx.gl.glLineWidth(1f)
    }

    /** Classic dome + hull saucer. */
    private fun renderClassic(sr: ShapeRenderer, s: Saucer) {
        sr.color = Color.WHITE
        val r = s.radius
        sr.arc(s.x, s.y + r * 0.2f, r * 0.5f, 0f, 180f, 12)
        sr.line(s.x - r, s.y, s.x + r, s.y)
        sr.arc(s.x, s.y, r, 180f, 180f, 12)
        sr.line(s.x - r * 0.5f, s.y + r * 0.2f, s.x + r * 0.5f, s.y + r * 0.2f)
    }

    /** Diamond/kite shape — aggressive, angular. */
    private fun renderDiamond(sr: ShapeRenderer, s: Saucer) {
        color.set(1f, 0.8f, 0.2f, 1f) // yellow tint
        sr.color = color
        val r = s.radius
        // Four points: top, right, bottom, left
        val tx = s.x;         val ty = s.y + r
        val rx = s.x + r;     val ry = s.y
        val bx = s.x;         val by = s.y - r * 0.6f
        val lx = s.x - r;     val ly = s.y
        sr.line(tx, ty, rx, ry)
        sr.line(rx, ry, bx, by)
        sr.line(bx, by, lx, ly)
        sr.line(lx, ly, tx, ty)
        // Inner cross struts
        sr.line(s.x - r * 0.3f, s.y, s.x + r * 0.3f, s.y)
        sr.line(s.x, s.y - r * 0.2f, s.x, s.y + r * 0.4f)
    }

    /** Crescent/claw shape — two arcs forming open jaws. */
    private fun renderCrescent(sr: ShapeRenderer, s: Saucer) {
        color.set(0.4f, 1f, 0.4f, 1f) // green tint
        sr.color = color
        val r = s.radius
        // Upper arc
        sr.arc(s.x, s.y + r * 0.15f, r * 0.9f, 20f, 140f, 14)
        // Lower arc
        sr.arc(s.x, s.y - r * 0.15f, r * 0.9f, 200f, 140f, 14)
        // Connecting tips on the open side
        sr.line(s.x + r * 0.7f, s.y + r * 0.5f, s.x + r * 0.85f, s.y + r * 0.2f)
        sr.line(s.x + r * 0.7f, s.y - r * 0.5f, s.x + r * 0.85f, s.y - r * 0.2f)
        // Center dot (core)
        sr.circle(s.x - r * 0.1f, s.y, r * 0.15f, 8)
    }
}
