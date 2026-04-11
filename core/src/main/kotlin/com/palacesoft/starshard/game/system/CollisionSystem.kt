package com.palacesoft.starshard.game.system

import com.palacesoft.starshard.events.GameEvent
import com.palacesoft.starshard.events.GameEventBus
import com.palacesoft.starshard.game.World
import com.palacesoft.starshard.game.entity.Asteroid
import com.palacesoft.starshard.game.entity.AsteroidFactory
import com.palacesoft.starshard.game.entity.AsteroidSize
import com.palacesoft.starshard.game.entity.SaucerSize
import com.palacesoft.starshard.game.entity.SaucerType
import com.palacesoft.starshard.util.circlesOverlap

class CollisionSystem(private val world: World) {

    fun update() {
        checkBulletsVsAsteroids()
        checkBulletsVsSaucers()
        checkShipVsPowerUps()
        checkShipVsAsteroids()
        checkShipVsSaucers()
        checkShipVsSaucerBullets()
    }

    private fun checkShipVsPowerUps() {
        if (!world.ship.alive) return
        for (p in world.powerUps) {
            if (!p.alive) continue
            if (circlesOverlap(world.ship.x, world.ship.y, world.ship.radius, p.x, p.y, p.radius)) {
                p.alive = false
                world.powerUpSystem.collect(p.type, p.x, p.y)
            }
        }
    }

    private fun checkBulletsVsAsteroids() {
        val toAdd = mutableListOf<Asteroid>()
        for (bullet in world.bullets) {
            if (!bullet.alive || !bullet.fromPlayer) continue
            for (ast in world.asteroids) {
                if (!ast.alive) continue
                if (circlesOverlap(bullet.x, bullet.y, bullet.radius, ast.x, ast.y, ast.radius)) {
                    bullet.alive = false
                    ast.alive = false
                    val points = ast.size.score * world.scoreMultiplier
                    world.score += points
                    when (ast.size) {
                        AsteroidSize.LARGE  -> world.sounds?.playBangLarge()
                        AsteroidSize.MEDIUM -> world.sounds?.playBangMedium()
                        AsteroidSize.SMALL  -> world.sounds?.playBangSmall()
                    }
                    toAdd.addAll(AsteroidFactory.split(ast))
                    GameEventBus.emit(GameEvent.AsteroidDestroyed(ast.x, ast.y, ast.size))
                    GameEventBus.emit(GameEvent.ScoreAwarded(ast.x, ast.y, points))
                    break
                }
            }
        }
        world.asteroids.addAll(toAdd)
    }

    private fun checkBulletsVsSaucers() {
        for (bullet in world.bullets) {
            if (!bullet.alive || !bullet.fromPlayer) continue
            for (saucer in world.saucers) {
                if (!saucer.alive) continue
                if (circlesOverlap(bullet.x, bullet.y, bullet.radius, saucer.x, saucer.y, saucer.radius)) {
                    bullet.alive = false
                    saucer.alive = false
                    val saucerScore = (if (saucer.size == SaucerSize.LARGE) 200 else 1000) * world.scoreMultiplier
                    world.score += saucerScore
                    world.sounds?.playBangLarge()
                    val (sr, sg, sb) = when (saucer.type) {
                        SaucerType.CLASSIC  -> Triple(1f, 1f, 1f)
                        SaucerType.DIAMOND  -> Triple(1f, 0.8f, 0.2f)
                        SaucerType.CRESCENT -> Triple(0.4f, 1f, 0.4f)
                    }
                    GameEventBus.emit(GameEvent.SaucerDestroyed(saucer.x, saucer.y, sr, sg, sb))
                    GameEventBus.emit(GameEvent.ScoreAwarded(saucer.x, saucer.y, saucerScore))
                    break
                }
            }
        }
    }

    private fun killShip() {
        if (world.powerUpSystem.shieldActive) {
            world.powerUpSystem.breakShield(world.ship.x, world.ship.y)
            world.ship.invulnerableTimer = 1f
            return
        }
        world.ship.alive = false
        world.lives--
        world.sounds?.playShipBang()
        GameEventBus.emit(GameEvent.PlayerHit(world.ship.x, world.ship.y))
    }

    private fun checkShipVsAsteroids() {
        if (!world.ship.alive || world.ship.invulnerableTimer > 0f) return
        for (ast in world.asteroids) {
            if (!ast.alive) continue
            if (circlesOverlap(world.ship.x, world.ship.y, world.ship.radius, ast.x, ast.y, ast.radius)) {
                killShip()
                return
            }
        }
    }

    private fun checkShipVsSaucers() {
        if (!world.ship.alive || world.ship.invulnerableTimer > 0f) return
        for (saucer in world.saucers) {
            if (!saucer.alive) continue
            if (circlesOverlap(world.ship.x, world.ship.y, world.ship.radius, saucer.x, saucer.y, saucer.radius)) {
                killShip()
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
                killShip()
                return
            }
        }
    }
}
