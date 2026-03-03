package com.sidhart.walkover.utils

import org.osmdroid.util.GeoPoint
import kotlin.math.*

/**
 * Convex Hull computation using the Graham Scan algorithm.
 *
 * All points are first projected to a local East-North metric plane centred
 * at the mean of the input set, so the algorithm operates in plain 2-D
 * Cartesian space.  This avoids the numerical issues that arise when
 * longitude values straddle the ±180° meridian and keeps the code simple.
 */
object ConvexHull {

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Compute the convex hull of [points] and return the hull vertices in
     * counter-clockwise order.  Returns an empty list if fewer than 3 distinct
     * points are supplied.
     */
    fun compute(points: List<GeoPoint>): List<GeoPoint> {
        if (points.size < 3) return emptyList()

        val projected = projectToPlane(points)
        val hullIndices = grahamScan(projected)

        return hullIndices.map { points[it] }
    }

    /**
     * Merge two convex hulls into one by taking the convex hull of all
     * vertices from both polygons.
     */
    fun merge(hull1: List<GeoPoint>, hull2: List<GeoPoint>): List<GeoPoint> {
        val combined = hull1 + hull2
        return if (combined.size < 3) combined else compute(combined)
    }

    // ── Internal helpers ────────────────────────────────────────────────────

    /** Flat (x, y) projection in metres, centred on the mean of all points. */
    private data class Pt(val x: Double, val y: Double, val idx: Int)

    private fun projectToPlane(points: List<GeoPoint>): List<Pt> {
        val meanLat = points.map { it.latitude  }.average()
        val meanLon = points.map { it.longitude }.average()
        val cosLat  = cos(Math.toRadians(meanLat))
        val R       = 6_378_137.0 // WGS-84 equatorial radius in metres

        return points.mapIndexed { i, p ->
            val x = R * Math.toRadians(p.longitude - meanLon) * cosLat
            val y = R * Math.toRadians(p.latitude  - meanLat)
            Pt(x, y, i)
        }
    }

    /** Cross product of vectors (O→A) and (O→B). */
    private fun cross(O: Pt, A: Pt, B: Pt): Double =
        (A.x - O.x) * (B.y - O.y) - (A.y - O.y) * (B.x - O.x)

    /**
     * Graham scan.  Returns indices (into [pts]) of the hull vertices in
     * counter-clockwise order.
     */
    private fun grahamScan(pts: List<Pt>): List<Int> {
        val n = pts.size
        if (n < 3) return pts.map { it.idx }

        // 1. Sort by (x, y)
        val sorted = pts.sortedWith(compareBy({ it.x }, { it.y }))

        // 2. Build lower hull
        val lower = mutableListOf<Pt>()
        for (p in sorted) {
            while (lower.size >= 2 && cross(lower[lower.size - 2], lower[lower.size - 1], p) <= 0)
                lower.removeAt(lower.size - 1)
            lower.add(p)
        }

        // 3. Build upper hull
        val upper = mutableListOf<Pt>()
        for (p in sorted.reversed()) {
            while (upper.size >= 2 && cross(upper[upper.size - 2], upper[upper.size - 1], p) <= 0)
                upper.removeAt(upper.size - 1)
            upper.add(p)
        }

        // Remove last of each because it's the first of the other
        lower.removeAt(lower.size - 1)
        upper.removeAt(upper.size - 1)

        return (lower + upper).map { it.idx }
    }
}
