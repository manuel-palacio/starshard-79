package com.palacesoft.starshard.util

import com.badlogic.gdx.Gdx
import com.palacesoft.starshard.effects.EffectQuality
import com.palacesoft.starshard.effects.FxSettings
import com.palacesoft.starshard.effects.fxSettingsFor
import com.palacesoft.starshard.input.TouchScheme

object Settings {
    const val WORLD_WIDTH  = 1600f
    const val WORLD_HEIGHT = 900f

    // bloomEnabled is derived from fxSettings.enableBloom at runtime.
    // Override it here only for testing; prefer setting fxQuality instead.
    val bloomEnabled get() = fxSettings.enableBloom

    var sfxEnabled         = true

    var highScore: Int
        get() = Gdx.app?.getPreferences("asteroids")?.getInteger("highScore", 0) ?: 0
        set(value) {
            val prefs = Gdx.app?.getPreferences("asteroids") ?: return
            prefs.putInteger("highScore", value)
            prefs.flush()
        }

    /** Active touch control scheme. BUTTONS preserves the original feel; JOYSTICK suits players who prefer analogue-style input. */
    var touchScheme: TouchScheme = TouchScheme.GESTURES

    /**
     * False on first run — WaveSystem spawns the scripted tutorial wave and
     * HudRenderer shows contextual control hints. Set to true after Wave 1
     * completes so subsequent sessions skip directly to gameplay.
     */
    var tutorialCompleted  = false

    // ── FX quality ────────────────────────────────────────────────────────────
    // Default: MEDIUM (Android-safe). Override to HIGH from DesktopLauncher
    // or let the player change it in settings. Never set to HIGH on first boot
    // without confirming device capability.
    var fxQuality: EffectQuality = EffectQuality.MEDIUM
        set(value) { field = value; fxSettings = fxSettingsFor(value) }

    var fxSettings: FxSettings = fxSettingsFor(EffectQuality.MEDIUM)
        private set

    // ── Persistence ─────────────────────────────────────────────────────────

    private fun prefs() = Gdx.app?.getPreferences("asteroids")

    fun loadAll() {
        val p = prefs() ?: return
        sfxEnabled = p.getBoolean("sfxEnabled", true)
        touchScheme = try {
            TouchScheme.valueOf(p.getString("touchScheme", TouchScheme.GESTURES.name))
        } catch (_: Exception) { TouchScheme.GESTURES }
        val savedQuality = try {
            EffectQuality.valueOf(p.getString("fxQuality", fxQuality.name))
        } catch (_: Exception) { null }
        if (savedQuality != null) fxQuality = savedQuality
    }

    fun saveSfxEnabled() {
        val p = prefs() ?: return
        p.putBoolean("sfxEnabled", sfxEnabled)
        p.flush()
    }

    fun saveTouchScheme() {
        val p = prefs() ?: return
        p.putString("touchScheme", touchScheme.name)
        p.flush()
    }

    fun saveFxQuality() {
        val p = prefs() ?: return
        p.putString("fxQuality", fxQuality.name)
        p.flush()
    }
}
