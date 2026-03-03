package com.sidhart.walkover.utils

import org.osmdroid.util.GeoPoint
import kotlin.math.*

/**
 * Territory conflict / overlap detection.
 *
 * Uses the Separating Axis Theorem (SAT) on the 2-D projected polygons,
 * which is fast and reliable for convex hulls.  The same local flat-plane
 * projection used in [ConvexHull] is applied here.
 */
object TerritoryConflict {

    /**
     * Returns true if [polyA] and [polyB] overlap (including the case where
     * one contains the other or they share only an edge/vertex).
     *
     * Both polygons are assumed to be convex and non-degenerate (≥ 3 points).
     */
    fun polygonsOverlap(polyA: List<GeoPoint>, polyB: List<GeoPoint>): Boolean {
        if (polyA.size < 3 || polyB.size < 3) return false

        val all = polyA + polyB
        val meanLat = all.map { it.latitude  }.average()
        val meanLon = all.map { it.longitude }.average()
        val cosLat  = cos(Math.toRadians(meanLat))
        val R       = 6_378_137.0

        fun project(p: GeoPoint): Pair<Double, Double> {
            val x = R * Math.toRadians(p.longitude - meanLon) * cosLat
            val y = R * Math.toRadians(p.latitude  - meanLat)
            return Pair(x, y)
        }

        val ptsA = polyA.map { project(it) }
        val ptsB = polyB.map { project(it) }

        // Test all edge normals of both polygons as separating axes
        fun axes(pts: List<Pair<Double, Double>>): List<Pair<Double, Double>> =
            pts.indices.map { i ->
                val j  = (i + 1) % pts.size
                val dx = pts[j].first  - pts[i].first
                val dy = pts[j].second - pts[i].second
                Pair(-dy, dx) // perpendicular (normal)
            }

        fun projectOntoAxis(pts: List<Pair<Double, Double>>, ax: Pair<Double, Double>): Pair<Double, Double> {
            var min = Double.MAX_VALUE
            var max = Double.MIN_VALUE
            for ((x, y) in pts) {
                val p = x * ax.first + y * ax.second
                if (p < min) min = p
                if (p > max) max = p
            }
            return Pair(min, max)
        }

        fun separated(ax: Pair<Double, Double>): Boolean {
            val (minA, maxA) = projectOntoAxis(ptsA, ax)
            val (minB, maxB) = projectOntoAxis(ptsB, ax)
            return maxA < minB || maxB < minA
        }

        for (ax in axes(ptsA) + axes(ptsB)) {
            if (separated(ax)) return false  // Found a gap → no overlap
        }
        return true  // No separating axis found → polygons overlap
    }

    /**
     * Returns true if [polyA] and [polyB] overlap OR are within [bufferMeters] of each other.
     * Used for same-user territory merging — adjacent territories should be unified into
     * one contiguous empire block even if they don't perfectly touch.
     *
     * @param bufferMeters how close (in metres) two polygons need to be to trigger a merge
     */
    fun polygonsOverlapWithBuffer(
        polyA: List<GeoPoint>,
        polyB: List<GeoPoint>,
        bufferMeters: Double = 50.0
    ): Boolean {
        if (polyA.size < 3 || polyB.size < 3) return false

        // Fast path: exact overlap
        if (polygonsOverlap(polyA, polyB)) return true

        // Proximity check: find minimum distance between any two vertices across the polygons.
        val all = polyA + polyB
        val meanLat = all.map { it.latitude  }.average()
        val meanLon = all.map { it.longitude }.average()
        val cosLat  = cos(Math.toRadians(meanLat))
        val R       = 6_378_137.0

        fun project(p: GeoPoint): Pair<Double, Double> {
            val x = R * Math.toRadians(p.longitude - meanLon) * cosLat
            val y = R * Math.toRadians(p.latitude  - meanLat)
            return Pair(x, y)
        }

        val ptsA = polyA.map { project(it) }
        val ptsB = polyB.map { project(it) }

        // Check if any vertex of A is within bufferMeters of any vertex of B
        for (a in ptsA) {
            for (b in ptsB) {
                val dx = a.first - b.first
                val dy = a.second - b.second
                val dist = sqrt(dx * dx + dy * dy)
                if (dist <= bufferMeters) return true
            }
        }

        // Also check centroid-to-centroid distance against combined extents + buffer
        val centAx = ptsA.map { it.first  }.average()
        val centAy = ptsA.map { it.second }.average()
        val centBx = ptsB.map { it.first  }.average()
        val centBy = ptsB.map { it.second }.average()

        val radA = ptsA.maxOf { pt -> sqrt((pt.first - centAx).pow(2) + (pt.second - centAy).pow(2)) }
        val radB = ptsB.maxOf { pt -> sqrt((pt.first - centBx).pow(2) + (pt.second - centBy).pow(2)) }
        val centDist = sqrt((centAx - centBx).pow(2) + (centAy - centBy).pow(2))

        return centDist <= (radA + radB + bufferMeters)
    }

    /**
     * Compute the fraction of [subject] polygon's area that falls inside [clipping] polygon.
     * Uses a Monte Carlo grid sampling approach (fast approximation).
     * Returns a value in [0.0, 1.0].
     */
    fun overlapFraction(subject: List<GeoPoint>, clipping: List<GeoPoint>, samples: Int = 50): Double {
        if (subject.size < 3 || clipping.size < 3) return 0.0
        if (!polygonsOverlap(subject, clipping)) return 0.0

        val minLat = subject.minOf { it.latitude }
        val maxLat = subject.maxOf { it.latitude }
        val minLng = subject.minOf { it.longitude }
        val maxLng = subject.maxOf { it.longitude }

        val step = sqrt(samples.toDouble()).toInt().coerceAtLeast(5)
        var inside = 0
        var total = 0

        for (i in 0 until step) {
            for (j in 0 until step) {
                val lat = minLat + (maxLat - minLat) * i / step
                val lng = minLng + (maxLng - minLng) * j / step
                val pt = GeoPoint(lat, lng)
                if (isPointInPolygon(pt, subject)) {
                    total++
                    if (isPointInPolygon(pt, clipping)) inside++
                }
            }
        }

        return if (total == 0) 0.0 else inside.toDouble() / total
    }

    /** Ray-casting point-in-polygon test. */
    fun isPointInPolygon(p: GeoPoint, poly: List<GeoPoint>): Boolean {
        var inside = false
        var j = poly.size - 1
        for (i in poly.indices) {
            if (poly[i].longitude > p.longitude != poly[j].longitude > p.longitude &&
                p.latitude < (poly[j].latitude - poly[i].latitude) * (p.longitude - poly[i].longitude) /
                (poly[j].longitude - poly[i].longitude) + poly[i].latitude) {
                inside = !inside
            }
            j = i
        }
        return inside
    }
}
