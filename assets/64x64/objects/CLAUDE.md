# Map Objects

This directory contains 64x64 pixel art sprites for medieval sim game objects (terrain objects, buildings, etc.).

## Creating New Objects

Use the Pixellab API to generate objects:

```bash
curl -X POST "https://api.pixellab.ai/v2/map-objects" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d "$(cat <<'EOF'
{
  "description": "A pixel art <object type> viewed from top-down, medieval style",
  "image_size": {"width": 64, "height": 64},
  "view": "high top-down",
  "outline": "single color outline",
  "shading": "medium shading",
  "detail": "medium detail"
}
EOF
)"
```

The API returns a job ID. Poll the status endpoint or wait for completion.

## Style Matching

For consistent art style, reference an existing game object or tile:

```bash
"background_image": {
  "base64": "$(base64 -w 0 assets/64x64/objects/tree-large.png)"
}
```

Reference images for style:
- Terrain objects: `single-tiles/grass.png`, `single-tiles/stone.png`
- Existing objects: any `64x64/objects/*.png`

## Downloading

Download the completed image:

```bash
curl --fail -o <object-name>.png "https://api.pixellab.ai/mcp/map-objects/<job-id>/download"
```

Save to `assets/64x64/objects/<object-name>.png`.

## Object List

See `OBJECTS.md` for the complete list of objects and their intended use.

## Naming Convention

Use lowercase with hyphens: `tree-large.png`, `boulder-large.png`, `house-simple.png`
