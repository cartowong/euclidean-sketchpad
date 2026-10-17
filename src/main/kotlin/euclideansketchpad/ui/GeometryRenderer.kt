package euclideansketchpad.ui

import euclideansketchpad.geometry.*
import euclideansketchpad.model.GeometricObjectManager
import javafx.scene.Group
import javafx.scene.shape.Arc
import javafx.scene.shape.ArcType
import javafx.scene.shape.Circle as FxCircle
import javafx.scene.shape.Line as FxLine
import javafx.scene.shape.Polygon as FxPolygon
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.transform.Affine
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val SELECTED_COLOR = Color.web("#2196F3")
private val HOVER_COLOR = Color.web("#2196F3")
private val REGION_FILL = Color.web("#FFEB3B", 0.3)
private val ANGLE_FILL = Color.web("#4CAF50", 0.3)
private val PREVIEW_COLOR = Color.web("#FF5722")
private const val PREVIEW_OPACITY = 0.4

object GeometryRenderer {

    private fun isRenderable(meta: euclideansketchpad.model.ObjectMetadata): Boolean =
        meta.visible && !meta.isLocked

    private const val POINT_RADIUS = 4.0
    private const val STROKE_WIDTH = 2.0
    private const val LINE_EXTENT = 10000.0
    private const val ANGLE_ARC_RADIUS = 20.0
    private const val ARROWHEAD_SIZE = 8.0

    fun renderAll(
        manager: GeometricObjectManager,
        group: Group,
        transform: Affine,
        selectedIds: Set<Int> = emptySet(),
        hoveredId: Int? = null
    ) {
        group.children.clear()
        val scale = transform.mxx
        val counterScale = if (scale != 0.0) 1.0 / scale else 1.0

        // Pass 1: Regions (bottom)
        for (obj in manager.allObjects()) {
            if (obj !is Region || !obj.isDefined) continue
            val meta = manager.getMetadata(obj.id) ?: continue
            if (!isRenderable(meta)) continue
            val isSelected = obj.id in selectedIds
            val isHovered = obj.id == hoveredId

            if (obj.vertices.size >= 3) {
                val coords = DoubleArray(obj.vertices.size * 2)
                for (i in obj.vertices.indices) {
                    coords[i * 2] = obj.vertices[i].first
                    coords[i * 2 + 1] = obj.vertices[i].second
                }
                val polygon = FxPolygon(*coords)
                polygon.fill = when {
                    isSelected || isHovered -> Color.web("#2196F3", 0.4)
                    else -> REGION_FILL
                }
                polygon.stroke = null
                polygon.strokeWidth = 0.0
                polygon.styleClass.add("region")
                // Color set programmatically; no CSS class needed
                group.children.add(polygon)
            }
        }

        // Pass 2: Angles
        for (obj in manager.allObjects()) {
            if (obj !is Angle || !obj.isDefined) continue
            val meta = manager.getMetadata(obj.id) ?: continue
            if (!isRenderable(meta)) continue
            val isSelected = obj.id in selectedIds
            val isHovered = obj.id == hoveredId

            val arcRadius = ANGLE_ARC_RADIUS * counterScale
            // JavaFX Arc angles are in degrees, measured counter-clockwise from positive x-axis
            // But our world group has y-flipped (myy=-1), so JavaFX CCW becomes CW visually
            // Our startAngleRadians is the angle of ray BA, sweepAngleRadians is the CW sweep
            // In JavaFX coords (with y-flip), CW sweep = negative JavaFX sweep
            val startDeg = -obj.startAngleRadians * 180.0 / PI // negate for y-flip
            val sweepDeg = obj.sweepAngleRadians * 180.0 / PI // positive = CW visual after y-flip

            val arc = Arc(obj.vertexX, obj.vertexY, arcRadius, arcRadius, startDeg, sweepDeg)
            arc.type = ArcType.ROUND
            arc.fill = when {
                isSelected || isHovered -> Color.web("#2196F3", 0.5)
                else -> ANGLE_FILL
            }
            arc.stroke = if (isSelected) SELECTED_COLOR else if (isHovered) HOVER_COLOR else Color.web("#4CAF50", 0.7)
            arc.strokeWidth = 1.0 * counterScale
            arc.styleClass.add("angle")
            // Color set programmatically; no CSS class needed
            group.children.add(arc)
        }

        // Pass 3: Lines/Rays/Segments/Circles
        for (obj in manager.allObjects()) {
            val meta = manager.getMetadata(obj.id) ?: continue
            if (!isRenderable(meta) || !obj.isDefined) continue
            val isSelected = obj.id in selectedIds
            val isHovered = obj.id == hoveredId
            val color = when {
                isSelected -> SELECTED_COLOR
                isHovered -> HOVER_COLOR
                else -> Color.BLACK
            }
            val strokeWidth = if (isHovered && !isSelected) STROKE_WIDTH * 1.5 * counterScale else STROKE_WIDTH * counterScale

            if (obj is LinearLike) {
                val dx = obj.x2 - obj.x1
                val dy = obj.y2 - obj.y1
                val len = kotlin.math.hypot(dx, dy)
                if (len == 0.0) continue
                val ux = dx / len
                val uy = dy / len

                when (obj.linearType) {
                    LinearType.SEGMENT -> {
                        val line = FxLine(obj.x1, obj.y1, obj.x2, obj.y2)
                        line.strokeWidth = strokeWidth
                        line.stroke = color
                        line.styleClass.add("segment")
                        group.children.add(line)
                    }
                    LinearType.LINE -> {
                        val line = FxLine(
                            obj.x1 - ux * LINE_EXTENT, obj.y1 - uy * LINE_EXTENT,
                            obj.x1 + ux * LINE_EXTENT, obj.y1 + uy * LINE_EXTENT
                        )
                        line.strokeWidth = strokeWidth
                        line.stroke = color
                        line.styleClass.add("line")
                        group.children.add(line)
                        drawArrowhead(group, obj.x1 + ux * LINE_EXTENT, obj.y1 + uy * LINE_EXTENT, ux, uy, counterScale, color)
                        drawArrowhead(group, obj.x1 - ux * LINE_EXTENT, obj.y1 - uy * LINE_EXTENT, -ux, -uy, counterScale, color)
                    }
                    LinearType.RAY -> {
                        val line = FxLine(obj.x1, obj.y1, obj.x1 + ux * LINE_EXTENT, obj.y1 + uy * LINE_EXTENT)
                        line.strokeWidth = strokeWidth
                        line.stroke = color
                        line.styleClass.add("ray")
                        group.children.add(line)
                        drawArrowhead(group, obj.x1 + ux * LINE_EXTENT, obj.y1 + uy * LINE_EXTENT, ux, uy, counterScale, color)
                    }
                }
            } else if (obj is Circle) {
                val circle = FxCircle(obj.cx, obj.cy, obj.radius)
                circle.fill = Color.TRANSPARENT
                circle.strokeWidth = strokeWidth
                circle.stroke = color
                group.children.add(circle)
            }
        }

        // Pass 4: Points on top
        for (obj in manager.allObjects()) {
            if (obj !is PointLike || !obj.isDefined) continue
            val meta = manager.getMetadata(obj.id) ?: continue
            if (!isRenderable(meta)) continue
            val isSelected = obj.id in selectedIds
            val isHovered = obj.id == hoveredId
            val color = when {
                isSelected -> SELECTED_COLOR
                isHovered -> HOVER_COLOR
                else -> Color.BLACK
            }
            val radius = if (isHovered && !isSelected) POINT_RADIUS * 1.3 * counterScale else POINT_RADIUS * counterScale
            val ptCircle = FxCircle(obj.x, obj.y, radius)
            ptCircle.fill = color
            ptCircle.styleClass.add("point")
            group.children.add(ptCircle)
        }

        // Pass 5: Labels
        for (obj in manager.allObjects()) {
            val meta = manager.getMetadata(obj.id) ?: continue
            if (!isRenderable(meta) || !meta.labelVisible || meta.label == null) continue
            if (!obj.isDefined) continue

            val (anchorX, anchorY) = getObjectAnchor(obj, meta) ?: continue
            val text = Text(meta.label)
            text.font = Font.font(12.0 * counterScale)
            // Position with pixel offset converted to world coords
            text.x = anchorX + meta.labelOffsetX * counterScale
            text.y = anchorY + meta.labelOffsetY * counterScale
            // Counter the y-flip so text isn't mirrored
            text.scaleY = -1.0
            text.fill = Color.BLACK
            text.styleClass.add("label")
            group.children.add(text)
        }
    }

    fun renderPreviewObjects(
        objects: List<GeometricObject>,
        group: Group,
        transform: Affine
    ) {
        val scale = transform.mxx
        val counterScale = if (scale != 0.0) 1.0 / scale else 1.0
        val strokeWidth = STROKE_WIDTH * counterScale
        val color = PREVIEW_COLOR

        // Pass 1: Regions (bottom)
        for (obj in objects) {
            if (obj !is Region || !obj.isDefined) continue
            if (obj.vertices.size >= 3) {
                val coords = DoubleArray(obj.vertices.size * 2)
                for (i in obj.vertices.indices) {
                    coords[i * 2] = obj.vertices[i].first
                    coords[i * 2 + 1] = obj.vertices[i].second
                }
                val polygon = FxPolygon(*coords)
                polygon.fill = Color.web("#FF5722", 0.2)
                polygon.stroke = null
                polygon.opacity = PREVIEW_OPACITY
                polygon.isMouseTransparent = true
                group.children.add(polygon)
            }
        }

        // Pass 2: Angles
        for (obj in objects) {
            if (obj !is Angle || !obj.isDefined) continue
            val arcRadius = ANGLE_ARC_RADIUS * counterScale
            val startDeg = -obj.startAngleRadians * 180.0 / PI
            val sweepDeg = obj.sweepAngleRadians * 180.0 / PI
            val arc = Arc(obj.vertexX, obj.vertexY, arcRadius, arcRadius, startDeg, sweepDeg)
            arc.type = ArcType.ROUND
            arc.fill = Color.web("#FF5722", 0.2)
            arc.stroke = color
            arc.strokeWidth = 1.0 * counterScale
            arc.opacity = PREVIEW_OPACITY
            arc.isMouseTransparent = true
            group.children.add(arc)
        }

        // Pass 3: Lines/Rays/Segments/Circles
        for (obj in objects) {
            if (!obj.isDefined) continue
            if (obj is LinearLike) {
                val dx = obj.x2 - obj.x1
                val dy = obj.y2 - obj.y1
                val len = kotlin.math.hypot(dx, dy)
                if (len == 0.0) continue
                val ux = dx / len
                val uy = dy / len

                when (obj.linearType) {
                    LinearType.SEGMENT -> {
                        val line = FxLine(obj.x1, obj.y1, obj.x2, obj.y2)
                        line.strokeWidth = strokeWidth
                        line.stroke = color
                        line.opacity = PREVIEW_OPACITY
                        line.isMouseTransparent = true
                        group.children.add(line)
                    }
                    LinearType.LINE -> {
                        val line = FxLine(
                            obj.x1 - ux * LINE_EXTENT, obj.y1 - uy * LINE_EXTENT,
                            obj.x1 + ux * LINE_EXTENT, obj.y1 + uy * LINE_EXTENT
                        )
                        line.strokeWidth = strokeWidth
                        line.stroke = color
                        line.opacity = PREVIEW_OPACITY
                        line.isMouseTransparent = true
                        group.children.add(line)
                        drawArrowhead(group, obj.x1 + ux * LINE_EXTENT, obj.y1 + uy * LINE_EXTENT, ux, uy, counterScale, color, PREVIEW_OPACITY)
                        drawArrowhead(group, obj.x1 - ux * LINE_EXTENT, obj.y1 - uy * LINE_EXTENT, -ux, -uy, counterScale, color, PREVIEW_OPACITY)
                    }
                    LinearType.RAY -> {
                        val line = FxLine(obj.x1, obj.y1, obj.x1 + ux * LINE_EXTENT, obj.y1 + uy * LINE_EXTENT)
                        line.strokeWidth = strokeWidth
                        line.stroke = color
                        line.opacity = PREVIEW_OPACITY
                        line.isMouseTransparent = true
                        group.children.add(line)
                        drawArrowhead(group, obj.x1 + ux * LINE_EXTENT, obj.y1 + uy * LINE_EXTENT, ux, uy, counterScale, color, PREVIEW_OPACITY)
                    }
                }
            } else if (obj is Circle) {
                val circle = FxCircle(obj.cx, obj.cy, obj.radius)
                circle.fill = Color.TRANSPARENT
                circle.strokeWidth = strokeWidth
                circle.stroke = color
                circle.opacity = PREVIEW_OPACITY
                circle.isMouseTransparent = true
                group.children.add(circle)
            }
        }

        // Pass 4: Points on top
        for (obj in objects) {
            if (obj !is PointLike || !obj.isDefined) continue
            val radius = POINT_RADIUS * counterScale
            val ptCircle = FxCircle(obj.x, obj.y, radius)
            ptCircle.fill = color
            ptCircle.opacity = PREVIEW_OPACITY
            ptCircle.isMouseTransparent = true
            group.children.add(ptCircle)
        }
    }

    private fun drawArrowhead(
        group: Group,
        tipX: Double, tipY: Double,
        dirX: Double, dirY: Double,
        counterScale: Double, color: Color,
        opacity: Double = 1.0
    ) {
        val size = ARROWHEAD_SIZE * counterScale
        // Two base points of the arrowhead triangle
        val perpX = -dirY
        val perpY = dirX
        val baseX = tipX - dirX * size
        val baseY = tipY - dirY * size
        val p1x = baseX + perpX * size * 0.4
        val p1y = baseY + perpY * size * 0.4
        val p2x = baseX - perpX * size * 0.4
        val p2y = baseY - perpY * size * 0.4

        val arrow = FxPolygon(tipX, tipY, p1x, p1y, p2x, p2y)
        arrow.fill = color
        arrow.stroke = color
        arrow.isMouseTransparent = true
        if (opacity < 1.0) arrow.opacity = opacity
        group.children.add(arrow)
    }

    private fun getObjectAnchor(obj: GeometricObject, meta: euclideansketchpad.model.ObjectMetadata? = null): Pair<Double, Double>? {
        if (obj is PointLike) return Pair(obj.x, obj.y)
        if (obj is LinearLike) {
            val t = meta?.labelAnchorT ?: if (obj.linearType == LinearType.RAY) 0.0 else 0.5
            return Pair(obj.x1 + t * (obj.x2 - obj.x1), obj.y1 + t * (obj.y2 - obj.y1))
        }
        return when (obj) {
            is Circle -> {
                val angle = meta?.labelAnchorAngle ?: (PI / 2)
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
}
