package com.palacesoft.starshard.game.entity

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

    @Test fun `split children are offset from parent center`() {
        val parent = AsteroidFactory.createRandom(500f, 500f, AsteroidSize.LARGE)
        val kids = AsteroidFactory.split(parent)
        assertTrue(kids.none { it.x == parent.x && it.y == parent.y },
            "child must not spawn exactly on the parent")
        val dx = kids[0].x - kids[1].x
        val dy = kids[0].y - kids[1].y
        val sep = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        assertTrue(sep >= AsteroidSize.MEDIUM.radius * 1.9f,
            "children should be at least ~2x child radius apart, got $sep")
    }

    @Test fun `createRandom velocity magnitude within expected range`() {
        val a = AsteroidFactory.createRandom(0f, 0f, AsteroidSize.LARGE)
        val speed = Math.sqrt((a.velX * a.velX + a.velY * a.velY).toDouble()).toFloat()
        assertTrue(speed in AsteroidSize.LARGE.minSpeed..AsteroidSize.LARGE.maxSpeed)
    }
}
