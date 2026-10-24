---
name: export-diagram
description: Export a diagram JSON file to PNG and SVG in all three export modes (canvas, custom, autofit). Use when user says "export diagram", "export-diagram", or wants to test diagram rendering.
allowed-tools: Bash(./gradlew *) Bash(ls *)
---

# Export Diagram

Headless export of a diagram file to 6 images (3 modes × 2 formats).

## Inputs

- **Diagram path** (required): path to a `.json` diagram file.
- **Output directory** (optional, default `exports/`): where to write the exported files.

If the user did not provide a diagram path, **ask for one before proceeding**.

## Steps

1. Verify the diagram file exists.
2. Run the headless export:
   ```
   ./gradlew run --args="--export <diagram-path> <output-dir>"
   ```
3. Verify output with `ls -la <output-dir>/`. Expect 6 non-zero-size files:
   - `<name>_canvas.png`, `<name>_canvas.svg`
   - `<name>_custom.png`, `<name>_custom.svg`
   - `<name>_autofit.png`, `<name>_autofit.svg`
4. Report the results to the user.
