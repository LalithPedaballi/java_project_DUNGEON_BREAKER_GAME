# DUNGEON BREAKER

A cinematic 2D dark-fantasy fighting and dungeon crawl game built from scratch in pure **Java 17** using **Swing** and **Java2D**. Featuring an expansive six-hero roster, real-time physics and combat simulation, atmospheric multi-stage dungeon encounters, and multi-phase boss battles against **The Rift Warden**.

Zero external frameworks, zero third-party libraries, and zero build tool bloat — implemented strictly using the standard Java 17 runtime.

---

## 🎮 Key Features

- **Six Distinct Playable Champions**:
  - **Aegis** (The Iron Vanguard): High armor, defensive shield guards, and devastating shield bashes.
  - **Nyx** (The Shadow Assassin): Agile rogue with poisoned dual daggers and lethal low-health execution strikes.
  - **Zephyra** (The Arcane Weaver): Ranged sorceress with piercing arcane novas and floating mystic shard barriers.
  - **Kael** (The Spellsword Duelist): High-mobility duelist combining rapid thrusts with elemental flame trails.
  - **Vexara** (The Soul Reaper): Dark scythe wielder siphoning enemy life essence with every reap.
  - **Draven** (The Magma Berserker): Unstoppable powerhouse triggering magma eruptions and earthquake slams.

- **Real-Time Combat & Physics Engine**:
  - Continuous movement, jump trajectories with simulated gravity, and directional dash evasion with invulnerability frames (i-frames).
  - Axis-Aligned Bounding Box (AABB) hitboxes aligned with weapon sweeps and character facing direction.
  - Attack windup, recovery frames, knockback impulses, hit-stop, and dynamic screen shakes.

- **Atmospheric Multi-Stage Dungeon Crawl**:
  - **Entrance**: Atmospheric dungeon infiltration.
  - **Encounter 1 (Void Patrol)**: Fight through approaching void thralls and skirmishers.
  - **Encounter 2 (Shadow Guard)**: Confront coordinated melee squads and ranged rift casters.
  - **Boss Gate & Entrance**: Unseal the ancient rift threshold.
  - **The Rift Warden Boss Duel**: Multi-phase boss battle with telegraphed cleaves, homing void bolts, ground shockwaves, and phase cataclysms.

- **Pure Java2D Visual Presentation**:
  - High-resolution 16:10 arena backdrop (ruined gothic architecture, purple dimensional rift, flaming torches).
  - Multi-frame sprite animation cycles (idle, walk, jump, dash, 3-hit attack strings, hurt, defeat).
  - Dynamic visual effects: hit sparks, particle bursts, ground contact shadows, and glowing energy auras.

---

## 🕹️ Controls & Keybindings

All inputs are registered globally using Swing window-level input maps (`WHEN_IN_FOCUSED_WINDOW`) and responsive mouse handlers.

| Key / Input | Action | Description |
| :--- | :--- | :--- |
| **`A` / `D`** *(or Left/Right Arrows)* | **Move Left / Right** | Continuous horizontal movement clamped to arena boundaries. |
| **`W` / `Space`** *(or Up Arrow)* | **Jump** | Vertical leap with gravity simulation. Avoid ground shockwaves! |
| **`J`** *(or **Left Mouse Click**)* | **Primary Attack** | 3-hit weapon combo with active hitboxes and directional reach. |
| **`K`** *(or **Right Mouse Click**)* | **Special Move** | Class-specific ultimate ability gated by cooldown / power meter. |
| **`Shift`** | **Dash / Evade** | High-velocity evasive burst with invulnerability frames (i-frames). |
| **`ESC`** | **Pause / Resume** | Freezes active combat simulation and physics without losing state. |
| **`R`** | **Restart** | Reinitializes the encounter with full HP and reset positions. |

---

## 💻 System Requirements

- **Java Development Kit (JDK) 17 or higher** (`java -version` to verify)
- Standard Java GUI libraries (`java.awt`, `javax.swing`) included with standard JDK distributions.

---

## 🚀 How to Compile and Run

### macOS & Linux
```bash
# 1. Clean build directory and compile all Java sources:
rm -rf out && mkdir -p out
javac -d out $(find src test -name "*.java")

# 2. Launch the game:
java -cp out com.riftarena.main.GameApp
```

### Windows (PowerShell)
```powershell
# 1. Clean build directory and compile all Java sources:
if (Test-Path out) { Remove-Item -Recurse -Force out }
New-Item -ItemType Directory -Path out
javac -d out (Get-ChildItem -Recurse src,test -Filter *.java).FullName

# 2. Launch the game:
java -cp out com.riftarena.main.GameApp
```

---

## 🧪 Running the Automated Test Suites

The project includes **120 comprehensive automated unit, physics, and integration tests** across **8 test suites**:

```bash
# Suite 1: Core Combat Engine & Turn-Based Rules (31 tests)
java -cp out com.riftarena.game.CombatEngineTest

# Suite 2: Six-Hero Roster & Enemy Asset Integrity (9 tests)
java -cp out com.riftarena.game.SixHeroRosterAndEnemiesTest

# Suite 3: Real-Time Combat & Physics Engine (17 tests)
java -cp out com.riftarena.realtime.RealTimeCombatTest

# Suite 4: Dungeon Progression & Stage Transitions (26 tests)
java -cp out com.riftarena.realtime.DungeonProgressionTest

# Suite 5: Swing UI Integration & Battle State Wiring (13 tests)
java -cp out com.riftarena.ui.UIIntegrationTest

# Suite 6: Visual Presentation & Java2D Renderers (9 tests)
java -cp out com.riftarena.ui.VisualPresentationTest

# Suite 7: Live Gameplay Verification & Controls Flow (10 tests)
java -cp out com.riftarena.ui.LiveGameplayVerificationTest

# Suite 8: Main Menu Navigation & Layout Bounds (5 tests)
java -cp out com.riftarena.ui.MenuPanelTest
```

### Run All 8 Suites in One Command
```bash
java -cp out com.riftarena.game.CombatEngineTest && \
java -cp out com.riftarena.game.SixHeroRosterAndEnemiesTest && \
java -cp out com.riftarena.realtime.RealTimeCombatTest && \
java -cp out com.riftarena.realtime.DungeonProgressionTest && \
java -cp out com.riftarena.ui.UIIntegrationTest && \
java -cp out com.riftarena.ui.VisualPresentationTest && \
java -cp out com.riftarena.ui.LiveGameplayVerificationTest && \
java -cp out com.riftarena.ui.MenuPanelTest
```

---

## 📂 Project Architecture

```
java project/
├── assets/                     # Runtime graphical game assets
│   ├── arena/                  # Clean high-definition arena backgrounds
│   ├── enemies/                # Cutout artwork for monsters and bosses
│   ├── heroes/                 # 12-frame animation sets for all 6 champions
│   └── menu/                   # High-definition main menu background
├── screenshots/                # 18 official showcase screenshots
├── src/                        # Game source code
│   └── com/riftarena/
│       ├── main/               # Game entry point (GameApp)
│       ├── game/               # Core data models, stats, combatants
│       ├── realtime/           # Physics, collision hitboxes, AI loops, dungeon manager
│       ├── ui/                 # Swing panels, layout managers, theme configuration
│       └── ui/render/          # Java2D custom renderers, sprite loaders, VFX
├── test/                       # 8 automated test suites and screenshot capture utility
├── README.md                   # Project documentation and student guide
└── .gitignore                  # Git ignore rules for build and IDE files
```

### Package Responsibilities

1. **`com.riftarena.main`**:
   - `GameApp.java`: Initializes Swing look-and-feel on the Event Dispatch Thread (EDT) and boots `MainFrame`.

2. **`com.riftarena.game`**:
   - `Combatant.java`, `Hero.java`, `Enemy.java`: Base character models tracking health, spirit, and combat stats.
   - `Warrior.java`, `Assassin.java`, `Mage.java`, `Kael.java`, `Vexara.java`, `Draven.java`: Champion concrete classes with unique stat distributions and special abilities.
   - `RiftWarden.java`: Boss model with multi-phase transition logic.
   - `CombatEngine.java`: Authoritative turn-based and mathematical combat rules.

3. **`com.riftarena.realtime`**:
   - `RealTimeCombatEngine.java`: Fixed-timestep game loop managing physics, entity velocities, gravity, and attacks.
   - `RealTimePlayer.java` & `RealTimeEnemy.java`: Real-time entities handling inputs, state machines (`IDLE`, `WALKING`, `JUMPING`, `ATTACKING`, `HURT`, `DEFEAT`), and AI behavior.
   - `Hitbox.java`: Precision Axis-Aligned Bounding Box (AABB) intersection detection.
   - `DungeonRun.java` & `DungeonStage.java`: Oversees stage progression from Dungeon Entrance to Boss Victory.

4. **`com.riftarena.ui`**:
   - `MainFrame.java`: Top-level window managing card-layout transitions between screens.
   - `MenuPanel.java`: Cinematic title screen with interactive options (`ENTER DUNGEON`, `BOSS CHALLENGE`, etc.).
   - `HeroSelectPanel.java`: Fighting-game character selection screen with animated champion previews and stat charts.
   - `CombatPanel.java`: Real-time combat viewport, top HUD (HP/SP bars, portrait frames), and bottom controls bar.
   - `ResultPanel.java`: Victory/defeat summary screen displaying clear times, kills, and Hunter Ranks.
   - `Theme.java`: Centralized color palette, glowing fonts, and gold borders.

5. **`com.riftarena.ui.render`**:
   - `HeroSpriteLoader.java`: Thread-safe caching sprite loader for character frame sequences and cutouts.
   - `CharacterRendererFactory.java`: Factory mapping character types to dedicated custom Java2D renderers.
   - `WarriorRenderer.java`, `MageRenderer.java`, `AssassinRenderer.java`, `KaelRenderer.java`, `VexaraRenderer.java`, `DravenRenderer.java`: Champion-specific renderers with directional orientation and weapon glow.
   - `RiftWardenRenderer.java`: Boss renderer featuring multi-phase cataclysm aura and ground slam shocks.
   - `CombatEffectManager.java`: Particle simulation for hit sparks, projectile trails, and floating damage numbers.
