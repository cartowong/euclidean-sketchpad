package euclideansketchpad.ui

import euclideansketchpad.geometry.*
import euclideansketchpad.model.GeometricObjectManager
import euclideansketchpad.model.ManagerState
import euclideansketchpad.model.UndoManager
import javafx.scene.Group
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.scene.shape.Circle as FxCircle
import javafx.scene.shape.Line as FxLine
import javafx.scene.transform.Affine
import kotlin.math.abs
import kotlin.math.hypot

enum class LineSubMode { LINE, RAY, SEGMENT }

class LineTool(
    private val manager: GeometricObjectManager,
    private val hitTester: HitTester,
    private val undoManager: UndoManager,
    private val previewGroup: Group,
    private val transform: Affine,
    private val requestRender: () -> Unit
) : Tool {

    var subMode: LineSubMode = LineSubMode.SEGMENT

    private companion object {
        const val POINT_RADIUS = 4.0
        const val STROKE_WIDTH = 2.0
        const val LINE_EXTENT = 10000.0
    }

    // First point state
    private var firstPointId: Int? = null
    private var firstX: Double = 0.0
    private var firstY: Double = 0.0
    private var preConstructionSnapshot: ManagerState? = null

    override fun activate() {
        reset()
    }

    override fun deactivate(previewGroup: Group) {
        if (preConstructionSnapshot != null) {
            manager.restore(preConstructionSnapshot!!)
            preConstructionSnapshot = null
            hitTester.rebuildIntersectionCache()
        }
        reset()
        previewGroup.children.clear()
    }

    override fun onMouseDragged(e: MouseEvent, worldX: Double, worldY: Double) = onMouseMoved(e, worldX, worldY)

    override fun onMouseMoved(e: MouseEvent, worldX: Double, worldY: Double) {
        previewGroup.children.clear()
        val scale = abs(transform.mxx)
        val counterScale = if (scale > 0) 1.0 / scale else 1.0

        // Show ghost at intersection preview
        val hit = hitTester.hitTest(e.x, e.y)
        if (hit is HitResult.IntersectionPreview) {
            val ghost = FxCircle(hit.candidate.x, hit.candidate.y, POINT_RADIUS * counterScale)
            ghost.fill = Color.BLACK
            ghost.opacity = 0.4
            ghost.isMouseTransparent = true
            previewGroup.children.add(ghost)
        }

        if (firstPointId != null) {
            // Show preview line from first point to cursor
            val targetX = if (hit is HitResult.ConstructedPoint) {
                (manager.getObject(hit.id) as? PointLike)?.x ?: worldX
            } else if (hit is HitResult.IntersectionPreview) {
                hit.candidate.x
            } else worldX

            val targetY = if (hit is HitResult.ConstructedPoint) {
                (manager.getObject(hit.id) as? PointLike)?.y ?: worldY
            } else if (hit is HitResult.IntersectionPreview) {
                hit.candidate.y
            } else worldY

            drawPreviewLine(firstX, firstY, targetX, targetY, counterScale)
        }
    }

    override fun onMousePressed(e: MouseEvent, worldX: Double, worldY: Double) {
        val hit = hitTester.hitTest(e.x, e.y)

        if (firstPointId == null) {
            // First click: set first point
            preConstructionSnapshot = manager.snapshot()
            val pointId = ToolUtils.resolveOrCreatePoint(hit, worldX, worldY, manager, hitTester, requestRender)
            val pt = manager.getObject(pointId) as? PointLike ?: return
            firstPointId = pointId
            firstX = pt.x
            firstY = pt.y
        } else {
            // Second click: set second point and create linear object
            val secondPointId = ToolUtils.resolveOrCreatePoint(hit, worldX, worldY, manager, hitTester, requestRender)
            if (secondPointId == firstPointId) return // same point, ignore

            when (subMode) {
                LineSubMode.LINE -> manager.addObject { id -> Line(id, firstPointId!!, secondPointId) }
                LineSubMode.RAY -> manager.addObject { id -> Ray(id, firstPointId!!, secondPointId) }
                LineSubMode.SEGMENT -> manager.addObject { id -> Segment(id, firstPointId!!, secondPointId) }
            }
            preConstructionSnapshot = null
            reset()
            hitTester.rebuildIntersectionCache()
            undoManager.checkpoint()
            requestRender()
        }
        previewGroup.children.clear()
    }

    override fun onEscapePressed(previewGroup: Group): Boolean {
        val hadWork = preConstructionSnapshot != null
        if (hadWork) {
            manager.restore(preConstructionSnapshot!!)
            preConstructionSnapshot = null
            hitTester.rebuildIntersectionCache()
        }
        reset()
        previewGroup.children.clear()
        requestRender()
        return hadWork
    }

    private fun drawPreviewLine(x1: Double, y1: Double, x2: Double, y2: Double, counterScale: Double) {
        val dx = x2 - x1
        val dy = y2 - y1
        val len = hypot(dx, dy)
        if (len == 0.0) return

        val line = when (subMode) {
            LineSubMode.SEGMENT -> FxLine(x1, y1, x2, y2)
            LineSubMode.RAY -> {
                val ux = dx / len; val uy = dy / len
                FxLine(x1, y1, x1 + ux * LINE_EXTENT, y1 + uy * LINE_EXTENT)
            }
            LineSubMode.LINE -> {
                val ux = dx / len; val uy = dy / len
                FxLine(x1 - ux * LINE_EXTENT, y1 - uy * LINE_EXTENT, x1 + ux * LINE_EXTENT, y1 + uy * LINE_EXTENT)
            }
        }
        line.strokeWidth = STROKE_WIDTH * counterScale
        line.stroke = Color.BLACK
        line.opacity = 0.4
        line.isMouseTransparent = true
        previewGroup.children.add(line)

        // Ghost endpoints
        val ghost1 = FxCircle(x1, y1, POINT_RADIUS * counterScale)
        ghost1.fill = Color.BLACK; ghost1.opacity = 0.4; ghost1.isMouseTransparent = true
        val ghost2 = FxCircle(x2, y2, POINT_RADIUS * counterScale)
        ghost2.fill = Color.BLACK; ghost2.opacity = 0.4; ghost2.isMouseTransparent = true
        previewGroup.children.addAll(ghost1, ghost2)
    }

    private fun reset() {
        firstPointId = null
    }
}
