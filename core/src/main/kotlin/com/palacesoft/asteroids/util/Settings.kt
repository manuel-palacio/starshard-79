package com.palacesoft.asteroids.util

import com.palacesoft.asteroids.effects.EffectQuality
import com.palacesoft.asteroids.effects.FxSettings
import com.palacesoft.asteroids.effects.fxSettingsFor

object Settings {
    const val WORLD_WIDTH  = 1600f
    const val WORLD_HEIGHT = 900f

    // bloomEnabled is derived from fxSettings.enableBloom at runtime.
    // Override it here only for testing; prefer setting fxQuality instead.
    val bloomEnabled get() = fxSettings.enableBloom

    var sfxEnabled   = true

    // ── FX quality ────────────────────────────────────────────────────────────
    // Default: MEDIUM (Android-safe). Override to HIGH from DesktopLauncher
    // or let the player change it in settings. Never set to HIGH on first boot
    // without confirming device capability.
    var fxQuality: EffectQuality = EffectQuality.MEDIUM
        set(value) { field = value; fxSettings = fxSettingsFor(value) }

    var fxSettings: FxSettings = fxSettingsFor(EffectQuality.MEDIUM)
        private set
}
