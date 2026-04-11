package com.palacesoft.starshard.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.palacesoft.starshard.util.Settings
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

class SoundManager {
    private val SR = 22050

    val fire       = loadSound(genLaserFire())
    val bangLarge  = loadSound(genBang(0.5f,  60f,  0.9f))
    val bangMedium = loadSound(genBang(0.3f,  110f, 0.75f))
    val bangSmall  = loadSound(genBang(0.18f, 180f, 0.6f))
    val bangShip   = loadSound(genShipDeath())
    val beat1      = loadSound(genBeat(55.0,  0.22f))
    val beat2      = loadSound(genBeat(68.0,  0.22f))
    val saucerFire = loadSound(genSaucerFire())
    val thrust     = loadSound(genThrust())
    val saucerWarp = loadSound(genSaucerWarble())
    val extraLife  = loadSound(genExtraLife())

    private var thrustId      = -1L
    private var saucerWarpId  = -1L
    private var thrustPlaying = false
    private var saucerPlaying = false

    private var beatTimer       = 0f
    private var beatInterval    = 1.0f
    private var nextBeat        = 0
    private var duckTimer       = 0f   // seconds remaining of beat ducking

    private var lastScore       = 0
    private val extraLifeEvery  = 10_000

    // ── synthesis helpers ────────────────────────────────────────────────────

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)
    private fun sawtooth(phase: Double) = 2.0 * (phase - floor(phase)) - 1.0
    private fun square(phase: Double)   = if (phase % 1.0 < 0.5) 1.0 else -1.0

    private fun lowPass(buf: DoubleArray, cutHz: Float): DoubleArray {
        val alpha = (1.0 / (2.0 * PI * cutHz)) .let { rc -> 1.0 / SR / (rc + 1.0 / SR) }
        var prev = 0.0
        return DoubleArray(buf.size) { i -> prev += alpha * (buf[i] - prev); prev }
    }

    private fun normalize(buf: DoubleArray, peak: Float): ShortArray {
        val mx = buf.maxOf { abs(it) }.takeIf { it > 0.0 } ?: 1.0
        return ShortArray(buf.size) { i ->
            (buf[i] / mx * peak * Short.MAX_VALUE)
                .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    // ── individual generators ─────────────────────────────────────────────────

    private fun genBeat(freq: Double, dur: Float): ByteArray {
        val n = (SR * dur).toInt()
        val rng = java.util.Random(7L)
        val raw = DoubleArray(n) { i ->
            val t = i.toDouble() / SR
            val env = exp(-t / (dur * 0.55))          // slow decay → sustained bass body
            val fundamental = sin(2.0 * PI * freq * t) * env
            val octave      = sin(2.0 * PI * freq * 2.0 * t) * env * 0.35  // punch without shrillness
            val click       = rng.nextGaussian() * exp(-t / (dur * 0.04)) * 0.22  // transient attack click
            fundamental + octave + click
        }
        return toWav(normalize(raw, 0.98f))
    }

    private fun genLaserFire(): ByteArray {
        val dur = 0.08f;  val n = (SR * dur).toInt()
        var phase = 0.0
        val raw = DoubleArray(n) { i ->
            val t = i.toDouble() / SR
            val frac = t / dur
            val freq = 900.0 * exp(ln(150.0 / 900.0) * frac)
            phase += freq / SR
            square(phase) * (1.0 - frac).pow(0.4)
        }
        return toWav(normalize(raw, 0.65f))
    }

    private fun genBang(dur: Float, cutHz: Float, vol: Float): ByteArray {
        val n = (SR * dur).toInt()
        val rng = java.util.Random(42L)
        val noise = DoubleArray(n) { i ->
            val t = i.toDouble() / SR
            rng.nextGaussian() * exp(-t / (dur * 0.38)) * vol.toDouble()
        }
        return toWav(normalize(lowPass(noise, cutHz), vol))
    }

    private fun genShipDeath(): ByteArray {
        val dur = 0.7f;  val n = (SR * dur).toInt()
        val rng = java.util.Random(13L)
        var phase = 0.0
        val raw = DoubleArray(n) { i ->
            val t = i.toDouble() / SR
            val frac = t / dur
            val env = exp(-t / (dur * 0.5))
            val freq = 280.0 * exp(-frac * 3.5)
            phase += freq / SR
            (sawtooth(phase) * 0.55 + rng.nextGaussian() * 0.45) * env
        }
        return toWav(normalize(lowPass(raw, 320f), 0.9f))
    }

    private fun genThrust(): ByteArray {
        val dur = 0.45f;  val n = (SR * dur).toInt()
        val rng = java.util.Random(55L)
        var phase = 0.0
        val raw = DoubleArray(n) {
            phase += 55.0 / SR
            sawtooth(phase) * 0.65 + rng.nextGaussian() * 0.28
        }
        // Crossfade ends for seamless loop
        val fade = 512
        for (i in 0 until fade) {
            val a = i.toDouble() / fade
            val merged = raw[i] * a + raw[n - fade + i] * (1.0 - a)
            raw[i] = merged; raw[n - fade + i] = merged
        }
        return toWav(normalize(lowPass(raw, 220f), 0.62f))
    }

    private fun genSaucerWarble(): ByteArray {
        val dur = 0.5f;  val n = (SR * dur).toInt()
        var carrier = 0.0;  var modulator = 0.0
        val raw = DoubleArray(n) {
            modulator += 8.0 / SR
            val freq = 320.0 + sin(2.0 * PI * modulator) * 85.0
            carrier += freq / SR
            square(carrier) * 0.7
        }
        // Crossfade for loop
        val fade = 256
        for (i in 0 until fade) {
            val a = i.toDouble() / fade
            val m = raw[i] * a + raw[n - fade + i] * (1.0 - a)
            raw[i] = m; raw[n - fade + i] = m
        }
        return toWav(normalize(lowPass(raw, 900f), 0.55f))
    }

    private fun genExtraLife(): ByteArray {
        // Short "ding-thump": a bright high ping followed immediately by a low punchy boom
        val dur = 0.35f;  val n = (SR * dur).toInt()
        val rng = java.util.Random(99L)
        val raw = DoubleArray(n) { i ->
            val t = i.toDouble() / SR
            // High ping: 880 Hz sine, fast attack, medium decay
            val ping  = sin(2.0 * PI * 880.0 * t) * exp(-t / 0.04) * 0.7
            // Low thump: 55 Hz sine + octave, punchy with slower decay
            val thump = (sin(2.0 * PI * 55.0 * t) + sin(2.0 * PI * 110.0 * t) * 0.4) *
                        exp(-t / 0.12) * 0.9
            // Transient click
            val click = rng.nextGaussian() * exp(-t / 0.005) * 0.3
            ping + thump + click
        }
        return toWav(normalize(raw, 0.92f))
    }

    private fun genSaucerFire(): ByteArray {
        val dur = 0.06f;  val n = (SR * dur).toInt()
        var phase = 0.0
        val raw = DoubleArray(n) { i ->
            val t = i.toDouble() / SR
            val frac = t / dur
            val freq = 1400.0 * exp(ln(220.0 / 1400.0) * frac)
            phase += freq / SR
            square(phase) * (1.0 - frac).pow(0.25)
        }
        return toWav(normalize(raw, 0.62f))
    }

    // ── WAV writer ────────────────────────────────────────────────────────────

    private fun toWav(pcm: ShortArray): ByteArray {
        val data = pcm.size * 2
        val buf = ByteBuffer.allocate(44 + data).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray(Charsets.US_ASCII)); buf.putInt(36 + data)
        buf.put("WAVE".toByteArray(Charsets.US_ASCII))
        buf.put("fmt ".toByteArray(Charsets.US_ASCII)); buf.putInt(16)
        buf.putShort(1); buf.putShort(1); buf.putInt(SR)
        buf.putInt(SR * 2); buf.putShort(2); buf.putShort(16)
        buf.put("data".toByteArray(Charsets.US_ASCII)); buf.putInt(data)
        for (s in pcm) buf.putShort(s)
        return buf.array()
    }

    private fun loadSound(bytes: ByteArray): Sound {
        val f = File.createTempFile("ast_", ".wav").also { it.deleteOnExit() }
        f.writeBytes(bytes)
        return Gdx.audio.newSound(Gdx.files.absolute(f.absolutePath))
    }

    // ── public API ────────────────────────────────────────────────────────────

    fun update(delta: Float, asteroidCount: Int, waveMaxAsteroids: Int, score: Int,
               thrusting: Boolean, hasSaucer: Boolean) {
        if (!Settings.sfxEnabled) return

        // Extra life at every 10 000 pts
        val prevThreshold = lastScore / extraLifeEvery
        val currThreshold = score    / extraLifeEvery
        if (currThreshold > prevThreshold) extraLife.play(0.95f)
        lastScore = score

        // Heartbeat: danger = 1 when wave is clear, 0 when full.
        // Lerp interval 1.0s (calm) → 0.28s (hunting last rock).
        val danger   = 1f - (asteroidCount.toFloat() / waveMaxAsteroids.toFloat()).coerceIn(0f, 1f)
        beatInterval = lerp(1.0f, 0.28f, danger)

        // Duck slightly during loud events (saucer / big explosion)
        if (duckTimer > 0f) duckTimer -= delta
        val beatVol = if (duckTimer > 0f) 0.45f else 0.72f   // background pulse, never dominant

        beatTimer += delta
        if (beatTimer >= beatInterval) {
            beatTimer = 0f   // reset cleanly — no drift accumulation
            if (nextBeat == 0) beat1.play(beatVol) else beat2.play(beatVol)
            nextBeat = 1 - nextBeat
        }

        // Thrust loop
        if (thrusting && !thrustPlaying) {
            thrustId = thrust.loop(0.72f); thrustPlaying = true
        } else if (!thrusting && thrustPlaying) {
            thrust.stop(thrustId); thrustPlaying = false
        }

        // Saucer warp loop
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

    /** Stop looping sounds without releasing GL resources — call from GameScreen.pause(). */
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
