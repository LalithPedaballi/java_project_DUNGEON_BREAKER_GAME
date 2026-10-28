# DUNGEON BREAKER

A cinematic 2D dark-fantasy fighting and dungeon crawl game built in pure **Java 17** with standard **Swing** and **Java2D**. Pick your champion from a six-hero roster, explore multi-stage rift chambers, and battle through void monsters to challenge **The Rift Warden** — an imposing multi-phase boss.

Zero external frameworks, zero third-party libraries, and zero build tool bloat — implemented strictly using the standard Java 17 library (`java.desktop`).

---

## 🎓 College Viva & Architecture Overview

This project is structured specifically to demonstrate core **Object-Oriented Programming (OOP)** and standard **Computer Science** principles taught in an introductory Java course.

### Package Responsibilities & Layered Design

```
src/com/riftarena/
├── main/          # Entry point & application bootstrapping (GameApp)
├── game/          # Domain layer: Core OOP character models, stats, combat rules
├── realtime/      # Simulation layer: 2D physics, collision detection (AABB), game loop
├── ui/            # Presentation layer: Swing windows, panels, CardLayout, inputs
└── ui/render/     # Graphics layer: Java2D sprite animation, visual VFX, particles
```

### Understanding `game` vs. `realtime` (Common Viva Question!)

A professor may ask: *"Why are `game` and `realtime` separate packages?"*

- **`com.riftarena.game` (The Rulebook & Character Models)**:
  - Defines **what** a character is: base health, attack power, defense mitigation, and special skills.
  - Implements the mathematical formulas: damage calculations, cooldown increments, and state transitions.
  - Contains pure Java classes (`Hero`, `Warrior`, `Mage`, `Enemy`, `RiftWarden`) that do not depend on screen coordinates or pixels.
- **`com.riftarena.realtime` (The Physical Arena Simulation)**:
  - Defines **where** characters exist in 2D space: X and Y coordinates, horizontal velocity ($v_x$), gravity and vertical jump velocity ($v_y$).
  - Implements the real-time 60 FPS update loop, Axis-Aligned Bounding Box (`Hitbox`) collision detection, and multi-room dungeon progression (`DungeonRun`).
  - Wraps the character models from `game`: for example, `RealTimePlayer` holds a reference to a `Hero`.

This separation of concerns follows the classic **Model-View-Controller (MVC)** architectural pattern.

---

## 📚 Java Syllabus Concepts Demonstrated

| Syllabus Concept | Description | Project File Reference |
| :--- | :--- | :--- |
| **Interfaces** | Defines common contracts without implementation details. | [`Combatant.java`](src/com/riftarena/game/Combatant.java), [`CharacterRenderer.java`](src/com/riftarena/ui/render/CharacterRenderer.java) |
| **Abstract Classes** | Provides shared code while enforcing subclass implementations. | [`Hero.java`](src/com/riftarena/game/Hero.java), [`RealTimeEntity.java`](src/com/riftarena/realtime/RealTimeEntity.java) |
| **Inheritance (`extends`)** | Subclasses inherit properties and methods from parent classes. | [`Warrior.java`](src/com/riftarena/game/Warrior.java), [`RealTimePlayer.java`](src/com/riftarena/realtime/RealTimePlayer.java) |
| **Polymorphism & Overriding** | Subclasses override methods to provide distinct behavior at runtime. | [`@Override executeSpecial()`](src/com/riftarena/game/Mage.java), [`@Override render()`](src/com/riftarena/ui/render/KaelRenderer.java) |
| **Encapsulation** | Private member variables protected by getters, setters, and validation. | `Hero.java` (HP clamping and constructor argument checks) |
| **Constructor Chaining** | Using `super(...)` and `this(...)` to delegate initialization. | [`Enemy.java`](src/com/riftarena/game/Enemy.java), [`Warrior.java`](src/com/riftarena/game/Warrior.java) |
| **Enums** | Strongly typed named constants with methods and state. | [`Behaviour.java`](src/com/riftarena/game/Enemy.java), [`DungeonStage.java`](src/com/riftarena/realtime/DungeonStage.java), [`EntityState.java`](src/com/riftarena/realtime/EntityState.java) |
| **Collections & Generics** | Managing dynamic lists of entities, particles, and hitboxes. | `List<Projectile>`, `ArrayList<EmberParticle>` in [`DungeonRun.java`](src/com/riftarena/realtime/DungeonRun.java) |
| **Switch Expressions** | Modern Java pattern matching and concise decision making. | [`MainFrame.java`](src/com/riftarena/ui/MainFrame.java) (`createHero`) |
| **AABB Collision Detection** | Fundamental 2D physics math using rectangle intersections. | [`Hitbox.java`](src/com/riftarena/realtime/Hitbox.java) (`intersects` method) |
| **GUI & Event Handling** | Swing window management and key/mouse event listeners. | [`CombatPanel.java`](src/com/riftarena/ui/CombatPanel.java), [`MenuPanel.java`](src/com/riftarena/ui/MenuPanel.java) |
| **Custom Graphics (Java2D)** | Custom drawing using `Graphics2D`, affine transforms, and paint cycles. | [`FigureRenderer.java`](src/com/riftarena/ui/render/FigureRenderer.java) |

---

## 🎨 The Role of Swing & Java2D

- **Why Swing?**: Swing is the standard graphical user interface (GUI) toolkit built into the Java Development Kit. In this game, `MainFrame` uses a `CardLayout` to switch smoothly between the `MenuPanel`, `HeroSelectPanel`, `CombatPanel`, and `ResultPanel`.
- **Why Java2D?**: Java2D provides two-dimensional graphics APIs (`Graphics2D`). The game overrides `paintComponent(Graphics g)` to perform double-buffered drawing of background environments, multi-frame character animations, health bars, and particle effects.

---

## 🎮 The Six Playable Champions

1. **Aegis** (*The Iron Vanguard*): High HP, heavy plate armor, sunburst heater shield, and damage-mitigating Shield Slam.
2. **Nyx** (*The Shadow Assassin*): Swift rogue wielding poisoned dual daggers with lethal execution strikes against low-health enemies.
3. **Zephyra** (*The Arcane Weaver*): Ranged sorceress with floating mystic focus shards and high-damage Arcane Nova spells.
4. **Kael** (*The Spellsword Duelist*): Agile blade master combining rapid rapier thrusts with high forward dash mobility.
5. **Vexara** (*The Soul Reaper*): Dark scythe wielder whose Soul Reap ability siphons life from opponents back to herself.
6. **Draven** (*The Magma Berserker*): Molten powerhouse with temporary invulnerability and ground-shaking Magma Eruptions.

---

## 🕹️ Controls & Keybindings

All keybindings use Swing window-level input maps (`WHEN_IN_FOCUSED_WINDOW`) for responsive keyboard control:

| Control | Action | Function |
| :--- | :--- | :--- |
| **`A` / `D`** *(or $\leftarrow$ / $\rightarrow$)* | **Move Left / Right** | Continuous horizontal motion clamped to arena walls. |
| **`W` / `Space`** *(or $\uparrow$)* | **Jump** | Vertical jump with simulated gravity to leap over boss shockwaves. |
| **`J`** *(or Left Click)* | **Attack** | Multi-hit weapon swing with active directional hitboxes. |
| **`K`** *(or Right Click)* | **Special Move** | Unique champion skill with cooldown timer. |
| **`Shift`** | **Dash / Evade** | Fast evasive dash with invulnerability frames (i-frames). |
| **`ESC`** | **Pause / Resume** | Freezes game physics and animation updates. |
| **`R`** | **Restart** | Resets the run with full health and fresh enemies. |

---

## 💻 System Requirements & Java Verification

This game requires **JDK 17 or higher**.

To verify your Java installation:
```bash
java -version
javac -version
```
If both print version `17` or higher, your environment is ready.

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

## 🧪 Automated Test Suites

The project contains **120 automated tests across 8 test suites** verifying turn-based logic, real-time physics, AI patterns, UI state, and rendering:

```bash
# Run all 8 suites in a single command:
java -cp out com.riftarena.game.CombatEngineTest && \
java -cp out com.riftarena.game.SixHeroRosterAndEnemiesTest && \
java -cp out com.riftarena.realtime.RealTimeCombatTest && \
java -cp out com.riftarena.realtime.DungeonProgressionTest && \
java -cp out com.riftarena.ui.UIIntegrationTest && \
java -cp out com.riftarena.ui.VisualPresentationTest && \
java -cp out com.riftarena.ui.LiveGameplayVerificationTest && \
java -cp out com.riftarena.ui.MenuPanelTest
```

### Test Suite Summary

1. **`CombatEngineTest` (31 tests)**: Validates health clamping, turn order, defense mitigation (50%), and phase threshold triggers.
2. **`SixHeroRosterAndEnemiesTest` (9 tests)**: Verifies 6 hero classes, unique renderers, and presence of all 12 animation frames on disk.
3. **`RealTimeCombatTest` (17 tests)**: Tests velocity, gravity trajectory, jump landing, AABB hitbox collisions, and i-frames.
4. **`DungeonProgressionTest` (26 tests)**: Validates stage transitions (Entrance $\rightarrow$ Encounters 1 & 2 $\rightarrow$ Boss Gate $\rightarrow$ Victory).
5. **`UIIntegrationTest` (13 tests)**: Verifies Swing panel routing, HUD updates, button listeners, and result stats.
6. **`VisualPresentationTest` (9 tests)**: Ensures headless Java2D offscreen rendering executes with zero crashes.
7. **`LiveGameplayVerificationTest` (10 tests)**: Validates player input handling, dash speeds, attack combos, and end-to-end flow.
8. **`MenuPanelTest` (5 tests)**: Verifies responsive menu geometry, button bounding boxes, and keyboard arrow navigation.

---

## 📁 Clean Project Directory Structure

```
java project/
├── assets/                     # Required runtime visual assets
│   ├── arena/                  # Arena backgrounds (dungeon_arena_background.png)
│   ├── enemies/                # 6 transparent monster and boss cutouts
│   ├── heroes/                 # 6 champions with 12-frame animation sets
│   └── menu/                   # 2048x1150 main menu background
├── src/                        # Clean Java 17 source code (50 classes)
│   └── com/riftarena/
│       ├── main/               # GameApp.java
│       ├── game/               # Models (Hero, Enemy, CombatEngine, Combatant)
│       ├── realtime/           # Physics (RealTimePlayer, RealTimeEnemy, Hitbox)
│       ├── ui/                 # Swing UI (MainFrame, MenuPanel, CombatPanel)
│       └── ui/render/          # Java2D renderers, sprite loader, VFX
├── test/                       # 8 automated regression test suites
├── README.md                   # Project documentation and student viva guide
└── .gitignore                  # Git configuration ignoring build & IDE files
```
