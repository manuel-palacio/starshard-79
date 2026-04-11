package com.palacesoft.starshard.events

/**
 * Minimal pub/sub event bus.
 *
 * Rules to keep it GC-free during gameplay:
 *  - All listeners must be registered during init (GameScreen.init or VfxManager init).
 *  - Never subscribe/unsubscribe from within update() or render().
 *  - emit() iterates an ArrayList by index — no iterator allocation.
 */
object GameEventBus {
    private val listeners = ArrayList<(GameEvent) -> Unit>(8)

    fun subscribe(listener: (GameEvent) -> Unit) {
        listeners.add(listener)
    }

    /** Must only be called from the game thread. Not thread-safe by design. */
    fun emit(event: GameEvent) {
        for (i in 0 until listeners.size) {
            listeners[i](event)
        }
    }

    /** Call from GameScreen.dispose() to allow clean restart. */
    fun clear() = listeners.clear()
}
