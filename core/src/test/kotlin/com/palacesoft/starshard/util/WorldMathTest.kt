package com.palacesoft.starshard.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WorldMathTest {
    @Test fun `wrapCoord wraps below min`() =
        assertEquals(1990f, wrapCoord(-10f, 0f, 2000f), 0.01f)

    @Test fun `wrapCoord wraps above max`() =
        assertEquals(10f, wrapCoord(2010f, 0f, 2000f), 0.01f)

    @Test fun `wrapCoord unchanged within bounds`() =
        assertEquals(500f, wrapCoord(500f, 0f, 2000f), 0.01f)

    @Test fun `circlesOverlap true when overlapping`() =
        assertTrue(circlesOverlap(0f, 0f, 10f, 5f, 0f, 10f))

    @Test fun `circlesOverlap false when touching edge`() =
        assertFalse(circlesOverlap(0f, 0f, 5f, 10f, 0f, 5f))

    @Test fun `circlesOverlap false when apart`() =
        assertFalse(circlesOverlap(0f, 0f, 5f, 20f, 0f, 5f))
}
