package com.sidhart.walkover.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sidhart.walkover.data.LocationPoint
import com.sidhart.walkover.service.LocationService
import com.sidhart.walkover.service.FirebaseService
import com.sidhart.walkover.utils.MapUtils
import com.sidhart.walkover.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.isSystemInDarkTheme


enum class MapStyle(val displayName: String, val tileSource: org.osmdroid.tileprovider.tilesource.ITileSource, val isDark: Boolean = false) {
    CARTODB_POSITRON("Light", XYTileSource(
        "CARTODB_POSITRON",
        0, 19, 256, ".png",
        arrayOf("https://a.basemaps.cartocdn.com/light_all/",
            "https://b.basemaps.cartocdn.com/light_all/",
            "https://c.basemaps.cartocdn.com/light_all/",
            "https://d.basemaps.cartocdn.com/light_all/"),
        "© CARTO, © OpenStreetMap contributors"
    ), false),

    CARTODB_DARK_MATTER("Dark", XYTileSource(
        "CARTODB_DARK_MATTER",
        0, 19, 256, ".png",
        arrayOf("https://a.basemaps.cartocdn.com/dark_all/",
            "https://b.basemaps.cartocdn.com/dark_all/",
            "https://c.basemaps.cartocdn.com/dark_all/",
            "https://d.basemaps.cartocdn.com/dark_all/"),
        "© CARTO, © OpenStreetMap contributors"
    ), true),

    CARTO_VOYAGER("Voyager", XYTileSource(
        "CARTO_VOYAGER",
        0, 19, 256, ".png",
        arrayOf("https://a.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://d.basemaps.cartocdn.com/rastertiles/voyager/"),
        "© CARTO, © OpenStreetMap contributors"
    ), false)
}

class MapActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationService: LocationService
    private lateinit var firebaseService: FirebaseService
    private var mapView: MapView? = null
    private var currentMapStyle = MapStyle.CARTODB_DARK_MATTER


    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                checkLocationServicesAndSetupMap()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                checkLocationServicesAndSetupMap()
            }
            else -> {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_LONG).show()
                setupMap()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set initial map style based on system theme
        val isDarkMode = MapUtils.isDarkMode(this)
        currentMapStyle = if (isDarkMode) {
            MapStyle.CARTODB_DARK_MATTER
        } else {
            MapStyle.CARTODB_POSITRON
        }

        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = "WalkOver/1.0"
        Configuration.getInstance().cacheMapTileCount = 2000
        Configuration.getInstance().cacheMapTileOvershoot = 200
        Configuration.getInstance().osmdroidBasePath = getExternalFilesDir(null)
        Configuration.getInstance().osmdroidTileCache = getExternalFilesDir(null)
        Configuration.getInstance().tileDownloadThreads = 8
        Configuration.getInstance().tileFileSystemThreads = 4

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationService = LocationService(this)
        firebaseService = FirebaseService()

        setContent {
            WalkOverTheme {
                MapScreen(
                    onLocationPermissionRequest = {
                        locationPermissionRequest.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    },
                    onMapReady = { mapView ->
                        this.mapView = mapView
                        setupMap()
                    },
                    onMapStyleChange = { newStyle ->
                        changeMapStyle(newStyle)
                    },
                    currentMapStyle = currentMapStyle,
                    locationService = locationService,
                    firebaseService = firebaseService
                )
            }
        }
    }


    private fun checkLocationServicesAndSetupMap() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isLocationEnabled) {
            Toast.makeText(this, "Please enable location services", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
        setupMap()
    }

    private fun changeMapStyle(newStyle: MapStyle) {
        currentMapStyle = newStyle
        mapView?.let { map ->
            try {
                val currentCenter = map.mapCenter
                val currentZoom = map.zoomLevelDouble
                map.tileProvider.clearTileCache()
                map.setTileSource(newStyle.tileSource)
                map.controller.setCenter(currentCenter)
                map.controller.setZoom(currentZoom)
                map.invalidate()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to load map style", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupMap() {
        mapView?.let { map ->
            try {
                map.setTileSource(currentMapStyle.tileSource)
                map.setMultiTouchControls(true)
                map.setBuiltInZoomControls(false)
                map.setClickable(true)
                map.controller.setZoom(15.0)

                map.minZoomLevel = 3.0
                map.maxZoomLevel = 18.0

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            val currentLocation = GeoPoint(location.latitude, location.longitude)
                            map.controller.setCenter(currentLocation)
                            map.overlays.removeAll { overlay ->
                                overlay is Marker && overlay.title == "Current Location"
                            }
                            MapUtils.addMarker(
                                mapView = map,
                                geoPoint = currentLocation,
                                title = "Current Location",
                                snippet = "Lat: ${String.format("%.6f", location.latitude)}, Lng: ${String.format("%.6f", location.longitude)}",
                                isLocationMarker = true
                            )
                        } else {
                            val defaultCenter = GeoPoint(24.8607, 67.0011)
                            map.controller.setCenter(defaultCenter)
                        }
                    }
                } else {
                    val defaultCenter = GeoPoint(24.8607, 67.0011)
                    map.controller.setCenter(defaultCenter)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error setting up map", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onLocationPermissionRequest: () -> Unit,
    onMapReady: (MapView) -> Unit,
    onMapStyleChange: (MapStyle) -> Unit,
    currentMapStyle: MapStyle,
    locationService: LocationService,
    firebaseService: FirebaseService
) {
    val context = LocalContext.current
    val mapViewModel: MapViewModel = viewModel()
    val scope = rememberCoroutineScope()

    var isTracking by remember { mutableStateOf(false) }
    var locationPoints by remember { mutableStateOf<List<LocationPoint>>(emptyList()) }
    var distance by remember { mutableStateOf(0.0) }
    var area by remember { mutableStateOf(0.0) }
    var savedWalks by remember { mutableStateOf<List<com.sidhart.walkover.data.Walk>>(emptyList()) }
    var showStats by remember { mutableStateOf(false) }

    // THEME-AWARE MAP STYLE
    val isDarkTheme = isSystemInDarkTheme()
    var currentMapStyleState by remember {
        mutableStateOf(
            if (isDarkTheme) MapStyle.CARTODB_DARK_MATTER
            else MapStyle.CARTODB_POSITRON
        )
    }

    // Auto-switch map style when theme changes
    LaunchedEffect(isDarkTheme) {
        val newStyle = if (isDarkTheme) {
            MapStyle.CARTODB_DARK_MATTER
        } else {
            MapStyle.CARTODB_POSITRON
        }
        currentMapStyleState = newStyle
        onMapStyleChange(newStyle)
    }

    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(Unit) {
        val result = firebaseService.getWalks()
        result.fold(
            onSuccess = { walks -> savedWalks = walks },
            onFailure = { error ->
                Toast.makeText(context, "Failed to load walks", Toast.LENGTH_SHORT).show()
            }
        )
    }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            onLocationPermissionRequest()
        }
    }

    LaunchedEffect(isTracking) {
        if (isTracking) {
            try {
                locationService.getLocationUpdates()
                    .onEach { location ->
                        locationPoints = locationPoints + location
                        distance = calculateTotalDistance(locationPoints)
                        area = LocationService.calculateArea(locationPoints)
                    }
                    .catch { error ->
                        Toast.makeText(context, "Tracking error", Toast.LENGTH_SHORT).show()
                    }
                    .launchIn(this)
            } catch (e: Exception) {
                Toast.makeText(context, "Service error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map View
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    try {
                        setTileSource(currentMapStyle.tileSource)
                        setMultiTouchControls(true)
                        setBuiltInZoomControls(false)
                        setClickable(true)
                        controller.setZoom(15.0)
                        onMapReady(this)
                        mapViewModel.setMapView(this)
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Map init error", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { mapView ->
                // Update map style when theme changes
                try {
                    val currentTileSource = mapView.tileProvider.tileSource
                    val newTileSource = currentMapStyleState.tileSource

                    if (currentTileSource.name() != newTileSource.name()) {
                        val currentCenter = mapView.mapCenter
                        val currentZoom = mapView.zoomLevelDouble

                        mapView.tileProvider.clearTileCache()
                        mapView.setTileSource(newTileSource)
                        mapView.controller.setCenter(currentCenter)
                        mapView.controller.setZoom(currentZoom)
                        mapView.invalidate()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MapView", "Error updating map style", e)
                }

                if (isTracking && locationPoints.size > 1) {
                    mapViewModel.drawCurrentWalk(locationPoints)
                }
            }
        )

        // Top Control Bar - THEME-AWARE COLORS
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WalkOver",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Map Style Toggle - Shows current theme
                    IconButton(
                        onClick = {
                            val newStyle = if (currentMapStyleState.isDark) {
                                MapStyle.CARTODB_POSITRON
                            } else {
                                MapStyle.CARTODB_DARK_MATTER
                            }
                            currentMapStyleState = newStyle
                            onMapStyleChange(newStyle)
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (currentMapStyleState.isDark)
                                Icons.Outlined.WbSunny else Icons.Outlined.Brightness2,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Stats Toggle
                    IconButton(
                        onClick = { showStats = !showStats },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (showStats) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BarChart,
                            contentDescription = "Stats",
                            tint = if (showStats) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Tracking Stats - THEME-AWARE COLORS
        AnimatedVisibility(
            visible = isTracking,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 90.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        label = "Distance",
                        value = "${String.format("%.2f", distance / 1000)} km",
                        icon = Icons.Outlined.DirectionsWalk
                    )
                    Divider(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp),
                        color = MaterialTheme.colorScheme.outline
                    )
                    StatItem(
                        label = "Points",
                        value = "${locationPoints.size}",
                        icon = Icons.Outlined.LocationOn
                    )
                }
            }
        }

        // Floating Action Buttons - THEME-AWARE COLORS
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // My Location
            FloatingActionButton(
                onClick = { mapViewModel.centerOnCurrentLocation() },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.MyLocation,
                    contentDescription = "My Location"
                )
            }

            // Clear Map
            FloatingActionButton(
                onClick = { mapViewModel.clearMap() },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Clear,
                    contentDescription = "Clear"
                )
            }

            // Show Walks
            FloatingActionButton(
                onClick = {
                    savedWalks.forEach { walk ->
                        val points = walk.polylineCoordinates.map { geoPoint ->
                            LocationPoint(geoPoint.latitude, geoPoint.longitude, System.currentTimeMillis())
                        }
                        mapViewModel.drawRoute(points)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Route,
                    contentDescription = "Walks"
                )
            }
        }

        // Bottom Control Panel - THEME-AWARE COLORS
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            tonalElevation = 12.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stats Panel
                AnimatedVisibility(visible = showStats) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Walk Statistics",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DetailedStatItem(
                                label = "Distance",
                                value = "${String.format("%.2f", distance / 1000)} km"
                            )
                            DetailedStatItem(
                                label = "Area",
                                value = "${String.format("%.2f", area)} m²"
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                    }
                }

                // Main Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!isTracking) {
                        Button(
                            onClick = {
                                isTracking = true
                                locationPoints = emptyList()
                                distance = 0.0
                                area = 0.0
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start Walk",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                isTracking = false
                                if (locationPoints.size >= 2) {
                                    saveWalk(locationPoints, distance, area, firebaseService, context)
                                } else {
                                    Toast.makeText(context, "Walk too short", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Stop & Save",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }

        // Tracking Indicator - THEME-AWARE
        AnimatedVisibility(
            visible = isTracking,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 90.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
                                MaterialTheme.colorScheme.onError.copy(alpha = alpha),
                                CircleShape
                            )
                    )

                    Text(
                        text = "TRACKING",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onError
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}


@Composable
fun DetailedStatItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

class MapViewModel : androidx.lifecycle.ViewModel() {
    private var mapView: MapView? = null

    fun setMapView(mapView: MapView) {
        this.mapView = mapView
    }

    fun centerOnCurrentLocation() {
        mapView?.let { map ->
            MapUtils.getCurrentLocation(
                context = map.context,
                onSuccess = { location ->
                    val geoPoint = MapUtils.convertLocationToGeoPoint(location)
                    MapUtils.centerMapOnLocation(map, geoPoint)
                    map.overlays.removeAll { overlay ->
                        overlay is Marker && overlay.title == "Current Location"
                    }
                    MapUtils.addMarker(
                        mapView = map,
                        geoPoint = geoPoint,
                        title = "Current Location",
                        snippet = "Lat: ${String.format("%.6f", location.latitude)}, Lng: ${String.format("%.6f", location.longitude)}",
                        isLocationMarker = true
                    )
                    Toast.makeText(map.context, "Centered on location", Toast.LENGTH_SHORT).show()
                },
                onFailure = { error ->
                    Toast.makeText(map.context, "Failed to get location", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    fun clearMap() {
        mapView?.let { map ->
            MapUtils.clearMap(map)
            Toast.makeText(map.context, "Map cleared", Toast.LENGTH_SHORT).show()
        }
    }

    fun drawRoute(points: List<LocationPoint>) {
        mapView?.let { map ->
            val geoPoints = points.map { MapUtils.convertLocationPointToGeoPoint(it) }
            // Use theme-aware color from MapUtils
            MapUtils.drawPolyline(
                map,
                geoPoints,
                MapUtils.getSavedWalkPolylineColor(map.context),
                6f
            )
        }
    }

    fun drawCurrentWalk(points: List<LocationPoint>) {
        mapView?.let { map ->
            if (points.isNotEmpty()) {
                map.overlays.removeAll { overlay ->
                    overlay is Polyline && overlay.title == "Current Walk"
                }
                val geoPoints = points.map { MapUtils.convertLocationPointToGeoPoint(it) }
                // Use theme-aware color from MapUtils
                val polyline = MapUtils.drawPolyline(
                    map,
                    geoPoints,
                    MapUtils.getTrackingPolylineColor(map.context),
                    8f
                )
                polyline.title = "Current Walk"
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
    context: android.content.Context
) {
    val walk = com.sidhart.walkover.data.Walk(
        polylineCoordinates = points.map {
            com.google.firebase.firestore.GeoPoint(it.latitude, it.longitude)
        },
        distanceCovered = distance,
    )

    (context as ComponentActivity).lifecycleScope.launch {
        val result = firebaseService.saveWalk(walk)
        result.fold(
            onSuccess = {
                Toast.makeText(context, "Walk saved successfully!", Toast.LENGTH_SHORT).show()
            },
            onFailure = { error ->
                Toast.makeText(context, "Failed to save walk", Toast.LENGTH_SHORT).show()
            }
        )
    }
}