package com.palacesoft.starshard.util

fun wrapCoord(value: Float, min: Float, max: Float): Float {
    val range = max - min
    return when {
        value < min -> value + range
        value > max -> value - range
        else -> value
    }
}

fun circlesOverlap(
    x1: Float, y1: Float, r1: Float,
    x2: Float, y2: Float, r2: Float
): Boolean {
    val dx = x2 - x1
    val dy = y2 - y1
    val radSum = r1 + r2
    return dx * dx + dy * dy < radSum * radSum
}
