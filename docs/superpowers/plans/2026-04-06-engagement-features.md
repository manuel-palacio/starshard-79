# Engagement Features Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add three engagement features — streak score multiplier (up to 4×), persistent personal best, and a live wave/asteroid-count HUD indicator.

**Architecture:** `StreakSystem` is a new event-driven system alongside `CollisionSystem` and `WaveSystem`; it updates `world.scoreMultiplier` which `CollisionSystem` applies at score time. Personal best uses libGDX `Preferences` (cross-platform, no new interface). All HUD changes are isolated to `HudRenderer`.

**Tech Stack:** Kotlin, libGDX 1.12.1, JUnit 5, `GameEventBus` (existing pub/sub)

---

## File Map

| Action | Path | Responsibility |
|--------|------|----------------|
| Create | `core/src/main/kotlin/com/palacesoft/asteroids/game/system/StreakSystem.kt` | Streak counter, multiplier logic, GameEventBus subscriptions |
| Modify | `core/src/main/kotlin/com/palacesoft/asteroids/game/World.kt` | Add `scoreMultiplier`, `streakSystem`; wire update |
| Modify | `core/src/main/kotlin/com/palacesoft/asteroids/game/system/CollisionSystem.kt` | Multiply score by `world.scoreMultiplier` on kills |
| Modify | `core/src/main/kotlin/com/palacesoft/asteroids/util/Settings.kt` | Add `highScore` backed by libGDX `Preferences` |
| Modify | `core/src/main/kotlin/com/palacesoft/asteroids/screen/GameScreen.kt` | Save high score, pass `previousBest` to `GameOverScreen` |
| Modify | `core/src/main/kotlin/com/palacesoft/asteroids/screen/GameOverScreen.kt` | Accept `previousBest`, show "NEW BEST!" in gold |
| Modify | `core/src/main/kotlin/com/palacesoft/asteroids/render/HudRenderer.kt` | Add wave-count, personal-best, multiplier labels |
| Create | `core/src/test/kotlin/com/palacesoft/asteroids/game/system/StreakSystemTest.kt` | Unit tests for streak logic |

---

### Task 1: StreakSystem — core logic

**Files:**
- Create: `core/src/main/kotlin/com/palacesoft/asteroids/game/system/StreakSystem.kt`
- Modify: `core/src/main/kotlin/com/palacesoft/asteroids/game/World.kt`
- Create: `core/src/test/kotlin/com/palacesoft/asteroids/game/system/StreakSystemTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `core/src/test/kotlin/com/palacesoft/asteroids/game/system/StreakSystemTest.kt`:

```kotlin
package com.palacesoft.asteroids.game.system

import com.palacesoft.asteroids.events.GameEvent
import com.palacesoft.asteroids.events.GameEventBus
import com.palacesoft.asteroids.game.entity.AsteroidSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StreakSystemTest {

    private lateinit var streak: StreakSystem
    private var capturedMultiplier = 1

    @BeforeEach fun setup() {
        GameEventBus.clear()
        streak = StreakSystem { mult -> capturedMultiplier = mult }
        streak.subscribe()
    }

    @AfterEach fun teardown() {
        GameEventBus.clear()
    }

    @Test fun `initial multiplier is 1`() {
        assertEquals(1, streak.multiplier)
    }

    @Test fun `first kill does not increase multiplier`() {
        GameEventBus.emit(GameEvent.AsteroidDestroyed(0f, 0f, AsteroidSize.LARGE))
        assertEquals(1, streak.multiplier)
    }

    @Test fun `two rapid kills give 2x`() {
        GameEventBus.emit(GameEvent.AsteroidDestroyed(0f, 0f, AsteroidSize.LARGE))
        GameEventBus.emit(GameEvent.AsteroidDestroyed(0f, 0f, AsteroidSize.LARGE))
        assertEquals(2, streak.multiplier)
        assertEquals(2, capturedMultiplier)
    }

    @Test fun `saucer kill counts toward streak`() {
        GameEventBus.emit(GameEvent.AsteroidDestroyed(0f, 0f, AsteroidSize.LARGE))
        GameEventBus.emit(GameEvent.SaucerDestroyed(0f, 0f))
        assertEquals(2, streak.multiplier)
    }

    @Test fun `multiplier caps at 4`() {
        repeat(10) { GameEventBus.emit(GameEvent.AsteroidDestroyed(0f, 0f, AsteroidSize.LARGE)) }
        assertEquals(4, streak.multiplier)
    }

    @Test fun `PlayerHit resets multiplier to 1`() {
        repeat(3) { GameEventBus.emit(GameEvent.AsteroidDestroyed(0f, 0f, AsteroidSize.LARGE)) }
        GameEventBus.emit(GameEvent.PlayerHit(0f, 0f))
        assertEquals(1, streak.multiplier)
        assertEquals(1, capturedMultiplier)
    }

    @Test fun `streak timer expiry resets multiplier`() {
        GameEventBus.emit(GameEvent.AsteroidDestroyed(0f, 0f, AsteroidSize.LARGE))
        GameEventBus.emit(GameEvent.AsteroidDestroyed(0f, 0f, AsteroidSize.LARGE))
        assertEquals(2, streak.multiplier)
        streak.update(2.0f)   // past the 1.5s window
        assertEquals(1, streak.multiplier)
    }

    @Test fun `kill within window resets timer`() {
        GameEventBus.emit(GameEvent.AsteroidDestroyed(0f, 0f, AsteroidSize.LARGE))
        streak.update(1.0f)   // within window — still active
        GameEventBus.emit(GameEvent.AsteroidDestroyed(0f, 0f, AsteroidSize.LARGE))
        streak.update(1.0f)   // within new window from last kill
        assertEquals(2, streak.multiplier)
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
cd /Users/manuel.palacio/Code/asteroids && ./gradlew :core:test --tests "com.palacesoft.asteroids.game.system.StreakSystemTest" 2>&1 | tail -10
```

Expected: FAILED — `StreakSystem` not found.

- [ ] **Step 3: Create `StreakSystem.kt`**

Create `core/src/main/kotlin/com/palacesoft/asteroids/game/system/StreakSystem.kt`:

```kotlin
package com.palacesoft.asteroids.game.system

import com.palacesoft.asteroids.events.GameEvent
import com.palacesoft.asteroids.events.GameEventBus

/**
 * Tracks rapid-kill streaks and notifies caller of the current multiplier (1–4).
 * Multiplier increases with each kill within [STREAK_WINDOW] seconds.
 * Resets to 1 on PlayerHit or timeout.
 *
 * @param onMultiplierChanged called whenever the multiplier value changes.
 */
class StreakSystem(private val onMultiplierChanged: (Int) -> Unit) {

    var multiplier: Int = 1
        private set

    private var streakCount = 0
    private var streakTimer = 0f

    companion object {
        const val STREAK_WINDOW   = 1.5f
        const val MAX_MULTIPLIER  = 4
    }

    fun subscribe() {
        GameEventBus.subscribe { event ->
            when (event) {
                is GameEvent.AsteroidDestroyed -> onKill()
                is GameEvent.SaucerDestroyed   -> onKill()
                is GameEvent.PlayerHit         -> reset()
                else -> {}
            }
        }
    }

    fun update(delta: Float) {
        if (streakCount > 0) {
            streakTimer -= delta
            if (streakTimer <= 0f) reset()
        }
    }

    private fun onKill() {
        streakCount++
        streakTimer = STREAK_WINDOW
        val newMult = minOf(streakCount, MAX_MULTIPLIER)
        if (newMult != multiplier) {
            multiplier = newMult
            onMultiplierChanged(multiplier)
        }
    }

    private fun reset() {
        if (multiplier == 1 && streakCount == 0) return   // already reset
        streakCount = 0
        streakTimer = 0f
        multiplier  = 1
        onMultiplierChanged(1)
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
cd /Users/manuel.palacio/Code/asteroids && ./gradlew :core:test --tests "com.palacesoft.asteroids.game.system.StreakSystemTest" 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL, 8 tests passed.

- [ ] **Step 5: Add `scoreMultiplier` and `streakSystem` to `World`**

In `core/src/main/kotlin/com/palacesoft/asteroids/game/World.kt`:

Add two properties after `var waveMaxAsteroids`:
```kotlin
var scoreMultiplier: Int = 1
val streakSystem = StreakSystem { mult -> scoreMultiplier = mult }
```

Add `streakSystem.subscribe()` inside `World.start()` after `waveSystem.start()`:
```kotlin
fun start() {
    waveSystem.start()
    streakSystem.subscribe()
}
```

Add `streakSystem.update(delta)` inside `World.update()` before `collisionSystem.update()`:
```kotlin
fun update(delta: Float) {
    if (gameOver) return
    updateShip(delta)
    updateBullets(delta)
    updateAsteroids(delta)
    asteroids.removeAll { !it.alive }
    bullets.removeAll   { !it.alive }
    streakSystem.update(delta)
    collisionSystem.update()
    waveSystem.update(delta)
    val alive = asteroids.count { it.alive }
    if (alive > waveMaxAsteroids) waveMaxAsteroids = alive
    if (lives <= 0 && !ship.alive) gameOver = true
}
```

- [ ] **Step 6: Run full test suite**

```bash
cd /Users/manuel.palacio/Code/asteroids && ./gradlew :core:test 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 7: Commit**

```bash
cd /Users/manuel.palacio/Code/asteroids && git add \
  core/src/main/kotlin/com/palacesoft/asteroids/game/system/StreakSystem.kt \
  core/src/main/kotlin/com/palacesoft/asteroids/game/World.kt \
  core/src/test/kotlin/com/palacesoft/asteroids/game/system/StreakSystemTest.kt
git commit -m "feat: add StreakSystem with 1.5s window and 4x cap"
```

---

### Task 2: CollisionSystem — apply score multiplier

**Files:**
- Modify: `core/src/main/kotlin/com/palacesoft/asteroids/game/system/CollisionSystem.kt`

Current score lines in `checkBulletsVsAsteroids()` (lines 31, 39):
```kotlin
world.score += ast.size.score
GameEventBus.emit(GameEvent.ScoreAwarded(ast.x, ast.y, ast.size.score))
```

Current score lines in `checkBulletsVsSaucers()` (lines 55–56, 59):
```kotlin
val saucerScore = if (saucer.size == SaucerSize.LARGE) 200 else 1000
world.score += saucerScore
GameEventBus.emit(GameEvent.ScoreAwarded(saucer.x, saucer.y, saucerScore))
```

- [ ] **Step 1: Update `checkBulletsVsAsteroids`**

Replace the two score lines:
```kotlin
val points = ast.size.score * world.scoreMultiplier
world.score += points
```
And the ScoreAwarded emit:
```kotlin
GameEventBus.emit(GameEvent.ScoreAwarded(ast.x, ast.y, points))
```

- [ ] **Step 2: Update `checkBulletsVsSaucers`**

Replace the three score lines:
```kotlin
val saucerScore = (if (saucer.size == SaucerSize.LARGE) 200 else 1000) * world.scoreMultiplier
world.score += saucerScore
GameEventBus.emit(GameEvent.ScoreAwarded(saucer.x, saucer.y, saucerScore))
```

- [ ] **Step 3: Verify existing tests still pass**

```bash
cd /Users/manuel.palacio/Code/asteroids && ./gradlew :core:test --tests "com.palacesoft.asteroids.game.system.CollisionSystemTest" 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL (scoreMultiplier defaults to 1 so existing score values are unchanged).

- [ ] **Step 4: Run full test suite**

```bash
cd /Users/manuel.palacio/Code/asteroids && ./gradlew :core:test 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
cd /Users/manuel.palacio/Code/asteroids && git add core/src/main/kotlin/com/palacesoft/asteroids/game/system/CollisionSystem.kt
git commit -m "feat: multiply kill score by world.scoreMultiplier"
```

---

### Task 3: Personal Best

**Files:**
- Modify: `core/src/main/kotlin/com/palacesoft/asteroids/util/Settings.kt`
- Modify: `core/src/main/kotlin/com/palacesoft/asteroids/screen/GameScreen.kt`
- Modify: `core/src/main/kotlin/com/palacesoft/asteroids/screen/GameOverScreen.kt`

- [ ] **Step 1: Add `highScore` to `Settings`**

In `core/src/main/kotlin/com/palacesoft/asteroids/util/Settings.kt`, add the import at the top:
```kotlin
import com.badlogic.gdx.Gdx
```

Add the property after `var sfxEnabled`:
```kotlin
var highScore: Int
    get() = Gdx.app?.getPreferences("asteroids")?.getInteger("highScore", 0) ?: 0
    set(value) {
        val prefs = Gdx.app?.getPreferences("asteroids") ?: return
        prefs.putInteger("highScore", value)
        prefs.flush()
    }
```

The null-safe `?.` means this is safe in tests where `Gdx.app` is not initialised.

- [ ] **Step 2: Update `GameScreen` to save high score and pass it to `GameOverScreen`**

In `core/src/main/kotlin/com/palacesoft/asteroids/screen/GameScreen.kt`, replace the game-over block (lines 48–52):

```kotlin
if (world.gameOver && !gameOverHandled) {
    gameOverHandled = true
    game.gameServices?.submitScore(world.score)
    val previousBest = Settings.highScore
    if (world.score > previousBest) Settings.highScore = world.score
    game.setScreen(GameOverScreen(game, world.score, previousBest))
}
```

Add the import at the top of the file:
```kotlin
import com.palacesoft.asteroids.util.Settings
```

- [ ] **Step 3: Update `GameOverScreen` to accept and display personal best**

Replace the entire `core/src/main/kotlin/com/palacesoft/asteroids/screen/GameOverScreen.kt`:

```kotlin
package com.palacesoft.asteroids.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.palacesoft.asteroids.AsteroidsGame
import com.palacesoft.asteroids.render.Starfield
import com.palacesoft.asteroids.util.Settings

class GameOverScreen(
    private val game: AsteroidsGame,
    private val finalScore: Int,
    private val previousBest: Int = 0
) : Screen {

    private val isNewBest  = finalScore > previousBest
    private val bestScore  = Settings.highScore   // already updated by GameScreen

    private val starfield  = Starfield()
    private val titleFont  = BitmapFont().apply { data.setScale(3.5f); color = Color.RED }
    private val scoreFont  = BitmapFont().apply { data.setScale(2f);   color = Color.WHITE }
    private val subFont    = BitmapFont().apply { data.setScale(1.5f); color = Color.GRAY }
    private val lbFont     = BitmapFont().apply { data.setScale(1.5f); color = Color.CYAN }
    private val bestFont   = BitmapFont().apply {
        data.setScale(1.8f)
        color = if (isNewBest) Color.GOLD else Color.LIGHT_GRAY
    }

    private val LB_ZONE_THRESHOLD = 0.33f

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0.03f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        game.sr.projectionMatrix = game.camera.combined
        starfield.update(delta, 0f, 0f)
        starfield.render(game.sr)
        game.batch.projectionMatrix = game.camera.combined
        game.batch.begin()

        titleFont.draw(game.batch, "GAME OVER",
                       Settings.WORLD_WIDTH / 2f - 200f, Settings.WORLD_HEIGHT / 2f + 80f)
        scoreFont.draw(game.batch, "SCORE  $finalScore",
                       Settings.WORLD_WIDTH / 2f - 100f, Settings.WORLD_HEIGHT / 2f)

        val bestText = if (isNewBest) "NEW BEST!  $bestScore" else "BEST  $bestScore"
        bestFont.draw(game.batch, bestText,
                      Settings.WORLD_WIDTH / 2f - 120f, Settings.WORLD_HEIGHT / 2f - 60f)

        subFont.draw(game.batch, "PRESS SPACE OR TAP TO RETRY",
                     Settings.WORLD_WIDTH / 2f - 200f, Settings.WORLD_HEIGHT / 2f - 120f)
        if (game.gameServices != null) {
            lbFont.draw(game.batch, "LEADERBOARD",
                        Settings.WORLD_WIDTH / 2f - 100f, Settings.WORLD_HEIGHT / 2f - 180f)
        }
        game.batch.end()

        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE)) {
            game.setScreen(GameScreen(game))
            return
        }
        if (Gdx.input.justTouched()) {
            val normY = Gdx.input.y.toFloat() / Gdx.graphics.height.toFloat()
            if (game.gameServices != null && normY > (1f - LB_ZONE_THRESHOLD)) {
                game.gameServices?.showLeaderboard()
            } else {
                game.setScreen(GameScreen(game))
            }
        }
    }

    override fun resize(w: Int, h: Int) {}
    override fun show()   {}
    override fun hide()   {}
    override fun pause()  {}
    override fun resume() {}
    override fun dispose() {
        titleFont.dispose(); scoreFont.dispose(); subFont.dispose()
        lbFont.dispose(); bestFont.dispose()
    }
}
```

- [ ] **Step 4: Verify build**

```bash
cd /Users/manuel.palacio/Code/asteroids && ./gradlew :core:compileKotlin 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Run full test suite**

```bash
cd /Users/manuel.palacio/Code/asteroids && ./gradlew :core:test 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
cd /Users/manuel.palacio/Code/asteroids && git add \
  core/src/main/kotlin/com/palacesoft/asteroids/util/Settings.kt \
  core/src/main/kotlin/com/palacesoft/asteroids/screen/GameScreen.kt \
  core/src/main/kotlin/com/palacesoft/asteroids/screen/GameOverScreen.kt
git commit -m "feat: persist and display personal best score"
```

---

### Task 4: HudRenderer — wave count, personal best, multiplier labels

**Files:**
- Modify: `core/src/main/kotlin/com/palacesoft/asteroids/render/HudRenderer.kt`

The current `HudRenderer` has:
- `scoreLabel` at `(20f, WORLD_HEIGHT - 40f)` — top-left
- `livesLabel` at `(WORLD_WIDTH - 200f, WORLD_HEIGHT - 40f)` — top-right
- `waveLabel` — centred, mid-screen flash banner (unchanged)
- `hintLabel` — tutorial hint (unchanged)

New labels to add:
- `bestLabel` — top-centre `(0f, WORLD_HEIGHT - 40f)`, full-width, centred align, scale 1.6f, dim grey
- `waveCountLabel` — top-centre `(0f, WORLD_HEIGHT - 78f)`, full-width, centred align, scale 1.4f, white alpha 0.7
- `multiplierLabel` — near score `(160f, WORLD_HEIGHT - 40f)`, scale 2.2f, YELLOW, hidden when multiplier = 1

- [ ] **Step 1: Replace `HudRenderer.kt` with the updated version**

```kotlin
package com.palacesoft.asteroids.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
import com.palacesoft.asteroids.game.World
import com.palacesoft.asteroids.util.Settings

class HudRenderer(batch: SpriteBatch, camera: OrthographicCamera) {
    private val viewport = FitViewport(Settings.WORLD_WIDTH, Settings.WORLD_HEIGHT, camera)
    val stage = Stage(viewport, batch)

    private val font          = BitmapFont().apply { data.setScale(2f);   color = Color.CYAN }
    private val waveFont      = BitmapFont().apply { data.setScale(1.8f) }
    private val popupFont     = BitmapFont().apply { data.setScale(1.5f) }
    private val bestHudFont   = BitmapFont().apply { data.setScale(1.6f); color = Color(0.7f, 0.7f, 0.7f, 1f) }
    private val waveCountFont = BitmapFont().apply { data.setScale(1.4f); color = Color.WHITE }
    private val multFont      = BitmapFont().apply { data.setScale(2.2f); color = Color.YELLOW }

    private val scoreStyle     = Label.LabelStyle(font, Color.CYAN)
    private val waveStyle      = Label.LabelStyle(waveFont, Color.WHITE)
    private val popupStyle     = Label.LabelStyle(popupFont, Color.YELLOW)
    private val bestHudStyle   = Label.LabelStyle(bestHudFont, Color(0.7f, 0.7f, 0.7f, 1f))
    private val waveCountStyle = Label.LabelStyle(waveCountFont, Color.WHITE)
    private val multStyle      = Label.LabelStyle(multFont, Color.YELLOW)

    private val scoreLabel = Label("0", scoreStyle).apply {
        setPosition(20f, Settings.WORLD_HEIGHT - 40f)
    }
    private val livesLabel = Label("", scoreStyle).apply {
        setPosition(Settings.WORLD_WIDTH - 200f, Settings.WORLD_HEIGHT - 40f)
    }
    private val waveLabel = Label("", waveStyle).apply {
        setWidth(Settings.WORLD_WIDTH)
        setPosition(0f, Settings.WORLD_HEIGHT / 2f + 60f)
        setAlignment(Align.center)
        color.a = 0f
    }
    private val bestLabel = Label("", bestHudStyle).apply {
        setWidth(Settings.WORLD_WIDTH)
        setPosition(0f, Settings.WORLD_HEIGHT - 40f)
        setAlignment(Align.center)
    }
    private val waveCountLabel = Label("", waveCountStyle).apply {
        setWidth(Settings.WORLD_WIDTH)
        setPosition(0f, Settings.WORLD_HEIGHT - 78f)
        setAlignment(Align.center)
        color.a = 0.7f
    }
    private val multiplierLabel = Label("", multStyle).apply {
        setPosition(160f, Settings.WORLD_HEIGHT - 40f)
        color.a = 0f
    }

    // Tutorial hints
    private val tutorialHintStyle = Label.LabelStyle(BitmapFont().apply { data.setScale(1.6f) }, Color.WHITE)
    private val hintLabel = Label("", tutorialHintStyle).apply {
        setWidth(Settings.WORLD_WIDTH)
        setPosition(0f, Settings.WORLD_HEIGHT * 0.18f)
        setAlignment(Align.center)
        color.a = 0f
    }
    private var hintShown = false

    // Dirty-check caches
    private var lastScore         = -1
    private var lastLives         = -1
    private var lastWave          = -1
    private var lastHighScore     = -1
    private var lastWaveForCount  = -1
    private var lastAsteroidCount = -1
    private var lastMultiplier    = -1

    init {
        stage.addActor(scoreLabel)
        stage.addActor(livesLabel)
        stage.addActor(waveLabel)
        stage.addActor(bestLabel)
        stage.addActor(waveCountLabel)
        stage.addActor(multiplierLabel)
        stage.addActor(hintLabel)
    }

    fun render(world: World) {
        // Score
        if (world.score != lastScore) {
            val delta = world.score - lastScore
            lastScore = world.score
            scoreLabel.setText(world.score.toString())
            if (delta > 0) spawnScorePopup(delta)
        }
        // Lives
        if (world.lives != lastLives) {
            lastLives = world.lives
            livesLabel.setText("△ ".repeat(world.lives.coerceAtLeast(0)))
        }
        // Personal best (top-centre, dim)
        val best = Settings.highScore
        if (best != lastHighScore) {
            lastHighScore = best
            if (best > 0) bestLabel.setText("BEST  $best")
        }
        // Wave + asteroid count (below best)
        val liveCount = world.asteroids.count { it.alive }
        if (world.wave != lastWaveForCount || liveCount != lastAsteroidCount) {
            lastWaveForCount  = world.wave
            lastAsteroidCount = liveCount
            if (world.wave > 0) waveCountLabel.setText("WAVE ${world.wave}  ·  $liveCount LEFT")
        }
        // Streak multiplier
        val mult = world.streakSystem.multiplier
        if (mult != lastMultiplier) {
            lastMultiplier = mult
            multiplierLabel.clearActions()
            if (mult > 1) {
                multiplierLabel.setText("${mult}×")
                multiplierLabel.color.a = 1f
            } else {
                multiplierLabel.addAction(Actions.fadeOut(0.5f))
            }
        }
        // Wave banner flash
        if (world.wave != lastWave && world.wave > 0) {
            lastWave = world.wave
            waveLabel.setText("WAVE ${world.wave}")
            waveLabel.clearActions()
            waveLabel.color.a = 0f
            waveLabel.addAction(Actions.sequence(
                Actions.fadeIn(0.4f),
                Actions.delay(2f),
                Actions.fadeOut(0.6f)
            ))
        }
        // Tutorial hint
        if (!Settings.tutorialCompleted && world.wave == 1 && !hintShown) {
            hintShown = true
            hintLabel.setText("◁ ROTATE  ▲ THRUST  ▷ ROTATE     FIRE     ★ HYPERSPACE")
            hintLabel.clearActions()
            hintLabel.color.a = 0f
            hintLabel.addAction(Actions.sequence(
                Actions.delay(1.0f),
                Actions.fadeIn(0.5f),
                Actions.delay(4.0f),
                Actions.fadeOut(0.8f)
            ))
        }

        stage.act()
        stage.draw()
    }

    private fun spawnScorePopup(delta: Int) {
        val popup = Label("+$delta", popupStyle)
        popup.setPosition(scoreLabel.x + 120f, scoreLabel.y)
        popup.addAction(Actions.sequence(
            Actions.parallel(
                Actions.moveBy(0f, 60f, 1f),
                Actions.fadeOut(1f)
            ),
            Actions.removeActor()
        ))
        stage.addActor(popup)
    }

    fun resize(width: Int, height: Int) = viewport.update(width, height, true)

    fun dispose() {
        stage.dispose()
        font.dispose()
        waveFont.dispose()
        popupFont.dispose()
        bestHudFont.dispose()
        waveCountFont.dispose()
        multFont.dispose()
        tutorialHintStyle.font.dispose()
    }
}
```

- [ ] **Step 2: Verify build**

```bash
cd /Users/manuel.palacio/Code/asteroids && ./gradlew :core:compileKotlin 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Run full test suite**

```bash
cd /Users/manuel.palacio/Code/asteroids && ./gradlew :core:test 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Build Android APK**

```bash
cd /Users/manuel.palacio/Code/asteroids && ./gradlew :android:assembleDebug 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
cd /Users/manuel.palacio/Code/asteroids && git add core/src/main/kotlin/com/palacesoft/asteroids/render/HudRenderer.kt
git commit -m "feat: add wave count, personal best, and streak multiplier to HUD"
```

- [ ] **Step 6: Push**

```bash
cd /Users/manuel.palacio/Code/asteroids && git push
```
