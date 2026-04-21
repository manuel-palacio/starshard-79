package com.palacesoft.starshard.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.palacesoft.starshard.util.Settings

class SoundManager {

    val fire       = load("sounds/fire.wav")
    val bangLarge  = load("sounds/bang_large.wav")
    val bangMedium = load("sounds/bang_medium.wav")
    val bangSmall  = load("sounds/bang_small.wav")
    val bangShip   = load("sounds/bang_ship.wav")
    val beat1      = load("sounds/beat1.wav")
    val beat2      = load("sounds/beat2.wav")
    val saucerFire = load("sounds/saucer_fire.wav")
    val thrust     = load("sounds/thrust.wav")
    val saucerWarp = load("sounds/saucer_warp.wav")
    val extraLife  = load("sounds/extra_life.wav")

    private var thrustId      = -1L
    private var saucerWarpId  = -1L
    private var thrustPlaying = false
    private var saucerPlaying = false

    private var beatTimer       = 0f
    private var beatInterval    = 1.0f
    private var nextBeat        = 0
    private var duckTimer       = 0f

    private var lastScore       = 0
    private val extraLifeEvery  = 10_000

    private fun load(path: String): Sound = Gdx.audio.newSound(Gdx.files.internal(path))

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)

    // ── public API ────────────────────────────────────────────────────────────

    fun update(delta: Float, asteroidCount: Int, waveMaxAsteroids: Int, score: Int,
               thrusting: Boolean, hasSaucer: Boolean) {
        if (!Settings.sfxEnabled) return

        val prevThreshold = lastScore / extraLifeEvery
        val currThreshold = score    / extraLifeEvery
        if (currThreshold > prevThreshold) extraLife.play(0.95f)
        lastScore = score

        val danger   = 1f - (asteroidCount.toFloat() / waveMaxAsteroids.toFloat()).coerceIn(0f, 1f)
        beatInterval = lerp(1.0f, 0.28f, danger)

        if (duckTimer > 0f) duckTimer -= delta
        val beatVol = if (duckTimer > 0f) 0.45f else 0.72f

        beatTimer += delta
        if (beatTimer >= beatInterval) {
            beatTimer = 0f
            if (nextBeat == 0) beat1.play(beatVol) else beat2.play(beatVol)
            nextBeat = 1 - nextBeat
        }

        if (thrusting && !thrustPlaying) {
            thrustId = thrust.loop(0.72f); thrustPlaying = true
        } else if (!thrusting && thrustPlaying) {
            thrust.stop(thrustId); thrustPlaying = false
        }

        if (hasSaucer && !saucerPlaying) {
            saucerWarpId = saucerWarp.loop(0.5f); saucerPlaying = true
        } else if (!hasSaucer && saucerPlaying) {
            saucerWarp.stop(saucerWarpId); saucerPlaying = false
        }
    }

    fun playFire()       { if (Settings.sfxEnabled) fire.play(0.6f) }
    fun playBangLarge()  { if (Settings.sfxEnabled) { bangLarge.play(0.85f);  duckTimer = 0.5f } }
    fun playBangMedium() { if (Settings.sfxEnabled) { bangMedium.play(0.75f); duckTimer = 0.3f } }
    fun playBangSmall()  { if (Settings.sfxEnabled) bangSmall.play(0.65f) }
    fun playShipBang()   { if (Settings.sfxEnabled) { bangShip.play(0.92f);   duckTimer = 0.8f } }
    fun playSaucerFire() { if (Settings.sfxEnabled) saucerFire.play(0.58f) }

    fun pauseLoops() {
        if (thrustPlaying)  { thrust.stop(thrustId);         thrustPlaying  = false }
        if (saucerPlaying)  { saucerWarp.stop(saucerWarpId); saucerPlaying  = false }
    }

    fun dispose() {
        thrust.stop(); saucerWarp.stop()
        listOf(fire, bangLarge, bangMedium, bangSmall, bangShip,
               beat1, beat2, saucerFire, thrust, saucerWarp, extraLife).forEach { it.dispose() }
    }
}
