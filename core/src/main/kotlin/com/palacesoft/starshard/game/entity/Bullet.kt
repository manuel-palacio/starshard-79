package com.palacesoft.starshard.game.entity

import com.palacesoft.starshard.util.Settings

class Bullet {
    var x = 0f; var y = 0f
    var velX = 0f; var velY = 0f
    var alive = false
    var distanceTravelled = 0f
    var fromPlayer = true
    val radius = 3f
    var colorR = 1f; var colorG = 1f; var colorB = 1f

    companion object {
        const val SPEED        = 650f
        val MAX_DISTANCE       = Settings.WORLD_WIDTH * 0.8f
    }
}
