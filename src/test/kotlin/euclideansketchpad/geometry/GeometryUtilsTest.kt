package euclideansketchpad.geometry

import org.junit.jupiter.api.Test
import kotlin.math.sqrt
import kotlin.test.assertEquals

class GeometryUtilsTest {

    private val eps = 1e-9

    @Test
    fun `euclidean distance between two points`() {
        assertEquals(5.0, GeometryUtils.euclideanDistance(0.0, 0.0, 3.0, 4.0), eps)
        assertEquals(0.0, GeometryUtils.euclideanDistance(1.0, 1.0, 1.0, 1.0), eps)
    }

    @Test
    fun `point to line distance`() {
        // Point (0,1) to line through (0,0)-(1,0) = 1
        assertEquals(1.0, GeometryUtils.pointToLineDistance(0.0, 1.0, 0.0, 0.0, 1.0, 0.0), eps)
        // Point on the line
        assertEquals(0.0, GeometryUtils.pointToLineDistance(0.5, 0.0, 0.0, 0.0, 1.0, 0.0), eps)
        // Point to diagonal line y=x: (1,0) to line (0,0)-(1,1) = 1/sqrt(2)
        assertEquals(1.0 / sqrt(2.0), GeometryUtils.pointToLineDistance(1.0, 0.0, 0.0, 0.0, 1.0, 1.0), eps)
    }

    @Test
    fun `point to segment distance - projection within segment`() {
        assertEquals(1.0, GeometryUtils.pointToSegmentDistance(0.5, 1.0, 0.0, 0.0, 1.0, 0.0), eps)
    }

    @Test
    fun `point to segment distance - projection outside segment clamps to endpoint`() {
        // Point (2, 0) to segment (0,0)-(1,0): nearest is (1,0), distance = 1
        assertEquals(1.0, GeometryUtils.pointToSegmentDistance(2.0, 0.0, 0.0, 0.0, 1.0, 0.0), eps)
        // Point (-1, 0) to segment (0,0)-(1,0): nearest is (0,0), distance = 1
        assertEquals(1.0, GeometryUtils.pointToSegmentDistance(-1.0, 0.0, 0.0, 0.0, 1.0, 0.0), eps)
    }

    @Test
    fun `point to ray distance - behind endpoint clamps`() {
        // Point (-1, 0) to ray from (0,0) through (1,0): nearest is (0,0), distance = 1
        assertEquals(1.0, GeometryUtils.pointToRayDistance(-1.0, 0.0, 0.0, 0.0, 1.0, 0.0), eps)
    }

    @Test
    fun `point to ray distance - forward projects normally`() {
        // Point (5, 1) to ray from (0,0) through (1,0): nearest is (5,0), distance = 1
        assertEquals(1.0, GeometryUtils.pointToRayDistance(5.0, 1.0, 0.0, 0.0, 1.0, 0.0), eps)
    }

    @Test
    fun `point to circle distance`() {
        // Point on the circle
        assertEquals(0.0, GeometryUtils.pointToCircleDistance(5.0, 0.0, 0.0, 0.0, 5.0), eps)
        // Point inside the circle
        assertEquals(2.0, GeometryUtils.pointToCircleDistance(3.0, 0.0, 0.0, 0.0, 5.0), eps)
        // Point outside the circle
        assertEquals(3.0, GeometryUtils.pointToCircleDistance(8.0, 0.0, 0.0, 0.0, 5.0), eps)
    }

    @Test
    fun `closest parameter on line`() {
        // Midpoint of (0,0)-(2,0) is at t=0.5
        assertEquals(0.5, GeometryUtils.closestParameterOnLine(1.0, 5.0, 0.0, 0.0, 2.0, 0.0), eps)
        // Before start
        assertEquals(-0.5, GeometryUtils.closestParameterOnLine(-1.0, 0.0, 0.0, 0.0, 2.0, 0.0), eps)
    }
}
