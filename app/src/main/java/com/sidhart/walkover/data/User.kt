package com.sidhart.walkover.data

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val totalDistanceWalked: Double = 0.0, // in meters
    val totalWalks: Int = 0,
    val lastWalkDate: Long = 0L, // timestamp
    val isAnonymous: Boolean = false
) {
    constructor() : this("", "", "", 0.0, 0, 0L, false)
}