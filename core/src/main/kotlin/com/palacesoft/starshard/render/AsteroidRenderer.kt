package com.palacesoft.starshard.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.starshard.game.entity.Asteroid
import kotlin.math.cos
import kotlin.math.sin

class AsteroidRenderer {
    private val COLOR = Color.WHITE.cpy()

    fun render(sr: ShapeRenderer, asteroids: List<Asteroid>) {
        if (asteroids.none { it.alive }) return
        Gdx.gl.glLineWidth(2f)
        sr.begin(ShapeRenderer.ShapeType.Line)
        sr.color = COLOR
        for (ast in asteroids) {
            if (!ast.alive) continue
            val rad = Math.toRadians(ast.rotation.toDouble()).toFloat()
            val cosR = cos(rad); val sinR = sin(rad)
            val verts = ast.shape.size / 2
            for (i in 0 until verts) {
                val x0 = ast.shape[i * 2];               val y0 = ast.shape[i * 2 + 1]
                val x1 = ast.shape[((i+1) % verts) * 2]; val y1 = ast.shape[((i+1) % verts) * 2 + 1]
                sr.line(
                    cosR * x0 - sinR * y0 + ast.x, sinR * x0 + cosR * y0 + ast.y,
                    cosR * x1 - sinR * y1 + ast.x, sinR * x1 + cosR * y1 + ast.y
                )
            }
        }
        sr.end()
        Gdx.gl.glLineWidth(1f)
    }
}
