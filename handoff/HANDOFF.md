# Euclidean Sketchpad — Handoff Document

**Date:** 2026-10-18
**Branch:** `feat/1-arrowheads-at-canvas-edge` (main branch is `main`)

## Current State

- **Build:** `./gradlew clean test` passes (124 tests). Clean build.
- **Git:** `master` branch. `.claude/` is gitignored.
- **Runtime:** `./gradlew run` launches the full app.
- **Packaging:** `./gradlew jpackage` produces `build/jpackage/EuclideanSketchpad-1.1.0.dmg` (~35 MB). `./gradlew jpackageImage` builds just the `.app` (faster, for testing). Not code-signed.
- **Spec alignment:** All features described in `spec/SPEC.md` are implemented, including the `LinearLike` interface, 4 built-in constructions, specific type display names, and "Custom Constructions" table rename.

## Architecture Notes

- **Colors are programmatic, not CSS.** Selection (blue), hover (blue+thicker), and normal (black) styles are set in code via `stroke`/`fill` properties, not via CSS classes. CSS classes (`segment`, `line`, `ray`, `point`, `circle`) are still added to nodes for test assertions and potential future CSS use, but colors are applied after CSS to ensure they aren't overridden.
- **Hover is canvas-level.** `hoveredId` lives on `CanvasPane`, not on individual tools. The canvas calls `hitTester.hitTest()` on every mouse move and passes the result to the renderer.
- **`LinearLike` is an interface, not a sealed class.** Kotlin exhaustive `when` on `GeometricObject` (sealed) can't resolve `is LinearLike` / `is PointLike` branches as exhaustive. Where needed, `getObjectAnchor()` functions use early-return `if` checks for `PointLike`/`LinearLike` followed by a `when` with `else` for the remaining types.
- **`LinearType` enum lives in `geometry/Line.kt`**, not in `IntersectionUtils.kt`. `LinearData` remains in `IntersectionUtils.kt`.
- **Built-in constructions use `startApplyMode()`** with `"Linear"` as the type string for any-LinearLike inputs. `ConstructionApplicator.matchesExpectedType()` resolves `"Linear"` → `obj is LinearLike`.
- **`ReflectionPoint` reflects across the infinite line** defined by a `LinearLike` parent's direction, regardless of whether the parent is a Line, Ray, or Segment.
- **Construction preview uses a dedicated scene graph group.** `constructionPreviewGroup` is a sibling of `previewGroup` in `worldGroup` (not nested inside it), so tool previews (segment/circle ghosts) and construction previews don't interfere. `ConstructionApplicator.computePreview()` creates temporary objects with negative IDs to avoid collision with the real object map. `startApplyMode()` accepts an optional `previewProvider` callback; preview appears only on last-input hover.
- **Selection state is centralized via helpers.** Selection lives in three places: `selectionTool.selectedIds`, `canvasPane.selectedIds`, and `sidebar`. `SketchpadApp` provides `clearSelection()` and `setSelection(ids)` local functions that sync all three. All call sites use these helpers. `requestRender()` also syncs from `selectionTool` → canvas/sidebar (idempotent).
- **Window size and pane ratios differ from spec.** Window is 1440×720 (spec says 1280×720) and dividers are 25%/50%/25% (spec says ~22%/56%/22%) so the left pane's object table columns (Parents, Label) are all visible at startup.
- **Label hit-box accounts for scaleY=-1 visual shift.** The renderer uses `text.scaleY = -1.0` to un-mirror labels in the y-flipped world group. JavaFX pivots this around the layout-bounds center, shifting the visual text ~7px below the computed baseline. `LabelTool.hitTestLabel()` extends its hit box downward (`labelY - 10` to `labelY + 18`) to match the actual visual position.
- **`APP_VERSION` is the single source of truth for the version string.** Defined as a top-level `const val` in `Main.kt`. Referenced by `DiagramSerializer`, `ConstructionSerializer`, and the About dialog.
- **Diagram open validates before resetting.** `SketchpadApp` parses and validates the file's version before calling `resetDiagram()`, so a version mismatch doesn't destroy the user's current work. The entire open operation is wrapped in a try-catch that shows an error alert.
- **jpackage uses `org.beryx.runtime` plugin with hardcoded modules.** The plugin's auto-detection (`suggestModules`) crashes on deeply-nested Kotlin anonymous classes. Modules are explicitly set to `java.base`, `java.desktop`, `java.logging`, `jdk.unsupported`. The jpackage `appVersion` is set to `1.1.0` (macOS jpackage rejects versions starting with `0`); this now matches `APP_VERSION` in `Main.kt`. The `.icns` icon at `packaging/macos/EuclideanSketchpad.icns` was generated from `app-icon.png` via `sips` + `iconutil`.
- **Arrowhead clipping uses Liang-Barsky on the viewport.** `GeometryRenderer.renderAll()` and `renderPreviewObjects()` receive `viewportBounds` from `CanvasPane.getViewportMathBounds()`, compute a clip rect inset by 5%, and clip lines/rays to it. Both the `FxLine` stroke and arrowheads terminate at the same clip boundary. `LineTool` receives a `viewportProvider` lambda and applies identical clipping to tool preview (ghost) lines/rays. `GeometryUtils.clipLineToRect()` is the single shared Liang-Barsky implementation used by `GeometryRenderer`, `LineTool`, and `ExportRenderer`.
