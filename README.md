# WalkOver - Fitness Gamified App

A Kotlin Android app that gamifies fitness by tracking walking paths on a map and creating a leaderboard system.

## Features

- **Map Integration**: Interactive map functionality (to be added later)
- **GPS Tracking**: Real-time location tracking with high accuracy
- **Path Tracking**: GPS-based route tracking
- **Territory Calculation**: Area calculation for covered regions
- **Firebase Integration**: Cloud storage for walks and user data
- **Leaderboard**: Top 10 users by area captured or distance walked
- **Anonymous Authentication**: No registration required

## Project Structure

```
app/src/main/java/com/sidhart/walkover/
├── data/
│   ├── LocationPoint.kt      # GPS coordinate data class
│   ├── User.kt              # User statistics model
│   └── Walk.kt              # Walk session model
├── service/
│   ├── FirebaseService.kt   # Firebase operations
│   └── LocationService.kt   # GPS tracking service
├── ui/
│   └── LeaderboardActivity.kt
└── MainActivity.kt          # Main map screen (Compose-based)
```

## Setup Instructions

### 1. Firebase Configuration

✅ **Already configured!** Your `google-services.json` is already in place with:
- Project ID: `walkover-4707b`
- Package: `com.sidhart.walkover`
- API Key: Configured

**Next steps:**
1. Go to [Firebase Console](https://console.firebase.google.com/project/walkover-4707b)
2. Enable **Authentication** → **Sign-in method** → **Anonymous**
3. Enable **Firestore Database** → Create database in test mode

### 2. Map Integration

⚠️ **Map functionality temporarily removed** - Will be re-added later with your preferred mapping solution.

### 3. Build and Run

1. Open the project in Android Studio
2. Sync Gradle files
3. Build and run on a **physical device** (GPS required)
4. Grant location permissions when prompted

## Key Components

### LocationService
- Uses FusedLocationProviderClient for high-accuracy GPS
- Calculates distance and area using spherical geometry
- Provides reactive location updates via Kotlin Flow

### MainActivity (Compose)
- Jetpack Compose-based UI with Material 3 design
- Real-time state management with Compose
- Reactive location tracking and stats display
- Map integration to be added later

### FirebaseService
- Saves walk data to Firestore
- Maintains user statistics
- Provides leaderboard queries

## Permissions Required

- `ACCESS_FINE_LOCATION`: High-accuracy GPS tracking
- `ACCESS_COARSE_LOCATION`: Fallback location access
- `INTERNET`: Firebase and map data

## Performance Features

- Location updates limited to 1-second intervals
- Battery-optimized location requests
- Efficient polyline rendering
- Minimal Firebase writes
- Proper lifecycle management

## Usage

1. **Start Walk**: Tap "Start Walk" to begin tracking
2. **Real-time Tracking**: Watch your path draw on the map
3. **Stats Display**: See distance and area covered
4. **Stop Walk**: Tap "Stop Walk" to save your walk
5. **Leaderboard**: View top users by area or distance

## Dependencies

- **Jetpack Compose**: Latest BOM with Material 3
- **Firebase**: 32.7.0 (BOM)
- **Google Play Services Location**: 21.0.1
- **Kotlin Coroutines**: 1.7.3
- **AndroidX**: Latest stable versions

## Future Enhancements

- Map integration and visualization
- Social features and friend challenges
- Achievement badges and rewards
- Route sharing and discovery
- Offline map caching
- Advanced analytics and insights

## Troubleshooting

**Map not loading?**
- Map functionality has been temporarily removed
- Will be re-added in a future update

**Location not working?**
- Ensure location permissions are granted
- Test on physical device (not emulator)
- Check GPS is enabled

**Firebase errors?**
- Verify Firebase project is properly configured
- Check Authentication and Firestore are enabled
- Ensure `google-services.json` is in `app/` directory

## License

This project is for educational and development purposes.

