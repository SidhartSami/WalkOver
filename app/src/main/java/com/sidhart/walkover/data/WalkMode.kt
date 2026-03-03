package com.sidhart.walkover.data

/**
 * WalkMode defines the two walking sessions available:
 * - ROAM: pure exploration, no territory capture
 * - COMPETE: walk to capture territory, visible to all users on the map
 */
enum class WalkMode {
    ROAM,
    GHOST,
    COMPETE,
    DUEL
}
