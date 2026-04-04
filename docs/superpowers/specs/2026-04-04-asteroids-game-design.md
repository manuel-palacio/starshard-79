# Asteroids Game — Design Spec
**Date:** 2026-04-04
**Status:** Approved

---

## Overview

A flashy, neon-arcade Asteroids clone built with libGDX in Kotlin. Targets Android and desktop (iOS deferred). Uses procedural vector rendering (no sprites), a lightweight manual game-object architecture, and a layered VFX pipeline to achieve a premium arcade feel.

---

## Decisions Made

| Question | Decision |
|----------|----------|
| iOS support | Deferred — Android + desktop only for now |
| Visual style | Procedural vector (ShapeRenderer / custom mesh, no art assets) |
| Architecture style | Screen-as-orchestrator + plain Kotlin game objects |
| ECS approach | Lightweight manual (no Ashley) |
| HUD framework | Scene2D Stage (HUD/menus only) |

---

## Section 1: Project Structure

```
asteroids/
├── core/                          # Shared game logic (Kotlin)
│   └── src/main/kotlin/com/asteroids/
│       ├── AsteroidsGame.kt       # ApplicationListener, screen stack
│       ├── screen/
│       │   ├── GameScreen.kt      # Main game loop
│       │   ├── MenuScreen.kt
│       │   └── GameOverScreen.kt
│       ├── game/
│       │   ├── World.kt           # Owns all game objects + systems
│       │   ├── entity/
│       │   │   ├── Ship.kt
│       │   │   ├── Asteroid.kt          # includes AsteroidFactory companion object
│       │   │   ├── Bullet.kt
│       │   │   └── Saucer.kt
│       │   └── system/
│       │       ├── CollisionSystem.kt
│       │       ├── WaveSystem.kt        # handles wave progression + asteroid spawning
│       │       └── BulletPool.kt
│       ├── render/
│       │   ├── GameRenderer.kt    # Orchestrates all render passes
│       │   ├── ShipRenderer.kt
│       │   ├── AsteroidRenderer.kt
│       │   └── HudRenderer.kt     # Scene2D stage for HUD
│       ├── vfx/
│       │   ├── VfxManager.kt      # Central VFX coordinator
│       │   ├── ParticlePool.kt
│       │   ├── Explosion.kt
│       │   ├── ThrustTrail.kt
│       │   ├── BloomPass.kt       # FBO-based bloom
│       │   └── ScreenShake.kt
│       └── input/
│           ├── InputHandler.kt    # Routes keyboard + touch
│           └── TouchControls.kt   # Virtual joystick + buttons
├── android/                       # Android launcher
├── desktop/                       # Desktop launcher (dev/testing)
└── assets/                        # Shared assets (audio, fonts)
    ├── audio/                     # SFX stubs
    └── fonts/                     # Bitmap font for HUD
```

---

## Section 2: Core Game Architecture

### `AsteroidsGame`
Extends libGDX `Game`. Holds shared resources: `SpriteBatch`, `ShapeRenderer`, `AssetManager`. Manages a screen stack (push/pop for menu → game → game over flow).

### `World`
Single source of truth for all game state. Owns:
- `val ship: Ship`
- `val asteroids: MutableList<Asteroid>`
- `val bullets: MutableList<Bullet>` (managed via `BulletPool`)
- `val saucers: MutableList<Saucer>`
- `val particles: ParticlePool`
- `var score: Int`, `var lives: Int`, `var wave: Int`

`World.update(delta)` calls systems in order:
1. Input → ship velocity
2. Entity movement + screen wrap
3. Collision detection
4. Bullet pool recycle
5. Wave progression
6. VFX updates

### `GameScreen`
Owns `World`, `GameRenderer`, `VfxManager`, `InputHandler`. Its `render(delta)` calls `world.update(delta)` then `renderer.render(world, delta)`. Contains no game logic — purely coordinates the loop and rendering stack.

### Game Objects
Plain Kotlin classes with mutable state. Common fields: `position: Vector2`, `velocity: Vector2`, `rotation: Float`, `radius: Float`, `alive: Boolean`. Asteroid also carries `size: AsteroidSize` (LARGE / MEDIUM / SMALL).

---

## Section 3: Rendering Pipeline

`GameRenderer` executes four explicit passes each frame:

### Pass 1 — Background (`ShapeRenderer`)
~150 stars stored as `Vector2` array. Two parallax layers at different drift speeds. Per-star brightness oscillation for twinkling.

### Pass 2 — Geometry (`ShapeRenderer`)
Entities drawn as `GL_LINE_LOOP` / `GL_LINES` outlines. Neon colour palette:
- Ship = cyan
- Bullets = yellow
- Asteroids = white
- Saucer = magenta

### Pass 3 — Bloom / Glow (FBO)
Emissive geometry (ship engine, bullets, saucer) rendered into half-resolution `FrameBuffer`. Two-pass separable Gaussian blur (9-tap horizontal + vertical). Additively blended back onto screen. Disabled via `Settings.bloomEnabled = false` on low-end devices.

### Pass 4 — HUD (Scene2D `Stage`)
Score (top-left), lives as ship icons (top-right), wave banner (fade-in/out). Score popups as flying Scene2D `Label` actions. Drawn after bloom — never blurred.

**Camera shake** applied to `OrthographicCamera` before passes 2–3, reset before pass 4 so HUD stays stable.

---

## Section 4: VFX System

### `VfxManager`
Single entry point for all effects:
```kotlin
vfxManager.spawnExplosion(position, size)
vfxManager.spawnDebris(position, velocity)
vfxManager.spawnHitFlash(entity)
vfxManager.triggerScreenShake(intensity)
```

### `ParticlePool`
Fixed array of ~400 `Particle` objects, pre-allocated. Fields: `position`, `velocity`, `life`, `maxLife`, `color`, `size`. Dead particles recycled from pool head. Exceeding pool capacity silently drops new particles — no GC pressure.

### Explosion
On asteroid death: 8–16 particles radiating outward. Speed proportional to asteroid size. Colour: white → orange → transparent over ~0.6s. LARGE asteroids also spawn 3–4 slow tumbling debris line-segments.

### ThrustTrail
While thrust held: 2–3 particles/frame from ship exhaust, ±spread, life ~0.2s, blue-white colour.

### ScreenShake
Trauma-based: `camera.offset = sin(time * frequency) * trauma² * maxOffset`. `trauma` set on hit, decays over time. Squaring trauma makes subtle shakes feel small and violent shakes feel violent.

### BloomPass
Two half-resolution `FrameBuffer`s. Draw emissive → blur H → blur V → additive blend. Skipped entirely when `Settings.bloomEnabled = false`.

---

## Section 5: Input & Touch Controls

### `GameInput` state object
```kotlin
data class GameInput(
    var rotateLeft: Boolean,
    var rotateRight: Boolean,
    var thrust: Boolean,
    var fire: Boolean,
    var hyperspace: Boolean
)
```
Systems read from this — never touch libGDX input directly.

### Keyboard
`Gdx.input.isKeyPressed` — WASD + space and arrow keys, both supported simultaneously. Desktop testing only.

### `TouchControls`
- **Left half of screen** — virtual joystick (horizontal drag → rotate). Translucent circle + inner dot.
- **Right half, lower zone** — fire (tap + hold, auto-repeats at bullet rate cap).
- **Right half, upper zone** — hyperspace (tap only).

Rendered at ~30% opacity via `ShapeRenderer` after HUD pass. Touch routed to game controls first; Scene2D HUD stage only active on menu screens.

**Android:** `Gdx.input.setCatchBackKey(true)` prevents accidental exit.

---

## Section 6: Gameplay Systems

### `BulletPool`
Pre-allocated array of 20 `Bullet` objects. Max 4 active at once. Bullets die after travelling ~80% of screen width. Spawn at ship nose, velocity = ship direction × bulletSpeed + ship velocity.

### Asteroid Splitting
`AsteroidFactory.split(asteroid)` (companion object on `Asteroid`) on collision:
- LARGE → 2× MEDIUM, +20 score
- MEDIUM → 2× SMALL, +50 score
- SMALL → destroyed, +100 score

New asteroids receive randomised velocity offset from parent. If spawned within ship safe radius, nudged outward.

### `CollisionSystem`
Circle-circle overlap test each frame. Checks: bullet↔asteroid, bullet↔saucer, ship↔asteroid, ship↔saucer, ship↔saucer-bullet. No spatial partitioning — max ~30 entities. Runs after movement, before VFX.

### `SaucerAI`
`Saucer.shoot(target: Vector2)`: fires toward ship with ±15° random spread. Large saucer: random direction. Small saucer: accurate aim. Saucers enter from random screen edge; spawn timer managed by `WaveSystem`.

### `WaveSystem`
Tracks `asteroidsRemaining`. On reaching 0: 3-second pause, then spawn `wave * 2 + 2` large asteroids at screen edges (outside ship safe radius). Increments wave counter. Resets saucer spawn timer.

### Respawn Invulnerability
`ship.invulnerableTimer: Float` set to 3.0s on spawn. `CollisionSystem` skips ship checks while > 0. Ship flickers (visible/invisible every 0.1s) as visual cue.

---

## Performance Notes

- Bloom FBO at half resolution keeps fill-rate cost low on mobile.
- Particle pool pre-allocated; zero GC during play.
- `ShapeRenderer` batches geometry per pass — avoid switching between `SpriteBatch` and `ShapeRenderer` mid-pass.
- Target 60fps on mid-range Android (Snapdragon 6xx). Bloom can be disabled on low-end.
- No Box2D — circle math is cheaper and sufficient.

---

## Android/iOS Packaging Checklist

### Android
- [ ] Set `targetSdk` and `compileSdk` to current (API 34+)
- [ ] Configure `proguard-rules.pro` for libGDX (keep reflection-used classes)
- [ ] Set `android:screenOrientation="landscape"` in `AndroidManifest.xml`
- [ ] Enable hardware acceleration in manifest
- [ ] Test on physical device — emulator GPU varies wildly
- [ ] Profile with Android Studio's GPU debugger for draw call count
- [ ] Set `Gdx.graphics.setVSync(false)` on Android (handled by surface)

### Desktop (dev)
- [ ] LWJGL3 backend via `desktop/` module
- [ ] Fixed window size matching target aspect ratio (e.g. 1280×720)

### iOS (deferred)
- [ ] Add MobiVM (RoboVM fork) Gradle plugin when ready
- [ ] Add `ios/` module to `settings.gradle`
- [ ] Test Metal vs OpenGL ES backend — Metal preferred on iOS 13+
- [ ] Sign with Apple Developer certificate before TestFlight

### Performance Tuning
- [ ] Run with `Gdx.app.setLogLevel(Application.LOG_DEBUG)` during dev, remove before release
- [ ] Profile particle pool utilisation — tune pool size to actual peak usage
- [ ] Use `GLProfiler` to catch redundant state changes
- [ ] Consider `SpriteBatch` for any future sprite additions (don't mix batch types per pass)
- [ ] Bloom: benchmark with/without on lowest target device; gate behind settings
