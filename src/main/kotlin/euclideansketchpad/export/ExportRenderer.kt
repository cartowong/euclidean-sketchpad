package euclideansketchpad.export

import euclideansketchpad.geometry.*
import euclideansketchpad.model.GeometricObjectManager
import euclideansketchpad.model.ObjectMetadata
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Rectangle2D
import javafx.scene.Group
import javafx.scene.SnapshotParameters
import javafx.scene.paint.Color
import javafx.scene.shape.Arc
import javafx.scene.shape.ArcType
import javafx.scene.shape.Circle as FxCircle
import javafx.scene.shape.Line as FxLine
import javafx.scene.shape.Polygon as FxPolygon
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import javafx.scene.text.Text
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.*

enum class ExportMethod { CANVAS, CUSTOM, AUTO_FIT }
enum class ExportFormat { PNG, SVG }

data class ExportSettings(
    val method: ExportMethod,
    val format: ExportFormat,
    val outputWidth: Int,
    val outputHeight: Int,
    val transparentBackground: Boolean,
    val includeGrid: Boolean,
    val boundsMinX: Double,
    val boundsMinY: Double,
    val boundsMaxX: Double,
    val boundsMaxY: Double
)

object ExportRenderer {

    private const val POINT_RADIUS = 4.0
    private const val STROKE_WIDTH = 2.0
    private const val ARROWHEAD_SIZE = 8.0
    private const val ANGLE_ARC_RADIUS = 20.0
    private const val FONT_SIZE = 12.0

    fun export(settings: ExportSettings, manager: GeometricObjectManager, file: File) {
        when (settings.format) {
            ExportFormat.PNG -> exportPng(settings, manager, file)
            ExportFormat.SVG -> exportSvg(settings, manager, file)
        }
    }

    // --- Auto-fit bounds calculation ---

    fun computeAutoFitBounds(manager: GeometricObjectManager): DoubleArray {
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE

        fun include(x: Double, y: Double) {
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y
        }

        for (obj in manager.allObjects()) {
            val meta = manager.getMetadata(obj.id) ?: continue
            if (!meta.visible || meta.isLocked || !obj.isDefined) continue

            when {
                obj is LinearLike -> {
                    for (pid in obj.parentIds) {
                        val parent = manager.getObject(pid) ?: continue
                        if (parent is PointLike && parent.isDefined) {
                            include(parent.x, parent.y)
                        }
                    }
                }
                obj is Circle -> {
                    include(obj.cx + obj.radius, obj.cy)
                    include(obj.cx - obj.radius, obj.cy)
                    include(obj.cx, obj.cy + obj.radius)
                    include(obj.cx, obj.cy - obj.radius)
                }
                obj is Region -> {
                    for (pid in obj.parentIds) {
                        val parent = manager.getObject(pid) ?: continue
                        if (parent is PointLike && parent.isDefined) {
                            include(parent.x, parent.y)
                        }
                    }
                }
                obj is Angle -> {} // Angles don't contribute to bounding box
                obj is PointLike -> include(obj.x, obj.y)
            }
        }

        if (minX > maxX) {
            // No visible objects — fallback
            return doubleArrayOf(-100.0, -100.0, 100.0, 100.0)
        }

        // Handle degenerate cases (all objects at same point)
        if (maxX - minX < 1e-9) { minX -= 50.0; maxX += 50.0 }
        if (maxY - minY < 1e-9) { minY -= 50.0; maxY += 50.0 }

        // Add 5% margin
        val marginX = (maxX - minX) * 0.05
        val marginY = (maxY - minY) * 0.05
        return doubleArrayOf(minX - marginX, minY - marginY, maxX + marginX, maxY + marginY)
    }

    // --- PNG Export ---

    private fun exportPng(settings: ExportSettings, manager: GeometricObjectManager, file: File) {
        val w = settings.outputWidth.toDouble()
        val h = settings.outputHeight.toDouble()
        val group = Group()

        // Background
        if (!settings.transparentBackground) {
            group.children.add(Rectangle(w, h).apply { fill = Color.WHITE })
        }

        val scaleX = w / (settings.boundsMaxX - settings.boundsMinX)
        val scaleY = h / (settings.boundsMaxY - settings.boundsMinY)
        val scale = min(scaleX, scaleY)

        val tx = { mx: Double -> (mx - settings.boundsMinX) * scale }
        val ty = { my: Double -> (settings.boundsMaxY - my) * scale }

        if (settings.includeGrid) {
            renderGridPng(group, settings, scale, tx, ty)
        }

        renderObjectsPng(group, manager, settings, scale, tx, ty)

        // Snapshot
        val params = SnapshotParameters()
        params.fill = if (settings.transparentBackground) Color.TRANSPARENT else Color.WHITE
        params.viewport = Rectangle2D(0.0, 0.0, w, h)

        val image = group.snapshot(params, null)
        val buffered = SwingFXUtils.fromFXImage(image, null)
        ImageIO.write(buffered, "png", file)
    }

    private fun renderGridPng(group: Group, settings: ExportSettings, scale: Double, tx: (Double) -> Double, ty: (Double) -> Double) {
        val bMinX = settings.boundsMinX
        val bMinY = settings.boundsMinY
        val bMaxX = settings.boundsMaxX
        val bMaxY = settings.boundsMaxY
        val w = settings.outputWidth.toDouble()
        val h = settings.outputHeight.toDouble()

        // Adaptive grid spacing (same logic as CanvasPane)
        val rawSpacing = 50.0 / scale
        val magnitude = 10.0.pow(floor(log10(rawSpacing)))
        val normalized = rawSpacing / magnitude
        val spacing = when {
            normalized < 2.0 -> magnitude
            normalized < 5.0 -> 2.0 * magnitude
            else -> 5.0 * magnitude
        }

        val gridColor = Color.gray(0.85)
        val axisColor = Color.gray(0.5)

        // Vertical grid lines
        var x = floor(bMinX / spacing) * spacing
        while (x <= bMaxX) {
            val px = tx(x)
            if (px in 0.0..w) {
                val line = FxLine(px, 0.0, px, h)
                line.stroke = if (abs(x) < spacing * 0.01) axisColor else gridColor
                line.strokeWidth = if (abs(x) < spacing * 0.01) 1.0 else 0.5
                group.children.add(line)
            }
            x += spacing
        }

        // Horizontal grid lines
        var y = floor(bMinY / spacing) * spacing
        while (y <= bMaxY) {
            val py = ty(y)
            if (py in 0.0..h) {
                val line = FxLine(0.0, py, w, py)
                line.stroke = if (abs(y) < spacing * 0.01) axisColor else gridColor
                line.strokeWidth = if (abs(y) < spacing * 0.01) 1.0 else 0.5
                group.children.add(line)
            }
            y += spacing
        }
    }

    private fun renderObjectsPng(group: Group, manager: GeometricObjectManager, settings: ExportSettings, scale: Double, tx: (Double) -> Double, ty: (Double) -> Double) {
        val w = settings.outputWidth.toDouble()
        val h = settings.outputHeight.toDouble()
        // 5% margin for line/ray clipping so arrowheads don't touch the edge
        val clipMarginX = w * 0.05
        val clipMarginY = h * 0.05

        // Pass 1: Regions
        for (obj in manager.allObjects()) {
            if (obj !is Region || !obj.isDefined) continue
            val meta = manager.getMetadata(obj.id) ?: continue
            if (!isExportable(meta)) continue
            if (obj.vertices.size >= 3) {
                val coords = DoubleArray(obj.vertices.size * 2)
                for (i in obj.vertices.indices) {
                    coords[i * 2] = tx(obj.vertices[i].first)
                    coords[i * 2 + 1] = ty(obj.vertices[i].second)
                }
                val polygon = FxPolygon(*coords)
                polygon.fill = Color.web("#FFEB3B", 0.3)
                polygon.stroke = null
                group.children.add(polygon)
            }
        }

        // Pass 2: Angles
        for (obj in manager.allObjects()) {
            if (obj !is Angle || !obj.isDefined) continue
            val meta = manager.getMetadata(obj.id) ?: continue
            if (!isExportable(meta)) continue

            val arcRadius = ANGLE_ARC_RADIUS
            val cx = tx(obj.vertexX)
            val cy = ty(obj.vertexY)

            // In export pixel coords (y-down), we need to negate angles from math coords (y-up)
            val startDeg = -obj.startAngleRadians * 180.0 / PI
            val sweepDeg = obj.sweepAngleRadians * 180.0 / PI

            val arc = Arc(cx, cy, arcRadius, arcRadius, startDeg, sweepDeg)
            arc.type = ArcType.ROUND
            arc.fill = Color.web("#4CAF50", 0.3)
            arc.stroke = Color.web("#4CAF50", 0.7)
            arc.strokeWidth = 1.0
            group.children.add(arc)
        }

        // Pass 3: Lines/Rays/Segments/Circles
        for (obj in manager.allObjects()) {
            val meta = manager.getMetadata(obj.id) ?: continue
            if (!isExportable(meta) || !obj.isDefined) continue

            if (obj is LinearLike) {
                val dx = obj.x2 - obj.x1
                val dy = obj.y2 - obj.y1
                val len = hypot(dx, dy)
                if (len == 0.0) continue
                val ux = dx / len
                val uy = dy / len
                val extent = 100000.0

                when (obj.linearType) {
                    LinearType.SEGMENT -> {
                        val line = FxLine(tx(obj.x1), ty(obj.y1), tx(obj.x2), ty(obj.y2))
                        line.strokeWidth = STROKE_WIDTH
                        line.stroke = Color.BLACK
                        group.children.add(line)
                    }
                    LinearType.LINE -> {
                        val ex1 = obj.x1 - ux * extent
                        val ey1 = obj.y1 - uy * extent
                        val ex2 = obj.x1 + ux * extent
                        val ey2 = obj.y1 + uy * extent
                        val clipped = clipLineToRect(tx(ex1), ty(ey1), tx(ex2), ty(ey2), clipMarginX, clipMarginY, w - clipMarginX, h - clipMarginY)
                        if (clipped != null) {
                            val line = FxLine(clipped[0], clipped[1], clipped[2], clipped[3])
                            line.strokeWidth = STROKE_WIDTH
                            line.stroke = Color.BLACK
                            group.children.add(line)
                            val adx = clipped[2] - clipped[0]
                            val ady = clipped[3] - clipped[1]
                            val alen = hypot(adx, ady)
                            if (alen > 0) {
                                addArrowheadPng(group, clipped[2], clipped[3], adx / alen, ady / alen)
                                addArrowheadPng(group, clipped[0], clipped[1], -adx / alen, -ady / alen)
                            }
                        }
                    }
                    LinearType.RAY -> {
                        val ex = obj.x1 + ux * extent
                        val ey = obj.y1 + uy * extent
                        val clipped = clipLineToRect(tx(obj.x1), ty(obj.y1), tx(ex), ty(ey), clipMarginX, clipMarginY, w - clipMarginX, h - clipMarginY)
                        if (clipped != null) {
                            val line = FxLine(clipped[0], clipped[1], clipped[2], clipped[3])
                            line.strokeWidth = STROKE_WIDTH
                            line.stroke = Color.BLACK
                            group.children.add(line)
                            val adx = clipped[2] - clipped[0]
                            val ady = clipped[3] - clipped[1]
                            val alen = hypot(adx, ady)
                            if (alen > 0) {
                                addArrowheadPng(group, clipped[2], clipped[3], adx / alen, ady / alen)
                            }
                        }
                    }
                }
            } else if (obj is Circle) {
                val circle = FxCircle(tx(obj.cx), ty(obj.cy), obj.radius * scale)
                circle.fill = Color.TRANSPARENT
                circle.strokeWidth = STROKE_WIDTH
                circle.stroke = Color.BLACK
                group.children.add(circle)
            }
        }

        // Pass 4: Points
        for (obj in manager.allObjects()) {
            if (obj !is PointLike || !obj.isDefined) continue
            val meta = manager.getMetadata(obj.id) ?: continue
            if (!isExportable(meta)) continue
            val circle = FxCircle(tx(obj.x), ty(obj.y), POINT_RADIUS)
            circle.fill = Color.BLACK
            group.children.add(circle)
        }

        // Pass 5: Labels
        for (obj in manager.allObjects()) {
            val meta = manager.getMetadata(obj.id) ?: continue
            if (!isExportable(meta) || !meta.labelVisible || meta.label == null) continue
            if (!obj.isDefined) continue

            val anchor = getObjectAnchor(obj, meta) ?: continue
            val text = Text(meta.label)
            text.font = Font.font(FONT_SIZE)
            text.x = tx(anchor.first) + meta.labelOffsetX
            text.y = ty(anchor.second) + meta.labelOffsetY
            text.fill = Color.BLACK
            group.children.add(text)
        }
    }

    private fun addArrowheadPng(group: Group, tipX: Double, tipY: Double, dirX: Double, dirY: Double) {
        val size = ARROWHEAD_SIZE
        val perpX = -dirY
        val perpY = dirX
        val baseX = tipX - dirX * size
        val baseY = tipY - dirY * size
        val p1x = baseX + perpX * size * 0.4
        val p1y = baseY + perpY * size * 0.4
        val p2x = baseX - perpX * size * 0.4
        val p2y = baseY - perpY * size * 0.4
        val arrow = FxPolygon(tipX, tipY, p1x, p1y, p2x, p2y)
        arrow.fill = Color.BLACK
        arrow.stroke = Color.BLACK
        group.children.add(arrow)
    }

    // --- SVG Export ---

    private fun exportSvg(settings: ExportSettings, manager: GeometricObjectManager, file: File) {
        val w = settings.outputWidth
        val h = settings.outputHeight
        val scaleX = w.toDouble() / (settings.boundsMaxX - settings.boundsMinX)
        val scaleY = h.toDouble() / (settings.boundsMaxY - settings.boundsMinY)
        val scale = min(scaleX, scaleY)

        val tx = { mx: Double -> (mx - settings.boundsMinX) * scale }
        val ty = { my: Double -> (settings.boundsMaxY - my) * scale }

        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<svg xmlns="http://www.w3.org/2000/svg" width="$w" height="$h" viewBox="0 0 $w $h">""")

        // Background
        if (!settings.transparentBackground) {
            sb.appendLine("""  <rect width="$w" height="$h" fill="white"/>""")
        }

        if (settings.includeGrid) {
            renderGridSvg(sb, settings, scale, tx, ty)
        }

        renderObjectsSvg(sb, manager, settings, scale, tx, ty)

        sb.appendLine("</svg>")
        file.writeText(sb.toString())
    }

    private fun renderGridSvg(sb: StringBuilder, settings: ExportSettings, scale: Double, tx: (Double) -> Double, ty: (Double) -> Double) {
        val bMinX = settings.boundsMinX
        val bMinY = settings.boundsMinY
        val bMaxX = settings.boundsMaxX
        val bMaxY = settings.boundsMaxY
        val w = settings.outputWidth.toDouble()
        val h = settings.outputHeight.toDouble()

        val rawSpacing = 50.0 / scale
        val magnitude = 10.0.pow(floor(log10(rawSpacing)))
        val normalized = rawSpacing / magnitude
        val spacing = when {
            normalized < 2.0 -> magnitude
            normalized < 5.0 -> 2.0 * magnitude
            else -> 5.0 * magnitude
        }

        // Vertical lines
        var x = floor(bMinX / spacing) * spacing
        while (x <= bMaxX) {
            val px = tx(x)
            if (px in 0.0..w) {
                val isAxis = abs(x) < spacing * 0.01
                val color = if (isAxis) "#808080" else "#d9d9d9"
                val sw = if (isAxis) 1.0 else 0.5
                sb.appendLine("""  <line x1="${fmt(px)}" y1="0" x2="${fmt(px)}" y2="$h" stroke="$color" stroke-width="$sw"/>""")
            }
            x += spacing
        }

        // Horizontal lines
        var y = floor(bMinY / spacing) * spacing
        while (y <= bMaxY) {
            val py = ty(y)
            if (py in 0.0..h) {
                val isAxis = abs(y) < spacing * 0.01
                val color = if (isAxis) "#808080" else "#d9d9d9"
                val sw = if (isAxis) 1.0 else 0.5
                sb.appendLine("""  <line x1="0" y1="${fmt(py)}" x2="$w" y2="${fmt(py)}" stroke="$color" stroke-width="$sw"/>""")
            }
            y += spacing
        }
    }

    private fun renderObjectsSvg(sb: StringBuilder, manager: GeometricObjectManager, settings: ExportSettings, scale: Double, tx: (Double) -> Double, ty: (Double) -> Double) {
        val w = settings.outputWidth.toDouble()
        val h = settings.outputHeight.toDouble()
        // 5% margin for line/ray clipping so arrowheads don't touch the edge
        val clipMarginX = w * 0.05
        val clipMarginY = h * 0.05

        // Pass 1: Regions
        for (obj in manager.allObjects()) {
            if (obj !is Region || !obj.isDefined) continue
            val meta = manager.getMetadata(obj.id) ?: continue
            if (!isExportable(meta)) continue
            if (obj.vertices.size >= 3) {
                val points = obj.vertices.joinToString(" ") { (vx, vy) ->
                    "${fmt(tx(vx))},${fmt(ty(vy))}"
                }
                sb.appendLine("""  <polygon points="$points" fill="#FFEB3B" fill-opacity="0.3" stroke="none"/>""")
            }
        }

        // Pass 2: Angles
        for (obj in manager.allObjects()) {
            if (obj !is Angle || !obj.isDefined) continue
            val meta = manager.getMetadata(obj.id) ?: continue
            if (!isExportable(meta)) continue

            val cx = tx(obj.vertexX)
            val cy = ty(obj.vertexY)
            val r = ANGLE_ARC_RADIUS

            // Start and end angles in pixel space (y-down = negate from math y-up)
            val startAngle = -obj.startAngleRadians
            val endAngle = startAngle - obj.sweepAngleRadians // CW in math → more negative in pixel

            val x1 = cx + r * cos(startAngle)
            val y1 = cy + r * sin(startAngle)
            val x2 = cx + r * cos(endAngle)
            val y2 = cy + r * sin(endAngle)

            val largeArc = if (obj.sweepAngleRadians > PI) 1 else 0
            // In pixel coords, CW sweep becomes CW in SVG, which is sweep-flag=1
            val sweepFlag = 1

            sb.appendLine("""  <path d="M ${fmt(cx)} ${fmt(cy)} L ${fmt(x1)} ${fmt(y1)} A $r $r 0 $largeArc $sweepFlag ${fmt(x2)} ${fmt(y2)} Z" fill="#4CAF50" fill-opacity="0.3" stroke="#4CAF50" stroke-opacity="0.7" stroke-width="1"/>""")
        }

        // Pass 3: Lines/Rays/Segments/Circles
        for (obj in manager.allObjects()) {
            val meta = manager.getMetadata(obj.id) ?: continue
            if (!isExportable(meta) || !obj.isDefined) continue

            if (obj is LinearLike) {
                val dx = obj.x2 - obj.x1
                val dy = obj.y2 - obj.y1
                val len = hypot(dx, dy)
                if (len == 0.0) continue
                val ux = dx / len
                val uy = dy / len
                val extent = 100000.0

                when (obj.linearType) {
                    LinearType.SEGMENT -> {
                        sb.appendLine("""  <line x1="${fmt(tx(obj.x1))}" y1="${fmt(ty(obj.y1))}" x2="${fmt(tx(obj.x2))}" y2="${fmt(ty(obj.y2))}" stroke="black" stroke-width="$STROKE_WIDTH"/>""")
                    }
                    LinearType.LINE -> {
                        val ex1 = obj.x1 - ux * extent
                        val ey1 = obj.y1 - uy * extent
                        val ex2 = obj.x1 + ux * extent
                        val ey2 = obj.y1 + uy * extent
                        val clipped = clipLineToRect(tx(ex1), ty(ey1), tx(ex2), ty(ey2), clipMarginX, clipMarginY, w - clipMarginX, h - clipMarginY)
                        if (clipped != null) {
                            sb.appendLine("""  <line x1="${fmt(clipped[0])}" y1="${fmt(clipped[1])}" x2="${fmt(clipped[2])}" y2="${fmt(clipped[3])}" stroke="black" stroke-width="$STROKE_WIDTH"/>""")
                            val adx = clipped[2] - clipped[0]
                            val ady = clipped[3] - clipped[1]
                            val alen = hypot(adx, ady)
                            if (alen > 0) {
                                addArrowheadSvg(sb, clipped[2], clipped[3], adx / alen, ady / alen)
                                addArrowheadSvg(sb, clipped[0], clipped[1], -adx / alen, -ady / alen)
                            }
                        }
                    }
                    LinearType.RAY -> {
                        val ex = obj.x1 + ux * extent
                        val ey = obj.y1 + uy * extent
                        val clipped = clipLineToRect(tx(obj.x1), ty(obj.y1), tx(ex), ty(ey), clipMarginX, clipMarginY, w - clipMarginX, h - clipMarginY)
                        if (clipped != null) {
                            sb.appendLine("""  <line x1="${fmt(clipped[0])}" y1="${fmt(clipped[1])}" x2="${fmt(clipped[2])}" y2="${fmt(clipped[3])}" stroke="black" stroke-width="$STROKE_WIDTH"/>""")
                            val adx = clipped[2] - clipped[0]
                            val ady = clipped[3] - clipped[1]
                            val alen = hypot(adx, ady)
                            if (alen > 0) {
                                addArrowheadSvg(sb, clipped[2], clipped[3], adx / alen, ady / alen)
                            }
                        }
                    }
                }
            } else if (obj is Circle) {
                val cr = obj.radius * scale
                sb.appendLine("""  <circle cx="${fmt(tx(obj.cx))}" cy="${fmt(ty(obj.cy))}" r="${fmt(cr)}" fill="none" stroke="black" stroke-width="$STROKE_WIDTH"/>""")
            }
        }

        // Pass 4: Points
        for (obj in manager.allObjects()) {
            if (obj !is PointLike || !obj.isDefined) continue
            val meta = manager.getMetadata(obj.id) ?: continue
            if (!isExportable(meta)) continue
            sb.appendLine("""  <circle cx="${fmt(tx(obj.x))}" cy="${fmt(ty(obj.y))}" r="$POINT_RADIUS" fill="black"/>""")
        }

        // Pass 5: Labels
        for (obj in manager.allObjects()) {
            val meta = manager.getMetadata(obj.id) ?: continue
            if (!isExportable(meta) || !meta.labelVisible || meta.label == null) continue
            if (!obj.isDefined) continue

            val anchor = getObjectAnchor(obj, meta) ?: continue
            val lx = tx(anchor.first) + meta.labelOffsetX
            val ly = ty(anchor.second) + meta.labelOffsetY
            val escaped = meta.label!!.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            sb.appendLine("""  <text x="${fmt(lx)}" y="${fmt(ly)}" font-size="$FONT_SIZE" fill="black">$escaped</text>""")
        }
    }

    private fun addArrowheadSvg(sb: StringBuilder, tipX: Double, tipY: Double, dirX: Double, dirY: Double) {
        val size = ARROWHEAD_SIZE
        val perpX = -dirY
        val perpY = dirX
        val baseX = tipX - dirX * size
        val baseY = tipY - dirY * size
        val p1x = baseX + perpX * size * 0.4
        val p1y = baseY + perpY * size * 0.4
        val p2x = baseX - perpX * size * 0.4
        val p2y = baseY - perpY * size * 0.4
        sb.appendLine("""  <polygon points="${fmt(tipX)},${fmt(tipY)} ${fmt(p1x)},${fmt(p1y)} ${fmt(p2x)},${fmt(p2y)}" fill="black"/>""")
    }

    // --- Shared helpers ---

    private fun isExportable(meta: ObjectMetadata): Boolean =
        meta.visible && !meta.isLocked

    private fun getObjectAnchor(obj: GeometricObject, meta: ObjectMetadata): Pair<Double, Double>? {
        if (obj is PointLike) return Pair(obj.x, obj.y)
        if (obj is LinearLike) {
            val t = meta.labelAnchorT ?: if (obj.linearType == LinearType.RAY) 0.0 else 0.5
            return Pair(obj.x1 + t * (obj.x2 - obj.x1), obj.y1 + t * (obj.y2 - obj.y1))
        }
        return when (obj) {
            is Circle -> {
                val angle = meta.labelAnchorAngle ?: (PI / 2)
                Pair(obj.cx + obj.radius * cos(angle), obj.cy + obj.radius * sin(angle))
            }
            is Angle -> Pair(obj.vertexX, obj.vertexY)
            is Region -> {
                if (obj.vertices.isNotEmpty()) {
                    val cx = obj.vertices.sumOf { it.first } / obj.vertices.size
                    val cy = obj.vertices.sumOf { it.second } / obj.vertices.size
                    Pair(cx, cy)
                } else null
            }
            else -> null
        }
    }

    /** Liang-Barsky line clipping. Returns [x1, y1, x2, y2] or null if fully outside. */
    private fun clipLineToRect(
        x1: Double, y1: Double, x2: Double, y2: Double,
        xmin: Double, ymin: Double, xmax: Double, ymax: Double
    ): DoubleArray? {
        val dx = x2 - x1
        val dy = y2 - y1
        val p = doubleArrayOf(-dx, dx, -dy, dy)
        val q = doubleArrayOf(x1 - xmin, xmax - x1, y1 - ymin, ymax - y1)

        var t0 = 0.0
        var t1 = 1.0

        for (i in 0..3) {
            if (p[i] == 0.0) {
                if (q[i] < 0.0) return null
            } else {
                val t = q[i] / p[i]
                if (p[i] < 0.0) {
                    if (t > t1) return null
                    if (t > t0) t0 = t
                } else {
                    if (t < t0) return null
                    if (t < t1) t1 = t
                }
            }
        }

        return doubleArrayOf(
            x1 + t0 * dx, y1 + t0 * dy,
            x1 + t1 * dx, y1 + t1 * dy
        )
    }

    private fun fmt(v: Double): String = "%.2f".format(v)
}
