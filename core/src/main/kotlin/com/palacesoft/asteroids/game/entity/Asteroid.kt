package com.palacesoft.asteroids.game.entity

import kotlin.math.*
import kotlin.random.Random

enum class AsteroidSize(
    val radius: Float,
    val minSpeed: Float,
    val maxSpeed: Float,
    val score: Int
) {
    LARGE(48f,  30f,  70f, 20),
    MEDIUM(24f, 60f, 110f, 50),
    SMALL(12f, 100f, 150f, 100)
}

class Asteroid(
    var x: Float,
    var y: Float,
    var velX: Float,
    var velY: Float,
    var rotation: Float,
    val rotSpeed: Float,
    val size: AsteroidSize,
    val shape: FloatArray   // interleaved x,y offsets for polygon vertices
) {
    val radius get() = size.radius
    var alive = true
}

object AsteroidFactory {
    fun createRandom(x: Float, y: Float, size: AsteroidSize, rng: Random = Random): Asteroid {
        val angle   = rng.nextFloat() * 2f * PI.toFloat()
        val speed   = size.minSpeed + rng.nextFloat() * (size.maxSpeed - size.minSpeed)
        val velX    = cos(angle) * speed
        val velY    = sin(angle) * speed
        val rotSpd  = (rng.nextFloat() - 0.5f) * 120f
        val verts   = 8 + rng.nextInt(5)
        val shape   = generateShape(size.radius, verts, rng)
        return Asteroid(x, y, velX, velY, rng.nextFloat() * 360f, rotSpd, size, shape)
    }

    private fun generateShape(radius: Float, verts: Int, rng: Random): FloatArray {
        val pts = FloatArray(verts * 2)
        for (i in 0 until verts) {
            val a = (i.toFloat() / verts) * 2f * PI.toFloat()
            val r = radius * (0.75f + rng.nextFloat() * 0.25f)
            pts[i * 2]     = cos(a) * r
            pts[i * 2 + 1] = sin(a) * r
        }
        return pts
    }

    fun split(asteroid: Asteroid, rng: Random = Random): List<Asteroid> = when (asteroid.size) {
        AsteroidSize.LARGE  -> List(2) { createRandom(asteroid.x, asteroid.y, AsteroidSize.MEDIUM, rng) }
        AsteroidSize.MEDIUM -> List(2) { createRandom(asteroid.x, asteroid.y, AsteroidSize.SMALL, rng) }
        AsteroidSize.SMALL  -> emptyList()
    }
}
