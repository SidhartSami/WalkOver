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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import androidx.lifecycle.lifecycleScope

enum class MapStyle(val displayName: String, val tileSource: org.osmdroid.tileprovider.tilesource.ITileSource, val isDark: Boolean = false) {
    // Light themes
    CARTODB_POSITRON("CartoDB Positron", XYTileSource(
        "CARTODB_POSITRON",
        0, 19, 256, ".png",
        arrayOf("https://a.basemaps.cartocdn.com/light_all/",
            "https://b.basemaps.cartocdn.com/light_all/",
            "https://c.basemaps.cartocdn.com/light_all/",
            "https://d.basemaps.cartocdn.com/light_all/"),
        "© CARTO, © OpenStreetMap contributors"
    ), false),

    CARTO_LIGHT("Carto Light NoLabels", XYTileSource(
        "CARTO_LIGHT",
        0, 19, 256, ".png",
        arrayOf("https://a.basemaps.cartocdn.com/light_nolabels/",
            "https://b.basemaps.cartocdn.com/light_nolabels/",
            "https://c.basemaps.cartocdn.com/light_nolabels/",
            "https://d.basemaps.cartocdn.com/light_nolabels/"),
        "© CARTO, © OpenStreetMap contributors"
    ), false),

    CARTO_VOYAGER("Carto Voyager", XYTileSource(
        "CARTO_VOYAGER",
        0, 19, 256, ".png",
        arrayOf("https://a.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://d.basemaps.cartocdn.com/rastertiles/voyager/"),
        "© CARTO, © OpenStreetMap contributors"
    ), false),

    // Dark themes - Fixed CartoDB Dark (this is the proper dark equivalent of Positron)
    CARTODB_DARK_MATTER("CartoDB Dark Matter", XYTileSource(
        "CARTODB_DARK_MATTER",
        0, 19, 256, ".png",
        arrayOf("https://a.basemaps.cartocdn.com/dark_all/",
            "https://b.basemaps.cartocdn.com/dark_all/",
            "https://c.basemaps.cartocdn.com/dark_all/",
            "https://d.basemaps.cartocdn.com/dark_all/"),
        "© CARTO, © OpenStreetMap contributors"
    ), true),

    CARTO_DARK_NOLABELS("Carto Dark NoLabels", XYTileSource(
        "CARTO_DARK_NOLABELS",
        0, 19, 256, ".png",
        arrayOf("https://a.basemaps.cartocdn.com/dark_nolabels/",
            "https://b.basemaps.cartocdn.com/dark_nolabels/",
            "https://c.basemaps.cartocdn.com/dark_nolabels/",
            "https://d.basemaps.cartocdn.com/dark_nolabels/"),
        "© CARTO, © OpenStreetMap contributors"
    ), true)
}

class MapActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationService: LocationService
    private lateinit var firebaseService: FirebaseService
    private var mapView: MapView? = null
    private var currentMapStyle = MapStyle.CARTODB_POSITRON // Start with CartoDB Positron

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted
                checkLocationServicesAndSetupMap()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted
                checkLocationServicesAndSetupMap()
            }
            else -> {
                Toast.makeText(this, "Location permission is required for map functionality", Toast.LENGTH_LONG).show()
                setupMap() // Setup map without location
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize osmdroid configuration
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))

        // Set user agent for tile requests (required for OpenStreetMap)
        Configuration.getInstance().userAgentValue = "WalkOver/1.0"

        // Enable tile caching with more aggressive settings
        Configuration.getInstance().cacheMapTileCount = 2000
        Configuration.getInstance().cacheMapTileOvershoot = 200

        // Set cache directory
        Configuration.getInstance().osmdroidBasePath = getExternalFilesDir(null)
        Configuration.getInstance().osmdroidTileCache = getExternalFilesDir(null)

        // Set tile download thread count
        Configuration.getInstance().tileDownloadThreads = 8
        Configuration.getInstance().tileFileSystemThreads = 4

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationService = LocationService(this)
        firebaseService = FirebaseService()

        setContent {
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
                onToggleTheme = {
                    toggleTheme()
                },
                currentMapStyle = currentMapStyle,
                locationService = locationService,
                firebaseService = firebaseService
            )
        }
    }

    private fun checkLocationServicesAndSetupMap() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isLocationEnabled) {
            Toast.makeText(this, "Please enable location services", Toast.LENGTH_LONG).show()
            // Open location settings
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }

        setupMap()
    }

    fun changeMapStyle(newStyle: MapStyle) {
        android.util.Log.d("MapActivity", "Changing map style from ${currentMapStyle.displayName} to ${newStyle.displayName}")
        currentMapStyle = newStyle
        mapView?.let { map ->
            try {
                android.util.Log.d("MapActivity", "Setting tile source: ${newStyle.tileSource.name()}")

                // Store current map state
                val currentCenter = map.mapCenter
                val currentZoom = map.zoomLevelDouble

                // Clear tile cache for the new style to ensure fresh tiles
                map.tileProvider.clearTileCache()

                // Set new tile source
                map.setTileSource(newStyle.tileSource)

                // Restore map state and force refresh
                map.controller.setCenter(currentCenter)
                map.controller.setZoom(currentZoom)
                map.invalidate()

                // Force tile refresh by triggering a zoom change and back
                map.controller.zoomIn()
                map.controller.zoomOut()

                Toast.makeText(this, "Changed to ${newStyle.displayName}", Toast.LENGTH_SHORT).show()
                android.util.Log.d("MapActivity", "Successfully changed to ${newStyle.displayName}")
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to load ${newStyle.displayName}: ${e.message}", Toast.LENGTH_LONG).show()
                android.util.Log.e("MapActivity", "Failed to change tile source to ${newStyle.displayName}", e)
            }
        }
    }

    fun toggleTheme() {
        android.util.Log.d("MapActivity", "Toggling theme from ${currentMapStyle.displayName} (isDark: ${currentMapStyle.isDark})")
        val newStyle = when (currentMapStyle) {
            // Light to Dark - Fixed mapping
            MapStyle.CARTODB_POSITRON -> MapStyle.CARTODB_DARK_MATTER
            MapStyle.CARTO_LIGHT -> MapStyle.CARTO_DARK_NOLABELS
            MapStyle.CARTO_VOYAGER -> MapStyle.CARTODB_DARK_MATTER  // Voyager -> Dark Matter

            // Dark to Light - Fixed mapping
            MapStyle.CARTODB_DARK_MATTER -> MapStyle.CARTODB_POSITRON
            MapStyle.CARTO_DARK_NOLABELS -> MapStyle.CARTO_LIGHT

            // Fallback
            else -> if (currentMapStyle.isDark) MapStyle.CARTODB_POSITRON else MapStyle.CARTODB_DARK_MATTER
        }
        android.util.Log.d("MapActivity", "Selected new style: ${newStyle.displayName} (isDark: ${newStyle.isDark})")
        changeMapStyle(newStyle)
    }

    private fun setupMap() {
        mapView?.let { map ->
            try {
                // Set tile source based on current map style
                map.setTileSource(currentMapStyle.tileSource)

                // Enable multi-touch gestures
                map.setMultiTouchControls(true)
                map.setBuiltInZoomControls(true)
                map.setClickable(true)

                // Set initial zoom level
                map.controller.setZoom(15.0)

                // Get current location
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                    // Try to get current location
                    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            val currentLocation = GeoPoint(location.latitude, location.longitude)
                            map.controller.setCenter(currentLocation)

                            // Clear any existing location markers first
                            map.overlays.removeAll { overlay ->
                                overlay is Marker && overlay.title == "Current Location"
                            }

                            // Add professional location marker using MapUtils
                            MapUtils.addMarker(
                                mapView = map,
                                geoPoint = currentLocation,
                                title = "Current Location",
                                snippet = "Lat: ${String.format("%.6f", location.latitude)}, Lng: ${String.format("%.6f", location.longitude)}",
                                isLocationMarker = true
                            )

                            Toast.makeText(this, "Location found: ${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}", Toast.LENGTH_SHORT).show()
                        } else {
                            // No location available, use default center (Karachi since you're in Pakistan)
                            val defaultCenter = GeoPoint(24.8607, 67.0011) // Karachi coordinates
                            map.controller.setCenter(defaultCenter)
                            Toast.makeText(this, "No location available. Showing Karachi area.", Toast.LENGTH_LONG).show()
                        }
                    }.addOnFailureListener { exception ->
                        // Location request failed, use default center
                        val defaultCenter = GeoPoint(24.8607, 67.0011) // Karachi coordinates
                        map.controller.setCenter(defaultCenter)
                        Toast.makeText(this, "Failed to get location: ${exception.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    // No permission, use default center (Karachi)
                    val defaultCenter = GeoPoint(24.8607, 67.0011)
                    map.controller.setCenter(defaultCenter)
                }
            } catch (e: Exception) {
                android.util.Log.e("MapActivity", "Error setting up map", e)
                Toast.makeText(this, "Error setting up map: ${e.message}", Toast.LENGTH_LONG).show()
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
    onToggleTheme: () -> Unit,
    currentMapStyle: MapStyle,
    locationService: LocationService,
    firebaseService: FirebaseService
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapViewModel: MapViewModel = viewModel()

    var isTracking by remember { mutableStateOf(false) }
    var locationPoints by remember { mutableStateOf<List<LocationPoint>>(emptyList()) }
    var distance by remember { mutableStateOf(0.0) }
    var area by remember { mutableStateOf(0.0) }
    var savedWalks by remember { mutableStateOf<List<com.sidhart.walkover.data.Walk>>(emptyList()) }

    // Check location permissions
    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(Unit) {
        // Load saved walks
        val result = firebaseService.getWalks()
        result.fold(
            onSuccess = { walks ->
                savedWalks = walks
            },
            onFailure = { error ->
                Toast.makeText(context, "Failed to load walks: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Check permissions and request if needed
    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            onLocationPermissionRequest()
        }
    }

    // Location tracking effect
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
        // Top App Bar
        TopAppBar(
            title = { Text("Walk Over Map") },
            actions = {
                // Map Style Selector with Dark/Light toggle
                var expanded by remember { mutableStateOf(false) }

                Row {
                    // Toggle between Light/Dark maps
                    Button(
                        onClick = onToggleTheme,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentMapStyle.isDark) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(
                            text = if (currentMapStyle.isDark) "🌙" else "☀️",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Map style selector
                    Box {
                        Button(
                            onClick = { expanded = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text(
                                text = currentMapStyle.displayName,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            MapStyle.values().filter { it.isDark == currentMapStyle.isDark }.forEach { style ->
                                DropdownMenuItem(
                                    text = { Text(style.displayName) },
                                    onClick = {
                                        onMapStyleChange(style)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        // Map View
        AndroidView(
            factory = { context ->
                MapView(context).apply {
                    try {
                        // Set tile source immediately based on current style
                        setTileSource(currentMapStyle.tileSource)

                        // Enable multi-touch gestures
                        setMultiTouchControls(true)
                        setBuiltInZoomControls(true)
                        setClickable(true)

                        // Set initial zoom (center will be set by setupMap)
                        controller.setZoom(15.0)

                        onMapReady(this)
                        mapViewModel.setMapView(this)
                    } catch (e: Exception) {
                        android.util.Log.e("MapView", "Error initializing map", e)
                        Toast.makeText(context, "Error initializing map: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { mapView ->
                // Draw current walk route in real-time
                if (isTracking && locationPoints.size > 1) {
                    mapViewModel.drawCurrentWalk(locationPoints)
                }
            }
        )

        // Stats Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Walk Statistics",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text("Distance: ${String.format("%.2f", distance)} m")
                Text("Area: ${String.format("%.2f", area)} m²")
                Text("Points: ${locationPoints.size}")

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            isTracking = true
                            locationPoints = emptyList()
                        },
                        enabled = !isTracking
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

                Spacer(modifier = Modifier.height(8.dp))

                // Control buttons
                Text(
                    text = "Map Controls",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            // Center map on current location
                            mapViewModel.centerOnCurrentLocation()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("My Location")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            // Clear map overlays
                            mapViewModel.clearMap()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear Map")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            // Show saved walks
                            showSavedWalks(savedWalks, mapViewModel)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Show Walks")
                    }
                }
            }
        }
    }
}

class MapViewModel : androidx.lifecycle.ViewModel() {
    private var mapView: MapView? = null

    fun setMapView(mapView: MapView) {
        this.mapView = mapView
    }

    fun centerOnCurrentLocation() {
        mapView?.let { map ->
            // Get current location and center map
            MapUtils.getCurrentLocation(
                context = map.context,
                onSuccess = { location ->
                    val geoPoint = MapUtils.convertLocationToGeoPoint(location)
                    MapUtils.centerMapOnLocation(map, geoPoint)

                    // Clear existing location markers first
                    map.overlays.removeAll { overlay ->
                        overlay is Marker && overlay.title == "Current Location"
                    }

                    // Add professional location marker using MapUtils
                    MapUtils.addMarker(
                        mapView = map,
                        geoPoint = geoPoint,
                        title = "Current Location",
                        snippet = "Lat: ${String.format("%.6f", location.latitude)}, Lng: ${String.format("%.6f", location.longitude)}",
                        isLocationMarker = true
                    )

                    Toast.makeText(map.context, "Centered on your location", Toast.LENGTH_SHORT).show()
                },
                onFailure = { error ->
                    android.util.Log.e("MapViewModel", "Failed to get current location", error)
                    Toast.makeText(map.context, "Failed to get current location: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    fun clearMap() {
        mapView?.let { map ->
            MapUtils.clearMap(map)
        }
    }

    fun addLocationPoint(locationPoint: LocationPoint) {
        mapView?.let { map ->
            MapUtils.addMarker(
                mapView = map,
                geoPoint = MapUtils.convertLocationPointToGeoPoint(locationPoint),
                title = "Walk Point"
            )
        }
    }

    fun drawRoute(points: List<LocationPoint>) {
        mapView?.let { map ->
            val geoPoints = points.map { point ->
                MapUtils.convertLocationPointToGeoPoint(point)
            }
            MapUtils.drawPolyline(map, geoPoints)
        }
    }

    fun drawCurrentWalk(points: List<LocationPoint>) {
        mapView?.let { map ->
            if (points.isNotEmpty()) {
                // Clear existing current walk
                map.overlays.removeAll { overlay ->
                    overlay is Polyline && overlay.title == "Current Walk"
                }

                val geoPoints = points.map { point ->
                    MapUtils.convertLocationPointToGeoPoint(point)
                }
                val polyline = MapUtils.drawPolyline(map, geoPoints, android.graphics.Color.RED, 8f)
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
        areaCaptured = area
    )

    // Use coroutine scope from context
    (context as ComponentActivity).lifecycleScope.launch {
        val result = firebaseService.saveWalk(walk)
        result.fold(
            onSuccess = {
                Toast.makeText(context, "Walk saved successfully!", Toast.LENGTH_SHORT).show()
            },
            onFailure = { error ->
                Toast.makeText(context, "Failed to save walk: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
    }
}

private fun showSavedWalks(
    walks: List<com.sidhart.walkover.data.Walk>,
    mapViewModel: MapViewModel
) {
    walks.forEach { walk ->
        val points = walk.polylineCoordinates.map { geoPoint ->
            LocationPoint(geoPoint.latitude, geoPoint.longitude, System.currentTimeMillis())
        }
        mapViewModel.drawRoute(points)
    }
}