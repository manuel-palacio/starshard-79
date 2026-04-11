package com.palacesoft.starshard.game.entity

import com.palacesoft.starshard.util.Settings

class Ship {
    var x = Settings.WORLD_WIDTH / 2f
    var y = Settings.WORLD_HEIGHT / 2f
    var velX = 0f
    var velY = 0f
    var rotation = 90f          // degrees; 0=right, 90=up
    val radius = 18f
    var alive = true
    var invulnerableTimer = 0f
    var thrusting = false
    var visible = true
    var flickerAccum = 0f
    var lastBarrel = false  // false=left, true=right — alternates each shot

    fun reset() {
        x = Settings.WORLD_WIDTH / 2f
        y = Settings.WORLD_HEIGHT / 2f
        velX = 0f; velY = 0f
        rotation = 90f
        alive = true
        invulnerableTimer = 3f
        visible = true
        flickerAccum = 0f
    }
}
