package com.palacesoft.starshard.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.palacesoft.starshard.AsteroidsGame
import com.palacesoft.starshard.util.Settings

object DesktopLauncher {
    @JvmStatic
    fun main(args: Array<String>) {
        Settings.fxQuality = com.palacesoft.starshard.effects.EffectQuality.HIGH
        val config = Lwjgl3ApplicationConfiguration().apply {
            setTitle("Starshard 79")
            setWindowedMode(1600, 900)
            setForegroundFPS(60)
            useVsync(true)
        }
        Lwjgl3Application(AsteroidsGame(), config)
    }
}
