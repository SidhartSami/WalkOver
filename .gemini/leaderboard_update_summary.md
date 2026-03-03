# Leaderboard Filtering Update

## Problem
The leaderboard was showing "dead users" - users with 0 distance or very little activity, cluttering the ranking.

## Solution Implemented

### 1. Repository Logic Update
Modified `LeaderboardRepository.kt` to apply minimum distance filters.

**Global Leaderboard (`getDistanceLeaderboard`):**
```kotlin
// In filters
.filter { it.score >= 1.0 } // Filter out users with < 1km
```

**Friends Leaderboard (`getFriendsDistanceLeaderboard`):**
```kotlin
// In filters
.filter { it.second >= 1000.0 } // Filter out users with < 1000m (1km)
```

### 2. UI Updates
Updated `LeaderboardScreen.kt` empty state message to be clear about the requirement:
- Old: "Complete some walks to appear on the leaderboard"
- New: "Walk at least 1 km to appear on the leaderboard"

### 3. Impact
- Users with 0km or <1km will not appear in the "Distance" leaderboard.
- Cleaner views focusing on active users.
- Clearer instructions for new users on how to join the ranking.

## Previous Changes (Recap)
- **Share Walk Dialog**: Unified colors (White background + Neon Green icons) for both Camera and Gallery options.
- **Story Time Font**: Changed to Monospace font for better alignment of time digits.
- **Gallery Picker**: Added ability to pick photos from gallery for walk stories.
