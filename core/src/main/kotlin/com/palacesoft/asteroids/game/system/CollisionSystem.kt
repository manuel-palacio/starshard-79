package com.palacesoft.asteroids.game.system

import com.palacesoft.asteroids.game.World
import com.palacesoft.asteroids.game.entity.AsteroidFactory
import com.palacesoft.asteroids.game.entity.AsteroidSize
import com.palacesoft.asteroids.game.entity.SaucerSize
import com.palacesoft.asteroids.util.circlesOverlap

class CollisionSystem(private val world: World) {

    fun update() {
        checkBulletsVsAsteroids()
        checkBulletsVsSaucers()
        checkShipVsAsteroids()
        checkShipVsSaucers()
        checkShipVsSaucerBullets()
    }

    private fun checkBulletsVsAsteroids() {
        for (bullet in world.bullets) {
            if (!bullet.alive || !bullet.fromPlayer) continue
            for (ast in world.asteroids) {
                if (!ast.alive) continue
                if (circlesOverlap(bullet.x, bullet.y, bullet.radius, ast.x, ast.y, ast.radius)) {
                    bullet.alive = false
                    ast.alive = false
                    world.score += ast.size.score
                    when (ast.size) {
                        AsteroidSize.LARGE  -> world.sounds?.playBangLarge()
                        AsteroidSize.MEDIUM -> world.sounds?.playBangMedium()
                        AsteroidSize.SMALL  -> world.sounds?.playBangSmall()
                    }
                    world.asteroids.addAll(AsteroidFactory.split(ast))
                    world.vfx?.spawnExplosion(ast.x, ast.y, ast.size)
                    break
                }
            }
        }
    }

    private fun checkBulletsVsSaucers() {
        for (bullet in world.bullets) {
            if (!bullet.alive || !bullet.fromPlayer) continue
            for (saucer in world.saucers) {
                if (!saucer.alive) continue
                if (circlesOverlap(bullet.x, bullet.y, bullet.radius, saucer.x, saucer.y, saucer.radius)) {
                    bullet.alive = false
                    saucer.alive = false
                    world.score += if (saucer.size == SaucerSize.LARGE) 200 else 1000
                    world.sounds?.playBangLarge()
                    val exSize = if (saucer.size == SaucerSize.LARGE) AsteroidSize.LARGE else AsteroidSize.MEDIUM
                    world.vfx?.spawnExplosion(saucer.x, saucer.y, exSize)
                    break
                }
            }
        }
    }

    private fun checkShipVsAsteroids() {
        if (!world.ship.alive || world.ship.invulnerableTimer > 0f) return
        for (ast in world.asteroids) {
            if (!ast.alive) continue
            if (circlesOverlap(world.ship.x, world.ship.y, world.ship.radius, ast.x, ast.y, ast.radius)) {
                world.ship.alive = false
                world.sounds?.playShipBang()
                world.vfx?.spawnShipExplosion(world.ship.x, world.ship.y)
                return
            }
        }
    }

    private fun checkShipVsSaucers() {
        if (!world.ship.alive || world.ship.invulnerableTimer > 0f) return
        for (saucer in world.saucers) {
            if (!saucer.alive) continue
            if (circlesOverlap(world.ship.x, world.ship.y, world.ship.radius, saucer.x, saucer.y, saucer.radius)) {
                world.ship.alive = false
                return
            }
        }
    }

    private fun checkShipVsSaucerBullets() {
        if (!world.ship.alive || world.ship.invulnerableTimer > 0f) return
        for (bullet in world.bullets) {
            if (!bullet.alive || bullet.fromPlayer) continue
            if (circlesOverlap(world.ship.x, world.ship.y, world.ship.radius, bullet.x, bullet.y, bullet.radius)) {
                bullet.alive = false
                world.ship.alive = false
                return
            }
        }
    }
}
