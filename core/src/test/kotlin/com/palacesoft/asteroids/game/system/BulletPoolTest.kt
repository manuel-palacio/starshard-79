package com.palacesoft.asteroids.game.system

import com.palacesoft.asteroids.game.entity.Bullet
import com.palacesoft.asteroids.game.entity.Ship
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BulletPoolTest {
    private lateinit var pool: BulletPool
    private val bullets = mutableListOf<Bullet>()
    private val ship = Ship()

    @BeforeEach fun setup() { pool = BulletPool(); bullets.clear() }

    @Test fun `acquire adds a live bullet`() {
        pool.acquire(ship, bullets)
        assertEquals(1, bullets.count { it.alive })
    }

    @Test fun `cannot exceed 4 active bullets`() {
        repeat(10) { pool.acquire(ship, bullets) }
        assertEquals(4, bullets.count { it.alive })
    }

    @Test fun `dead bullet is recycled on next acquire`() {
        pool.acquire(ship, bullets)
        bullets.first { it.alive }.alive = false
        val sizeBefore = bullets.size
        pool.acquire(ship, bullets)
        assertEquals(sizeBefore, bullets.size)
    }
}
