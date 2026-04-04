# Production FX/VFX Architecture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the ad-hoc VFX calls scattered across gameplay systems with a clean, event-driven FX layer that is GC-free during gameplay, performant on Android, and effortless to extend.

**Architecture:** Gameplay systems emit `GameEvent` objects; `VfxManager` subscribes and dispatches to pre-allocated `EffectPool<T>` instances. No gameplay code directly touches renderers or particle systems. Every pool is pre-allocated at startup; `acquire()` returns null silently under pressure — the game never pauses for GC.

**Tech Stack:** libGDX 1.12.1, Kotlin 1.9, ShapeRenderer (lines/circles), BitmapFont (score popups), OrthographicCamera (world + HUD cameras), BloomPass (existing, behind feature flag).

---

## Package Structure

```
core/src/main/kotlin/com/palacesoft/asteroids/
  events/
    GameEvent.kt                   NEW — sealed class hierarchy
    GameEventBus.kt                NEW — zero-allocation pub/sub
  effects/
    EffectPool.kt                  NEW — generic pre-allocated pool
    ExplosionEffect.kt             NEW — asteroid / saucer debris + ring
    HitSparkEffect.kt              NEW — bullet impact sparks
    MuzzleFlashEffect.kt           NEW — flash at bullet spawn
    GlowStreakEffect.kt            NEW — short bullet trail
    WaveStartEffect.kt             NEW — expanding ring on wave start
    SpawnWarningEffect.kt          NEW — pulsing circle before saucer entry
    RespawnFlashEffect.kt          NEW — invulnerability blink aura
  effects/text/
    FloatingTextEffect.kt          NEW — single score popup instance
    FloatingTextSystem.kt          NEW — pool of text effects
  vfx/
    VfxManager.kt                  REWRITE — event subscriber, effect orchestrator
    CameraShakeController.kt       NEW — replaces ScreenShake
    BloomPass.kt                   KEPT unchanged
    ParticlePool.kt                KEPT — low-level primitive reused by ThrustTrail
    ThrustTrail.kt                 KEPT unchanged
    ScreenShake.kt                 DELETE after VfxManager migration
    Explosion.kt                   DELETE after ExplosionEffect migration
  render/
    GameRenderer.kt                MODIFY — canonical render order, PostProcessingPipeline
    PostProcessingPipeline.kt      NEW — thin BloomPass wrapper with feature flag
  game/system/
    CollisionSystem.kt             MODIFY — emit events instead of direct VFX calls
    WaveSystem.kt                  MODIFY — emit WaveStarted + SaucerSpawned events
  game/
    World.kt                       MODIFY — emit BulletFired + PlayerHit + PlayerRespawned
  screen/
    GameScreen.kt                  MODIFY — wire event bus subscriptions at startup
```

---

## Architecture Overview

```
Gameplay Systems                  FX Layer
─────────────────                 ────────────────────────────────────────
CollisionSystem  ──emit()──►  GameEventBus  ──dispatch──►  VfxManager
WaveSystem       ──emit()──►       │                           │
World            ──emit()──►       │                           ├── EffectPool<ExplosionEffect>
                                   │                           ├── EffectPool<HitSparkEffect>
                                   │                           ├── EffectPool<MuzzleFlashEffect>
                                   │                           ├── EffectPool<GlowStreakEffect>
                                   │                           ├── EffectPool<WaveStartEffect>
                                   │                           ├── EffectPool<SpawnWarningEffect>
                                   │                           ├── EffectPool<RespawnFlashEffect>
                                   │                           ├── FloatingTextSystem
                                   │                           ├── CameraShakeController
                                   │                           └── ThrustTrail (unchanged)

Render Order (GameRenderer.render)
  1. Clear screen (black)
  2. Starfield (no bloom, parallax layer)
  3. ── BloomPass.beginCapture() ─────────────────────────────────────
  4.   Game geometry (asteroids, ship, saucer, bullets)   ← 2px lines
  5.   GlowStreakEffect, MuzzleFlashEffect, RespawnFlashEffect
  6. ── BloomPass.endCapture() ──────────────────────────────────────
  7. BloomPass.render()  (horizontal blur → vertical blur → additive)
  8. ExplosionEffect, HitSparkEffect, WaveStartEffect, SpawnWarningEffect
  9. ThrustTrail particles
 10. FloatingTextSystem
 11. HUD (second camera, no shake, no bloom)
```

**Why this order matters:**
- Steps 4–5 inside the bloom capture get the glow halo. Explosions (step 8) are *outside* bloom intentionally — they are already bright and adding glow makes them muddy.
- HUD always uses a separate camera with no shake offset applied.
- FloatingTextSystem renders after bloom so score popups are always crisp (no glow blur on text).

---

## Tuning Reference Table

| Effect | Duration | Particle/Line Count | Shake Intensity | Spawn Rate |
|---|---|---|---|---|
| Large explosion | 0.70s | 12–16 debris lines + ring | 0.60 | once |
| Medium explosion | 0.50s | 7–10 debris lines + ring | 0.35 | once |
| Small explosion | 0.30s | 4–6 debris lines | 0.15 | once |
| Saucer destruction | 0.65s | 10–14 debris lines + ring | 0.50 | once |
| Ship death | 0.90s | 20 lines, cyan tint | 0.80 | once |
| Bullet hit spark | 0.12s | 3–5 tiny lines | 0 | per hit |
| Muzzle flash | 0.06s | 1 circle, scale 0→8 | 0 | per shot |
| Glow streak | 0.10s | 1 fading line per bullet | 0 | per frame |
| Wave start ring | 1.20s | 1 expanding circle | 0 | per wave |
| Spawn warning | 0.80s | 1 pulsing circle, 3 pulses | 0 | per saucer |
| Respawn flash | 3.00s | blink on ship (not particles) | 0 | on respawn |
| Score popup | 1.00s | BitmapFont label, drifts up | 0 | per score |
| Heartbeat duck | 0.3–0.8s | — (audio only) | 0 | on bang |

---

## Common FX Mistakes in Arcade Shooters

1. **Spawning new objects per frame** — always pre-allocate pools; `acquire()` returns null silently under load.
2. **Shaking the HUD camera** — use separate orthographic cameras for world and HUD; apply shake offset only to the world camera.
3. **Bloom on explosions** — explosions captured inside bloom become washed-out blobs; render them *after* the bloom composite.
4. **GC spikes from lambda allocation** — the event bus `listeners` list stores `(GameEvent) -> Unit` references registered at startup, never during gameplay. Never create lambdas in `update()`.
5. **Long-lived particles** — every effect should time out hard. A leaked `alive = true` particle lasts forever and wastes a pool slot.
6. **Particle count creep** — each wave adds more asteroids; if each asteroid spawns 16 debris particles that live 0.7s, a wave-5 clear with 10 large asteroids spawns 160 particles simultaneously. Size the pool to `(maxExpectedSimultaneous × particlesPerEffect)`.
7. **ShapeRenderer.begin/end per effect** — batch all ShapeRenderer calls of the same type into one begin/end block. A single `sr.begin(Line)` / loop over all effects / `sr.end()` is faster than N begin/end pairs.
8. **Camera shake on every hit** — only shake on events the player *caused* or that *surprised* them. Bullet impacts on asteroids: no shake. Large asteroid destroyed: small shake. Ship hit: big shake.
9. **Post-processing on Android without a flag** — FBOs and blur passes are expensive. Always gate behind `Settings.bloomEnabled`, which should default `false` on Android.
10. **Procedural audio + FX in same frame** — WAV generation (your `SoundManager`) runs at startup; ensure it completes before `GameScreen` renders frame 1.

---

## Task 1: GameEvent + GameEventBus

**Files:**
- Create: `core/src/main/kotlin/com/palacesoft/asteroids/events/GameEvent.kt`
- Create: `core/src/main/kotlin/com/palacesoft/asteroids/events/GameEventBus.kt`

- [ ] **Step 1: Create GameEvent.kt**

```kotlin
// core/src/main/kotlin/com/palacesoft/asteroids/events/GameEvent.kt
package com.palacesoft.asteroids.events

import com.palacesoft.asteroids.game.entity.AsteroidSize
import com.palacesoft.asteroids.game.entity.SaucerSize

/**
 * All game events. Use class (not data class) to avoid toString overhead.
 * Instances are created at emit sites — keeping them tiny (a few floats)
 * means they land in Eden space and are collected cheaply between frames.
 */
sealed class GameEvent {
    class BulletFired(val x: Float, val y: Float, val angle: Float) : GameEvent()
    class AsteroidHit(val x: Float, val y: Float, val size: AsteroidSize) : GameEvent()
    class AsteroidDestroyed(val x: Float, val y: Float, val size: AsteroidSize) : GameEvent()
    class PlayerHit(val x: Float, val y: Float) : GameEvent()
    class PlayerRespawned(val x: Float, val y: Float) : GameEvent()
    class SaucerSpawned(val x: Float, val y: Float) : GameEvent()
    class SaucerDestroyed(val x: Float, val y: Float) : GameEvent()
    class WaveStarted(val wave: Int) : GameEvent()
    class ScoreAwarded(val x: Float, val y: Float, val amount: Int) : GameEvent()
}
```

- [ ] **Step 2: Create GameEventBus.kt**

```kotlin
// core/src/main/kotlin/com/palacesoft/asteroids/events/GameEventBus.kt
package com.palacesoft.asteroids.events

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
```

- [ ] **Step 3: Compile check**

```bash
./gradlew :core:compileKotlin
```
Expected: BUILD SUCCESSFUL, no errors.

- [ ] **Step 4: Commit**

```bash
git add core/src/main/kotlin/com/palacesoft/asteroids/events/
git commit -m "feat: GameEvent sealed class + zero-alloc GameEventBus"
```

---

## Task 2: Generic EffectPool

**Files:**
- Create: `core/src/main/kotlin/com/palacesoft/asteroids/effects/EffectPool.kt`

- [ ] **Step 1: Create EffectPool.kt**

```kotlin
// core/src/main/kotlin/com/palacesoft/asteroids/effects/EffectPool.kt
package com.palacesoft.asteroids.effects

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
```

- [ ] **Step 2: Compile check**

```bash
./gradlew :core:compileKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add core/src/main/kotlin/com/palacesoft/asteroids/effects/EffectPool.kt
git commit -m "feat: generic pre-allocated EffectPool<T> with PooledEffect base"
```

---

## Task 3: CameraShakeController (complete implementation)

**Files:**
- Create: `core/src/main/kotlin/com/palacesoft/asteroids/vfx/CameraShakeController.kt`

The existing `ScreenShake` uses the same algorithm. `CameraShakeController` is a clean, documented replacement. Migrate `VfxManager` to it in Task 10; delete `ScreenShake.kt` then.

- [ ] **Step 1: Create CameraShakeController.kt**

```kotlin
// core/src/main/kotlin/com/palacesoft/asteroids/vfx/CameraShakeController.kt
package com.palacesoft.asteroids.vfx

import kotlin.math.sin

/**
 * Trauma-based camera shake.
 *
 * How it works
 * ────────────
 * "Trauma" is a value in [0, 1] that accumulates on impact.
 * The actual shake magnitude is trauma² — this gives a punchy onset
 * that softens as trauma decays, which feels more natural than linear.
 * Two sine oscillators at incommensurable frequencies produce a
 * non-repeating Lissajous-style motion without any noise library.
 *
 * Usage
 * ────────────
 *   // On explosion:
 *   shake.trigger(0.6f)
 *
 *   // In GameRenderer.render():
 *   camera.position.set(
 *       WORLD_WIDTH / 2f + shake.offsetX,
 *       WORLD_HEIGHT / 2f + shake.offsetY, 0f
 *   )
 *   // HUD camera: do NOT add offsetX/Y.
 *
 * Tuning
 * ────────────
 * Adjust MAX_OFFSET for physical screen size. On a phone in landscape,
 * 12–16 world units is enough to feel punchy without disorienting.
 * On desktop at 1600×900, 18 is comfortable.
 */
class CameraShakeController {

    // ── Tuning constants ─────────────────────────────────────────────────────
    private val MAX_OFFSET = 18f    // world units at trauma = 1.0
    private val DECAY_RATE = 1.8f   // trauma/second — how fast it settles
    private val FREQ_X     = 37f    // Hz — must be incommensurable with FREQ_Y
    private val FREQ_Y     = 41f    //       to avoid repeating patterns

    // ── Runtime state ────────────────────────────────────────────────────────
    private var trauma = 0f
    private var time   = 0f

    /** Apply this to the game world camera's X position each frame. */
    var offsetX = 0f
        private set

    /** Apply this to the game world camera's Y position each frame. */
    var offsetY = 0f
        private set

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Trigger shake.
     * @param intensity  0 = nothing, 1 = maximum. Values accumulate (capped at 1).
     *                   Typical values: bullet impact 0.0, small bang 0.15,
     *                   medium bang 0.35, large bang 0.60, ship death 0.80.
     */
    fun trigger(intensity: Float) {
        trauma = (trauma + intensity).coerceAtMost(1f)
    }

    fun update(delta: Float) {
        if (trauma <= 0f) {
            offsetX = 0f; offsetY = 0f
            return
        }
        time   += delta
        trauma  = (trauma - DECAY_RATE * delta).coerceAtLeast(0f)

        val mag = trauma * trauma    // trauma² = punchy onset, smooth tail
        offsetX = sin(time * FREQ_X) * MAX_OFFSET * mag
        offsetY = sin(time * FREQ_Y) * MAX_OFFSET * mag
    }

    fun isShaking() = trauma > 0.01f

    /** Hard-reset. Use when transitioning screens so shake doesn't carry over. */
    fun reset() {
        trauma  = 0f
        time    = 0f
        offsetX = 0f
        offsetY = 0f
    }
}
```

- [ ] **Step 2: Compile check**

```bash
./gradlew :core:compileKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add core/src/main/kotlin/com/palacesoft/asteroids/vfx/CameraShakeController.kt
git commit -m "feat: CameraShakeController — documented replacement for ScreenShake"
```

---

## Task 4: ExplosionEffect (complete pooled implementation)

**Files:**
- Create: `core/src/main/kotlin/com/palacesoft/asteroids/effects/ExplosionEffect.kt`

This is the reference implementation for all pooled effects. Read it before writing the others.

- [ ] **Step 1: Create ExplosionEffect.kt**

```kotlin
// core/src/main/kotlin/com/palacesoft/asteroids/effects/ExplosionEffect.kt
package com.palacesoft.asteroids.effects

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.asteroids.game.entity.AsteroidSize
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Pooled explosion — asteroid and saucer destructions.
 *
 * Visual design
 * ─────────────
 * - Outward debris lines: each is a velocity vector rendered as a
 *   short line segment, fading white → gold → transparent.
 * - Expanding ring (LARGE and MEDIUM only): gold circle that grows
 *   to [ringMaxR] over the effect duration, fading out.
 *
 * Pool sizing: wave N spawns up to N*2+2 large asteroids. Each splits
 * to 2 medium and 4 small. Worst-case simultaneous explosions on a
 * wave-clear: ~10 large. Use pool capacity ≥ 24 for ExplosionEffect.
 *
 * Performance
 * ───────────
 * - Zero heap allocation in update() and render().
 * - All state in primitive FloatArrays and Boolean flags.
 * - ShapeRenderer.setColor() is called per debris line — batch with
 *   other effects in a single sr.begin(Line)/sr.end() block.
 */
class ExplosionEffect : PooledEffect() {

    companion object {
        private const val MAX_DEBRIS = 16
    }

    // Spawn position (fixed for the effect's lifetime)
    private var originX = 0f
    private var originY = 0f

    // Debris: positions relative to origin, velocities in world units/s
    private val debrisPx  = FloatArray(MAX_DEBRIS)
    private val debrisPy  = FloatArray(MAX_DEBRIS)
    private val debrisVx  = FloatArray(MAX_DEBRIS)
    private val debrisVy  = FloatArray(MAX_DEBRIS)
    private val debrisLen = FloatArray(MAX_DEBRIS)   // half-length of each line
    private var debrisCount = 0

    // Ring
    private var ringRadius  = 0f
    private var ringMaxR    = 0f
    private var ringEnabled = false

    // Timing
    private var life    = 0f
    private var maxLife = 0f

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Activate this effect. Must be called after acquire() from the pool.
     * Caller is responsible for setting alive=true on the returned instance
     * (done here via reset guard).
     */
    fun spawn(x: Float, y: Float, size: AsteroidSize, rng: Random = Random) {
        alive   = true
        originX = x
        originY = y

        // Size-specific configuration
        when (size) {
            AsteroidSize.LARGE  -> { debrisCount = 12 + rng.nextInt(5); maxLife = 0.70f; ringMaxR = 80f;  ringEnabled = true  }
            AsteroidSize.MEDIUM -> { debrisCount =  7 + rng.nextInt(4); maxLife = 0.50f; ringMaxR = 44f;  ringEnabled = true  }
            AsteroidSize.SMALL  -> { debrisCount =  4 + rng.nextInt(3); maxLife = 0.30f; ringMaxR =  0f;  ringEnabled = false }
        }
        life = maxLife

        val speed = when (size) {
            AsteroidSize.LARGE  -> 220f
            AsteroidSize.MEDIUM -> 180f
            AsteroidSize.SMALL  -> 140f
        }

        // Distribute debris evenly with per-line angle/speed jitter
        for (i in 0 until debrisCount) {
            val baseAngle = (i.toFloat() / debrisCount) * 2f * PI.toFloat()
            val angle     = baseAngle + (rng.nextFloat() - 0.5f) * 0.45f   // ±25° jitter
            val spd       = speed * (0.55f + rng.nextFloat() * 0.90f)
            debrisPx[i]   = 0f
            debrisPy[i]   = 0f
            debrisVx[i]   = cos(angle) * spd
            debrisVy[i]   = sin(angle) * spd
            debrisLen[i]  = 5f + rng.nextFloat() * 9f
        }
        ringRadius = 0f
    }

    // ── PooledEffect overrides ───────────────────────────────────────────────

    override fun update(delta: Float) {
        life -= delta
        if (life <= 0f) { reset(); return }

        val drag = 1f - delta * 2.2f   // exponential velocity decay
        for (i in 0 until debrisCount) {
            debrisPx[i] += debrisVx[i] * delta
            debrisPy[i] += debrisVy[i] * delta
            debrisVx[i] *= drag
            debrisVy[i] *= drag
        }
        if (ringEnabled) {
            ringRadius = (1f - life / maxLife) * ringMaxR
        }
    }

    override fun render(sr: ShapeRenderer) {
        val t = (life / maxLife).coerceIn(0f, 1f)   // 1 = fresh, 0 = done

        // Debris: white → gold → transparent
        for (i in 0 until debrisCount) {
            val wx   = originX + debrisPx[i]
            val wy   = originY + debrisPy[i]
            val vx   = debrisVx[i]
            val vy   = debrisVy[i]
            val norm = sqrt(vx * vx + vy * vy).coerceAtLeast(0.001f)
            val len  = debrisLen[i] * t
            val ex   = wx + (vx / norm) * len
            val ey   = wy + (vy / norm) * len

            // Color: full white at t=1, gold at t=0.5, fades at t=0
            sr.setColor(1f, 0.55f + t * 0.45f, t * 0.18f, t)
            sr.line(wx, wy, ex, ey)
        }

        // Ring: expanding warm-gold halo, fades out
        if (ringEnabled && ringRadius > 0f) {
            val ringAlpha = t * 0.55f
            sr.setColor(1f, 0.78f, 0.22f, ringAlpha)
            sr.circle(originX, originY, ringRadius, 24)
        }
    }

    override fun reset() {
        alive       = false
        debrisCount = 0
        ringEnabled = false
        life        = 0f
    }
}
```

- [ ] **Step 2: Compile check**

```bash
./gradlew :core:compileKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add core/src/main/kotlin/com/palacesoft/asteroids/effects/ExplosionEffect.kt
git commit -m "feat: ExplosionEffect — pooled debris lines + expanding ring, 3 sizes"
```

---

## Task 5: HitSparkEffect

**Files:**
- Create: `core/src/main/kotlin/com/palacesoft/asteroids/effects/HitSparkEffect.kt`

Short-lived micro-sparks at bullet impact. Duration 0.12s. Pool capacity: 24.

- [ ] **Step 1: Create HitSparkEffect.kt**

```kotlin
// core/src/main/kotlin/com/palacesoft/asteroids/effects/HitSparkEffect.kt
package com.palacesoft.asteroids.effects

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 3–5 tiny spark lines radiating from a bullet impact point.
 * Triggered on AsteroidHit (bullet hit, no kill) — not on destruction.
 * These are deliberately subtle: they confirm the hit without obscuring gameplay.
 *
 * Pool capacity recommendation: 24 (4 active bullets × ~6 sparks per hit).
 */
class HitSparkEffect : PooledEffect() {

    companion object {
        private const val MAX_SPARKS = 5
        const val DURATION = 0.12f
    }

    private val px   = FloatArray(MAX_SPARKS)
    private val py   = FloatArray(MAX_SPARKS)
    private val vx   = FloatArray(MAX_SPARKS)
    private val vy   = FloatArray(MAX_SPARKS)
    private var count = 0
    private var life  = 0f

    fun spawn(x: Float, y: Float, rng: Random = Random) {
        alive = true
        life  = DURATION
        count = 3 + rng.nextInt(3)   // 3–5
        for (i in 0 until count) {
            val angle = rng.nextFloat() * 2f * PI.toFloat()
            val spd   = 80f + rng.nextFloat() * 80f
            px[i] = x; py[i] = y
            vx[i] = cos(angle) * spd
            vy[i] = sin(angle) * spd
        }
    }

    override fun update(delta: Float) {
        life -= delta
        if (life <= 0f) { reset(); return }
        val drag = 1f - delta * 6f
        for (i in 0 until count) {
            px[i] += vx[i] * delta; py[i] += vy[i] * delta
            vx[i] *= drag;          vy[i] *= drag
        }
    }

    override fun render(sr: ShapeRenderer) {
        val t = (life / DURATION).coerceIn(0f, 1f)
        sr.setColor(1f, 0.9f, 0.5f, t)   // bright yellow, fades out
        for (i in 0 until count) {
            sr.line(px[i], py[i], px[i] + vx[i] * 0.02f, py[i] + vy[i] * 0.02f)
        }
    }

    override fun reset() { alive = false; count = 0; life = 0f }
}
```

- [ ] **Step 2: Compile and commit**

```bash
./gradlew :core:compileKotlin
git add core/src/main/kotlin/com/palacesoft/asteroids/effects/HitSparkEffect.kt
git commit -m "feat: HitSparkEffect — 3-5 yellow sparks on bullet impact"
```

---

## Task 6: MuzzleFlashEffect + GlowStreakEffect

**Files:**
- Create: `core/src/main/kotlin/com/palacesoft/asteroids/effects/MuzzleFlashEffect.kt`
- Create: `core/src/main/kotlin/com/palacesoft/asteroids/effects/GlowStreakEffect.kt`

- [ ] **Step 1: Create MuzzleFlashEffect.kt**

```kotlin
// core/src/main/kotlin/com/palacesoft/asteroids/effects/MuzzleFlashEffect.kt
package com.palacesoft.asteroids.effects

import com.badlogic.gdx.graphics.glutils.ShapeRenderer

/**
 * Very brief white circle flash at the ship's nose when firing.
 * Duration 0.06s. Radius scales from 0 → 8 → 0. Pool capacity: 4.
 * Rendered inside the bloom capture (see GameRenderer) so it gets a glow halo.
 */
class MuzzleFlashEffect : PooledEffect() {

    companion object {
        const val DURATION = 0.06f
    }

    private var x    = 0f
    private var y    = 0f
    private var life = 0f

    fun spawn(x: Float, y: Float) {
        alive  = true
        this.x = x; this.y = y
        life   = DURATION
    }

    override fun update(delta: Float) {
        life -= delta
        if (life <= 0f) reset()
    }

    override fun render(sr: ShapeRenderer) {
        val t = (life / DURATION).coerceIn(0f, 1f)
        // triangle wave: peak at t=0.5 (mid-flash)
        val scale = 1f - kotlin.math.abs(t - 0.5f) * 2f
        sr.setColor(1f, 1f, 1f, t * 0.9f)
        sr.circle(x, y, 8f * scale, 10)
    }

    override fun reset() { alive = false; life = 0f }
}
```

- [ ] **Step 2: Create GlowStreakEffect.kt**

```kotlin
// core/src/main/kotlin/com/palacesoft/asteroids/effects/GlowStreakEffect.kt
package com.palacesoft.asteroids.effects

import com.badlogic.gdx.graphics.glutils.ShapeRenderer

/**
 * Short fading white streak behind each bullet (trail effect).
 * Spawned each frame per live bullet; the streak runs from (prevX, prevY)
 * to (x, y). Duration 0.10s. Pool capacity: 16.
 *
 * Rendered inside bloom capture for the neon-tube look.
 *
 * Usage:
 *   // In World.updateBullets() or GameRenderer, per live bullet per frame:
 *   val streak = streakPool.acquire()
 *   streak?.spawn(bullet.prevX, bullet.prevY, bullet.x, bullet.y)
 *
 * Note: bullets need a prevX/prevY field for this to work properly.
 * Alternatively, compute the previous position from velocity × -delta.
 */
class GlowStreakEffect : PooledEffect() {

    companion object {
        const val DURATION = 0.10f
    }

    private var x0 = 0f; private var y0 = 0f
    private var x1 = 0f; private var y1 = 0f
    private var life = 0f

    fun spawn(x0: Float, y0: Float, x1: Float, y1: Float) {
        alive    = true
        this.x0  = x0; this.y0 = y0
        this.x1  = x1; this.y1 = y1
        life     = DURATION
    }

    override fun update(delta: Float) {
        life -= delta
        if (life <= 0f) reset()
    }

    override fun render(sr: ShapeRenderer) {
        val t = (life / DURATION).coerceIn(0f, 1f)
        sr.setColor(1f, 1f, 1f, t * 0.6f)
        sr.line(x0, y0, x1, y1)
    }

    override fun reset() { alive = false; life = 0f }
}
```

- [ ] **Step 3: Compile and commit**

```bash
./gradlew :core:compileKotlin
git add core/src/main/kotlin/com/palacesoft/asteroids/effects/MuzzleFlashEffect.kt
git add core/src/main/kotlin/com/palacesoft/asteroids/effects/GlowStreakEffect.kt
git commit -m "feat: MuzzleFlashEffect + GlowStreakEffect for punchy bullet FX"
```

---

## Task 7: WaveStartEffect + SpawnWarningEffect + RespawnFlashEffect

**Files:**
- Create: `core/src/main/kotlin/com/palacesoft/asteroids/effects/WaveStartEffect.kt`
- Create: `core/src/main/kotlin/com/palacesoft/asteroids/effects/SpawnWarningEffect.kt`
- Create: `core/src/main/kotlin/com/palacesoft/asteroids/effects/RespawnFlashEffect.kt`

- [ ] **Step 1: Create WaveStartEffect.kt**

```kotlin
// core/src/main/kotlin/com/palacesoft/asteroids/effects/WaveStartEffect.kt
package com.palacesoft.asteroids.effects

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.asteroids.util.Settings

/**
 * Single large expanding ring that crosses the entire screen over 1.2s.
 * Triggered once per WaveStarted event. Pool capacity: 1.
 * Rendered OUTSIDE bloom to stay crisp (it's a large transparent shape).
 */
class WaveStartEffect : PooledEffect() {

    companion object {
        const val DURATION  = 1.2f
        const val MAX_RADIUS = 1100f   // exceeds screen diagonal at 1600×900
    }

    private var life = 0f

    fun spawn() {
        alive = true
        life  = DURATION
    }

    override fun update(delta: Float) {
        life -= delta
        if (life <= 0f) reset()
    }

    override fun render(sr: ShapeRenderer) {
        val t = (life / DURATION).coerceIn(0f, 1f)
        val progress = 1f - t                            // 0 = start, 1 = done
        val radius   = progress * MAX_RADIUS
        val alpha    = t * 0.35f                         // subtle, never dominant
        sr.setColor(0.4f, 0.7f, 1f, alpha)              // cool blue
        sr.circle(
            Settings.WORLD_WIDTH / 2f,
            Settings.WORLD_HEIGHT / 2f,
            radius, 48
        )
    }

    override fun reset() { alive = false; life = 0f }
}
```

- [ ] **Step 2: Create SpawnWarningEffect.kt**

```kotlin
// core/src/main/kotlin/com/palacesoft/asteroids/effects/SpawnWarningEffect.kt
package com.palacesoft.asteroids.effects

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import kotlin.math.sin
import kotlin.math.PI

/**
 * Three quick pulses at the saucer's entry point before it appears.
 * Gives the player a moment to react. Duration 0.8s. Pool capacity: 2.
 * Rendered outside bloom for readability on small screens.
 */
class SpawnWarningEffect : PooledEffect() {

    companion object {
        const val DURATION = 0.8f
        private const val PULSE_FREQ = 3f / DURATION   // 3 pulses in total
    }

    private var x    = 0f
    private var y    = 0f
    private var life = 0f

    fun spawn(x: Float, y: Float) {
        alive  = true
        this.x = x; this.y = y
        life   = DURATION
    }

    override fun update(delta: Float) {
        life -= delta
        if (life <= 0f) reset()
    }

    override fun render(sr: ShapeRenderer) {
        val t       = (life / DURATION).coerceIn(0f, 1f)
        val elapsed = DURATION - life
        val pulse   = sin(elapsed * PULSE_FREQ * 2f * PI.toFloat()).coerceAtLeast(0f)
        val radius  = 24f + pulse * 20f
        val alpha   = t * pulse * 0.9f
        sr.setColor(1f, 0.3f, 0.3f, alpha)    // danger red
        sr.circle(x, y, radius, 20)
    }

    override fun reset() { alive = false; life = 0f }
}
```

- [ ] **Step 3: Create RespawnFlashEffect.kt**

```kotlin
// core/src/main/kotlin/com/palacesoft/asteroids/effects/RespawnFlashEffect.kt
package com.palacesoft.asteroids.effects

import com.badlogic.gdx.graphics.glutils.ShapeRenderer

/**
 * Aura ring around the ship during invulnerability.
 * Not a particle effect — renders a single circle whose radius and alpha
 * pulse in sync with the ship's flicker blink. Duration = invulnerableTimer
 * (typically 3s). Pool capacity: 1.
 *
 * Called with ship.alive and ship.invulnerableTimer each frame from VfxManager.
 * No spawn/despawn — just set alive=true on respawn, alive=false when timer ends.
 */
class RespawnFlashEffect : PooledEffect() {

    private var x      = 0f
    private var y      = 0f
    private var timer  = 0f
    private var maxTime = 3f

    fun begin(x: Float, y: Float, duration: Float) {
        alive    = true
        this.x   = x; this.y = y
        timer    = duration
        maxTime  = duration
    }

    fun updatePosition(x: Float, y: Float) {
        this.x = x; this.y = y
    }

    override fun update(delta: Float) {
        timer -= delta
        if (timer <= 0f) reset()
    }

    override fun render(sr: ShapeRenderer) {
        val t     = (timer / maxTime).coerceIn(0f, 1f)
        val pulse = kotlin.math.sin(timer * 12f).coerceIn(0f, 1f)
        val alpha = t * pulse * 0.5f
        sr.setColor(0.6f, 0.8f, 1f, alpha)    // cool white-blue
        sr.circle(x, y, 22f, 16)
    }

    override fun reset() { alive = false; timer = 0f }
}
```

- [ ] **Step 4: Compile and commit**

```bash
./gradlew :core:compileKotlin
git add core/src/main/kotlin/com/palacesoft/asteroids/effects/
git commit -m "feat: WaveStartEffect, SpawnWarningEffect, RespawnFlashEffect"
```

---

## Task 8: FloatingTextSystem (complete implementation)

**Files:**
- Create: `core/src/main/kotlin/com/palacesoft/asteroids/effects/text/FloatingTextEffect.kt`
- Create: `core/src/main/kotlin/com/palacesoft/asteroids/effects/text/FloatingTextSystem.kt`

- [ ] **Step 1: Create FloatingTextEffect.kt**

```kotlin
// core/src/main/kotlin/com/palacesoft/asteroids/effects/text/FloatingTextEffect.kt
package com.palacesoft.asteroids.effects.text

/**
 * A single floating score label. Managed entirely by FloatingTextSystem.
 * No GDX types here — the system owns the BitmapFont.
 */
class FloatingTextEffect {
    var alive   = false
    var x       = 0f
    var y       = 0f
    var velY    = 0f
    var life    = 0f
    var maxLife = 0f
    var text    = ""    // pre-formatted, e.g. "+100" — reuse a string map to avoid allocations

    fun spawn(x: Float, y: Float, text: String, duration: Float = 1.0f) {
        alive      = true
        this.x     = x
        this.y     = y
        this.text  = text
        this.life  = duration
        this.maxLife = duration
        this.velY  = 55f   // drifts upward in world units/s
    }

    fun update(delta: Float) {
        if (!alive) return
        life -= delta
        y    += velY * delta
        velY *= (1f - delta * 1.8f)   // slow drift
        if (life <= 0f) reset()
    }

    fun reset() { alive = false; text = "" }
}
```

- [ ] **Step 2: Create FloatingTextSystem.kt**

```kotlin
// core/src/main/kotlin/com/palacesoft/asteroids/effects/text/FloatingTextSystem.kt
package com.palacesoft.asteroids.effects.text

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable

/**
 * Manages pool of floating score labels ("+20", "+1000", etc.).
 *
 * Rendered with SpriteBatch in world space, AFTER bloom and particles,
 * so text is always crisp (not blurred).
 *
 * String allocations
 * ──────────────────
 * We pre-build a lookup map of the common score strings at init time.
 * The 5 standard values (20, 50, 100, 200, 1000) are covered; any
 * other value falls back to String.format which may allocate — acceptable
 * for rare events like extra-life awards.
 */
class FloatingTextSystem : Disposable {

    private val POOL_SIZE = 12
    private val pool = Array(POOL_SIZE) { FloatingTextEffect() }

    private val font = BitmapFont().apply {
        data.setScale(1.6f)
        color = Color(0.9f, 0.9f, 0.3f, 1f)   // warm yellow — distinct from UI cyan
    }

    // Pre-allocated label strings to avoid String.format during gameplay
    private val labelCache = mapOf(
        20   to "+20",
        50   to "+50",
        100  to "+100",
        200  to "+200",
        1000 to "+1000"
    )

    // ── Public API ────────────────────────────────────────────────────────────

    fun spawn(x: Float, y: Float, amount: Int) {
        val effect = pool.firstOrNull { !it.alive } ?: return   // pool exhausted: silent drop
        val label  = labelCache[amount] ?: "+$amount"
        effect.spawn(x, y, label)
    }

    fun update(delta: Float) {
        for (e in pool) e.update(delta)
    }

    /**
     * Must be called between batch.begin() and batch.end() with the world-space
     * camera projection. Do NOT call inside the bloom FBO capture.
     */
    fun render(batch: SpriteBatch) {
        for (e in pool) {
            if (!e.alive) continue
            val alpha = (e.life / e.maxLife).coerceIn(0f, 1f)
            font.color.a = alpha
            font.draw(batch, e.text, e.x, e.y)
        }
    }

    override fun dispose() { font.dispose() }
}
```

- [ ] **Step 3: Compile and commit**

```bash
./gradlew :core:compileKotlin
git add core/src/main/kotlin/com/palacesoft/asteroids/effects/text/
git commit -m "feat: FloatingTextSystem — pooled score popups, pre-cached label strings"
```

---

## Task 9: PostProcessingPipeline

**Files:**
- Create: `core/src/main/kotlin/com/palacesoft/asteroids/render/PostProcessingPipeline.kt`

Thin wrapper around `BloomPass` that documents the contract and adds the Android feature flag.

- [ ] **Step 1: Create PostProcessingPipeline.kt**

```kotlin
// core/src/main/kotlin/com/palacesoft/asteroids/render/PostProcessingPipeline.kt
package com.palacesoft.asteroids.render

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import com.palacesoft.asteroids.util.Settings
import com.palacesoft.asteroids.vfx.BloomPass

/**
 * Thin wrapper around BloomPass that owns the feature flag check and
 * documents the render contract.
 *
 * Usage in GameRenderer.render():
 *
 *   // 1. Draw starfield directly (never bloomed)
 *   // 2. Bloom capture:
 *   pipeline.beginCapture()
 *   //   draw ship, asteroids, saucer, bullets, muzzle flashes, glow streaks
 *   pipeline.endCapture()
 *   // 3. Composite bloom over scene:
 *   pipeline.render()
 *   // 4. Draw explosions, sparks, wave ring AFTER bloom (intentionally unblurred)
 *
 * Android guidance:
 *   Set Settings.bloomEnabled = false in the Android launcher or detect via
 *   Gdx.app.type == Application.ApplicationType.Android.
 *   BloomPass allocates two half-res FBOs. On low-end devices, this alone
 *   can drop you below 60fps. Gate aggressively.
 */
class PostProcessingPipeline(batch: SpriteBatch) : Disposable {

    private val bloom = BloomPass(batch)
    private val enabled get() = Settings.bloomEnabled

    fun beginCapture() { if (enabled) bloom.beginCapture() }
    fun endCapture()   { if (enabled) bloom.endCapture() }
    fun render()       { bloom.render() }   // BloomPass.render() already checks the flag

    override fun dispose() { bloom.dispose() }
}
```

- [ ] **Step 2: Compile and commit**

```bash
./gradlew :core:compileKotlin
git add core/src/main/kotlin/com/palacesoft/asteroids/render/PostProcessingPipeline.kt
git commit -m "feat: PostProcessingPipeline — documented BloomPass wrapper with feature flag"
```

---

## Task 10: Rewrite VfxManager

**Files:**
- Modify: `core/src/main/kotlin/com/palacesoft/asteroids/vfx/VfxManager.kt`

This is the largest change. VfxManager becomes the single FX facade — no other class needs to know about individual effect types.

- [ ] **Step 1: Replace VfxManager.kt**

```kotlin
// core/src/main/kotlin/com/palacesoft/asteroids/vfx/VfxManager.kt
package com.palacesoft.asteroids.vfx

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.asteroids.effects.*
import com.palacesoft.asteroids.effects.text.FloatingTextSystem
import com.palacesoft.asteroids.events.GameEvent
import com.palacesoft.asteroids.events.GameEventBus
import com.palacesoft.asteroids.game.entity.AsteroidSize
import com.palacesoft.asteroids.game.entity.Ship

/**
 * Single FX facade. All gameplay code talks only to this class via events.
 *
 * Subscriptions are registered in [subscribeToEvents]. Call this once
 * from GameScreen.init after the event bus is ready. Call [dispose] from
 * GameScreen.dispose and [GameEventBus.clear] to avoid leak on screen switch.
 *
 * Pool sizing rationale:
 *   explosions: 24 — wave-clear with 10 large asteroids = ~10 simultaneous, with headroom
 *   hitSparks:  24 — 4 bullets × several hits in one frame
 *   flashes:     4 — at most 1 per bullet (4 active bullets)
 *   streaks:    16 — 1 per live bullet per frame, duration 0.1s
 *   waveStart:   1 — one at a time
 *   warnings:    2 — max 2 saucers
 *   respawn:     1 — one player
 */
class VfxManager(private val sr: ShapeRenderer, private val batch: SpriteBatch) {

    // ── Effect pools ─────────────────────────────────────────────────────────
    private val explosions = EffectPool(24) { ExplosionEffect() }
    private val hitSparks  = EffectPool(24) { HitSparkEffect() }
    private val flashes    = EffectPool( 4) { MuzzleFlashEffect() }
    private val streaks    = EffectPool(16) { GlowStreakEffect() }
    private val waveRings  = EffectPool( 1) { WaveStartEffect() }
    private val warnings   = EffectPool( 2) { SpawnWarningEffect() }
    private val respawnAura = RespawnFlashEffect()   // single instance, not pooled

    // ── Sub-systems ───────────────────────────────────────────────────────────
    val shake    = CameraShakeController()
    val textFx   = FloatingTextSystem()
    private val thrust = ThrustTrail(EffectPool(80) { com.palacesoft.asteroids.vfx.Particle() })

    // Expose shake offsets for GameRenderer
    val offsetX get() = shake.offsetX
    val offsetY get() = shake.offsetY

    // ── Event subscription ────────────────────────────────────────────────────

    fun subscribeToEvents() {
        GameEventBus.subscribe { event ->
            when (event) {
                is GameEvent.BulletFired     -> onBulletFired(event)
                is GameEvent.AsteroidHit     -> onAsteroidHit(event)
                is GameEvent.AsteroidDestroyed -> onAsteroidDestroyed(event)
                is GameEvent.PlayerHit       -> onPlayerHit(event)
                is GameEvent.PlayerRespawned -> onPlayerRespawned(event)
                is GameEvent.SaucerSpawned   -> onSaucerSpawned(event)
                is GameEvent.SaucerDestroyed -> onSaucerDestroyed(event)
                is GameEvent.WaveStarted     -> onWaveStarted(event)
                is GameEvent.ScoreAwarded    -> onScoreAwarded(event)
            }
        }
    }

    // ── Event handlers ────────────────────────────────────────────────────────

    private fun onBulletFired(e: GameEvent.BulletFired) {
        flashes.acquire()?.spawn(e.x, e.y)
    }

    private fun onAsteroidHit(e: GameEvent.AsteroidHit) {
        hitSparks.acquire()?.spawn(e.x, e.y)
        // No shake on bullet hits — only on destructions
    }

    private fun onAsteroidDestroyed(e: GameEvent.AsteroidDestroyed) {
        explosions.acquire()?.spawn(e.x, e.y, e.size)
        val intensity = when (e.size) {
            AsteroidSize.LARGE  -> 0.60f
            AsteroidSize.MEDIUM -> 0.35f
            AsteroidSize.SMALL  -> 0.15f
        }
        shake.trigger(intensity)
    }

    private fun onPlayerHit(e: GameEvent.PlayerHit) {
        // Ship death explosion uses existing particle pool for now
        // TODO: replace with a dedicated ShipExplosionEffect in Phase 2
        shake.trigger(0.80f)
    }

    private fun onPlayerRespawned(e: GameEvent.PlayerRespawned) {
        respawnAura.begin(e.x, e.y, 3f)
    }

    private fun onSaucerSpawned(e: GameEvent.SaucerSpawned) {
        warnings.acquire()?.spawn(e.x, e.y)
    }

    private fun onSaucerDestroyed(e: GameEvent.SaucerDestroyed) {
        explosions.acquire()?.spawn(e.x, e.y, AsteroidSize.LARGE)
        shake.trigger(0.50f)
    }

    private fun onWaveStarted(e: GameEvent.WaveStarted) {
        waveRings.acquire()?.spawn()
    }

    private fun onScoreAwarded(e: GameEvent.ScoreAwarded) {
        textFx.spawn(e.x, e.y, e.amount)
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    fun update(delta: Float) {
        shake.update(delta)
        explosions.update(delta)
        hitSparks.update(delta)
        flashes.update(delta)
        streaks.update(delta)
        waveRings.update(delta)
        warnings.update(delta)
        if (respawnAura.alive) respawnAura.update(delta)
        textFx.update(delta)
    }

    fun updateThrust(delta: Float, ship: Ship) {
        thrust.update(delta, ship)
    }

    // ── Render passes (called from GameRenderer in order) ─────────────────────

    /**
     * Bloom-captured pass: effects that should receive glow halo.
     * Call between PostProcessingPipeline.beginCapture() and endCapture().
     */
    fun renderBloomedEffects() {
        sr.begin(ShapeRenderer.ShapeType.Line)
        flashes.render(sr)    // muzzle flash inside bloom
        streaks.render(sr)    // bullet glow streaks inside bloom
        sr.end()

        // Respawn aura as filled circle
        if (respawnAura.alive) {
            sr.begin(ShapeRenderer.ShapeType.Line)
            respawnAura.render(sr)
            sr.end()
        }
    }

    /**
     * Non-bloomed pass: effects that should be crisp, rendered after bloom composite.
     */
    fun renderUnbloomedEffects() {
        sr.begin(ShapeRenderer.ShapeType.Line)
        explosions.render(sr)
        hitSparks.render(sr)
        waveRings.render(sr)
        warnings.render(sr)
        sr.end()
    }

    /** Thrust particles — rendered after bloom, uses the existing ParticlePool renderer. */
    fun renderThrustParticles() {
        thrust.renderParticles(sr)
    }

    /** Score popups — rendered last (after bloom, after particles). */
    fun renderTextEffects() {
        batch.begin()
        textFx.render(batch)
        batch.end()
    }

    fun dispose() {
        textFx.dispose()
        shake.reset()
    }
}
```

Note: `ThrustTrail` currently uses `ParticlePool` from `vfx`. Keep `ThrustTrail.kt` as-is; VfxManager wires it. The `Particle` reference above needs adjustment — see Step 2.

- [ ] **Step 2: Keep ThrustTrail dependency — verify ThrustTrail still compiles**

`ThrustTrail` takes a `ParticlePool` in its current form. Check its constructor signature and keep passing `ParticlePool` from `vfx`. If VfxManager needs to change the ThrustTrail init, update only the constructor call:

```kotlin
// In VfxManager, replace the thrust line with:
private val particlePool = com.palacesoft.asteroids.vfx.ParticlePool(80)
private val thrust = ThrustTrail(particlePool)

// And update renderThrustParticles():
fun renderThrustParticles() {
    particlePool.render(sr)
}
```

- [ ] **Step 3: Compile check**

```bash
./gradlew :core:compileKotlin 2>&1 | grep -E "error:|BUILD"
```
Expected: BUILD SUCCESSFUL (fix any import errors for ThrustTrail/ParticlePool).

- [ ] **Step 4: Commit**

```bash
git add core/src/main/kotlin/com/palacesoft/asteroids/vfx/VfxManager.kt
git commit -m "feat: VfxManager rewrite — event-driven, typed effect pools, single FX facade"
```

---

## Task 11: Emit Events from Gameplay Systems

**Files:**
- Modify: `core/src/main/kotlin/com/palacesoft/asteroids/game/system/CollisionSystem.kt`
- Modify: `core/src/main/kotlin/com/palacesoft/asteroids/game/system/WaveSystem.kt`
- Modify: `core/src/main/kotlin/com/palacesoft/asteroids/game/World.kt`

- [ ] **Step 1: Update CollisionSystem — emit instead of calling VFX directly**

In `CollisionSystem.kt`, add the import and replace every `world.vfx?.spawnExplosion(...)` call with an event emission. Also emit `ScoreAwarded` after score changes.

Find the block in `checkBulletsVsAsteroids()`:
```kotlin
world.score += ast.size.score
// ... bang sounds ...
world.asteroids.addAll(AsteroidFactory.split(ast))
world.vfx?.spawnExplosion(ast.x, ast.y, ast.size)
```

Replace with:
```kotlin
world.score += ast.size.score
// ... bang sounds ...
world.asteroids.addAll(AsteroidFactory.split(ast))
GameEventBus.emit(GameEvent.AsteroidDestroyed(ast.x, ast.y, ast.size))
GameEventBus.emit(GameEvent.ScoreAwarded(ast.x, ast.y, ast.size.score))
```

In `checkBulletsVsSaucers()`, replace the direct `world.vfx?.spawnExplosion(...)` call:
```kotlin
GameEventBus.emit(GameEvent.SaucerDestroyed(saucer.x, saucer.y))
GameEventBus.emit(GameEvent.ScoreAwarded(saucer.x, saucer.y,
    if (saucer.size == SaucerSize.LARGE) 200 else 1000))
```

In `checkShipVsAsteroids()` and `checkShipVsSaucerBullets()`, after `world.ship.alive = false`:
```kotlin
GameEventBus.emit(GameEvent.PlayerHit(world.ship.x, world.ship.y))
```

- [ ] **Step 2: Update WaveSystem — emit WaveStarted + SaucerSpawned**

In `spawnWave()`, after the repeat loop:
```kotlin
GameEventBus.emit(GameEvent.WaveStarted(world.wave))
```

In `spawnSaucer()`, after `saucer.alive = true`:
```kotlin
GameEventBus.emit(GameEvent.SaucerSpawned(saucer.x, saucer.y))
```

- [ ] **Step 3: Update World — emit BulletFired + PlayerRespawned**

In `World.kt`, in `updateShip()`, after `bulletPool.acquire(ship, bullets)`:
```kotlin
val rad = Math.toRadians(ship.rotation.toDouble())
GameEventBus.emit(GameEvent.BulletFired(
    ship.x + cos(rad).toFloat() * ship.radius,
    ship.y + sin(rad).toFloat() * ship.radius,
    ship.rotation
))
```

In `WaveSystem.handleRespawn()`, after `world.ship.reset()`:
```kotlin
GameEventBus.emit(GameEvent.PlayerRespawned(world.ship.x, world.ship.y))
```

- [ ] **Step 4: Add import to all modified files**

```kotlin
import com.palacesoft.asteroids.events.GameEvent
import com.palacesoft.asteroids.events.GameEventBus
```

- [ ] **Step 5: Compile check**

```bash
./gradlew :core:compileKotlin 2>&1 | grep -E "error:|BUILD"
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add core/src/main/kotlin/com/palacesoft/asteroids/game/system/CollisionSystem.kt
git add core/src/main/kotlin/com/palacesoft/asteroids/game/system/WaveSystem.kt
git add core/src/main/kotlin/com/palacesoft/asteroids/game/World.kt
git commit -m "refactor: gameplay systems emit events; remove direct VFX calls"
```

---

## Task 12: Update GameRenderer + GameScreen

**Files:**
- Modify: `core/src/main/kotlin/com/palacesoft/asteroids/render/GameRenderer.kt`
- Modify: `core/src/main/kotlin/com/palacesoft/asteroids/screen/GameScreen.kt`

- [ ] **Step 1: Replace bloomPass field with PostProcessingPipeline in GameRenderer**

In `GameRenderer.kt`:

```kotlin
// Replace:
var bloomPass: BloomPass? = null

// With:
var pipeline: PostProcessingPipeline? = null
```

Update the render method to the canonical order:

```kotlin
fun render(world: World, shakeOffX: Float = 0f, shakeOffY: Float = 0f) {
    Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    camera.position.set(
        Settings.WORLD_WIDTH / 2f + shakeOffX,
        Settings.WORLD_HEIGHT / 2f + shakeOffY, 0f)
    camera.update()
    sr.projectionMatrix  = camera.combined
    batch.projectionMatrix = camera.combined

    // 1. Starfield — never bloomed
    starfield.render(sr)

    // 2. Emissive geometry to screen (full resolution)
    drawEmissive(sr, world)

    // 3. Bloom: capture emissive geometry + bloomed FX to FBO, composite
    pipeline?.beginCapture()
    sr.projectionMatrix = camera.combined
    drawEmissive(sr, world)
    vfx?.renderBloomedEffects()
    pipeline?.endCapture()
    sr.projectionMatrix  = camera.combined
    batch.projectionMatrix = camera.combined
    pipeline?.render()

    // 4. Post-bloom effects (crisp, no glow blur)
    vfx?.renderUnbloomedEffects()

    // 5. Thrust particles
    vfx?.renderThrustParticles()

    // 6. Score popups (batch — world space, no bloom)
    batch.projectionMatrix = camera.combined
    vfx?.renderTextEffects()

    // 7. HUD — separate camera, no shake, no bloom
    camera.position.set(Settings.WORLD_WIDTH / 2f, Settings.WORLD_HEIGHT / 2f, 0f)
    camera.update()
    hudRenderer?.render(world)
}
```

- [ ] **Step 2: Update GameScreen to use new VfxManager constructor + wire event bus**

In `GameScreen.kt`:

```kotlin
// In GameScreen class body, replace:
private val vfx   = VfxManager(game.sr)
private val bloom = BloomPass(game.batch)

// With:
private val vfx      = VfxManager(game.sr, game.batch)
private val pipeline = PostProcessingPipeline(game.batch)

// In init {}:
renderer.pipeline = pipeline
// (remove renderer.bloomPass = bloom)
world.vfx = vfx  // keep — VfxManager is still the World's reference for compat

// After world.vfx = vfx, register the event bus:
vfx.subscribeToEvents()

// In dispose():
vfx.dispose()
pipeline.dispose()
GameEventBus.clear()
// Remove: bloom.dispose()
```

- [ ] **Step 3: Remove old World.vfx direct calls that are now replaced by events**

Search for any remaining `world.vfx?.spawnExplosion` or `world.vfx?.spawnShipExplosion` calls and remove them (they should all be gone after Task 11).

```bash
grep -r "spawnExplosion\|spawnShipExplosion" core/src/main/kotlin/
```
Expected: zero results.

- [ ] **Step 4: Full compile + run smoke test**

```bash
./gradlew :desktop:run
```
Expected: game launches, explosions appear, score popups float upward, wave ring appears on wave start.

- [ ] **Step 5: Commit**

```bash
git add core/src/main/kotlin/com/palacesoft/asteroids/render/GameRenderer.kt
git add core/src/main/kotlin/com/palacesoft/asteroids/screen/GameScreen.kt
git commit -m "refactor: GameRenderer canonical render order + PostProcessingPipeline + new VfxManager wiring"
```

---

## Task 13: Delete Deprecated Files

**Files:**
- Delete: `core/src/main/kotlin/com/palacesoft/asteroids/vfx/ScreenShake.kt`
- Delete: `core/src/main/kotlin/com/palacesoft/asteroids/vfx/Explosion.kt`

- [ ] **Step 1: Verify nothing imports ScreenShake or Explosion**

```bash
grep -r "ScreenShake\|import.*Explosion" core/src/main/kotlin/ | grep -v "ExplosionEffect"
```
Expected: zero results.

- [ ] **Step 2: Delete files and compile**

```bash
rm core/src/main/kotlin/com/palacesoft/asteroids/vfx/ScreenShake.kt
rm core/src/main/kotlin/com/palacesoft/asteroids/vfx/Explosion.kt
./gradlew :core:compileKotlin 2>&1 | grep -E "error:|BUILD"
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "chore: remove deprecated ScreenShake and Explosion (replaced by CameraShakeController + ExplosionEffect)"
```

---

## Phased Implementation Roadmap

### Phase 1 — Foundation (Tasks 1–3) — do first, low risk
- Event bus + GameEvent model
- EffectPool base class
- CameraShakeController
- Estimated effort: ~2–3h

### Phase 2 — Core Explosion FX (Task 4) — highest visual impact
- ExplosionEffect with debris lines and ring
- Replace old Explosion.kt calls
- Estimated effort: ~1h

### Phase 3 — Bullet FX (Tasks 5–6) — feel improvement
- HitSparkEffect, MuzzleFlashEffect, GlowStreakEffect
- Wire to BulletFired + AsteroidHit events
- Estimated effort: ~1h

### Phase 4 — Gameplay Readability FX (Task 7) — player communication
- WaveStartEffect, SpawnWarningEffect, RespawnFlashEffect
- Estimated effort: ~1.5h

### Phase 5 — Text FX (Task 8) — score feedback
- FloatingTextSystem
- Estimated effort: ~1h

### Phase 6 — Integration (Tasks 9–13) — architecture cleanup
- PostProcessingPipeline, new VfxManager, event wiring
- Estimated effort: ~3–4h

### Phase 7 — Android profiling
- Run on device, measure frame time
- Disable bloom if < 60fps
- Profile pool utilization (add debug overlay if needed)

---

## Android Performance Checklist

- [ ] All pools pre-allocated at startup; no `new` calls during gameplay
- [ ] `GameEventBus.emit()` iterates by index, not iterator
- [ ] `BitmapFont` instances: 1 per text system, cached across frames
- [ ] `ShapeRenderer.begin/end` called once per render pass, not per effect
- [ ] `Settings.bloomEnabled = false` on Android by default
- [ ] No `String.format` or `"$var"` in hot paths (score labels pre-cached)
- [ ] `FloatArray` fields in effects (not `ArrayList<Float>`)
- [ ] `glLineWidth(2f)` reset to `1f` after each section (some drivers misbehave otherwise)
- [ ] Pool capacity: measure `activeCount()` in debug; right-size before ship

---

## Minimum Viable Polish (MVP)

Must have for the game to feel finished:
- [ ] ExplosionEffect with debris lines (replace current Explosion.kt)
- [ ] CameraShakeController replacing ScreenShake
- [ ] FloatingTextSystem (score popups)
- [ ] MuzzleFlashEffect (bullet fire punch)
- [ ] SpawnWarningEffect (enemy readability)

## Premium Polish

Nice to have if time/budget allows:
- [ ] GlowStreakEffect on bullets (needs prevX/prevY tracking)
- [ ] WaveStartEffect expanding ring
- [ ] RespawnFlashEffect aura pulse
- [ ] Bloom enabled on desktop (already wired; just flip `Settings.bloomEnabled = true`)
- [ ] Per-size explosion ring color (LARGE: gold, MEDIUM: orange, SMALL: white)
- [ ] Screen-edge parallax distortion on big explosions
- [ ] Subtle vignette during low-life state
