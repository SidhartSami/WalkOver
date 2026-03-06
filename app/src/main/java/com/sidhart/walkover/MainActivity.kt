package com.sidhart.walkover

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import java.util.Locale
import androidx.core.graphics.toColorInt
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sidhart.walkover.data.*
import com.sidhart.walkover.service.FirebaseService
import com.sidhart.walkover.service.LocationService
import com.sidhart.walkover.service.WalkTrackingService
import com.sidhart.walkover.service.NetworkMonitor
import com.sidhart.walkover.service.OfflineQueueManager
import com.sidhart.walkover.ui.*
import com.sidhart.walkover.ui.theme.WalkOverTheme
import com.sidhart.walkover.utils.MapUtils
import com.sidhart.walkover.utils.AppPreferencesManager
import com.sidhart.walkover.ui.DiscoverScreen
import com.sidhart.walkover.ui.SearchFriendsScreen
import com.sidhart.walkover.ui.FriendsListScreen
import com.sidhart.walkover.ui.LeaderboardScreen
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import androidx.compose.foundation.isSystemInDarkTheme
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder

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
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var offlineQueueManager: OfflineQueueManager
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
        
        // Initialize offline queue manager
        offlineQueueManager = OfflineQueueManager(this, firebaseService)
        firebaseService.offlineQueueManager = offlineQueueManager
        
        // Initialize network monitor
        networkMonitor = NetworkMonitor(this)
        networkMonitor.startMonitoring()
        
        // Monitor network connectivity and process pending operations when online
        lifecycleScope.launch {
            networkMonitor.isConnected.collect { isConnected ->
                if (isConnected) {
                    android.util.Log.d("MainActivity", "🌐 Internet connected - processing pending operations")
                    offlineQueueManager.processPendingOperations()
                } else {
                    android.util.Log.d("MainActivity", "📡 Internet disconnected")
                }
            }
        }

        // Bind to WalkTrackingService
        Intent(this, WalkTrackingService::class.java).also { intent ->
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
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
        
        // Stop network monitoring
        if (::networkMonitor.isInitialized) {
            networkMonitor.stopMonitoring()
        }
        
        // Unbind service
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
        
        android.util.Log.d("MainActivity", "MainActivity destroyed, cleanup complete")
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
                    android.util.Log.d("MainActivity", "onLoginSuccess called, setting isAuthenticated=true and navigating")
                    isAuthenticated.value = true
                    kotlinx.coroutines.MainScope().launch {
                        kotlinx.coroutines.delay(200)
                        android.util.Log.d("MainActivity", "Navigating to 'main'")
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

    // Persistent walk state - service is source of truth, backed up by SharedPreferences
    val activity = context as? MainActivity
    val walkRecoveryManager = remember { com.sidhart.walkover.utils.WalkRecoveryManager(context) }
    var walkState by remember { mutableStateOf(walkRecoveryManager.getSavedWalkState() ?: LiveWalkState()) }

    // Save state on any changes
    LaunchedEffect(walkState) {
        walkRecoveryManager.saveWalkState(walkState)
    }

    // Always read from service as source of truth
    LaunchedEffect(activity?.isServiceBound, activity?.walkTrackingService) {
        val mainActivity = activity
        if (mainActivity?.isServiceBound == true && mainActivity.walkTrackingService != null) {
            val service = mainActivity.walkTrackingService!!
            val serviceState = service.walkState.value
            
            // If the service just started empty but we recovered a walk, push ours upstream!
            if (!serviceState.isTracking && walkState.isTracking) {
                service.syncState(walkState)
            } else {
                walkState = serviceState
            }

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

    // Dynamic map style based on system theme
    val currentMapStyle = if (isDarkTheme) MapStyle.MAPBOX_DARK else MapStyle.MAPBOX_STREETS

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
        ) {
            composable("home") {
                EnhancedMapScreenContent(
                    currentMapStyle = currentMapStyle,
                    locationService = locationService,
                    firebaseService = firebaseService,
                    context = context,
                    onRequestPermission = onRequestPermission,
                    onEnableLocation = onRequestLocationSettings,
                    persistentWalkState = walkState,
                    onWalkStateChange = { walkState = it },
                    onNavigateToChallenge = { navController.navigate("friends_list") },
                    onNavigateToDiscover = { navController.navigate("discover") },
                    onNavigateToProfile = { navController.navigate("profile") }
                )
            }
            
            composable("discover") {
                DiscoverScreen(
                    firebaseService = firebaseService,
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToLeaderboard = { navController.navigate("leaderboard") },
                    onNavigateToFriendsList = { navController.navigate("friends_list") },
                    onNavigateToSearchFriends = { navController.navigate("search_friends") },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable("profile") {
                ProfileScreen(
                    firebaseService = firebaseService,
                    onNavigateToWalkHistory = { navController.navigate("walk_history") },
                    onNavigateToDetailedStats = { navController.navigate("detailed_stats") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToFriendsList = { navController.navigate("friends_list") },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("detailed_stats") {
                StatsScreen(
                    firebaseService = firebaseService,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }


            composable("search_friends") {
                SearchFriendsScreen(
                    firebaseService = firebaseService,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }


            composable("friends_list") {
                FriendsListScreen(
                    firebaseService = firebaseService,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }


            composable("leaderboard") {
                LeaderboardScreen(
                    firebaseService = firebaseService,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToFriendsList = {
                        navController.navigate("friends_list")
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

            composable("settings") {
                SettingsScreen(
                    firebaseService = firebaseService,
                    onLogout = onLogout,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
 fun EnhancedMapScreenContent(
    currentMapStyle: MapStyle,
    locationService: LocationService,
    firebaseService: FirebaseService,
    context: Context,
    onRequestPermission: ((Boolean) -> Unit) -> Unit,
    onEnableLocation: () -> Unit,
    persistentWalkState: LiveWalkState,
    onWalkStateChange: (LiveWalkState) -> Unit,
    onNavigateToChallenge: () -> Unit = {},
    onNavigateToDiscover: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val scaffoldState = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()

    // Start with sheet fully expanded (so the full sheet is visible on launch)
    LaunchedEffect(Unit) {
        scaffoldState.bottomSheetState.expand()
    }
    val activity = context as? MainActivity

    val duelViewModel: com.sidhart.walkover.viewmodel.DuelViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val activeDuel by duelViewModel.activeDuel.collectAsState()
    val pendingResultDuel by duelViewModel.pendingResultDuel.collectAsState()
    val hasActiveDuel = activeDuel != null && activeDuel?.status == DuelStatus.ACTIVE.name


    LaunchedEffect(Unit) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            duelViewModel.checkActiveDuel(uid)
            // Also check for any completed duels the user hasn't seen yet
            duelViewModel.checkForUnseenDuelResult(uid)
        }
    }

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
    var selectedMode by remember { mutableStateOf("Ghost") } // Default mode

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

    // territory overlay state
    var territories by remember { mutableStateOf<List<Territory>>(emptyList()) }
    var showTerritories by remember { mutableStateOf(false) } // Default OFF
    var showCleanupDialog by remember { mutableStateOf(false) }
    var celebrationEvent by remember { mutableStateOf<CelebrationEvent?>(null) }

    val canStartWalk = hasLocationPermission && isLocationEnabled
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


    // Location tracking - RESTORE DIRECT TRACKING (was working before)
    // start territory listener when screen shown
    LaunchedEffect(Unit) {
        try {
            firebaseService.observeTerritoriesRealtime()
                .catch { error ->
                    android.util.Log.e("MainActivity", "❌ Territory Flow ERROR: ${error.message}", error)
                }
                .collect { updated ->
                    android.util.Log.d("MainActivity", "Territories fetched: ${updated.size}")
                    territories = updated
                }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Exception collecting territories: ${e.message}", e)
        }
    }

    // redraw overlays when territories change
    LaunchedEffect(territories, showTerritories, walkState.isTracking, selectedMode) {
        mapView?.let { map ->
            if (showTerritories && territories.isNotEmpty()) {
                val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                val filtered = if (selectedMode == "Duel") {
                    // Only show territories for current user and their single duel opponent
                    val opponentId = activeDuel?.let { duel ->
                        if (duel.challengerId == currentUserId) duel.opponentId else duel.challengerId
                    }
                    territories.filter { t ->
                        t.ownerId == currentUserId || (opponentId != null && t.ownerId == opponentId)
                    }
                } else {
                    territories // show all for Competitive
                }
                drawTerritoriesOnMap(map, filtered)
            } else {
                map.overlays.removeAll { it is org.osmdroid.views.overlay.Polygon && it.title?.startsWith("Territory_") == true }
                map.invalidate()
            }
        }
    }

    LaunchedEffect(walkState.isTracking, walkState.isPaused, hasLocationPermission, isLocationEnabled) {
        if (walkState.isTracking && !walkState.isPaused && hasLocationPermission && isLocationEnabled) {
            try {
                locationService.getLocationUpdates()
                    .onEach { location ->
                        val updatedPoints = walkState.points + location
                        val distance = calculateTotalDistance(updatedPoints)

                        val newState = walkState.copy(points = updatedPoints)
                            .updateStats(distance, updatedPoints.size)

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
                    .catch { _ ->
                        // Error handled silently
                    }
                    .launchIn(this)
            } catch (_: Exception) {
                // Error handled silently
            }
        } else if (walkState.isTracking && (!hasLocationPermission || !isLocationEnabled)) {
            val pausedState = walkState.copy(
                isPaused = true,
                pauseStartTime = System.currentTimeMillis()
            )
            walkState = pausedState
            onWalkStateChange(pausedState) // Persist to parent
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
                    val offsetGeoPoint = try {
                        val latSpan = mapView!!.boundingBox.latitudeSpan
                        if (latSpan > 0) {
                            org.osmdroid.util.GeoPoint(location.latitude - (latSpan * 0.20), location.longitude)
                        } else {
                            org.osmdroid.util.GeoPoint(location.latitude - 0.005, location.longitude)
                        }
                    } catch (e: Exception) {
                        org.osmdroid.util.GeoPoint(location.latitude - 0.005, location.longitude)
                    }
                    mapView?.controller?.setCenter(offsetGeoPoint)

                    MapUtils.addModernMarker(
                        mapView = mapView!!,
                        geoPoint = currentLocation,
                        title = "Current Location",
                        snippet = "Lat: ${String.format(Locale.US, "%.6f", location.latitude)}, Lng: ${String.format(Locale.US, "%.6f", location.longitude)}",
                        isLocationMarker = true,
                    )

                    hasInitializedLocation = true
                },
                onFailure = { _ -> }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
        // Peeked state: just the drag handle row visible, enough to swipe up
        sheetPeekHeight = 80.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        sheetContainerColor = Color.Transparent,
        sheetShadowElevation = 0.dp,
        sheetTonalElevation = 0.dp,
        sheetDragHandle = null,
        sheetContent = {
            MapScreenBottomSheet(
                walkState = walkState,
                selectedMode = selectedMode,
                hasActiveDuel = hasActiveDuel,
                onModeSelect = { selectedMode = it },
                onStartWalk = {
                    if (selectedMode == "Duel" && !hasActiveDuel) {
                        onNavigateToChallenge()
                    } else if (!canStartWalk) {
                        if (!hasLocationPermission) {
                            onRequestPermission { granted ->
                                if (granted && !isLocationEnabled) onEnableLocation()
                            }
                        } else if (!isLocationEnabled) {
                            onEnableLocation()
                        }
                    } else {
                        val newState = LiveWalkState(
                            isTracking = true,
                            isPaused = false,
                            mode = selectedMode,
                            startTime = System.currentTimeMillis()
                        )
                        activity?.walkTrackingService?.syncState(newState)
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
                onPauseResume = {
                    val newState = if (walkState.isPaused) {
                        val pauseDuration = System.currentTimeMillis() - walkState.pauseStartTime
                        walkState.copy(isPaused = false, totalPausedTime = walkState.totalPausedTime + pauseDuration)
                    } else {
                        walkState.copy(isPaused = true, pauseStartTime = System.currentTimeMillis())
                    }
                    activity?.walkTrackingService?.syncState(newState)
                    walkState = newState
                    onWalkStateChange(newState)
                },
                onStop = {
                    val finalState = walkState
                    context.startService(Intent(context, WalkTrackingService::class.java).apply {
                        action = WalkTrackingService.ACTION_STOP_TRACKING
                    })
                    walkState = walkState.copy(isTracking = false, isPaused = false)
                    onWalkStateChange(walkState)
                    if (finalState.stats.distanceKm >= 0.05) {
                        (context as ComponentActivity).lifecycleScope.launch {
                            saveEnhancedWalk(
                                walkState = finalState,
                                firebaseService = firebaseService,
                                context = context,
                                onCelebration = { event -> celebrationEvent = event }
                            )
                        }
                    } else {
                        Toast.makeText(context, "Walk too short", Toast.LENGTH_SHORT).show()
                    }
                    // Clear ALL walk overlays from the map
                    mapView?.let { map ->
                        map.overlays.removeAll { overlay ->
                            overlay is org.osmdroid.views.overlay.Marker ||
                            overlay is org.osmdroid.views.overlay.Polyline
                        }
                        currentLocationMarker = null
                        walkPolyline = null
                        map.invalidate()
                    }
                },
                scaffoldState = scaffoldState,
            )
        }
    ) { padding ->
        // Calculate dynamic height of the bottom sheet to keep FABs above it
        val density = androidx.compose.ui.platform.LocalDensity.current
        val sheetOffset = try { scaffoldState.bottomSheetState.requireOffset() } catch (_: Exception) { 0f }
        
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val constraints = this
            val sheetHeight = with(density) { 
                (constraints.maxHeight.toPx() - sheetOffset).toDp() 
            }.coerceAtLeast(0.dp)
            
            // The padding should be at least the peek height (nav bar aware) or the actual sheet height
            val fabBottomPadding = (sheetHeight + 16.dp).coerceAtLeast(padding.calculateBottomPadding() + 8.dp)

            Box(modifier = Modifier.fillMaxSize()) {
                // Map View
                AndroidView(
                    factory = { ctx ->
                        org.osmdroid.views.MapView(ctx).apply {
                            setTileSource(currentMapStyle.tileSource)
                            setMultiTouchControls(true)
                            controller.setZoom(15.0)
                            val defaultCenter = org.osmdroid.util.GeoPoint(24.8607, 67.0011)
                            controller.setCenter(defaultCenter)
                            mapView = this
                        }
                    },
                    update = { map ->
                        if (map.tileProvider.tileSource != currentMapStyle.tileSource) {
                            map.setTileSource(currentMapStyle.tileSource)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                val showLocationWarning = !hasLocationPermission || !isLocationEnabled

                // Top Navigation Buttons (Snapchat style, above the map)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Button
                    FloatingActionButton(
                        onClick = onNavigateToProfile,
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        contentColor = Color(0xFF007AFF),
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(2.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = "Profile")
                    }

                    // Location Warning Banner
                    AnimatedVisibility(
                        visible = showLocationWarning,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.clickable { 
                                if (!hasLocationPermission) {
                                    onRequestPermission { granted ->
                                        if (granted && !isLocationEnabled) onEnableLocation()
                                    }
                                } else {
                                    onEnableLocation()
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp), 
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (!hasLocationPermission) "Location required" else "GPS disabled", 
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Discover Button
                    FloatingActionButton(
                        onClick = onNavigateToDiscover,
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        contentColor = Color(0xFF007AFF),
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(2.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Public, contentDescription = "Discover")
                    }
                }

                // Map UI Controls (Right side buttons - always above the bottom sheet)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = fabBottomPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Territory toggle — only shown when walk is active AND mode is Duel or Competitive
                    if (walkState.isTracking && (selectedMode == "Duel" || selectedMode == "Competitive")) {
                        FloatingActionButton(
                            onClick = { showTerritories = !showTerritories },
                            containerColor = if (showTerritories) Color(0xFF007AFF) else MaterialTheme.colorScheme.surface,
                            contentColor = if (showTerritories) Color.White else Color(0xFF007AFF),
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Map, null, modifier = Modifier.size(20.dp))
                        }
                    }

                    // My Location FAB — always shown
                    FloatingActionButton(
                        onClick = {
                            mapView?.let { map ->
                                MapUtils.getCurrentLocation(
                                    context = context,
                                    onSuccess = { location ->
                                        val geoPoint = org.osmdroid.util.GeoPoint(location.latitude, location.longitude)
                                        val offsetGeoPoint = try {
                                            val latSpan = map.boundingBox.latitudeSpan
                                            org.osmdroid.util.GeoPoint(location.latitude - (latSpan * 0.25), location.longitude)
                                        } catch (e: Exception) {
                                            geoPoint
                                        }
                                        map.controller.animateTo(offsetGeoPoint)
                                    },
                                    onFailure = { _ -> }
                                )
                            }
                        },
                        containerColor = if (isSystemInDarkTheme()) Color(0xFF1C2A3A) else Color.White,
                        contentColor = Color(0xFF007AFF),
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(20.dp))
                    } // closes MyLocation FAB content
                } // closes Column
            } // closes inner Map Box
        } // closes BoxWithConstraints
    } // closes BottomSheetScaffold



        // ── Duel celebration overlay — renders directly over everything ──
        val celebUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

        if (activeDuel?.status == DuelStatus.COMPLETED.name) {
            DuelCompletionDialog(
                challenge = activeDuel!!,
                currentUserId = celebUid,
                onDismiss = { duelViewModel.clearActiveDuel() }
            )
        }

        if (pendingResultDuel != null) {
            DuelVictoryCelebration(
                challenge = pendingResultDuel!!,
                currentUserId = celebUid,
                onDismiss = { duelViewModel.markResultSeen(pendingResultDuel!!, celebUid) }
            )
        }
    } // Close the Box

    // ── Merge territories confirmation dialog ─────────────────────
    if (showCleanupDialog) {
        AlertDialog(
            onDismissRequest = { showCleanupDialog = false },
            icon = { Text("🧹", fontSize = 28.sp) },
            title = { Text("Merge My Territories", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Scans all your territories in Firestore, merges overlapping or " +
                    "adjacent ones (within 80 m) into unified polygons, and removes " +
                    "old fragmented documents.\n\nRun once to fix historical data."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showCleanupDialog = false
                    scope.launch {
                        firebaseService.cleanupAllConflicts().fold(
                            onSuccess = { (merged, resolved) ->
                                val msg = when {
                                    merged == 0 && resolved == 0 -> "✅ All territories are clean!"
                                    merged > 0 && resolved > 0  -> "✅ Merged $merged + resolved $resolved conflict(s)."
                                    merged > 0                  -> "✅ Merged $merged own territory docs."
                                    else                        -> "✅ Resolved $resolved cross-user conflict(s)."
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            },
                            onFailure = { err ->
                                Toast.makeText(context, "❌ Cleanup failed: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }) { Text("Merge Now") }
            },
            dismissButton = {
                TextButton(onClick = { showCleanupDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Territory celebration overlay ────────────────────────────
    val currentCelebration = celebrationEvent
    if (currentCelebration != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { celebrationEvent = null },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            TerritoryCaptureCelebration(
                event     = currentCelebration,
                onDismiss = { celebrationEvent = null }
            )
        }
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
        )
        onMarkerUpdate(newMarker)
    } else {
        currentMarker.position = geoPoint
        currentMarker.snippet = "Lat: ${String.format(Locale.US, "%.6f", location.latitude)}, Lng: ${String.format(Locale.US, "%.6f", location.longitude)}"
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

    val offsetGeoPoint = try {
        val latSpan = map.boundingBox.latitudeSpan
        org.osmdroid.util.GeoPoint(location.latitude - (latSpan * 0.25), location.longitude)
    } catch (e: Exception) {
        geoPoint
    }
    map.controller.animateTo(offsetGeoPoint)
    map.invalidate()
}

// --- territory drawing helper ---
private fun drawTerritoriesOnMap(
    map: org.osmdroid.views.MapView,
    territories: List<Territory>
) {
    map.overlays.removeAll { it is org.osmdroid.views.overlay.Polygon && it.title?.startsWith("Territory_") == true }
    territories.forEach { territory ->
        if (territory.polygon.size >= 3) {
            val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            val isMine = territory.ownerId == currentUserId
            val fillColor = if (isMine) "#4400B4FF".toColorInt()
                else "#44FF3B30".toColorInt()
            val borderColor = if (isMine) "#FF00B4FF".toColorInt()
                else "#FFFF3B30".toColorInt()
            val poly = org.osmdroid.views.overlay.Polygon().apply {
                title = "Territory_${territory.id}"
                points = territory.polygon.map { org.osmdroid.util.GeoPoint(it.latitude, it.longitude) } +
                         listOf(org.osmdroid.util.GeoPoint(territory.polygon.first().latitude, territory.polygon.first().longitude))
                fillPaint.color = fillColor
                outlinePaint.color = borderColor
                outlinePaint.strokeWidth = if (isMine) 3f else 2f
                isVisible = true
            }
            map.overlays.add(poly)
        }
    }
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
    context: Context,
    onCelebration: ((CelebrationEvent) -> Unit)? = null
) {
    try {
        val finalMode = when (walkState.mode) {
            "Competitive" -> WalkMode.COMPETE.name
            "Duel" -> WalkMode.DUEL.name
            "Ghost" -> WalkMode.GHOST.name
            else -> WalkMode.ROAM.name
        }
        var polygonArea = 0.0
        var capturedPoly = emptyList<com.google.firebase.firestore.GeoPoint>()
        
        if (walkState.mode == "Competitive") {
            val osmdroidPoints = walkState.points.map { org.osmdroid.util.GeoPoint(it.latitude, it.longitude) }

            // Always compute a convex hull from ALL walk points — no closed-loop required.
            // This is the territory you "walked around", regardless of whether you returned to start.
            if (osmdroidPoints.size >= 3) {
                val hull = com.sidhart.walkover.utils.ConvexHull.compute(osmdroidPoints)
                if (hull.size >= 3) {
                    polygonArea = MapUtils.calculatePolygonArea(hull)
                    capturedPoly = hull.map { com.google.firebase.firestore.GeoPoint(it.latitude, it.longitude) }
                    android.util.Log.d("MainActivity",
                        "🏙️ Competitive hull: ${hull.size} pts, area=${polygonArea.toInt()} m²")
                }
            }
        }

        // ── ANTI-CHEAT ENGINE ──
        var antiCheatStatus = "VALID"
        val averageSpeedKmh = walkState.stats.averageSpeedKmh
        
        // 1. Average Speed Check (Cap at 15 km/h)
        if (averageSpeedKmh > 15.0) {
            antiCheatStatus = "REJECTED_SPEED"
        } else if (walkState.points.size >= 2) {
            // 2. High-Speed Segment Spikes (The "Highway" Rule)
            var highSpeedSegmentsCount = 0
            val totalSegments = walkState.points.size - 1
            
            for (i in 1 until walkState.points.size) {
                val p1 = walkState.points[i-1]
                val p2 = walkState.points[i]
                val distMeters = LocationService.calculateDistance(p1, p2)
                val timeDiffMillis = p2.timestamp - p1.timestamp
                
                if (timeDiffMillis > 0) {
                    val speedKmh = (distMeters / (timeDiffMillis / 1000.0)) * 3.6
                    if (speedKmh > 30.0) {
                        highSpeedSegmentsCount++
                    }
                }
            }
            
            // If more than 5% of segments are over 30km/h
            if (totalSegments > 0 && (highSpeedSegmentsCount.toDouble() / totalSegments) > 0.05) {
                antiCheatStatus = "REJECTED_SPEED"
            }
        }
        
        // 3. Minimum Duration & Distance Rule
        val durationMins = walkState.stats.elapsedTimeMillis / 60000.0
        val distanceMeters = walkState.stats.distanceMeters
        if (durationMins < 2.0 || distanceMeters < 50.0) {
            antiCheatStatus = "REJECTED_DISTANCE"
        }

        val walk = Walk(
            polylineCoordinates = walkState.points.map {
                com.google.firebase.firestore.GeoPoint(it.latitude, it.longitude)
            },
            distanceCovered = walkState.stats.distanceMeters,
            duration = walkState.stats.elapsedTimeMillis,
            mode = finalMode,
            capturedPolygon = capturedPoly,
            capturedAreaM2 = polygonArea,
            status = antiCheatStatus
        )

        val result = firebaseService.saveWalk(walk)

        result.fold(
            onSuccess = { walkId ->
                if (walk.status == "VALID") {
                    // Update challenge progress after saving walk
                    firebaseService.updateChallengesAfterWalk(walk)

                    if (walkState.mode == "Competitive") {
                        // ── Territory save (always attempted for competitive) ──
                        val hullPoints = capturedPoly.map {
                            org.osmdroid.util.GeoPoint(it.latitude, it.longitude)
                        }
                        if (hullPoints.size >= 3) {
                            firebaseService.resolveConflictsAndSave(
                                newHullPoints = hullPoints,
                                sourceWalkId  = walkId,
                            ).onFailure { err ->
                                android.util.Log.e("MainActivity",
                                    "❌ Territory save failed: ${err.message}", err)
                            }
                        }

                        // ── Auto-cleanup: ALWAYS resolve cross-user overlaps ──
                        // Runs regardless of territory save result
                        try {
                            firebaseService.cleanupAllConflicts()
                        } catch (e: Exception) {
                            android.util.Log.w("MainActivity", "Cleanup warning: ${e.message}")
                        }

                        // ── Celebrate competitive walk ────────────────────────
                        onCelebration?.invoke(
                            CelebrationEvent(
                                areaM2     = polygonArea,
                                distanceKm = walkState.stats.distanceKm,
                                isRoam     = false
                            )
                        )
                    } else {
                        // ── Roam walk celebration ─────────────────────────────
                        onCelebration?.invoke(
                            CelebrationEvent(
                                areaM2     = 0.0,
                                distanceKm = walkState.stats.distanceKm,
                                isRoam     = true
                            )
                        )
                    }
                } else {
                    Toast.makeText(context, "Walk rejected by Anti-Cheat: ${walk.status}", Toast.LENGTH_LONG).show()
                }

                Toast.makeText(
                    context,
                    "Walk saved! (${String.format(Locale.US, "%.2f", walkState.stats.distanceKm)} km)",
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

