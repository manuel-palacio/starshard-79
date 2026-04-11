package com.palacesoft.starshard.input

/** Active touch control scheme for mobile play. Selectable from settings. */
enum class TouchScheme {
    /** Scheme A — discrete dual-zone buttons. Preserves original game verb grammar. */
    BUTTONS,
    /** Scheme B — floating joystick on left half + right half fire/hyperspace. */
    JOYSTICK,
    /** Scheme C — dual-zone gestures. Hold left = thrust, drag = rotate. Tap right = fire. */
    GESTURES
}
