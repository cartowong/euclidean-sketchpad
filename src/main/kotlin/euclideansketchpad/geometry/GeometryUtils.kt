package euclideansketchpad.geometry

import kotlin.math.sqrt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs
import kotlin.math.hypot

object GeometryUtils {

    fun euclideanDistance(x1: Double, y1: Double, x2: Double, y2: Double): Double =
        hypot(x2 - x1, y2 - y1)

    /**
     * Distance from point (px, py) to the infinite line through (x1, y1) and (x2, y2).
     */
    fun pointToLineDistance(px: Double, py: Double, x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val dx = x2 - x1
        val dy = y2 - y1
        val lenSq = dx * dx + dy * dy
        if (lenSq == 0.0) return euclideanDistance(px, py, x1, y1)
        val cross = abs((px - x1) * dy - (py - y1) * dx)
        return cross / sqrt(lenSq)
    }

    /**
     * Distance from point (px, py) to segment from (x1, y1) to (x2, y2).
     */
    fun pointToSegmentDistance(px: Double, py: Double, x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val dx = x2 - x1
        val dy = y2 - y1
        val lenSq = dx * dx + dy * dy
        if (lenSq == 0.0) return euclideanDistance(px, py, x1, y1)
        val t = ((px - x1) * dx + (py - y1) * dy) / lenSq
        val clamped = t.coerceIn(0.0, 1.0)
        val projX = x1 + clamped * dx
        val projY = y1 + clamped * dy
        return euclideanDistance(px, py, projX, projY)
    }

    /**
     * Distance from point (px, py) to ray starting at (x1, y1) going through (x2, y2).
     */
    fun pointToRayDistance(px: Double, py: Double, x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val dx = x2 - x1
        val dy = y2 - y1
        val lenSq = dx * dx + dy * dy
        if (lenSq == 0.0) return euclideanDistance(px, py, x1, y1)
        val t = ((px - x1) * dx + (py - y1) * dy) / lenSq
        val clamped = max(0.0, t)
        val projX = x1 + clamped * dx
        val projY = y1 + clamped * dy
        return euclideanDistance(px, py, projX, projY)
    }

    /**
     * Distance from point (px, py) to circle with center (cx, cy) and radius r.
     */
    fun pointToCircleDistance(px: Double, py: Double, cx: Double, cy: Double, r: Double): Double {
        val distToCenter = euclideanDistance(px, py, cx, cy)
        return abs(distToCenter - r)
    }

    /**
     * Closest parameter t on the infinite line through (x1,y1)-(x2,y2) to point (px,py).
     * t=0 at (x1,y1), t=1 at (x2,y2).
     */
    fun closestParameterOnLine(px: Double, py: Double, x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val dx = x2 - x1
        val dy = y2 - y1
        val lenSq = dx * dx + dy * dy
        if (lenSq == 0.0) return 0.0
        return ((px - x1) * dx + (py - y1) * dy) / lenSq
    }

    /**
     * Liang-Barsky line clipping against an axis-aligned rectangle.
     * Returns clipped endpoints [x1, y1, x2, y2] or null if the segment is fully outside.
     */
    fun clipLineToRect(
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
}
