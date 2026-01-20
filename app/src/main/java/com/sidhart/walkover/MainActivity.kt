package com.sidhart.walkover

import androidx.lifecycle.viewmodel.compose.viewModel
import com.sidhart.walkover.viewmodel.ProfileViewModel
import com.sidhart.walkover.viewmodel.ProfileViewModelFactory
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
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Description

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

    // Callbacks for permission and location state changes
    private var onPermissionGrantedCallback: (() -> Unit)? = null
    private var onLocationEnabledCallback: (() -> Unit)? = null

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

        if (allGranted) {
            // Permission granted - now check GPS
            onPermissionGrantedCallback?.invoke()
            checkAndPromptGPS()
        } else {
            Toast.makeText(
                this,
                "Location permission is required for tracking walks",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // User returned from location settings
        if (isLocationEnabled(this)) {
            onLocationEnabledCallback?.invoke()
            // Location enabled callback
        }
    }

    private val locationDialogLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        // User interacted with the location dialog
        if (isLocationEnabled(this)) {
            onLocationEnabledCallback?.invoke()
            // Location enabled via dialog
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_WalkOver)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // SharedPreferences for onboarding state
        val prefs = getSharedPreferences("walkover_prefs", MODE_PRIVATE)
        
        // Check onboarding state (only check, don't clear)
        val termsAccepted = prefs.getBoolean("terms_accepted", false)
        val onboardingCompleted = prefs.getBoolean("onboarding_completed", false)
        
        // Route to appropriate screen based on state
        when {
            !termsAccepted -> {
                // Step 1: Terms & Conditions (first time only)
                android.util.Log.d("OnboardingFlow", "First launch: Showing Terms & Conditions")
                startActivity(Intent(this, TermsAndConditionsActivity::class.java))
                finish()
                return
            }
            !onboardingCompleted -> {
                // Step 2: Onboarding Slides (first time only)
                android.util.Log.d("OnboardingFlow", "Showing Onboarding Slides")
                startActivity(Intent(this, OnboardingActivity::class.java))
                finish()
                return
            }
        }
        
        // Onboarding complete - proceed to main app
        android.util.Log.d("OnboardingFlow", "Onboarding complete - Loading main app")

        initializeOsmdroidConfiguration()

        locationService = LocationService(this)
        firebaseService = FirebaseService()

        // Bind to WalkTrackingService
        Intent(this, WalkTrackingService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        setContent {
            WalkOverTheme {
                AuthNavigationWrapper(
                    locationService = locationService,
                    firebaseService = firebaseService,
                    context = this@MainActivity,
                    onRequestPermissions = ::requestLocationPermissions,
                    onRequestLocationSettings = ::promptEnableLocation
                )
            }
        }
    }

    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestLocationPermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun checkAndPromptGPS() {
        if (!isLocationEnabled(this)) {
            promptEnableLocation()
        } else {
            onLocationEnabledCallback?.invoke()
        }
    }

    fun promptEnableLocation() {
        // Use Google Play Services to show native location dialog
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000
            fastestInterval = 5000
        }

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true) // Show dialog even if location is disabled

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // Location is already enabled
            onLocationEnabledCallback?.invoke()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    // Show the native dialog by launching the IntentSender
                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                    locationDialogLauncher.launch(intentSenderRequest)
                } catch (sendEx: Exception) {
                    // If dialog fails to show, fall back to settings
                    Toast.makeText(this, "Please enable location in settings", Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    locationSettingsLauncher.launch(intent)
                }
            }
        }
    }

    fun setPermissionGrantedCallback(callback: () -> Unit) {
        onPermissionGrantedCallback = callback
    }

    fun setLocationEnabledCallback(callback: () -> Unit) {
        onLocationEnabledCallback = callback
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
    onRequestPermissions: () -> Unit,
    onRequestLocationSettings: () -> Unit
) {
    val navController = rememberNavController()
    val isAuthenticated = remember { mutableStateOf<Boolean?>(null) }
    val shouldRequestPermissions = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val authStatus = firebaseService.isUserAuthenticated()
            kotlinx.coroutines.delay(300)
            isAuthenticated.value = authStatus
            
            // For returning users (already authenticated), check location immediately
            if (authStatus) {
                kotlinx.coroutines.delay(800) // Delay to ensure UI is ready
                shouldRequestPermissions.value = true
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthNavWrapper", "Auth check failed", e)
            isAuthenticated.value = false
        }
    }

    // Request permissions immediately after authentication
    LaunchedEffect(shouldRequestPermissions.value) {
        if (shouldRequestPermissions.value) {
            kotlinx.coroutines.delay(500) // Small delay for navigation to complete
            val mainActivity = context as? MainActivity
            if (mainActivity?.hasLocationPermission() == false) {
                onRequestPermissions()
            } else if (mainActivity?.hasLocationPermission() == true && !isLocationEnabled(context)) {
                onRequestLocationSettings()
            }
            shouldRequestPermissions.value = false
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
                        // Trigger permission request after navigation
                        kotlinx.coroutines.delay(300)
                        shouldRequestPermissions.value = true
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
                        // Trigger permission request after navigation
                        kotlinx.coroutines.delay(300)
                        shouldRequestPermissions.value = true
                    }
                },
                onNavigateToVerification = { email ->
                    navController.navigate("verification/$email") {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("register") {
            RegisterScreen(
                firebaseService = firebaseService,
                onRegisterSuccess = { userEmail ->
                    navController.navigate("verification/$userEmail") {
                        popUpTo("register") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable("verification/{email}") { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            EmailVerificationScreen(
                firebaseService = firebaseService,
                userEmail = email,
                onVerificationComplete = {
                    isAuthenticated.value = true
                    kotlinx.coroutines.MainScope().launch {
                        kotlinx.coroutines.delay(300)
                        navController.navigate("main") {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                        // Trigger permission request after navigation
                        kotlinx.coroutines.delay(300)
                        shouldRequestPermissions.value = true
                    }
                },
                onBackToLogin = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
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
                onRequestPermissions = onRequestPermissions,
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
    onRequestPermissions: () -> Unit,
    onRequestLocationSettings:  () -> Unit
) {
    val navController = rememberNavController()
    val isDarkTheme = isSystemInDarkTheme()

    // Tutorial State - show only if not completed yet
    val prefs = context.getSharedPreferences("walkover_prefs", android.content.Context.MODE_PRIVATE)
    val hasSeenTutorial = prefs.getBoolean("tutorial_completed", false)
    val shouldShowTutorial = !hasSeenTutorial
    
    var showTutorial by remember { mutableStateOf(shouldShowTutorial) }
    var currentStepIndex by remember { mutableStateOf(0) }

    // Coordinate Capture
    var startButtonCoords by remember { mutableStateOf(null as androidx.compose.ui.layout.LayoutCoordinates?) }
    var statsPanelCoords by remember { mutableStateOf(null as androidx.compose.ui.layout.LayoutCoordinates?) }
    var profileTabCoords by remember { mutableStateOf(null as androidx.compose.ui.layout.LayoutCoordinates?) }
    
    val tutorialSteps = remember {
        listOf(
             TutorialStep(
                "step1",
                "Start Walk",
                "Tap here to start tracking your walk instantly.",
                null
            ),
             TutorialStep(
                "step2",
                "Live Stats",
                "Tap here to see live distance, time, and pace.",
                null
            ),
             TutorialStep(
                "step3",
                "Profile",
                "View your weekly progress and activity stats here.",
                null
            )
        )
    }

    val activity = context as? MainActivity
    var walkState by remember { mutableStateOf(LiveWalkState()) }

    LaunchedEffect(activity?.isServiceBound, activity?.walkTrackingService) {
        val mainActivity = activity
        if (mainActivity?.isServiceBound == true && mainActivity.walkTrackingService != null) {
            val service = mainActivity.walkTrackingService!!
            walkState = service.walkState.value
            service.walkState.collect { state ->
                walkState = state
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500)
            val mainActivity = activity
            if (mainActivity?.isServiceBound == true && mainActivity.walkTrackingService != null) {
                val serviceState = mainActivity.walkTrackingService!!.walkState.value
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.navigationBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    val items = listOf(Screen.Profile, Screen.Map, Screen.Settings)
                    
                    items.forEach { screen ->
                        NavigationBarItem(
                            modifier = Modifier.onGloballyPositioned { 
                                if (screen == Screen.Profile) profileTabCoords = it
                            },
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
                        onRequestPermissions = onRequestPermissions,
                        onEnableLocation = onRequestLocationSettings,
                        persistentWalkState = walkState,
                        onWalkStateChange = { walkState = it },
                        // Pass coordinate setters
                        onFabPositioned = { startButtonCoords = it },
                        onStatsPositioned = { statsPanelCoords = it }
                    )
                }

                composable(Screen.Profile.route) {
                    val profileViewModel: ProfileViewModel = viewModel(
                        factory = ProfileViewModelFactory(firebaseService)
                    )

                    ProfileScreen(
                        viewModel = profileViewModel,
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
        
        // Tutorial Layer
        if (showTutorial && currentStepIndex < tutorialSteps.size) {
            val currentStep = tutorialSteps[currentStepIndex].copy(
                targetCoordinates = when(currentStepIndex) {
                    0 -> startButtonCoords
                    1 -> statsPanelCoords
                    2 -> profileTabCoords
                    else -> null
                }
            )
            
            TutorialOverlay(
                currentStep = currentStep,
                onNext = {
                    currentStepIndex++
                    if (currentStepIndex >= tutorialSteps.size) {
                        showTutorial = false
                        
                        // Save tutorial completion
                        context.getSharedPreferences("walkover_prefs", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("tutorial_completed", true)
                            .apply()
                        
                        android.util.Log.d("OnboardingFlow", "Step 4 Complete: Interactive Tutorial")
                        android.util.Log.d("OnboardingFlow", "All Onboarding Phases Complete!")
                    }
                },
                onSkip = {
                    showTutorial = false
                    
                    // Save tutorial as skipped (still completed)
                    context.getSharedPreferences("walkover_prefs", android.content.Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("tutorial_completed", true)
                        .apply()
                    
                    android.util.Log.d("OnboardingFlow", "Step 4 Skipped: Tutorial")
                    android.util.Log.d("OnboardingFlow", "All Onboarding Phases Complete!")
                }
            )
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
    onRequestPermissions: () -> Unit,
    onEnableLocation: () -> Unit,
    persistentWalkState: LiveWalkState,
    onWalkStateChange: (LiveWalkState) -> Unit,
    onFabPositioned: (androidx.compose.ui.layout.LayoutCoordinates?) -> Unit = {},
    onStatsPositioned: (androidx.compose.ui.layout.LayoutCoordinates?) -> Unit = {}
) {
    val isDarkTheme = isSystemInDarkTheme()
    val mainActivity = context as? MainActivity

    val sharedPrefs = context.getSharedPreferences("walkover_prefs", android.content.Context.MODE_PRIVATE)
    var showPolicyDialog by remember {
        mutableStateOf(!sharedPrefs.getBoolean("policy_accepted", false))
    }

    var hasLocationPermission by remember {
        mutableStateOf(mainActivity?.hasLocationPermission() ?: false)
    }
    val isLocationEnabled by rememberLocationState(context)

    var walkState by remember { mutableStateOf(persistentWalkState) }
    var showFullScreenStats by remember { mutableStateOf(false) }
    var currentMapStyleState by remember { mutableStateOf(currentMapStyle) }

    // Map state
    var mapView by remember { mutableStateOf<org.osmdroid.views.MapView?>(null) }
    var currentLocationMarker by remember { mutableStateOf<org.osmdroid.views.overlay.Marker?>(null) }
    var walkPolyline by remember { mutableStateOf<org.osmdroid.views.overlay.Polyline?>(null) }

    // CRITICAL FIX: Use derivedStateOf to track when map is truly ready for marker placement
    var isMapFullyReady by remember { mutableStateOf(false) }
    var shouldPlaceInitialMarker by remember { mutableStateOf(false) }

    LaunchedEffect(persistentWalkState) {
        if (persistentWalkState != walkState) {
            walkState = persistentWalkState
        }
    }

    LaunchedEffect(walkState.isTracking, walkState.points.size, walkState.stats.elapsedTimeMillis) {
        onWalkStateChange(walkState)
    }

    val activity = context as? MainActivity
    LaunchedEffect(walkState.isTracking) {
        if (walkState.isTracking) {
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

    LaunchedEffect(walkState.isTracking, walkState.stats.elapsedTimeMillis, walkState.points.size) {
        if (walkState.isTracking) {
            activity?.walkTrackingService?.syncState(walkState)
        }
    }

    val canStartWalk = hasLocationPermission && isLocationEnabled
    val showLocationWarning = !canStartWalk && !walkState.isTracking

    // Monitor permission changes continuously
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            val newPermissionState = mainActivity?.hasLocationPermission() ?: false
            if (newPermissionState != hasLocationPermission) {
                hasLocationPermission = newPermissionState
                if (newPermissionState && isMapFullyReady) {
                    shouldPlaceInitialMarker = true
                }
            }
        }
    }

    // CRITICAL FIX: Set callbacks for permission/GPS events
    LaunchedEffect(Unit) {
        mainActivity?.setPermissionGrantedCallback {
            hasLocationPermission = true
            if (isMapFullyReady) {
                shouldPlaceInitialMarker = true
            }
        }

        mainActivity?.setLocationEnabledCallback {
            if (hasLocationPermission && isMapFullyReady) {
                shouldPlaceInitialMarker = true
            }
        }
    }

    // CRITICAL FIX: Place marker when conditions are met
    LaunchedEffect(shouldPlaceInitialMarker, isMapFullyReady, hasLocationPermission, isLocationEnabled) {
        if (shouldPlaceInitialMarker && isMapFullyReady && hasLocationPermission && isLocationEnabled && !walkState.isTracking) {
            mapView?.let { map ->
                MapUtils.getCurrentLocation(
                    context = context,
                    onSuccess = { location ->
                        val currentLocation = org.osmdroid.util.GeoPoint(location.latitude, location.longitude)
                        map.controller.animateTo(currentLocation)

                        // Remove old marker if exists
                        currentLocationMarker?.let { map.overlays.remove(it) }

                        // Add new marker
                        val marker = MapUtils.addModernMarker(
                            mapView = map,
                            geoPoint = currentLocation,
                            title = "Current Location",
                            snippet = "Lat: ${String.format("%.6f", location.latitude)}, Lng: ${String.format("%.6f", location.longitude)}",
                            isLocationMarker = true,
                            isActiveTracking = false
                        )
                        currentLocationMarker = marker
                        map.invalidate()

                        // Initial location marker placed
                        shouldPlaceInitialMarker = false // Reset flag
                    },
                    onFailure = { error ->
                        android.util.Log.e("MapScreen", "Failed to get location", error)
                        shouldPlaceInitialMarker = false
                    }
                )
            }
        }
    }

    LaunchedEffect(currentMapStyle) {
        if (currentMapStyleState != currentMapStyle) {
            currentMapStyleState = currentMapStyle
        }
    }

    // Location tracking
    LaunchedEffect(walkState.isTracking, walkState.isPaused, hasLocationPermission, isLocationEnabled) {
        if (walkState.isTracking && !walkState.isPaused && hasLocationPermission && isLocationEnabled) {
            try {
                locationService.getLocationUpdates()
                    .onEach { location ->
                        val updatedPoints = walkState.points + location
                        val distance = calculateTotalDistance(updatedPoints)

                        val newState = walkState.copy(points = updatedPoints)
                            .updateStats(distance, 0.0, updatedPoints.size)

                        activity?.walkTrackingService?.syncState(newState)
                        walkState = newState
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
            onWalkStateChange(pausedState)
            Toast.makeText(context, "Tracking paused - location unavailable", Toast.LENGTH_SHORT).show()
        }
    }

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

                        // Mark map as ready after post to ensure it's fully laid out
                        post {
                            isMapFullyReady = true
                            // Trigger marker placement if conditions are already met
                            if (hasLocationPermission && isLocationEnabled && !walkState.isTracking) {
                                shouldPlaceInitialMarker = true
                            }
                        }
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
                            onRequestPermissions()
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
                .onGloballyPositioned { onStatsPositioned(it) }
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
                                        
                                        // Create marker if it doesn't exist
                                        if (currentLocationMarker == null && !walkState.isTracking) {
                                            // Remove old marker if somehow still on map
                                            currentLocationMarker?.let { map.overlays.remove(it) }
                                            
                                            // Add new marker
                                            val marker = MapUtils.addModernMarker(
                                                mapView = map,
                                                geoPoint = geoPoint,
                                                title = "Current Location",
                                                snippet = "Lat: ${String.format("%.6f", location.latitude)}, Lng: ${String.format("%.6f", location.longitude)}",
                                                isLocationMarker = true,
                                                isActiveTracking = false
                                            )
                                            currentLocationMarker = marker
                                            map.invalidate()
                                        }
                                        
                                        // Map centered on location
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
                                onRequestPermissions()
                            } else if (!isLocationEnabled) {
                                Toast.makeText(context, "Please enable GPS to start tracking", Toast.LENGTH_SHORT).show()
                                onEnableLocation()
                            }
                        } else {
                            val newState = LiveWalkState(
                                isTracking = true,
                                isPaused = false,
                                startTime = System.currentTimeMillis(),
                                points = emptyList()
                            )

                            activity?.walkTrackingService?.syncState(newState)
                            walkState = newState
                            onWalkStateChange(newState)

                            // Clear map overlays and reset marker reference
                            mapView?.let { map ->
                                currentLocationMarker?.let { map.overlays.remove(it) }
                                walkPolyline?.let { map.overlays.remove(it) }
                                currentLocationMarker = null
                                walkPolyline = null
                                map.invalidate()
                                
                                // Immediately get current location and create tracking marker
                                MapUtils.getCurrentLocation(
                                    context = context,
                                    onSuccess = { location ->
                                        val geoPoint = org.osmdroid.util.GeoPoint(location.latitude, location.longitude)
                                        
                                        // Create initial tracking marker
                                        val marker = MapUtils.addModernMarker(
                                            mapView = map,
                                            geoPoint = geoPoint,
                                            title = "Current Location",
                                            snippet = "Tracking...",
                                            isLocationMarker = true,
                                            isActiveTracking = true
                                        )
                                        currentLocationMarker = marker
                                        
                                        // Center map on current location
                                        map.controller.animateTo(geoPoint)
                                        map.invalidate()
                                    },
                                    onFailure = { error ->
                                        android.util.Log.e("StartTracking", "Failed to get initial location", error)
                                    }
                                )
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
                    modifier = Modifier
                        .size(56.dp)
                        .onGloballyPositioned { onFabPositioned(it) }, // Capture FAB position
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
                            val finalState = walkState
                            val stoppedState = walkState.copy(isTracking = false, isPaused = false)

                            val stopIntent = Intent(context, WalkTrackingService::class.java).apply {
                                action = WalkTrackingService.ACTION_STOP_TRACKING
                            }
                            context.startService(stopIntent)

                            walkState = stoppedState
                            onWalkStateChange(stoppedState)

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
        if (showPolicyDialog) {
            PolicyAcceptanceDialog(
                onAccept = {
                    sharedPrefs.edit().putBoolean("policy_accepted", true).apply()
                    showPolicyDialog = false
                    Toast.makeText(
                        context,
                        "Thank you for accepting our policies",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onDecline = {
                    sharedPrefs.edit().putBoolean("policy_accepted", false).apply()
                    showPolicyDialog = false
                    // Sign out and exit
                    firebaseService.signOut()
                    (context as? ComponentActivity)?.finish()
                    Toast.makeText(
                        context,
                        "You must accept the Privacy Policy to use WalkOver",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }
}

// Keep all other composables exactly as they were (CompactStatusIndicator, DetailedStatsSheet, etc.)
// ... [Rest of the composables remain unchanged]

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
fun PolicyAcceptanceDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { /* Prevent dismissal */ },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PrivacyTip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = "Privacy Policy & Terms",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Before you continue, please review and accept our Privacy Policy and Terms of Service.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(context, PolicyViewerActivity::class.java).apply {
                                putExtra("policy_type", "privacy")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PrivacyTip,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Privacy", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(context, PolicyViewerActivity::class.java).apply {
                                putExtra("policy_type", "terms")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Terms", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("I Accept", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDecline,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Decline",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    )
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