package com.palacesoft.starshard.input

data class GameInput(
    var rotateLeft: Boolean  = false,
    var rotateRight: Boolean = false,
    var thrust: Boolean      = false,
    var fire: Boolean        = false,
    var hyperspace: Boolean  = false
)
