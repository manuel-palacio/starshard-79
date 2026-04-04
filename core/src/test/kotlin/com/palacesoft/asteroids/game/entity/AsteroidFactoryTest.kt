package com.palacesoft.asteroids.game.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AsteroidFactoryTest {
    @Test fun `split LARGE yields 2 MEDIUM children`() {
        val a = AsteroidFactory.createRandom(100f, 100f, AsteroidSize.LARGE)
        val kids = AsteroidFactory.split(a)
        assertEquals(2, kids.size)
        assertTrue(kids.all { it.size == AsteroidSize.MEDIUM })
    }

    @Test fun `split MEDIUM yields 2 SMALL children`() {
        val a = AsteroidFactory.createRandom(100f, 100f, AsteroidSize.MEDIUM)
        val kids = AsteroidFactory.split(a)
        assertEquals(2, kids.size)
        assertTrue(kids.all { it.size == AsteroidSize.SMALL })
    }

    @Test fun `split SMALL yields no children`() {
        val a = AsteroidFactory.createRandom(100f, 100f, AsteroidSize.SMALL)
        assertTrue(AsteroidFactory.split(a).isEmpty())
    }

    @Test fun `createRandom velocity magnitude within expected range`() {
        val a = AsteroidFactory.createRandom(0f, 0f, AsteroidSize.LARGE)
        val speed = Math.sqrt((a.velX * a.velX + a.velY * a.velY).toDouble()).toFloat()
        assertTrue(speed in AsteroidSize.LARGE.minSpeed..AsteroidSize.LARGE.maxSpeed)
    }
}
