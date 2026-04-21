package com.palacesoft.starshard.tools

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

private const val SR = 22050

// ── Waveform helpers ────────────────────────────────────────────────────────

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)
private fun sawtooth(phase: Double) = 2.0 * (phase - floor(phase)) - 1.0
private fun square(phase: Double) = if (phase % 1.0 < 0.5) 1.0 else -1.0

private fun lowPass(buf: DoubleArray, cutHz: Float): DoubleArray {
    val alpha = (1.0 / (2.0 * PI * cutHz)).let { rc -> 1.0 / SR / (rc + 1.0 / SR) }
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

// ── Sound generators ────────────────────────────────────────────────────────

private fun genBeat(freq: Double, dur: Float): ByteArray {
    val n = (SR * dur).toInt()
    val rng = java.util.Random(7L)
    val raw = DoubleArray(n) { i ->
        val t = i.toDouble() / SR
        val env = exp(-t / (dur * 0.55))
        val fundamental = sin(2.0 * PI * freq * t) * env
        val octave = sin(2.0 * PI * freq * 2.0 * t) * env * 0.35
        val click = rng.nextGaussian() * exp(-t / (dur * 0.04)) * 0.22
        fundamental + octave + click
    }
    return toWav(normalize(raw, 0.98f))
}

private fun genLaserFire(): ByteArray {
    val dur = 0.08f; val n = (SR * dur).toInt()
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
    val dur = 0.7f; val n = (SR * dur).toInt()
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
    val dur = 0.45f; val n = (SR * dur).toInt()
    val rng = java.util.Random(55L)
    var phase = 0.0
    val raw = DoubleArray(n) {
        phase += 55.0 / SR
        sawtooth(phase) * 0.65 + rng.nextGaussian() * 0.28
    }
    val fade = 512
    for (i in 0 until fade) {
        val a = i.toDouble() / fade
        val merged = raw[i] * a + raw[n - fade + i] * (1.0 - a)
        raw[i] = merged; raw[n - fade + i] = merged
    }
    return toWav(normalize(lowPass(raw, 220f), 0.62f))
}

private fun genSaucerWarble(): ByteArray {
    val dur = 0.5f; val n = (SR * dur).toInt()
    var carrier = 0.0; var modulator = 0.0
    val raw = DoubleArray(n) {
        modulator += 8.0 / SR
        val freq = 320.0 + sin(2.0 * PI * modulator) * 85.0
        carrier += freq / SR
        square(carrier) * 0.7
    }
    val fade = 256
    for (i in 0 until fade) {
        val a = i.toDouble() / fade
        val m = raw[i] * a + raw[n - fade + i] * (1.0 - a)
        raw[i] = m; raw[n - fade + i] = m
    }
    return toWav(normalize(lowPass(raw, 900f), 0.55f))
}

private fun genExtraLife(): ByteArray {
    val dur = 0.35f; val n = (SR * dur).toInt()
    val rng = java.util.Random(99L)
    val raw = DoubleArray(n) { i ->
        val t = i.toDouble() / SR
        val ping = sin(2.0 * PI * 880.0 * t) * exp(-t / 0.04) * 0.7
        val thump = (sin(2.0 * PI * 55.0 * t) + sin(2.0 * PI * 110.0 * t) * 0.4) *
                exp(-t / 0.12) * 0.9
        val click = rng.nextGaussian() * exp(-t / 0.005) * 0.3
        ping + thump + click
    }
    return toWav(normalize(raw, 0.92f))
}

private fun genSaucerFire(): ByteArray {
    val dur = 0.06f; val n = (SR * dur).toInt()
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

// ── Main ────────────────────────────────────────────────────────────────────

fun main() {
    val outDir = File("assets/sounds").also { it.mkdirs() }

    val sounds = mapOf(
        "fire.wav"        to genLaserFire(),
        "bang_large.wav"  to genBang(0.5f, 60f, 0.9f),
        "bang_medium.wav" to genBang(0.3f, 110f, 0.75f),
        "bang_small.wav"  to genBang(0.18f, 180f, 0.6f),
        "bang_ship.wav"   to genShipDeath(),
        "beat1.wav"       to genBeat(55.0, 0.22f),
        "beat2.wav"       to genBeat(68.0, 0.22f),
        "saucer_fire.wav" to genSaucerFire(),
        "thrust.wav"      to genThrust(),
        "saucer_warp.wav" to genSaucerWarble(),
        "extra_life.wav"  to genExtraLife()
    )

    sounds.forEach { (name, bytes) ->
        File(outDir, name).writeBytes(bytes)
        println("  wrote $name (${bytes.size} bytes)")
    }
    println("Generated ${sounds.size} sound files in ${outDir.absolutePath}")
}
