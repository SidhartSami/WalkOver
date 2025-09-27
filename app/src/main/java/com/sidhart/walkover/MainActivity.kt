package com.sidhart.walkover

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sidhart.walkover.data.LocationPoint
import com.sidhart.walkover.data.Walk
import com.sidhart.walkover.service.FirebaseService
import com.sidhart.walkover.service.LocationService
import com.sidhart.walkover.ui.*
import com.sidhart.walkover.ui.theme.WalkOverTheme
import com.sidhart.walkover.utils.MapUtils
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class MainActivity : ComponentActivity() {
    
    private lateinit var locationService: LocationService
    private lateinit var firebaseService: FirebaseService
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize osmdroid configuration for maps
        initializeOsmdroidConfiguration()
        
        locationService = LocationService(this)
        firebaseService = FirebaseService()
        
        // Check permissions
        if (!hasLocationPermission()) {
            requestLocationPermission()
        }
        
        // Initialize Firebase authentication
        lifecycleScope.launch {
            try {
                // Check internet connectivity
                val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val network = connectivityManager.activeNetwork
                if (network == null) {
                    Toast.makeText(this@MainActivity, "No internet connection", Toast.LENGTH_LONG).show()
                    return@launch
                }
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
                if (networkCapabilities == null || !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    Toast.makeText(this@MainActivity, "No internet connection", Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                // Check if Firebase is properly initialized
                if (!firebaseService.isUserAuthenticated()) {
                    Toast.makeText(this@MainActivity, "Signing in to Firebase...", Toast.LENGTH_SHORT).show()
                    firebaseService.signInAnonymously().fold(
                        onSuccess = { 
                            Toast.makeText(this@MainActivity, "Signed in anonymously", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { error ->
                            Toast.makeText(this@MainActivity, "Firebase sign-in failed: ${error.message}", Toast.LENGTH_LONG).show()
                            // Log the full error for debugging
                            android.util.Log.e("FirebaseError", "Sign-in failed", error)
                        }
                    )
                } else {
                    Toast.makeText(this@MainActivity, "Already signed in", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "App initialization error: ${e.message}", Toast.LENGTH_LONG).show()
                android.util.Log.e("AppError", "Initialization failed", e)
            }
        }
        
        setContent {
            WalkOverTheme {
                MainNavigationScreen(
                    locationService = locationService,
                    firebaseService = firebaseService,
                    context = this@MainActivity
                )
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    
    private fun initializeOsmdroidConfiguration() {
        // Initialize osmdroid configuration
        org.osmdroid.config.Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        
        // Set user agent for tile requests (required for OpenStreetMap)
        org.osmdroid.config.Configuration.getInstance().userAgentValue = "WalkOver/1.0"
        
        // Enable tile caching with more aggressive settings
        org.osmdroid.config.Configuration.getInstance().cacheMapTileCount = 2000
        org.osmdroid.config.Configuration.getInstance().cacheMapTileOvershoot = 200
        
        // Set cache directory
        org.osmdroid.config.Configuration.getInstance().osmdroidBasePath = getExternalFilesDir(null)
        org.osmdroid.config.Configuration.getInstance().osmdroidTileCache = getExternalFilesDir(null)
        
        // Set tile download thread count
        org.osmdroid.config.Configuration.getInstance().tileDownloadThreads = 8
        org.osmdroid.config.Configuration.getInstance().tileFileSystemThreads = 4
    }
}

// Simplified map style selection - only Positron and Voyager
fun getDefaultMapStyle(): MapStyle {
    return MapStyle.CARTODB_POSITRON
}

// Navigation items
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Leaderboard : Screen("leaderboard", "Leaderboard", Icons.Default.Star)
    object Map : Screen("map", "Map", Icons.Default.LocationOn)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationScreen(
    locationService: LocationService,
    firebaseService: FirebaseService,
    context: Context
) {
    val navController = rememberNavController()
    var currentMapStyle by remember { mutableStateOf(getDefaultMapStyle()) }
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                listOf(Screen.Leaderboard, Screen.Map, Screen.Settings).forEach { screen ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    
                    NavigationBarItem(
                        icon = {
                            if (screen == Screen.Map) {
                                // Special circular button for Map
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.secondary
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title,
                                        tint = if (isSelected) 
                                            MaterialTheme.colorScheme.onPrimary 
                                        else 
                                            MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    tint = if (isSelected) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        label = {
                            Text(
                                text = screen.title,
                                color = if (isSelected) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        selected = isSelected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Map.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Leaderboard.route) {
                LeaderboardScreenContent(
                    firebaseService = firebaseService,
                    context = context
                )
            }
            
            composable(Screen.Map.route) {
                MapScreenContent(
                    currentMapStyle = currentMapStyle,
                    onMapStyleChange = { newStyle -> currentMapStyle = newStyle },
                    locationService = locationService,
                    firebaseService = firebaseService,
                    context = context
                )
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(
                    currentMapStyle = currentMapStyle,
                    onMapStyleChange = { newStyle -> 
                        currentMapStyle = newStyle
                        android.util.Log.d("MainActivity", "Map style changed to: ${newStyle.displayName}")
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreenContent(
    currentMapStyle: MapStyle,
    onMapStyleChange: (MapStyle) -> Unit,
    locationService: LocationService,
    firebaseService: FirebaseService,
    context: Context
) {
    var isTracking by remember { mutableStateOf(false) }
    var locationPoints by remember { mutableStateOf<List<LocationPoint>>(emptyList()) }
    var distance by remember { mutableStateOf(0.0) }
    var area by remember { mutableStateOf(0.0) }
    var mapView by remember { mutableStateOf<org.osmdroid.views.MapView?>(null) }
    
    // Check location permissions
    val hasLocationPermission = ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

    // Location tracking effect
    LaunchedEffect(isTracking) {
        if (isTracking && hasLocationPermission) {
            try {
                locationService.getLocationUpdates()
                    .onEach { location ->
                        locationPoints = locationPoints + location
                        distance = calculateTotalDistance(locationPoints)
                        area = LocationService.calculateArea(locationPoints)
                    }
                    .catch { error ->
                        Toast.makeText(context, "Location tracking error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                    .launchIn(this)
            } catch (e: Exception) {
                Toast.makeText(context, "Location service error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Map View
        AndroidView(
            factory = { ctx ->
                org.osmdroid.views.MapView(ctx).apply {
                    try {
                        // Set tile source immediately based on current style
                        setTileSource(currentMapStyle.tileSource)

                        // Enable multi-touch gestures
                        setMultiTouchControls(true)
                        setClickable(true)

                        // Set initial zoom and location
                        controller.setZoom(15.0)
                        
                        // Try to get current location first, fallback to default
                        if (hasLocationPermission) {
                            MapUtils.getCurrentLocation(
                                context = ctx,
                                onSuccess = { location ->
                                    val currentLocation = org.osmdroid.util.GeoPoint(location.latitude, location.longitude)
                                    controller.setCenter(currentLocation)
                                    
                                    // Add location pin
                                    MapUtils.addMarker(
                                        mapView = this,
                                        geoPoint = currentLocation,
                                        title = "Current Location",
                                        snippet = "Lat: ${String.format("%.6f", location.latitude)}, Lng: ${String.format("%.6f", location.longitude)}",
                                        isLocationMarker = true
                                    )
                                    
                                    Toast.makeText(ctx, "Location found: ${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { error ->
                                    // Fallback to default location (Karachi)
                                    val defaultCenter = org.osmdroid.util.GeoPoint(24.8607, 67.0011)
                                    controller.setCenter(defaultCenter)
                                    Toast.makeText(ctx, "Using default location: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            // No permission, use default location (Karachi)
                            val defaultCenter = org.osmdroid.util.GeoPoint(24.8607, 67.0011)
                            controller.setCenter(defaultCenter)
                        }
                        
                        mapView = this
                    } catch (e: Exception) {
                        android.util.Log.e("MapView", "Error initializing map", e)
                        Toast.makeText(ctx, "Error initializing map: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier.weight(1f),
            update = { mv ->
                // Update map style if changed
                try {
                    val currentTileSource = mv.tileProvider.tileSource
                    val newTileSource = currentMapStyle.tileSource
                    
                    android.util.Log.d("MapView", "Checking tile source: current=${currentTileSource.name()}, new=${newTileSource.name()}")
                    
                    if (currentTileSource.name() != newTileSource.name()) {
                        android.util.Log.d("MapView", "Updating map tile source from ${currentTileSource.name()} to ${newTileSource.name()}")
                        
                        // Store current map state
                        val currentCenter = mv.mapCenter
                        val currentZoom = mv.zoomLevelDouble
                        
                        // Clear tile cache for the new style to ensure fresh tiles
                        mv.tileProvider.clearTileCache()
                        
                        // Set new tile source
                        mv.setTileSource(newTileSource)
                        
                        // Restore map state and force refresh
                        mv.controller.setCenter(currentCenter)
                        mv.controller.setZoom(currentZoom)
                        mv.invalidate()
                        
                        // Force tile refresh by triggering a zoom change and back
                        mv.controller.zoomIn()
                        mv.controller.zoomOut()
                        
                        android.util.Log.d("MapView", "Successfully updated map style to ${currentMapStyle.displayName}")
                    } else {
                        android.util.Log.d("MapView", "Tile source already matches, no update needed")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MapView", "Error updating map style", e)
                }
            }
        )

        // Stats and Controls Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Walk Statistics",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text("Distance: ${String.format("%.2f", distance)} m")
                Text("Area: ${String.format("%.2f", area)} m²")
                Text("Points: ${locationPoints.size}")

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            isTracking = true
                            locationPoints = emptyList()
                        },
                        enabled = !isTracking && hasLocationPermission
                    ) {
                        Text("Start Walk")
                    }

                    Button(
                        onClick = {
                            isTracking = false
                            if (locationPoints.size >= 2) {
                                saveWalk(locationPoints, distance, area, firebaseService, context)
                            } else {
                                Toast.makeText(context, "Walk too short to save", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = isTracking
                    ) {
                        Text("Stop Walk")
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Center on Location Button
                Button(
                    onClick = {
                        mapView?.let { map ->
                            if (hasLocationPermission) {
                                MapUtils.getCurrentLocation(
                                    context = context,
                                    onSuccess = { location ->
                                        val geoPoint = MapUtils.convertLocationToGeoPoint(location)
                                        MapUtils.centerMapOnLocation(map, geoPoint)
                                        
                                        // Clear existing location markers first
                                        map.overlays.removeAll { overlay ->
                                            overlay is org.osmdroid.views.overlay.Marker && overlay.title == "Current Location"
                                        }
                                        
                                        // Add location pin
                                        MapUtils.addMarker(
                                            mapView = map,
                                            geoPoint = geoPoint,
                                            title = "Current Location",
                                            snippet = "Lat: ${String.format("%.6f", location.latitude)}, Lng: ${String.format("%.6f", location.longitude)}",
                                            isLocationMarker = true
                                        )
                                        
                                        Toast.makeText(context, "Centered on your location", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(context, "Failed to get location: ${error.message}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else {
                                Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = hasLocationPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📍 Center on My Location")
                }
                
                if (!hasLocationPermission) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Location permission required for tracking",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun calculateTotalDistance(points: List<LocationPoint>): Double {
    if (points.size < 2) return 0.0
    
    var totalDistance = 0.0
    for (i in 1 until points.size) {
        totalDistance += LocationService.calculateDistance(points[i - 1], points[i])
    }
    return totalDistance
}

private fun saveWalk(
    points: List<LocationPoint>,
    distance: Double,
    area: Double,
    firebaseService: FirebaseService,
    context: Context
) {
    val walk = Walk(
        polylineCoordinates = points.map { 
            com.google.firebase.firestore.GeoPoint(it.latitude, it.longitude) 
        },
        distanceCovered = distance,
        areaCaptured = area
    )
    
    // Use coroutine scope from context
    (context as ComponentActivity).lifecycleScope.launch {
        firebaseService.saveWalk(walk).fold(
            onSuccess = { 
                Toast.makeText(context, "Walk saved successfully!", Toast.LENGTH_SHORT).show()
            },
            onFailure = { error ->
                Toast.makeText(context, "Failed to save walk: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreenContent(
    modifier: Modifier = Modifier,
    firebaseService: FirebaseService,
    context: Context
) {
    var users by remember { mutableStateOf<List<com.sidhart.walkover.data.User>>(emptyList()) }
    var currentScoreType by remember { mutableStateOf(ScoreType.AREA) }
    var isLoading by remember { mutableStateOf(false) }
    
    val decimalFormat = remember { DecimalFormat("#.##") }
    
    // Load leaderboard when score type changes
    LaunchedEffect(currentScoreType) {
        isLoading = true
        val result = when (currentScoreType) {
            ScoreType.AREA -> firebaseService.getLeaderboard()
            ScoreType.DISTANCE -> firebaseService.getDistanceLeaderboard()
        }
        
        result.fold(
            onSuccess = { userList ->
                users = userList
                isLoading = false
                if (userList.isEmpty()) {
                    Toast.makeText(context, "No data available", Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = { error ->
                isLoading = false
                Toast.makeText(context, "Failed to load leaderboard: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Leaderboard",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Toggle buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { currentScoreType = ScoreType.AREA },
                modifier = Modifier.weight(1f),
                enabled = currentScoreType != ScoreType.AREA
            ) {
                Text("Area")
            }
            
            Button(
                onClick = { currentScoreType = ScoreType.DISTANCE },
                modifier = Modifier.weight(1f),
                enabled = currentScoreType != ScoreType.DISTANCE
            ) {
                Text("Distance")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Leaderboard list
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(users) { index, user ->
                    LeaderboardItem(
                        rank = index + 1,
                        user = user,
                        scoreType = currentScoreType,
                        decimalFormat = decimalFormat
                    )
                }
            }
        }
    }
}

@Composable
fun LeaderboardItem(
    rank: Int,
    user: com.sidhart.walkover.data.User,
    scoreType: ScoreType,
    decimalFormat: DecimalFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rank.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.username.ifEmpty { "Anonymous" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                val score = when (scoreType) {
                    ScoreType.AREA -> user.totalAreaCaptured
                    ScoreType.DISTANCE -> user.totalDistanceWalked
                }
                
                val unit = when (scoreType) {
                    ScoreType.AREA -> "m²"
                    ScoreType.DISTANCE -> "m"
                }
                
                Text(
                    text = "${decimalFormat.format(score)} $unit",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

enum class ScoreType {
    AREA, DISTANCE
}