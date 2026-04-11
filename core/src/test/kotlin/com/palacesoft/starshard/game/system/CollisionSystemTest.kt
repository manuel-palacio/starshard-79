package com.palacesoft.starshard.game.system

import com.palacesoft.starshard.game.World
import com.palacesoft.starshard.game.entity.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CollisionSystemTest {
    @Test fun `player bullet hitting asteroid marks both dead and scores`() {
        val world = World()
        val ast = AsteroidFactory.createRandom(400f, 400f, AsteroidSize.LARGE)
        world.asteroids.add(ast)
        val bullet = Bullet().apply { x = ast.x; y = ast.y; alive = true; fromPlayer = true }
        world.bullets.add(bullet)
        world.collisionSystem.update()
        assertFalse(bullet.alive)
        assertFalse(ast.alive)
        assertTrue(world.score > 0)
    }

    @Test fun `invulnerable ship not hit by asteroid`() {
        val world = World()
        world.ship.invulnerableTimer = 1f
        val ast = AsteroidFactory.createRandom(world.ship.x, world.ship.y, AsteroidSize.LARGE)
        world.asteroids.add(ast)
        world.collisionSystem.update()
        assertTrue(world.ship.alive)
    }

    @Test fun `vulnerable ship hit by asteroid dies`() {
        val world = World()
        world.ship.invulnerableTimer = 0f
        val ast = AsteroidFactory.createRandom(world.ship.x, world.ship.y, AsteroidSize.LARGE)
        world.asteroids.add(ast)
        world.collisionSystem.update()
        assertFalse(world.ship.alive)
    }
}
