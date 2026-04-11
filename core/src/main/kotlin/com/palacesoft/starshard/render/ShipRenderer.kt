package com.palacesoft.starshard.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.starshard.game.entity.Ship
import kotlin.math.cos
import kotlin.math.sin

class ShipRenderer {
    private val WHITE = Color.WHITE.cpy()
    private var thrustFlicker = 0f

    fun update(delta: Float) { thrustFlicker += delta }

    fun render(sr: ShapeRenderer, ship: Ship) {
        val rad = Math.toRadians(ship.rotation.toDouble()).toFloat()
        val r   = ship.radius

        // Classic Asteroids ship: nose + two rear wings + rear notch
        // Points in local space (rotation=0 means nose points right):
        val nx  =  cos(rad) * r;            val ny  =  sin(rad) * r          // nose
        val lx  =  cos(rad + 2.5f) * r * 0.85f; val ly  =  sin(rad + 2.5f) * r * 0.85f  // left wing
        val rx  =  cos(rad - 2.5f) * r * 0.85f; val ry  =  sin(rad - 2.5f) * r * 0.85f  // right wing
        // Rear notch (small indent at centre-back)
        val bx  = -cos(rad) * r * 0.35f;   val by  = -sin(rad) * r * 0.35f

        Gdx.gl.glLineWidth(2f)
        sr.begin(ShapeRenderer.ShapeType.Line)
        sr.color = WHITE
        // Hull: nose → left wing → notch → right wing → nose
        sr.line(ship.x + nx, ship.y + ny, ship.x + lx, ship.y + ly)
        sr.line(ship.x + lx, ship.y + ly, ship.x + bx, ship.y + by)
        sr.line(ship.x + bx, ship.y + by, ship.x + rx, ship.y + ry)
        sr.line(ship.x + rx, ship.y + ry, ship.x + nx, ship.y + ny)

        // Cockpit: small inner triangle ~40% scale
        val cr = r * 0.38f
        val cnx = cos(rad) * cr;             val cny = sin(rad) * cr
        val clx = cos(rad + 2.5f) * cr * 0.85f; val cly = sin(rad + 2.5f) * cr * 0.85f
        val crx = cos(rad - 2.5f) * cr * 0.85f; val cry = sin(rad - 2.5f) * cr * 0.85f
        sr.line(ship.x + cnx, ship.y + cny, ship.x + clx, ship.y + cly)
        sr.line(ship.x + clx, ship.y + cly, ship.x + crx, ship.y + cry)
        sr.line(ship.x + crx, ship.y + cry, ship.x + cnx, ship.y + cny)

        // Thrust: two flickering lines from rear notch, visible every other ~0.05s
        if (ship.thrusting && (thrustFlicker % 0.1f) < 0.05f) {
            val flameLen = r * (1.1f + (Math.random() * 0.4f).toFloat())
            val ex = -cos(rad) * flameLen; val ey = -sin(rad) * flameLen
            sr.line(ship.x + lx, ship.y + ly, ship.x + ex, ship.y + ey)
            sr.line(ship.x + ex, ship.y + ey, ship.x + rx, ship.y + ry)
        }
        sr.end()
        Gdx.gl.glLineWidth(1f)
    }
}
