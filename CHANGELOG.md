# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/), and this project adheres to [Semantic Versioning](https://semver.org/).

## [1.1.0] - 2026-10-18

### Added

- Arrowheads rendered at the visible canvas edge for lines and rays, clipped to the viewport boundary with a 5% margin inset using Liang-Barsky line clipping
- Tool preview (ghost) lines and rays also clip and show arrowheads matching the committed object positions

### Fixed

- Auto-fit export ray clipping: rays now extend to the clip boundary with a visible 5% margin from the image edge (previously arrowheads were either clipped too aggressively or flush with the edge)

### Changed

- Extracted shared rendering constants (`POINT_RADIUS`, `STROKE_WIDTH`, `LINE_EXTENT`, `ARROWHEAD_SIZE`, `ANGLE_ARC_RADIUS`, `FONT_SIZE`), `computeClipRect`, `computeArrowheadVertices`, and `computeAdaptiveGridSpacing` into `GeometryUtils`, removing duplicates from `GeometryRenderer`, `LineTool`, `CanvasPane`, and `ExportRenderer`
- Extracted `getObjectAnchor` into the `model` package as a shared top-level function, removing duplicates from `GeometryRenderer` and `ExportRenderer`
- Increased auto-fit export margin from 5% to 10% to accommodate the 5% clip inset without clipping data-edge points

## [1.0.0] - 2026-10-17

### Added

- Compass-and-straightedge construction tools: Point, Line, Ray, Segment, Circle, Angle, Region
- Selection tool with drag support for free and semi-free points
- Label tool with inline editing and drag repositioning
- Intersection detection and preview for linear-linear, linear-circle, and circle-circle pairs
- Measurement table for segment length, angle, circle circumference, and region area
- Construction recording, saving, and loading system
- Built-in constructions: Midpoint, Parallel Line, Perpendicular Line, Reflection Point
- Construction apply mode with type-filtered hit-testing and hover preview
- Snapshot-based undo/redo
- Diagram save/load (JSON format)
- Export to PNG and SVG (Canvas, Custom bounding box, and Auto-fit methods)
- Adaptive grid with toggle
- Pan (Space+drag) and scroll-wheel zoom
