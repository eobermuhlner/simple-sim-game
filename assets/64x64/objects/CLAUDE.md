# Map Objects

This directory contains 64x64 pixel art sprites for game objects (trees, rocks, buildings, etc.).

## Creating New Objects

Use the `pixellab_create_map_object` tool to generate objects:

```
create_map_object(
  description="Description of the object",
  width=64,
  height=64,
  view="high top-down",
  background_image={"type": "path", "path": "assets/64x64/single-tiles/grass.png"},
  inpainting={"type": "oval", "fraction": 0.3}
)
```

### Parameters

- **description**: Describe the object clearly (e.g., "pixel art oak tree viewed from top-down")
- **width/height**: Always `64` for this directory
- **view**: `"high top-down"` for top-down perspective
- **background_image**: Reference image for style matching (optional but recommended)
- **inpainting**: `"oval"` with fraction ~0.3-0.5 depending on object size

### Style Matching

For consistent art style, always provide a reference tile from `single-tiles/`:
```json
{"type": "path", "path": "assets/64x64/single-tiles/grass.png"}
```

This ensures the generated object matches the game's palette and shading.

### Inpainting Shape Guide

- **Trees/bushes**: oval fraction 0.3-0.4
- **Rocks/flowers**: oval fraction 0.2-0.3
- **Buildings**: rectangle fraction 0.5-0.7

## Downloading

After generation, use `pixellab_get_map_object` to get the download URL:

```bash
curl --fail -o <object-name>.png "https://api.pixellab.ai/mcp/map-objects/<id>/download"
```

Save to `assets/64x64/objects/<object-name>.png`.

## Naming Convention

Use lowercase with hyphens: `tree-oak.png`, `rock-granite.png`, `chest-wooden.png`
