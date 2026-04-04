# Asteroids

A faithful recreation of the 1979 Asteroids arcade game for Android and desktop, built with libGDX and Kotlin.

The core grammar of play is preserved: inertia-based movement, wraparound space, asteroid splitting into faster fragments, and rising tension through pacing and enemy pressure. Modern additions are limited to polish, touch usability, and performance scaling — the verb set and feel of the original are untouched.

![Demo](astorids-demo.png)

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | libGDX 1.12.1 |
| Language | Kotlin 1.9.22 |
| Build | Gradle 8 (Kotlin DSL) |
| Targets | Android (minSdk 24), Desktop (LWJGL3) |
| Rendering | ShapeRenderer (vector geometry) + SpriteBatch (particle overlays) |
| Audio | Procedurally synthesised WAV (no asset files) |

## Project Structure

```
core/       Game logic and rendering — platform-agnostic
desktop/    LWJGL3 launcher (1600×900, 60fps, HIGH quality)
android/    Android launcher (MEDIUM quality default)
assets/     Particle stub files (.p) for Particle Editor tuning
docs/       Architecture specs and implementation plans
```

## Architecture

### Event-Driven FX

Gameplay code emits events via `GameEventBus`; the `VfxManager` subscribes and handles all visual effects. Nothing in `World`, `CollisionSystem`, or `WaveSystem` references VFX directly.

```
CollisionSystem → GameEventBus.emit(AsteroidDestroyed) → VfxManager → ExplosionEffect + shake
WaveSystem      → GameEventBus.emit(WaveStarted)       → VfxManager → WaveStartEffect (HIGH only)
World           → GameEventBus.emit(BulletFired)       → VfxManager → MuzzleFlashEffect
```

### Effect Quality Ladder

Three quality levels control render cost and visual style. MEDIUM is the Android default; HIGH is the desktop default. LOW is for weak devices or power-save mode.

| Setting | LOW | MEDIUM | HIGH |
|---|---|---|---|
| Procedural ShapeRenderer effects | ✓ | ✓ | ✓ |
| Particle overlays (glow, sparks) | — | major events | all events |
| Bloom post-processing | — | — | ✓ |
| Camera shake multiplier | 0.45× | 0.75× | 1.0× |
| Score popups over playfield | — | — | ✓ |
| Wave-start ring effect | — | — | ✓ |

At every quality level the classic vector silhouette is preserved. Particles are additive overlays on top of line art — they never replace it.

### Render Pipeline

```
clear → starfield
     → [optional: beginBloomCapture]
         drawEmissive (FBO copy for bloom source)
         renderBloomedEffects (muzzle flash, glow streaks, respawn aura)
     → [optional: endCapture + bloomRender]
     → renderUnbloomedEffects (explosions, sparks, wave ring, spawn warning)
     → renderThrustParticles
     → renderParticleOverlays (SpriteBatch, additive)
     → renderTextEffects (score popups — HIGH only)
     → HUD (separate camera, no shake offset)
```

### Camera Shake

Trauma² model: `magnitude = trauma²` for punchy onset with smooth tail. Two incommensurable sine oscillators (37 Hz / 41 Hz) produce non-repeating Lissajous motion. HUD camera never receives the offset.

### Audio

All sounds are synthesised at startup from first principles (sine, sawtooth, square waves, Gaussian noise, low-pass filter). No audio asset files required. Sounds include: laser fire, asteroid bangs (large/medium/small), ship death, heartbeat pulse, thrust loop, saucer warble, saucer fire, extra life chime.

The heartbeat tempo scales with danger: 1.0 s interval when the wave is full → 0.28 s when hunting the last asteroid.

## Running

**Desktop:**
```bash
./gradlew :desktop:run
```

**Android:**
```bash
./gradlew :android:installDebug
```

## Controls

### Desktop

| Key | Action |
|---|---|
| Left / Right arrow | Rotate |
| Up arrow | Thrust |
| Space | Fire |
| Shift | Hyperspace |

### Android

Dual-zone touch layout. Left thumb: rotate and thrust. Right thumb: fire and hyperspace.

## Design Philosophy

> Modernise the surface, not the grammar of play.

**Preserved from 1979:**
- Inertia-based ship movement (rotation + thrust, no direct steering)
- Wraparound playfield
- Asteroid splitting (LARGE → 2 MEDIUM → 2 SMALL)
- Vector silhouette visual identity
- Minimalist tension-based audio

**Modern additions:**
- Touch-first controls
- Effect quality scaling (LOW / MEDIUM / HIGH)
- Camera shake and particle polish
- Procedurally synthesised audio (no asset dependencies)
- Stable Android performance (pre-allocated pools, no per-frame GC)

## Particle Assets

The `.p` stub files in `assets/particles/` are placeholders for tuning in the [libGDX Particle Editor](https://libgdx.com/wiki/tools/2d-particle-editor). They require a `particle.png` texture atlas — see `assets/particles/textures/README.txt`. Effects degrade gracefully if assets are missing (particles are an additive overlay, not the primary visual).
