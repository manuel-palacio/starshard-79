package com.palacesoft.asteroids.effects.text

/**
 * A single floating score label. Managed entirely by FloatingTextSystem.
 * No GDX types here — the system owns the BitmapFont.
 */
class FloatingTextEffect {
    var alive    = false
    var x        = 0f
    var y        = 0f
    var velY     = 0f
    var life     = 0f
    var maxLife  = 0f
    var text     = ""

    fun spawn(x: Float, y: Float, text: String, duration: Float = 1.0f) {
        alive        = true
        this.x       = x
        this.y       = y
        this.text    = text
        this.life    = duration
        this.maxLife = duration
        this.velY    = 55f
    }

    fun update(delta: Float) {
        if (!alive) return
        life -= delta
        y    += velY * delta
        velY *= (1f - delta * 1.8f)
        if (life <= 0f) reset()
    }

    fun reset() { alive = false; text = "" }
}
