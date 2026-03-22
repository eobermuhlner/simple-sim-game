# Characters

This directory contains 64x64 pixel art animated sprites for the medieval sim game.

## Creating New Characters

Use the Pixellab MCP to generate characters with animations.

### Step 1: Create Character

```java
// Create character with 4 directions
pixellab_create_character(
  body_type="humanoid",
  description="medieval trader wearing brown tunic and vest, carrying a large heavy backpack on back, walking pose",
  n_directions=4,  // or 8 for more directions
  size=48  // canvas size in pixels
)
```

### Step 2: Add Animation

```java
// Queue walking animation for all directions
pixellab_animate_character(
  character_id="<id from step 1>",
  template_animation_id="walking"  // see available animations
)
```

### Step 3: Download

Download the ZIP and extract into this directory:

```bash
curl --fail -o trader.zip "https://api.pixellab.ai/mcp/characters/<id>/download"
unzip trader.zip -d trader_walking/
```

## Available Animations

- `walking`, `walk`, `walk-1`, `walk-2`, etc.
- `breathing-idle`, `crouching`
- `running-4-frames`, `running-6-frames`, `running-8-frames`
- `jumping-1`, `jumping-2`, `running-jump`
- `backflip`, `front-flip`
- `high-kick`, `roundhouse-kick`, `flying-kick`
- `fireball`, `throw-object`
- See full list in `get_character` response

## Directory Structure

```
trader-walking/
├── animations/
│   └── walking/
│       ├── south/  (frame_000.png - frame_005.png)
│       ├── east/
│       ├── west/
│       └── north/
├── rotations/
│   ├── south.png
│   ├── east.png
│   ├── west.png
│   └── north.png
└── metadata.json
```

## Naming Convention

Use lowercase with underscores: `<character_name>/`

Example: `trader-walking/`, `trader-mule-walking/`, `guard_patrolling/`
