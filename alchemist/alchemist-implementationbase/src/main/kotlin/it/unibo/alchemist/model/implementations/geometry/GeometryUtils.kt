package it.unibo.alchemist.model.implementations.geometry

import it.unibo.alchemist.model.implementations.positions.Euclidean2DPosition
import it.unibo.alchemist.model.interfaces.geometry.Vector
import it.unibo.alchemist.model.interfaces.geometry.euclidean.twod.Euclidean2DSegment
import org.danilopianini.lang.MathUtils.fuzzyEquals
import java.awt.Shape
import java.awt.geom.PathIterator
import java.awt.geom.Point2D
import java.util.Optional
import kotlin.math.*

/**
 * Computes the angle with atan2(y, x)
 *
 * @return atan2(y, x) (in radians)
 */
fun Euclidean2DPosition.asAngle() = atan2(y, x)

/**
 * Obtains the vertices of a polygonal shape. Any curved segment connecting
 * two points will be considered as a straight line between them.
 */
fun Shape.vertices(): List<Euclidean2DPosition> {
    val vertices = mutableListOf<Euclidean2DPosition>()
    val coords = DoubleArray(6)
    val iterator = getPathIterator(null)
    while (!iterator.isDone) {
        when (iterator.currentSegment(coords)) {
            PathIterator.SEG_MOVETO, PathIterator.SEG_LINETO -> vertices.add(Euclidean2DPosition(coords[0], coords[1]))
        }
        iterator.next()
    }
    return vertices
}

/**
 * Multiplies each coordinate of a vector for a scalar number n.
 */
fun Euclidean2DPosition.times(n: Double) = Euclidean2DPosition(x * n, y * n)

/**
 * Normalizes the vector.
 */
fun Euclidean2DPosition.normalize(): Euclidean2DPosition = times(1.0 / sqrt(x * x + y * y))

/**
 * Resizes the vector in order for it to have a length equal
 * to the specified parameter. Its direction and verse are preserved.
 */
fun Euclidean2DPosition.resize(newLen: Double): Euclidean2DPosition = normalize().times(newLen)

/**
 * Find the normal of a vector.
 */
fun Euclidean2DPosition.normal(): Euclidean2DPosition = Euclidean2DPosition(-y, x)

/**
 * Computes the z component of the cross product of the given vectors.
 */
fun zCross(v1: Euclidean2DPosition, v2: Euclidean2DPosition) = v1.x * v2.y - v1.y * v2.x

/**
 * Dot product between bidimensional vectors.
 */
fun Euclidean2DPosition.dot(v: Euclidean2DPosition) = x * v.x + y * v.y

/**
 * Checks whether the given point is inside a rectangular region starting in
 * lowerBound and ending in upperBound (bounds are included).
 */
fun isInBoundaries(p: Euclidean2DPosition, lowerBound: Point2D, upperBound: Point2D) =
    p.x >= lowerBound.x && p.y >= lowerBound.y && p.x <= upperBound.x && p.y <= upperBound.y

/**
 * Checks whether the given edge is inside a rectangular region starting in
 * lowerBound and ending in upperBound (bounds are included).
 */
fun isInBoundaries(e: Euclidean2DSegment, lowerBound: Point2D, upperBound: Point2D) =
    isInBoundaries(e.first, lowerBound, upperBound) && isInBoundaries(e.second, lowerBound, upperBound)

/**
 * Mutates an edge to a vector. In particular, the vector representing the
 * movement from the first point to the second point of the edge.
 */
fun Euclidean2DSegment.toVector() = second - first

/**
 * Computes the slope of the line passing through a couple of points.
 * If the points coincide NaN is the result.
 */
fun Euclidean2DSegment.slope() = toVector().run { y / x }

/**
 * An edge is degenerate if its points coincide (and its length is zero).
 */
fun Euclidean2DSegment.isDegenerate(): Boolean = first == second

/**
 * Checks whether the segment (represented by a pair of positions)
 * contains the given point.
 */
fun Euclidean2DSegment.contains(p: Euclidean2DPosition) =
    areCollinear(first, second, p) && p.x.liesBetween(first.x, second.x) && p.y.liesBetween(first.y, second.y)

/**
 * Computes the medium point of the current segment.
 */
fun Euclidean2DSegment.midPoint() = Euclidean2DPosition((first.x + second.x) / 2, (first.y + second.y) / 2)

/**
 * Determines if three points are collinear (i.e. they lie on the same line).
 */
fun areCollinear(p1: Euclidean2DPosition, p2: Euclidean2DPosition, p3: Euclidean2DPosition): Boolean {
    return if (fuzzyEquals(p1.x, p2.x)) {
        fuzzyEquals(p1.x, p3.x)
    } else {
        val m = Pair(p1, p2).slope()
        val q = p1.y - m * p1.x
        fuzzyEquals((m * p3.x + q), p3.y)
    }
}

/**
 * Checks if a value lies between two values (included) provided in any order.
 */
fun Double.liesBetween(v1: Double, v2: Double) = this >= min(v1, v2) && this <= max(v1, v2)

/**
 * Finds the intersection point of two given segments (if exists). This method is
 * able to deal with degenerate edges (of length zero) and collinear segments.
 */
fun intersection(s1: Euclidean2DSegment, s2: Euclidean2DSegment): SegmentsIntersectionResult {
    if (s1.isDegenerate() && s2.isDegenerate()) {
        return if (s1.first == s2.first) { // points coincide
            SegmentsIntersectionResult(SegmentsIntersectionTypes.POINT, Optional.of(s1.first))
        } else {
            SegmentsIntersectionResult(SegmentsIntersectionTypes.EMPTY)
        }
    }
    if (s1.isDegenerate() || s2.isDegenerate()) {
        val degenerate = if (s1.isDegenerate()) s1 else s2
        val other = if (degenerate == s1) s2 else s1
        return if (other.contains(degenerate.first)) {
            SegmentsIntersectionResult(SegmentsIntersectionTypes.POINT, Optional.of(degenerate.first))
        } else {
            SegmentsIntersectionResult(SegmentsIntersectionTypes.EMPTY)
        }
    }
    val p = s1.first
    val r = s1.toVector()
    val q = s2.first
    val s = s2.toVector()
    val denom = zCross(r, s)
    val num = zCross((q - p), r)
    if (fuzzyEquals(num, 0.0) && fuzzyEquals(denom, 0.0)) { // segments are collinear
        val t0 = (q - p).dot(r) / r.dot(r)
        val t1 = t0 + s.dot(r) / r.dot(r)
        if (Pair(t0, t1).intersects(0.0, 1.0)) { // segments are overlapping
            // we found out that segments are collinear and overlapping, but they may only share an endpoint,
            // in which case their intersection is a single point
            if ((fuzzyEquals(t0, 0.0) || fuzzyEquals(t0, 1.0)) && !t1.liesBetween(0.0, 1.0)) {
                return SegmentsIntersectionResult(SegmentsIntersectionTypes.POINT, Optional.of(s2.first))
            }
            if ((fuzzyEquals(t1, 0.0) || fuzzyEquals(t1, 1.0)) && !t0.liesBetween(0.0, 1.0)) {
                return SegmentsIntersectionResult(SegmentsIntersectionTypes.POINT, Optional.of(s2.second))
            }
            return SegmentsIntersectionResult(SegmentsIntersectionTypes.SEGMENT)
        } else { // collinear but disjoint
            return SegmentsIntersectionResult(SegmentsIntersectionTypes.EMPTY)
        }
    }
    if (fuzzyEquals(denom, 0.0) && !fuzzyEquals(num, 0.0)) { // parallel
        return SegmentsIntersectionResult(SegmentsIntersectionTypes.EMPTY)
    }
    val t = zCross((q - p), s) / denom
    val u = zCross((q - p), r) / denom
    if (t.liesBetween(0.0, 1.0) && u.liesBetween(0.0, 1.0)) {
        return SegmentsIntersectionResult(SegmentsIntersectionTypes.POINT, Optional.of(p + r.times(t)))
    }
    return SegmentsIntersectionResult(SegmentsIntersectionTypes.EMPTY) // not parallel but not intersecting
}

/**
 * Checks whether two intervals (inclusive) intersects.
 */
fun Pair<Double, Double>.intersects(start: Double, end: Double) =
    first.liesBetween(start, end) || second.liesBetween(start, end) ||
        start.liesBetween(first, second) || end.liesBetween(first, second)

/**
 * In euclidean geometry, the intersection of two segments can be
 * an empty set, a point, or segment (in other words, infinite points).
 */
enum class SegmentsIntersectionTypes {
    /**
     * Segments have a single point of intersection.
     */
    POINT,
    /**
     * Segments have infinite points of intersection, lying on a segment.
     * In other words, they are collinear and overlapping.
     * Note that two segments may be collinear, overlapping and share a
     * single point (e.g. an endpoint). In this case the intersection
     * type is [POINT].
     */
    SEGMENT,
    /**
     * Segments do not intersect.
     */
    EMPTY;
}

/**
 */
data class SegmentsIntersectionResult(
    /**
     */
    val type: SegmentsIntersectionTypes,
    /**
     */
    val intersection: Optional<Euclidean2DPosition> = Optional.empty()
)

/**
 * Finds the magnitude of a vector.
 */
fun <V : Vector<V>> Vector<V>.magnitude(): Double {
    var sum = 0.0
    for (d in 0 until dimensions) {
        sum += getCoordinate(d).pow(2)
    }
    return sqrt(sum)
}

/**
 * Computes the dot product between two vectors.
 */
fun <V : Vector<V>> Vector<V>.dot(other: V): Double {
    var dot = 0.0
    for (d in 0 until dimensions) {
        dot += getCoordinate(d) * other.getCoordinate(d)
    }
    return dot
}

/**
 * Computes the angle in radians between two vectors.
 */
fun <V : Vector<V>> Vector<V>.angleBetween(other: V): Double =
    acos(dot(other) / (magnitude() * other.magnitude()))

/**
 * Checks whether the segment is aligned to the x axis.
 */
fun Euclidean2DSegment.isXAxisAligned(): Boolean = fuzzyEquals(first.y, second.y)

/**
 * Checks whether the segment is aligned to the y axis.
 */
fun Euclidean2DSegment.isYAxisAligned(): Boolean = fuzzyEquals(first.x, second.x)

/**
 * Checks whether the segment is axis-aligned.
 */
fun Euclidean2DSegment.isAxisAligned(): Boolean = isXAxisAligned() || isYAxisAligned()

/**
 * Finds the point of the segment which is closest to the provided position.
 */
fun Euclidean2DSegment.closestPointTo(p: Euclidean2DPosition): Euclidean2DPosition {
    if (isDegenerate()) {
        return first
    }
    if (contains(p)) {
        return p
    }
    val m1 = slope()
    val intersection = when {
        m1.isInfinite() -> Euclidean2DPosition(first.x, p.y)
        fuzzyEquals(m1, 0.0) -> Euclidean2DPosition(p.x, first.y)
        else -> {
            val q1 = first.y - m1 * first.x
            val m2 = -1 / m1
            val q2 = p.y - m2 * p.x
            val x = (q2 - q1) / (m1 - m2)
            val y = m1 * x + q1
            Euclidean2DPosition(x, y)
        }
    }
    return if (contains(intersection)) {
        intersection
    } else {
        if ((first - p).magnitude() < (second - p).magnitude()) {
            first
        } else {
            second
        }
    }
}

/**
 * Finds the intersection between a circle and a segment.
 */
fun intersection(s: Euclidean2DSegment, center: Euclidean2DPosition, r: Double): CircleSegmentIntersectionResult {
    val v = s.toVector()
    val a = v.x.pow(2) + v.y.pow(2)
    val b = 2 * (v.x * (s.first.x - center.x) + v.y * (s.first.y - center.y))
    val c = (s.first.x - center.x).pow(2) + (s.first.y - center.y).pow(2) - r.pow(2)
    val det = b.pow(2) - 4 * a * c
    if (fuzzyEquals(a, 0.0) || a < 0.0 || det < 0.0) {
        return CircleSegmentIntersectionResult(CircleSegmentIntersectionType.EMPTY)
    } else if (fuzzyEquals(det, 0.0)) {
        val t = -b / (2 * a)
        return if (t.liesBetween(0.0, 1.0)) {
            CircleSegmentIntersectionResult(CircleSegmentIntersectionType.POINT,
                Optional.of(Euclidean2DPosition(s.first.x + t * v.x, s.first.y + t * v.y)))
        } else {
            CircleSegmentIntersectionResult(CircleSegmentIntersectionType.EMPTY)
        }
    } else {
        val t1 = (-b + sqrt(det)) / (2 * a)
        val t2 = (-b - sqrt(det)) / (2 * a)
        val p1 = if (t1.liesBetween(0.0, 1.0)) {
            Optional.of(Euclidean2DPosition(s.first.x + t1 * v.x, s.first.y + t1 * v.y))
        } else {
            Optional.empty()
        }
        val p2 = if (t2.liesBetween(0.0, 1.0)) {
            Optional.of(Euclidean2DPosition(s.first.x + t2 * v.x, s.first.y + t2 * v.y))
        } else {
            Optional.empty()
        }
        return when (mutableListOf(t1, t2).filter { it.liesBetween(0.0, 1.0) }.count()) {
            0 -> CircleSegmentIntersectionResult(CircleSegmentIntersectionType.EMPTY)
            1 -> {
                if (p2.isEmpty) {
                    CircleSegmentIntersectionResult(CircleSegmentIntersectionType.POINT, p1, p2)
                } else {
                    CircleSegmentIntersectionResult(CircleSegmentIntersectionType.POINT, p2, p1)
                }
            }
            else -> CircleSegmentIntersectionResult(CircleSegmentIntersectionType.PAIR, p1, p2)
        }
    }
}

/**
 */
enum class CircleSegmentIntersectionType {
    /**
     */
    POINT,
    /**
     */
    PAIR,
    /**
     */
    EMPTY;
}

/**
 */
data class CircleSegmentIntersectionResult(
    /**
     */
    val type: CircleSegmentIntersectionType,
    /**
     */
    val p1: Optional<Euclidean2DPosition> = Optional.empty(),
    /**
     */
    val p2: Optional<Euclidean2DPosition> = Optional.empty()
)
