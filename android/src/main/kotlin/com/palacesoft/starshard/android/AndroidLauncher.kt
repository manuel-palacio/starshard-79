package com.palacesoft.starshard.android

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.palacesoft.starshard.AsteroidsGame

class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
        }
        val game = AsteroidsGame().also {
            it.gameServices = GooglePlayServices(this)
        }
        initialize(game, config)
    }
}
