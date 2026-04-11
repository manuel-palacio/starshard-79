package com.palacesoft.starshard.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.starshard.util.Settings
import kotlin.math.sin
import kotlin.random.Random

class Starfield {
    private data class Star(
        var x: Float, var y: Float,
        val layer: Int,
        val brightness: Float,
        val twinkleSpeed: Float
    )

    private val stars = List(50) {
        val layer = Random.nextInt(2)
        Star(
            x = Random.nextFloat() * Settings.WORLD_WIDTH,
            y = Random.nextFloat() * Settings.WORLD_HEIGHT,
            layer = layer,
            brightness = if (layer == 0) 0.2f + Random.nextFloat() * 0.3f   // far:  0.2–0.5
                         else             0.4f + Random.nextFloat() * 0.4f,  // near: 0.4–0.8
            twinkleSpeed = 0.5f + Random.nextFloat() * 2f
        )
    }
    private var time = 0f

    fun update(delta: Float, shipVelX: Float = 0f, shipVelY: Float = 0f) {
        time += delta
        stars.forEach { s ->
            val drift = if (s.layer == 0) 0.02f else 0.05f
            s.x = ((s.x - shipVelX * drift * delta) + Settings.WORLD_WIDTH)  % Settings.WORLD_WIDTH
            s.y = ((s.y - shipVelY * drift * delta) + Settings.WORLD_HEIGHT) % Settings.WORLD_HEIGHT
        }
    }

    fun render(sr: ShapeRenderer) {
        sr.begin(ShapeRenderer.ShapeType.Point)
        stars.forEach { s ->
            val twinkle = 0.5f + 0.5f * sin((time * s.twinkleSpeed).toDouble()).toFloat()
            val b = s.brightness * (0.35f + 0.15f * twinkle)
            sr.color = Color(b, b, b, 1f)
            sr.point(s.x, s.y, 0f)
        }
        sr.end()
    }
}
