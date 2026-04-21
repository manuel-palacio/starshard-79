@file:JvmName("TeaVMLauncher")

package com.palacesoft.starshard.teavm

import com.github.xpenatan.gdx.teavm.backends.web.WebApplication
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration
import com.palacesoft.starshard.AsteroidsGame
import com.palacesoft.starshard.effects.EffectQuality
import com.palacesoft.starshard.util.Settings

fun main() {
    Settings.fxQuality = EffectQuality.MEDIUM
    val config = WebApplicationConfiguration("canvas").apply {
        width = 0   // 0 = fill available space
        height = 0
    }
    WebApplication(AsteroidsGame(), config)
}
