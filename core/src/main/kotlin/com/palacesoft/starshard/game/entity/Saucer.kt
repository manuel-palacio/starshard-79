package com.palacesoft.starshard.game.entity

enum class SaucerSize { LARGE, SMALL }

enum class SaucerType {
    /** Classic flying saucer dome. Single aimed shot. Waves 1+. */
    CLASSIC,
    /** Diamond/kite shape. 3-round burst fire. Waves 4+. */
    DIAMOND,
    /** Crescent/claw shape. Twin simultaneous shots. Waves 7+. */
    CRESCENT
}

class Saucer(val size: SaucerSize, var type: SaucerType = SaucerType.CLASSIC) {
    var x = 0f; var y = 0f
    var velX = 0f; var velY = 0f
    var alive = false
    var shootTimer = 0f
    var sineTimer = 0f
    var burstCount = 0
    var burstTimer = 0f

    val radius get() = if (size == SaucerSize.LARGE) 22f else 11f

    val shootInterval get() = when (type) {
        SaucerType.CLASSIC  -> 2.0f
        SaucerType.DIAMOND  -> 4.0f   // longer cooldown, but fires 3-round burst
        SaucerType.CRESCENT -> 2.5f
    }

    val speed get() = when (type) {
        SaucerType.CLASSIC  -> 130f
        SaucerType.DIAMOND  -> 130f
        SaucerType.CRESCENT -> 110f   // slightly slower, compensated by twin fire
    }
}
