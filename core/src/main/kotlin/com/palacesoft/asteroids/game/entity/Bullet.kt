package com.palacesoft.asteroids.game.entity

import com.palacesoft.asteroids.util.Settings

class Bullet {
    var x = 0f; var y = 0f
    var velX = 0f; var velY = 0f
    var alive = false
    var distanceTravelled = 0f
    var fromPlayer = true
    val radius = 3f

    companion object {
        const val SPEED        = 650f
        val MAX_DISTANCE       = Settings.WORLD_WIDTH * 0.8f
    }
}
