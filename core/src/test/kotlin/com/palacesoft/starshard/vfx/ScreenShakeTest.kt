package com.palacesoft.starshard.vfx

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ScreenShakeTest {
    @Test fun `trauma decays to zero over time`() {
        val shake = CameraShakeController()
        shake.trigger(1f)
        repeat(100) { shake.update(0.1f) }
        assertEquals(0f, shake.offsetX, 0.01f)
    }

    @Test fun `trauma capped at 1`() {
        val shake = CameraShakeController()
        shake.trigger(5f)
        shake.update(0.016f)
        assertTrue(shake.offsetX != 0f || shake.offsetY != 0f)
    }

    @Test fun `no shake when trauma is zero`() {
        val shake = CameraShakeController()
        shake.update(0.016f)
        assertEquals(0f, shake.offsetX, 0.001f)
        assertEquals(0f, shake.offsetY, 0.001f)
    }
}
