package com.palacesoft.asteroids.game.entity

enum class SaucerSize { LARGE, SMALL }

class Saucer(val size: SaucerSize) {
    var x = 0f; var y = 0f
    var velX = 0f; var velY = 0f
    var alive = false
    var shootTimer = 0f
    val radius get() = if (size == SaucerSize.LARGE) 22f else 11f

    companion object {
        const val SHOOT_INTERVAL = 2f
        const val SPEED          = 130f
    }
}
