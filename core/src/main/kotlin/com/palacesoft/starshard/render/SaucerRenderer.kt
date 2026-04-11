package com.palacesoft.starshard.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.starshard.game.entity.Saucer

class SaucerRenderer {
    private val COLOR = Color.WHITE.cpy()

    fun render(sr: ShapeRenderer, saucers: List<Saucer>) {
        if (saucers.none { it.alive }) return
        Gdx.gl.glLineWidth(2f)
        sr.begin(ShapeRenderer.ShapeType.Line)
        sr.color = COLOR
        for (s in saucers) {
            if (!s.alive) continue
            val r = s.radius
            sr.arc(s.x, s.y + r * 0.2f, r * 0.5f, 0f, 180f, 12)
            sr.line(s.x - r, s.y, s.x + r, s.y)
            sr.arc(s.x, s.y, r, 180f, 180f, 12)
            sr.line(s.x - r * 0.5f, s.y + r * 0.2f, s.x + r * 0.5f, s.y + r * 0.2f)
        }
        sr.end()
        Gdx.gl.glLineWidth(1f)
    }
}
