package com.palacesoft.starshard.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.palacesoft.starshard.events.GameEvent
import com.palacesoft.starshard.events.GameEventBus
import com.palacesoft.starshard.util.Settings

/**
 * Bridges GameEventBus to platform vibration. Android backend actually vibrates;
 * desktop/web backends are no-ops by libGDX contract. Per-event durations are
 * tuned so destructive events feel heavier than collection events.
 */
class HapticManager {

    fun subscribe() {
        GameEventBus.subscribe { event ->
            when (event) {
                is GameEvent.PlayerHit        -> vibrate(150)
                is GameEvent.ShieldBroken     -> vibrate(80)
                is GameEvent.PowerUpCollected -> vibrate(40)
                is GameEvent.ExtraLife        -> vibrate(60)
                else -> {}
            }
        }
    }

    private fun vibrate(ms: Int) {
        if (!Settings.hapticEnabled) return
        val input = Gdx.input ?: return
        if (!input.isPeripheralAvailable(Input.Peripheral.Vibrator)) return
        input.vibrate(ms)
    }
}
