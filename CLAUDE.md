# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application
./gradlew lwjgl3:run

# Run with console (GUI + text commands)
./gradlew core:run -PmainClass=ch.obermuhlner.sim.GameWithConsole

# Run pure console (no GUI, text only)
./gradlew core:run -PmainClass=ch.obermuhlner.sim.GameConsole

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
```

## Debugging

### Debug Console

A text-based console for testing game state without the GUI. Use this to verify changes and debug issues quickly.

```bash
./gradlew core:run -PmainClass=ch.obermuhlner.sim.GameConsole
```

**Available commands:**
- `state` / `s` - Full game state
- `settlements` / `st` - Settlement details
- `resources` / `r` - Resource summary
- `tile <x> <y>` - Single tile info
- `map [radius] [cx] [cy]` - ASCII map (fog=#, grass=., forest=T, water=~, settlement=@)
- `reveal <x> <y>` - Reveal area
- `spawn <x> <y> [name]` - Create settlement
- `build <x> <y> [id]` - Place building
- `help` - Show all commands

**Key for ASCII map:**
- `#` = fog (unrevealed)
- `~` = water
- `.` = grass
- `T` = forest
- `O` = stone
- `*` = snow
- `@` = settlement center
- `B` = building

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
2. Run `./gradlew core:run -PmainClass=ch.obermuhlner.sim.GameConsole`
3. Use console commands to verify state
4. Example workflow:
   ```
   > spawn 5 5 TestTown
   > state
   > map 5
   > build 6 5 1
   > state
   ```

## Architecture

This is a [libGDX](https://libgdx.com/) desktop game using a standard two-module Gradle layout:

- **`core/`** — Platform-agnostic game logic. `ch.obermuhlner.sim.Main` (extends `ApplicationAdapter`) is the entry point for all game code. This is where gameplay, rendering, and simulation logic lives.
- **`lwjgl3/`** — Desktop launcher. `ch.obermuhlner.sim.lwjgl3.Lwjgl3Launcher` bootstraps the LWJGL3 OpenGL backend and starts `Main`. `StartupHelper` handles platform quirks (macOS `-XstartOnFirstThread`, Windows DLL paths, Linux NVIDIA threading).

**Assets** live in `/assets/` and are served to the game at runtime. A Gradle task generates `assets.txt` listing all assets.

**Runtime data** (persisted game state) is written to `data/` within libGDX's local storage directory (e.g. `~/.local/share/simple-sim-game/data/` on Linux). Fog-of-war chunk files live at `data/chunks/{cx}_{cy}.fow`.

**libGDX rendering model:** `Main.create()` initializes resources, `Main.render()` is called every frame (60 FPS, VSync on), `Main.dispose()` cleans up. Use `SpriteBatch` for 2D sprite drawing.

**Key versions:** libGDX 1.14.0, LWJGL3 3.4.1, Java 8 source/target (Construo bundles Java 21 JDK for distribution).
