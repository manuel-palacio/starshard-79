package com.palacesoft.asteroids.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.palacesoft.asteroids.util.Settings
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

class SoundManager {
    private val SAMPLE_RATE = 22050

    val fire        = loadSound(generateFire())
    val bangLarge   = loadSound(generateNoise(0.45f, 0.75f))
    val bangMedium  = loadSound(generateNoise(0.28f, 0.65f))
    val bangSmall   = loadSound(generateNoise(0.15f, 0.55f))
    val bangShip    = loadSound(generateNoise(0.6f, 0.85f))
    val beat1       = loadSound(generateBeat(110.0, 0.07f))
    val beat2       = loadSound(generateBeat(98.0,  0.07f))

    private var beatTimer    = 0f
    private var beatInterval = 0.9f
    private var nextBeat     = 0

    private fun generateBeat(freq: Double, duration: Float): ByteArray {
        val samples = (SAMPLE_RATE * duration).toInt()
        val pcm = ShortArray(samples) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val env = 1.0 - t / duration
            (sin(2.0 * PI * freq * t) * env * Short.MAX_VALUE * 0.85).toInt().toShort()
        }
        return toWav(pcm)
    }

    private fun generateFire(): ByteArray {
        val duration = 0.1f
        val samples = (SAMPLE_RATE * duration).toInt()
        val rng = java.util.Random(42L)
        val pcm = ShortArray(samples) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val env = (1.0 - t / duration).coerceAtLeast(0.0)
            val tone  = sin(2.0 * PI * 520.0 * t) * 0.6
            val noise = rng.nextGaussian() * 0.3
            ((tone + noise) * env * Short.MAX_VALUE * 0.55)
                .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return toWav(pcm)
    }

    private fun generateNoise(duration: Float, volume: Float): ByteArray {
        val samples = (SAMPLE_RATE * duration).toInt()
        val rng = java.util.Random(99L)
        val pcm = ShortArray(samples) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val env = (1.0 - t / duration).coerceAtLeast(0.0)
            (rng.nextGaussian() * env * Short.MAX_VALUE * volume)
                .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return toWav(pcm)
    }

    private fun toWav(pcm: ShortArray): ByteArray {
        val dataSize = pcm.size * 2
        val buf = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray(Charsets.US_ASCII))
        buf.putInt(36 + dataSize)
        buf.put("WAVE".toByteArray(Charsets.US_ASCII))
        buf.put("fmt ".toByteArray(Charsets.US_ASCII))
        buf.putInt(16)
        buf.putShort(1)                         // PCM
        buf.putShort(1)                         // mono
        buf.putInt(SAMPLE_RATE)
        buf.putInt(SAMPLE_RATE * 2)             // byte rate
        buf.putShort(2)                         // block align
        buf.putShort(16)                        // bits per sample
        buf.put("data".toByteArray(Charsets.US_ASCII))
        buf.putInt(dataSize)
        for (s in pcm) buf.putShort(s)
        return buf.array()
    }

    private fun loadSound(wavBytes: ByteArray): Sound {
        val tmp = File.createTempFile("ast_", ".wav").also { it.deleteOnExit() }
        tmp.writeBytes(wavBytes)
        return Gdx.audio.newSound(Gdx.files.absolute(tmp.absolutePath))
    }

    fun update(delta: Float, asteroidCount: Int) {
        if (!Settings.sfxEnabled) return
        beatInterval = (0.85f - asteroidCount.coerceAtMost(12) * 0.055f).coerceAtLeast(0.22f)
        beatTimer += delta
        if (beatTimer >= beatInterval) {
            beatTimer -= beatInterval
            if (nextBeat == 0) beat1.play(0.75f) else beat2.play(0.75f)
            nextBeat = 1 - nextBeat
        }
    }

    fun playFire()      { if (Settings.sfxEnabled) fire.play(0.5f) }
    fun playBangLarge() { if (Settings.sfxEnabled) bangLarge.play(0.8f) }
    fun playBangMedium(){ if (Settings.sfxEnabled) bangMedium.play(0.7f) }
    fun playBangSmall() { if (Settings.sfxEnabled) bangSmall.play(0.6f) }
    fun playShipBang()  { if (Settings.sfxEnabled) bangShip.play(0.9f) }

    fun dispose() {
        fire.dispose(); bangLarge.dispose(); bangMedium.dispose()
        bangSmall.dispose(); bangShip.dispose(); beat1.dispose(); beat2.dispose()
    }
}
