// core/src/main/kotlin/com/palacesoft/asteroids/effects/EffectPool.kt
package com.palacesoft.starshard.effects

import com.badlogic.gdx.graphics.glutils.ShapeRenderer

/**
 * Base class for every pooled effect.
 *
 * Lifecycle:
 *   1. Created once at startup (by EffectPool).
 *   2. init() called on acquire() — set alive=true, configure state.
 *   3. update() called every frame while alive==true.
 *   4. render() called every frame while alive==true.
 *   5. reset() called when effect finishes — sets alive=false, clears state.
 *
 * Subclasses must never allocate heap objects in update() or render().
 */
abstract class PooledEffect {
    var alive = false
    abstract fun update(delta: Float)
    abstract fun render(sr: ShapeRenderer)
    abstract fun reset()
}

/**
 * Pre-allocated fixed-capacity pool. acquire() returns null (not exception)
 * when the pool is exhausted — the game continues without spawning.
 *
 * @param capacity  Fixed pool size. Choose based on max expected simultaneous effects.
 * @param factory   Called exactly [capacity] times at construction. Never called again.
 */
class EffectPool<T : PooledEffect>(capacity: Int, factory: () -> T) {
    // Backing array. Never resized. Index iteration is faster than iterator on Android.
    @Suppress("UNCHECKED_CAST")
    private val pool: Array<T> = Array<PooledEffect>(capacity) { factory() } as Array<T>

    /** Returns the first free slot, or null if all slots are active. */
    fun acquire(): T? {
        for (e in pool) if (!e.alive) return e
        return null   // pool exhausted — caller decides whether to drop or warn
    }

    fun update(delta: Float) {
        for (e in pool) if (e.alive) e.update(delta)
    }

    /** Caller must call sr.begin() before and sr.end() after if batching multiple pools. */
    fun render(sr: ShapeRenderer) {
        for (e in pool) if (e.alive) e.render(sr)
    }

    fun activeCount(): Int {
        var n = 0
        for (e in pool) if (e.alive) n++
        return n
    }
}
