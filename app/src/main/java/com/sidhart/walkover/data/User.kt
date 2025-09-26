package com.sidhart.walkover.data

data class User(
    val id: String = "",
    val username: String = "",
    val totalDistanceWalked: Double = 0.0, // in meters
    val totalAreaCaptured: Double = 0.0, // in square meters
    val totalWalks: Int = 0,
    val lastWalkDate: Long = 0L // timestamp
) {
    constructor() : this("", "", 0.0, 0.0, 0, 0L)
}

