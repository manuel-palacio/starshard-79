package com.palacesoft.starshard.game.entity

enum class PowerUpType(val r: Float, val g: Float, val b: Float, val label: String) {
    RAPID_FIRE(1f, 0.9f, 0.2f, "RAPID FIRE"),
    SPREAD_SHOT(0.3f, 0.6f, 1f, "SPREAD SHOT"),
    NOVA_BURST(1f, 0.5f, 0.1f, "NOVA BURST"),
    SHIELD(0.3f, 1f, 0.4f, "SHIELD")
}

class PowerUp(val type: PowerUpType) {
    var x = 0f; var y = 0f
    var velX = 0f; var velY = 0f
    var alive = false
    var age = 0f
    var rotation = 0f
    val radius = 15f
    val maxAge = 8f
    val visible get() = age < 6f || ((age * 8f).toInt() % 2 == 0) // flash last 2s
}
