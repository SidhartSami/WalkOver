package com.sidhart.walkover

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
import com.sidhart.walkover.data.*
import com.sidhart.walkover.service.FirebaseService
import com.sidhart.walkover.service.LocationService
import com.sidhart.walkover.service.WalkTrackingService
import com.sidhart.walkover.ui.*
import com.sidhart.walkover.ui.theme.WalkOverTheme
import com.sidhart.walkover.utils.MapUtils
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import com.sidhart.walkover.utils.AppPreferencesManager

// Top-level helper functions
@Composable
fun rememberLocationState(context: Context): State<Boolean> {
    return produceState(initialValue = isLocationEnabled(context)) {
        while (true) {
            kotlinx.coroutines.delay(2000)
            value = isLocationEnabled(context)
        }
    }
}

private fun isLocationEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

class MainActivity : ComponentActivity() {

    private lateinit var locationService: LocationService
    private lateinit var firebaseService: FirebaseService
    var walkTrackingService: WalkTrackingService? = null
        private set
    var isServiceBound = false
        private set
    private var locationStateCallback: ((Boolean) -> Unit)? = null
    private var permissionCallback: ((Boolean) -> Unit)? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as WalkTrackingService.LocalBinder
            walkTrackingService = binder.getService()
            isServiceBound = true
            android.util.Log.d("MainActivity", "WalkTrackingService connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            walkTrackingService = null
            isServiceBound = false
            android.util.Log.d("MainActivity", "WalkTrackingService disconnected")
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        permissionCallback?.invoke(allGranted)

        if (!allGranted) {
            Toast.makeText(this, "Location permission is required for tracking", Toast.LENGTH_LONG).show()
        }
    }

    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        locationStateCallback?.invoke(isLocationEnabled(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_WalkOver)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        initializeOsmdroidConfiguration()

        locationService = LocationService(this)
        firebaseService = FirebaseService()

        // Bind to WalkTrackingService
        Intent(this, WalkTrackingService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        setContent {
            WalkOverTheme {
                val appPreferencesManager = remember {
                    AppPreferencesManager(this@MainActivity)
                }

                var hasAcceptedPrivacyPolicy by remember {
                    mutableStateOf(appPreferencesManager.hasAcceptedPrivacyPolicy())
                }

                var hasCompletedOnboarding by remember {
                    mutableStateOf(appPreferencesManager.hasCompletedOnboarding())
                }

                when {
                    // Show Privacy Policy first (only on first launch)
                    !hasAcceptedPrivacyPolicy -> {
                        PrivacyPolicyScreen(
                            onAcceptPrivacyPolicy = {
                                appPreferencesManager.setPrivacyPolicyAccepted(true)
                                hasAcceptedPrivacyPolicy = true
                            }
                        )
                    }
                    // Show Onboarding second (only after privacy policy, first time)
                    !hasCompletedOnboarding -> {
                        OnboardingScreen(
                            onOnboardingComplete = {
                                appPreferencesManager.setOnboardingCompleted(true)
                                hasCompletedOnboarding = true
                            }
                        )
                    }
                    // Show main auth flow (login/register)
                    else -> {
                        AuthNavigationWrapper(
                            locationService = locationService,
                            firebaseService = firebaseService,
                            context = this@MainActivity,
                            onRequestPermission = { callback ->
                                permissionCallback = callback
                                requestLocationPermission()
                            },
                            onRequestLocationSettings = {
                                promptEnableLocation()
                            }
                        )
                    }
                }
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

    private fun promptEnableLocation() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        locationSettingsLauncher.launch(intent)
    }

    private fun initializeOsmdroidConfiguration() {
        org.osmdroid.config.Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        org.osmdroid.config.Configuration.getInstance().userAgentValue = "WalkOver/1.0"
        org.osmdroid.config.Configuration.getInstance().cacheMapTileCount = 2000
        org.osmdroid.config.Configuration.getInstance().cacheMapTileOvershoot = 200
        org.osmdroid.config.Configuration.getInstance().osmdroidBasePath = getExternalFilesDir(null)
        org.osmdroid.config.Configuration.getInstance().osmdroidTileCache = getExternalFilesDir(null)
        org.osmdroid.config.Configuration.getInstance().tileDownloadThreads = 8
        org.osmdroid.config.Configuration.getInstance().tileFileSystemThreads = 4
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }
}

@Composable
fun AuthNavigationWrapper(
    locationService: LocationService,
    firebaseService: FirebaseService,
    context: Context,
    onRequestPermission: ((Boolean) -> Unit) -> Unit,
    onRequestLocationSettings: () -> Unit
) {
    val navController = rememberNavController()
    val isAuthenticated = remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        try {
            val authStatus = firebaseService.isUserAuthenticated()
            kotlinx.coroutines.delay(300)
            isAuthenticated.value = authStatus
        } catch (e: Exception) {
            android.util.Log.e("AuthNavWrapper", "Auth check failed", e)
            isAuthenticated.value = false
        }
    }

    if (isAuthenticated.value == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = com.sidhart.walkover.ui.theme.NeonGreen)
        }
        return
    }

    NavHost(
        navController = navController,
        startDestination = if (isAuthenticated.value == true) "main" else "login"
    ) {
        composable("login") {
            LoginScreen(
                firebaseService = firebaseService,
                onLoginSuccess = {
                    isAuthenticated.value = true
                    kotlinx.coroutines.MainScope().launch {
                        kotlinx.coroutines.delay(200)
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register") {
                        launchSingleTop = true
                    }
                },
                onContinueAsGuest = {
                    isAuthenticated.value = true
                    kotlinx.coroutines.MainScope().launch {
                        kotlinx.coroutines.delay(300)
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                onNavigateToVerification = { } // Not used anymore
            )
        }

        composable("register") {
            RegisterScreen(
                firebaseService = firebaseService,
                onRegisterSuccess = { userEmail ->
                    // Just navigate back to login after showing success dialog
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable("main") {
            MainNavigationScreen(
                locationService = locationService,
                firebaseService = firebaseService,
                context = context,
                onLogout = {
                    firebaseService.signOut()
                    isAuthenticated.value = false
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onRequestPermission = onRequestPermission,
                onRequestLocationSettings = onRequestLocationSettings
            )
        }
    }
}
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Map : Screen("map", "Map", Icons.Default.LocationOn)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationScreen(
    locationService: LocationService,
    firebaseService: FirebaseService,
    context: Context,
    onLogout: () -> Unit,
    onRequestPermission: ((Boolean) -> Unit) -> Unit,
    onRequestLocationSettings: () -> Unit
) {
    val navController = rememberNavController()
    val isDarkTheme = isSystemInDarkTheme()

    // Persistent walk state - service is source of truth
    val activity = context as? MainActivity
    var walkState by remember { mutableStateOf(LiveWalkState()) }

    // Always read from service as source of truth
    LaunchedEffect(activity?.isServiceBound, activity?.walkTrackingService) {
        val mainActivity = activity
        if (mainActivity?.isServiceBound == true && mainActivity.walkTrackingService != null) {
            val service = mainActivity.walkTrackingService!!
            // Restore state from service immediately
            walkState = service.walkState.value
            // Observe future updates from service
            service.walkState.collect { state ->
                walkState = state
            }
        }
    }

    // Poll service state periodically to catch updates
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500)
            val mainActivity = activity
            if (mainActivity?.isServiceBound == true && mainActivity.walkTrackingService != null) {
                val serviceState = mainActivity.walkTrackingService!!.walkState.value
                // Always sync with service state
                if (serviceState != walkState) {
                    walkState = serviceState
                }
            }
        }
    }

    var currentMapStyle by remember {
        mutableStateOf(
            if (isDarkTheme) MapStyle.CARTODB_DARK_MATTER
            else MapStyle.CARTODB_POSITRON
        )
    }

    LaunchedEffect(isDarkTheme) {
        currentMapStyle = if (isDarkTheme) {
            MapStyle.CARTODB_DARK_MATTER
        } else {
            MapStyle.CARTODB_POSITRON
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                listOf(Screen.Profile, Screen.Map, Screen.Settings).forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(if (screen == Screen.Map) 26.dp else 24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
            composable(Screen.Map.route) {
                EnhancedMapScreenContent(
                    currentMapStyle = currentMapStyle,
                    onMapStyleChange = { newStyle -> currentMapStyle = newStyle },
                    locationService = locationService,
                    firebaseService = firebaseService,
                    context = context,
                    onRequestPermission = onRequestPermission,
                    onEnableLocation = onRequestLocationSettings,
                    persistentWalkState = walkState,
                    onWalkStateChange = { walkState = it }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    firebaseService = firebaseService,
                    onNavigateToWalkHistory = {
                        navController.navigate("walk_history")
                    }
                )
            }

            composable("walk_history") {
                WalkHistoryScreen(
                    firebaseService = firebaseService,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onViewWalkOnMap = { walkId ->
                        navController.navigate("view_walk_map/$walkId")
                    }
                )
            }

            composable("view_walk_map/{walkId}") { backStackEntry ->
                val walkId = backStackEntry.arguments?.getString("walkId") ?: ""
                ViewWalkMapScreen(
                    walkId = walkId,
                    firebaseService = firebaseService,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    firebaseService = firebaseService,
                    onLogout = onLogout
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedMapScreenContent(
    currentMapStyle: MapStyle,
    onMapStyleChange: (MapStyle) -> Unit,
    locationService: LocationService,
    firebaseService: FirebaseService,
    context: Context,
    onRequestPermission: ((Boolean) -> Unit) -> Unit,
    onEnableLocation: () -> Unit,
    persistentWalkState: LiveWalkState,
    onWalkStateChange: (LiveWalkState) -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()

    // Permission and location states - checked on map screen only
    var hasLocationPermission by remember {
        mutableStateOf(
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val isLocationEnabled by rememberLocationState(context)

    // Use persistent walk state from parent - always sync
    var walkState by remember { mutableStateOf(persistentWalkState) }
    var showFullScreenStats by remember { mutableStateOf(false) }
    var currentMapStyleState by remember { mutableStateOf(currentMapStyle) }

    // Always sync with persistent state from parent
    LaunchedEffect(persistentWalkState) {
        if (persistentWalkState != walkState) {
            walkState = persistentWalkState
        }
    }

    // Update parent immediately when state changes (for persistence)
    LaunchedEffect(walkState.isTracking, walkState.points.size, walkState.stats.elapsedTimeMillis) {
        onWalkStateChange(walkState)
    }

    // Start notification service when tracking starts
    val activity = context as? MainActivity
    LaunchedEffect(walkState.isTracking) {
        if (walkState.isTracking) {
            // Start service for notifications
            val startIntent = Intent(context, WalkTrackingService::class.java).apply {
                action = WalkTrackingService.ACTION_START_TRACKING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent)
            } else {
                context.startService(startIntent)
            }
        }
    }

    // Sync local state to service for notifications
    LaunchedEffect(walkState.isTracking, walkState.stats.elapsedTimeMillis, walkState.points.size) {
        if (walkState.isTracking) {
            activity?.walkTrackingService?.syncState(walkState)
        }
    }

    // Map overlays
    var mapView by remember { mutableStateOf<org.osmdroid.views.MapView?>(null) }
    var currentLocationMarker by remember { mutableStateOf<org.osmdroid.views.overlay.Marker?>(null) }
    var walkPolyline by remember { mutableStateOf<org.osmdroid.views.overlay.Polyline?>(null) }
    var hasInitializedLocation by remember { mutableStateOf(false) }

    val canStartWalk = hasLocationPermission && isLocationEnabled
    val showLocationWarning = !canStartWalk && !walkState.isTracking

    // Monitor permission changes
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            val newPermissionState = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (newPermissionState != hasLocationPermission) {
                hasLocationPermission = newPermissionState
            }
        }
    }

    LaunchedEffect(currentMapStyle) {
        if (currentMapStyleState != currentMapStyle) {
            currentMapStyleState = currentMapStyle
        }
    }

    // Location tracking - RESTORE DIRECT TRACKING (was working before)
    LaunchedEffect(walkState.isTracking, walkState.isPaused, hasLocationPermission, isLocationEnabled) {
        if (walkState.isTracking && !walkState.isPaused && hasLocationPermission && isLocationEnabled) {
            try {
                locationService.getLocationUpdates()
                    .onEach { location ->
                        val updatedPoints = walkState.points + location
                        val distance = calculateTotalDistance(updatedPoints)

                        val newState = walkState.copy(points = updatedPoints)
                            .updateStats(distance, 0.0, updatedPoints.size)

                        // Update service first (source of truth)
                        activity?.walkTrackingService?.syncState(newState)

                        // Then update local state (will be synced from service)
                        walkState = newState

                        // Update parent state for persistence
                        onWalkStateChange(newState)

                        mapView?.let { map ->
                            updateMapVisuals(
                                map = map,
                                location = location,
                                points = updatedPoints,
                                currentMarker = currentLocationMarker,
                                polyline = walkPolyline,
                                onMarkerUpdate = { currentLocationMarker = it },
                                onPolylineUpdate = { walkPolyline = it }
                            )
                        }
                    }
                    .catch { error ->
                        Toast.makeText(context, "Location tracking error", Toast.LENGTH_SHORT).show()
                    }
                    .launchIn(this)
            } catch (e: Exception) {
                Toast.makeText(context, "Location service error", Toast.LENGTH_SHORT).show()
            }
        } else if (walkState.isTracking && (!hasLocationPermission || !isLocationEnabled)) {
            val pausedState = walkState.copy(
                isPaused = true,
                pauseStartTime = System.currentTimeMillis()
            )
            walkState = pausedState
            onWalkStateChange(pausedState) // Persist to parent
            Toast.makeText(context, "Tracking paused - location unavailable", Toast.LENGTH_SHORT).show()
        }
    }

    // Update map visuals when walk state changes
    LaunchedEffect(walkState.isTracking, walkState.points.size) {
        if (walkState.points.isNotEmpty() && walkState.isTracking) {
            val lastPoint = walkState.points.last()
            mapView?.let { map ->
                updateMapVisuals(
                    map = map,
                    location = lastPoint,
                    points = walkState.points,
                    currentMarker = currentLocationMarker,
                    polyline = walkPolyline,
                    onMarkerUpdate = { currentLocationMarker = it },
                    onPolylineUpdate = { walkPolyline = it }
                )
            }
        }
    }

    LaunchedEffect(hasLocationPermission, isLocationEnabled, mapView) {
        if (hasLocationPermission && isLocationEnabled && mapView != null && !hasInitializedLocation) {
            MapUtils.getCurrentLocation(
                context = context,
                onSuccess = { location ->
                    val currentLocation = org.osmdroid.util.GeoPoint(location.latitude, location.longitude)
                    mapView?.controller?.setCenter(currentLocation)

                    MapUtils.addModernMarker(
                        mapView = mapView!!,
                        geoPoint = currentLocation,
                        title = "Current Location",
                        snippet = "Lat: ${String.format("%.6f", location.latitude)}, Lng: ${String.format("%.6f", location.longitude)}",
                        isLocationMarker = true,
                        isActiveTracking = false
                    )

                    hasInitializedLocation = true
                    Toast.makeText(context, "Location found!", Toast.LENGTH_SHORT).show()
                },
                onFailure = { }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map View
        AndroidView(
            factory = { ctx ->
                org.osmdroid.views.MapView(ctx).apply {
                    try {
                        setTileSource(currentMapStyle.tileSource)
                        setMultiTouchControls(true)
                        setBuiltInZoomControls(false)
                        setClickable(true)
                        controller.setZoom(15.0)
                        minZoomLevel = 3.0
                        maxZoomLevel = 18.0

                        val defaultCenter = org.osmdroid.util.GeoPoint(24.8607, 67.0011)
                        controller.setCenter(defaultCenter)
                        mapView = this
                    } catch (e: Exception) {
                        android.util.Log.e("MapView", "Error initializing map", e)
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { mv ->
                try {
                    val currentTileSource = mv.tileProvider.tileSource
                    val newTileSource = currentMapStyleState.tileSource

                    if (currentTileSource.name() != newTileSource.name()) {
                        val currentCenter = mv.mapCenter
                        val currentZoom = mv.zoomLevelDouble

                        mv.tileProvider.clearTileCache()
                        mv.setTileSource(newTileSource)
                        mv.controller.setCenter(currentCenter)
                        mv.controller.setZoom(currentZoom)
                        mv.invalidate()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MapView", "Error updating map style", e)
                }
            }
        )

        // Location Warning Banner
        AnimatedVisibility(
            visible = showLocationWarning,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clickable {
                        if (!hasLocationPermission) {
                            onRequestPermission { granted ->
                                if (granted && !isLocationEnabled) {
                                    onEnableLocation()
                                }
                            }
                        } else if (!isLocationEnabled) {
                            onEnableLocation()
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (!hasLocationPermission) "Location Required"
                            else "GPS Disabled",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                        Text(
                            text = if (!hasLocationPermission) "Tap to grant permission"
                            else "Tap to enable GPS",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Compact Tracking Stats
        AnimatedVisibility(
            visible = walkState.isTracking,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            Surface(
                modifier = Modifier.clickable { showFullScreenStats = true },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!walkState.isPaused) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 0.3f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "alpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    MaterialTheme.colorScheme.error.copy(alpha = alpha),
                                    CircleShape
                                )
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = String.format("%.2f", walkState.stats.distanceKm),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "km",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = walkState.stats.formatElapsedTime(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "View Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Bottom Right FABs
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(
                visible = !walkState.isTracking,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        if (!canStartWalk) {
                            Toast.makeText(
                                context,
                                "Please enable location to use this feature",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            mapView?.let { map ->
                                MapUtils.getCurrentLocation(
                                    context = context,
                                    onSuccess = { location ->
                                        val geoPoint = MapUtils.convertLocationToGeoPoint(location)
                                        MapUtils.centerMapOnLocation(map, geoPoint)
                                        Toast.makeText(context, "Centered on location", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(context, "Failed to get location", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp),
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 10.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "My Location",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Start Walk Button - DISABLED if no location
            AnimatedVisibility(
                visible = !walkState.isTracking,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        if (!canStartWalk) {
                            if (!hasLocationPermission) {
                                Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
                                onRequestPermission { granted ->
                                    if (granted && !isLocationEnabled) {
                                        onEnableLocation()
                                    }
                                }
                            } else if (!isLocationEnabled) {
                                Toast.makeText(context, "Please enable GPS to start tracking", Toast.LENGTH_SHORT).show()
                                onEnableLocation()
                            }
                        } else {
                            // Start tracking - update service first (source of truth)
                            val newState = LiveWalkState(
                                isTracking = true,
                                isPaused = false,
                                startTime = System.currentTimeMillis(),
                                points = emptyList()
                            )

                            // Update service first
                            activity?.walkTrackingService?.syncState(newState)

                            // Then update local and parent
                            walkState = newState
                            onWalkStateChange(newState)

                            mapView?.let { map ->
                                currentLocationMarker?.let { map.overlays.remove(it) }
                                walkPolyline?.let { map.overlays.remove(it) }
                                currentLocationMarker = null
                                walkPolyline = null
                                map.invalidate()
                            }
                        }
                    },
                    containerColor = if (canStartWalk)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (canStartWalk)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(56.dp),
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Walk",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Bottom Control Bar (when tracking)
        AnimatedVisibility(
            visible = walkState.isTracking,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 12.dp,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingActionButton(
                        onClick = {
                            val newState = if (walkState.isPaused) {
                                val pauseDuration = System.currentTimeMillis() - walkState.pauseStartTime
                                walkState.copy(
                                    isPaused = false,
                                    totalPausedTime = walkState.totalPausedTime + pauseDuration
                                )
                            } else {
                                walkState.copy(
                                    isPaused = true,
                                    pauseStartTime = System.currentTimeMillis()
                                )
                            }
                            // Update service first (source of truth)
                            activity?.walkTrackingService?.syncState(newState)
                            walkState = newState
                            onWalkStateChange(newState)
                        },
                        modifier = Modifier.size(56.dp),
                        containerColor = if (walkState.isPaused)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (walkState.isPaused)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Icon(
                            imageVector = if (walkState.isPaused)
                                Icons.Default.PlayArrow
                            else
                                Icons.Default.Pause,
                            contentDescription = if (walkState.isPaused) "Resume" else "Pause",
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    FloatingActionButton(
                        onClick = {
                            // Stop tracking
                            val finalState = walkState
                            val stoppedState = walkState.copy(isTracking = false, isPaused = false)

                            // Stop service first
                            val stopIntent = Intent(context, WalkTrackingService::class.java).apply {
                                action = WalkTrackingService.ACTION_STOP_TRACKING
                            }
                            context.startService(stopIntent)

                            // Then update local state
                            walkState = stoppedState
                            onWalkStateChange(stoppedState)

                            // MINIMUM 50m (0.05km) TO SAVE
                            if (finalState.stats.distanceKm >= 0.05) {
                                (context as ComponentActivity).lifecycleScope.launch {
                                    saveEnhancedWalk(finalState, firebaseService, context)
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Walk too short (minimum 50m required)",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            mapView?.let { map ->
                                currentLocationMarker?.let { map.overlays.remove(it) }
                                walkPolyline?.let { map.overlays.remove(it) }
                                currentLocationMarker = null
                                walkPolyline = null
                                map.invalidate()
                            }
                        },
                        modifier = Modifier.size(56.dp),
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Walk",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        if (showFullScreenStats) {
            FullScreenStatsView(
                walkState = walkState,
                onClose = { showFullScreenStats = false }
            )
        }
    }
}

@Composable
fun CompactStatusIndicator(
    walkState: LiveWalkState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (walkState.isPaused)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.primary
                    )
            )

            Text(
                text = walkState.stats.formatElapsedTime(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = String.format("%.2f km", walkState.stats.distanceKm),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "View details",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DetailedStatsSheet(
    walkState: LiveWalkState,
    onClose: () -> Unit
) {
    val stats = walkState.stats

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        StatCard(
            title = "Time",
            icon = Icons.Default.DateRange
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stats.formatElapsedTime(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (stats.pausedTimeMillis > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Active",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stats.formatActiveTime(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Distance",
                icon = Icons.Default.LocationOn,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = String.format("%.2f", stats.distanceKm),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "km",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            StatCard(
                title = "Avg Speed",
                icon = Icons.Default.Star,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = String.format("%.1f", stats.averageSpeedKmh),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "km/h",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Calories",
                icon = Icons.Default.FavoriteBorder,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = String.format("%.0f", stats.caloriesBurned),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "kcal",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            StatCard(
                title = "Points",
                icon = Icons.Default.Check,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stats.points.toString(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "tracked",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem(
                    label = "Status",
                    value = if (walkState.isPaused) "Paused" else "Active",
                    valueColor = if (walkState.isPaused)
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenStatsView(
    walkState: LiveWalkState,
    onClose: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Walk Statistics",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            DetailedStatsSheet(
                walkState = walkState,
                onClose = onClose
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun InfoItem(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

private fun updateMapVisuals(
    map: org.osmdroid.views.MapView,
    location: LocationPoint,
    points: List<LocationPoint>,
    currentMarker: org.osmdroid.views.overlay.Marker?,
    polyline: org.osmdroid.views.overlay.Polyline?,
    onMarkerUpdate: (org.osmdroid.views.overlay.Marker?) -> Unit,
    onPolylineUpdate: (org.osmdroid.views.overlay.Polyline?) -> Unit
) {
    val geoPoint = org.osmdroid.util.GeoPoint(location.latitude, location.longitude)

    if (currentMarker == null) {
        val newMarker = MapUtils.addModernMarker(
            mapView = map,
            geoPoint = geoPoint,
            title = "Current Location",
            snippet = "Tracking...",
            isLocationMarker = true,
            isActiveTracking = true
        )
        onMarkerUpdate(newMarker)
    } else {
        currentMarker.position = geoPoint
        currentMarker.snippet = "Lat: ${String.format("%.6f", location.latitude)}, Lng: ${String.format("%.6f", location.longitude)}"
    }

    if (points.size >= 2) {
        polyline?.let { map.overlays.remove(it) }

        val geoPoints = points.map { point ->
            org.osmdroid.util.GeoPoint(point.latitude, point.longitude)
        }

        val newPolyline = org.osmdroid.views.overlay.Polyline(map).apply {
            setPoints(geoPoints)
            outlinePaint.color = MapUtils.getTrackingPolylineColor(map.context)
            outlinePaint.strokeWidth = 10f
            title = "Current Walk Path"
        }
        map.overlays.add(newPolyline)
        onPolylineUpdate(newPolyline)
    }

    map.controller.animateTo(geoPoint)
    map.invalidate()
}

private fun calculateTotalDistance(points: List<LocationPoint>): Double {
    if (points.size < 2) return 0.0

    var totalDistance = 0.0
    for (i in 1 until points.size) {
        totalDistance += LocationService.calculateDistance(points[i - 1], points[i])
    }
    return totalDistance
}

private suspend fun saveEnhancedWalk(
    walkState: LiveWalkState,
    firebaseService: FirebaseService,
    context: Context
) {
    try {
        val walk = Walk(
            polylineCoordinates = walkState.points.map {
                com.google.firebase.firestore.GeoPoint(it.latitude, it.longitude)
            },
            distanceCovered = walkState.stats.distanceMeters,
            duration = walkState.stats.elapsedTimeMillis
        )

        val result = firebaseService.saveWalk(walk)

        result.fold(
            onSuccess = { walkId ->
                Toast.makeText(
                    context,
                    "Walk saved successfully! (${String.format("%.2f", walkState.stats.distanceKm)} km)",
                    Toast.LENGTH_LONG
                ).show()
                android.util.Log.d("MainActivity", "Walk saved with ID: $walkId, Distance: ${walkState.stats.distanceKm} km")
            },
            onFailure = { error ->
                Toast.makeText(
                    context,
                    "Failed to save walk: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
                android.util.Log.e("MainActivity", "Failed to save walk", error)
            }
        )
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Error saving walk: ${e.message}",
            Toast.LENGTH_LONG
        ).show()
        android.util.Log.e("MainActivity", "Exception while saving walk", e)
    }
}