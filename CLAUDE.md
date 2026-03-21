# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application
./gradlew lwjgl3:run

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

There are no automated tests configured.

## Architecture

This is a [libGDX](https://libgdx.com/) desktop game using a standard two-module Gradle layout:

- **`core/`** — Platform-agnostic game logic. `ch.obermuhlner.sim.Main` (extends `ApplicationAdapter`) is the entry point for all game code. This is where gameplay, rendering, and simulation logic lives.
- **`lwjgl3/`** — Desktop launcher. `ch.obermuhlner.sim.lwjgl3.Lwjgl3Launcher` bootstraps the LWJGL3 OpenGL backend and starts `Main`. `StartupHelper` handles platform quirks (macOS `-XstartOnFirstThread`, Windows DLL paths, Linux NVIDIA threading).

**Assets** live in `/assets/` and are served to the game at runtime. A Gradle task generates `assets.txt` listing all assets.

**libGDX rendering model:** `Main.create()` initializes resources, `Main.render()` is called every frame (60 FPS, VSync on), `Main.dispose()` cleans up. Use `SpriteBatch` for 2D sprite drawing.

**Key versions:** libGDX 1.14.0, LWJGL3 3.4.1, Java 8 source/target (Construo bundles Java 21 JDK for distribution).
