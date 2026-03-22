# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application
./gradlew lwjgl3:run

# Run with console (GUI + text commands)
./gradlew lwjgl3:run -PmainClass=ch.obermuhlner.sim.GameWithConsole

# Run pure console (no GUI, text only)
./gradlew lwjgl3:run -PmainClass=ch.obermuhlner.sim.GameConsole

# Build a runnable JAR
./gradlew lwjgl3:jar

# Platform-specific JARs (with bundled natives)
./gradlew lwjgl3:jarLinux
./gradlew lwjgl3:jarMac
./gradlew lwjgl3:jarWin

# Clean build artifacts
./gradlew clean

# Generate IntelliJ project files
./gradlew idea

# Run unit tests
./gradlew core:test
./gradlew core:test --info  # verbose output
```

## Testing

### Philosophy

**Unit tests** cover game logic without UI dependencies. **Integration tests** run the actual game in a headless environment with simulated input. libGDX rendering requires native OpenGL libraries, so tests that create textures are skipped.

### Test Files

Located in `core/src/test/java/ch/obermuhlner/sim/game/`:

| File | What it Tests |
|------|--------------|
| `SettlementTest.java` | Settlement creation, population, level transitions, upgrades |
| `SettlementLevelTest.java` | Level enum behavior, population thresholds |
| `TileTest.java` | Terrain types, buildability, walkability |
| `BuildingTypeTest.java` | Building type IDs, names, population capacities |
| `WorldTest.java` | Chunk loading, fog-of-war, reveals, settlements |
| `mode/BuildModeLogicTest.java` | Building placement, proximity, capacity rules |

Integration tests in `it/` subdirectory:

| File | What it Tests |
|------|--------------|
| `GameFlowIntegrationTest.java` | Mode switching, settlement lifecycle, camera controls |
| `HeadlessGameTest.java` | Base class with headless libGDX setup and input simulation |

### Running Tests

```bash
./gradlew core:test           # Run all tests
./gradlew core:test --info    # Verbose output
```

### Adding Tests

When fixing bugs or adding features:
1. Write tests that define expected behavior
2. Run tests to verify they fail (catches the bug)
3. Fix the code
4. Run tests to verify they pass

**Important:** If fixing a bug, the test documents the *correct* behavior—not the buggy behavior.

**Note:** Tests that require OpenGL (texture creation) cannot run in headless mode. Use the GUI debug console for testing UI rendering.

## Debugging

### Debug Console (GUI + Commands)

Run with GUI + text commands for UI debugging:

```bash
./gradlew lwjgl3:run -PmainClass=ch.obermuhlner.sim.GameWithConsole
```

The REST API starts on `http://localhost:8088/cmd`.

**REST API Examples:**

```bash
# Get help
curl -X POST -d "help" http://localhost:8088/cmd

# View game state
curl -X POST -d "state" http://localhost:8088/cmd

# List settlements
curl -X POST -d "settlements" http://localhost:8088/cmd

# Reveal fog at coordinates
curl -X POST -d "reveal 8 -3" http://localhost:8088/cmd

# View map
curl -X POST -d "map 5 8 -3" http://localhost:8088/cmd

# Create settlement
curl -X POST -d "spawn 6 -2 SecondTown" http://localhost:8088/cmd
```

**Commands Reference:**

| Command | Aliases | Description |
|---------|---------|-------------|
| `help` | | Show all commands |
| `state` | `s` | Full game state |
| `settlements` | `st` | Settlement details |
| `resources` | `r` | Resource summary |
| `tile <x> <y>` | `t` | Single tile info |
| `map [r] [cx] [cy]` | `m` | ASCII map |
| `reveal <x> <y>` | | Reveal fog around point |
| `spawn <x> <y> [name]` | | Create settlement |
| `status` | | Selected tile and available actions |
| `toolbar` | | Available toolbar buttons |

**Map Legend:**
- `#` = fog, `G/.` = grass, `F/T` = forest, `S/O` = stone, `W/~` = water, `X/*` = snow
- `@` = settlement center, `B` = building

**Tile selection simulation:**
- `select <tileX> <tileY>` - Select tile at world coordinates (reveals + selects)
- `click <screenX> <screenY> [button]` - Simulate mouse click at screen coords
- `key <keyname>` - Simulate key press

**Toolbar coordinates:**
- Toolbar is centered horizontally at top of screen
- Each button is 80px wide with 8px padding
- First button center: `(screenWidth/2, screenHeight - 48)`
- Calculate: `x = screenWidth/2 - (numButtons*88)/2 + 44`

### Pure Console (No GUI)

```bash
./gradlew lwjgl3:run -PmainClass=ch.obermuhlner.sim.GameConsole
```

Commands: `state`, `settlements`, `tile <x> <y>`, `map [r]`, `reveal <x> <y>`, `spawn <x> <y>`, `build <x> <y>`

### Programmatic Debugging

Use `GameDebugger` class to get text representations:

```java
GameDebugger debugger = new GameDebugger(world);
debugger.getState();           // Full state
debugger.getSettlementsInfo(); // Settlement list
debugger.getTileInfo(tx, ty); // Tile at coordinates
debugger.getMapAround(cx, cy, radius); // ASCII map
```

### Quick Test Pattern

When debugging features:
1. Make code changes
2. Run `./gradlew lwjgl3:run -PmainClass=ch.obermuhlner.sim.GameWithConsole`
3. Use REST API to verify state:
   ```bash
   curl -X POST -d "reveal 8 -3" http://localhost:8088/cmd
   curl -X POST -d "spawn 6 -2 TestTown" http://localhost:8088/cmd
   curl -X POST -d "settlements" http://localhost:8088/cmd
   curl -X POST -d "state" http://localhost:8088/cmd
   ```

## UX Model

The game uses a **tile-selection UX** (no mode switching):

1. Click a tile → reveals if hidden, selects if visible
2. Toolbar updates → shows actions for selected tile (buildings, settlement options)
3. Click toolbar action → performs action on selected tile
4. No build mode to enter/exit

## Architecture

This is a [libGDX](https://libgdx.com/) desktop game using a standard two-module Gradle layout:

- **`core/`** — Platform-agnostic game logic. `ch.obermuhlner.sim.Main` (extends `ApplicationAdapter`) is the entry point for all game code. This is where gameplay, rendering, and simulation logic lives.
- **`lwjgl3/`** — Desktop launcher. `ch.obermuhlner.sim.lwjgl3.Lwjgl3Launcher` bootstraps the LWJGL3 OpenGL backend and starts `Main`. `StartupHelper` handles platform quirks (macOS `-XstartOnFirstThread`, Windows DLL paths, Linux NVIDIA threading).

**Assets** live in `/assets/` and are served to the game at runtime. A Gradle task generates `assets.txt` listing all assets.

**Runtime data** (persisted game state) is written to `data/` within libGDX's local storage directory (e.g. `~/.local/share/simple-sim-game/data/` on Linux). Fog-of-war chunk files live at `data/chunks/{cx}_{cy}.fow`.

**libGDX rendering model:** `Main.create()` initializes resources, `Main.render()` is called every frame (60 FPS, VSync on), `Main.dispose()` cleans up. Use `SpriteBatch` for 2D sprite drawing.

**Key versions:** libGDX 1.14.0, LWJGL3 3.4.1, Java 8 source/target (Construo bundles Java 21 JDK for distribution).
