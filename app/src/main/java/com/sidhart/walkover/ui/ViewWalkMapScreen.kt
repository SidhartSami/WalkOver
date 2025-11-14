package com.sidhart.walkover.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.sidhart.walkover.data.Walk
import com.sidhart.walkover.service.FirebaseService
import com.sidhart.walkover.utils.MapUtils
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import com.sidhart.walkover.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewWalkMapScreen(
    walkId: String,
    firebaseService: FirebaseService,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var walk by remember { mutableStateOf<Walk?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var routeDrawn by remember { mutableStateOf(false) }
    
    val decimalFormat = remember { DecimalFormat("#,##0.##") }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()) }
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    
    // Load walk data
    LaunchedEffect(walkId) {
        routeDrawn = false // Reset when walkId changes
        scope.launch {
            firebaseService.getWalks().fold(
                onSuccess = { walks ->
                    walk = walks.find { it.id == walkId }
                    isLoading = false
                    if (walk == null) {
                        Toast.makeText(context, "Walk not found", Toast.LENGTH_SHORT).show()
                    }
                },
                onFailure = { error ->
                    isLoading = false
                    Toast.makeText(context, "Failed to load walk: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
    
    // Configure osmdroid
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE)
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Walk Route",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (walk == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Walk not found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                // Map View
                AndroidView(
                    factory = { ctx ->
                        val map = MapView(ctx).apply {
                            setTileSource(
                                if (isDarkTheme) MapStyle.CARTODB_DARK_MATTER.tileSource
                                else MapStyle.CARTODB_POSITRON.tileSource
                            )
                            setMultiTouchControls(true)
                            controller.setZoom(15.0)
                        }
                        mapView = map
                        
                        // Handle lifecycle
                        lifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
                            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                                when (event) {
                                    Lifecycle.Event.ON_RESUME -> map.onResume()
                                    Lifecycle.Event.ON_PAUSE -> map.onPause()
                                    else -> {}
                                }
                            }
                        })
                        
                        map
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { map ->
                        // Update tile source if theme changed
                        map.setTileSource(
                            if (isDarkTheme) MapStyle.CARTODB_DARK_MATTER.tileSource
                            else MapStyle.CARTODB_POSITRON.tileSource
                        )
                    }
                )
                
                // Draw route when walk data is loaded
                LaunchedEffect(walk) {
                    walk?.let { walkData ->
                        mapView?.let { map ->
                            if (walkData.polylineCoordinates.isNotEmpty() && !routeDrawn) {
                                // Clear existing overlays
                                map.overlays.clear()
                                
                                // Convert Firebase GeoPoint to osmdroid GeoPoint
                                val geoPoints = walkData.polylineCoordinates.map { firebaseGeoPoint ->
                                    GeoPoint(firebaseGeoPoint.latitude, firebaseGeoPoint.longitude)
                                }
                                
                                // Draw polyline
                                val polyline = org.osmdroid.views.overlay.Polyline()
                                polyline.setPoints(geoPoints)
                                polyline.color = MapUtils.getSavedWalkPolylineColor(map.context)
                                polyline.width = 10f
                                polyline.title = "Walk Route"
                                map.overlays.add(polyline)

                                // Replace the marker creation section in your LaunchedEffect(walk) block with this:

// Add start marker with custom drawable
                                if (geoPoints.isNotEmpty()) {
                                    val startPoint = geoPoints.first()
                                    val startMarker = Marker(map)
                                    startMarker.position = startPoint
                                    startMarker.title = "Start"
                                    startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                                    // Use custom drawable for start marker
                                    val startIcon = androidx.core.content.ContextCompat.getDrawable(
                                        map.context,
                                        R.drawable.location_pin_active
                                    )
                                    startMarker.icon = startIcon

                                    map.overlays.add(startMarker)

                                    // Add end marker with same custom drawable
                                    if (geoPoints.size > 1) {
                                        val endPoint = geoPoints.last()
                                        val endMarker = Marker(map)
                                        endMarker.position = endPoint
                                        endMarker.title = "End"
                                        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                                        // Use same drawable for end marker
                                        val endIcon = androidx.core.content.ContextCompat.getDrawable(
                                            map.context,
                                            R.drawable.location_pin_active
                                        )
                                        endMarker.icon = endIcon

                                        map.overlays.add(endMarker)
                                    }

                                    // Fit map to show entire route
                                    MapUtils.fitMapToPoints(map, geoPoints, padding = 100)
                                }
                                map.invalidate()
                                routeDrawn = true
                            }
                        }
                    }
                }
                
                // Walk Info Card (overlay at bottom)
                walk?.let { walkData ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            // Date and Time
                            Text(
                                text = dateFormat.format(walkData.timestamp),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Stats Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                WalkStatChip(
                                    icon = Icons.Outlined.Route,
                                    label = "Distance",
                                    value = "${decimalFormat.format(walkData.distanceCovered / 1000)} km",
                                    color = Color(0xFF2196F3)
                                )
                                WalkStatChip(
                                    icon = Icons.Outlined.Timer,
                                    label = "Duration",
                                    value = formatDuration(walkData.duration),
                                    color = Color(0xFF4CAF50)
                                )
                                WalkStatChip(
                                    icon = Icons.Outlined.LocationOn,
                                    label = "Points",
                                    value = walkData.polylineCoordinates.size.toString(),
                                    color = Color(0xFFFF9800)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WalkStatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


