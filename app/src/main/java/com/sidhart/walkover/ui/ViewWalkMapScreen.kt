package com.sidhart.walkover.ui

import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sidhart.walkover.data.Walk
import com.sidhart.walkover.service.FirebaseService
import com.sidhart.walkover.ui.theme.NeonGreen
import com.sidhart.walkover.utils.MapUtils
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.toColorInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewWalkMapScreen(
    walkId: String,
    firebaseService: FirebaseService,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    var walk by remember { mutableStateOf<Walk?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var routeDrawn by remember { mutableStateOf(false) }
    
    var isCapturingImage by remember { mutableStateOf(false) }
    var showStatsOverlay by remember { mutableStateOf(true) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    
    val decimalFormat = remember { DecimalFormat("#,##0.##") }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    
    // Gallery launcher for picking image
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                isCapturingImage = true
                
                try {
                    val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                        android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                            decoder.isMutableRequired = true
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                    
                    var errorMessage = "Unknown issue"
                    // Generate Strava-style overlay on user's photo
                    val storyBitmap = com.sidhart.walkover.utils.StoryTemplateGenerator
                        .generateStravaStyleStory(
                            context = context,
                            userPhotoBitmap = bitmap,
                            walk = walk!!,
                            onError = { errorMessage = it }
                        )
                    
                    if (storyBitmap != null) {
                        // Use Android's native share sheet
                        shareWithNativeSheet(context, storyBitmap)
                    } else {
                        Toast.makeText(context, "Story Error: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to load image: ${e.message}", Toast.LENGTH_LONG).show()
                }
                
                isCapturingImage = false
            }
        }
    }
    
    // Camera launcher for taking photo
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            scope.launch {
                isCapturingImage = true
                
                var errorMessage = "Unknown issue"
                // Generate Strava-style overlay on user's photo
                val storyBitmap = com.sidhart.walkover.utils.StoryTemplateGenerator
                    .generateStravaStyleStory(
                        context = context,
                        userPhotoBitmap = bitmap,
                        walk = walk!!,
                        onError = { errorMessage = it }
                    )
                
                if (storyBitmap != null) {
                    // Use Android's native share sheet
                    shareWithNativeSheet(context, storyBitmap)
                } else {
                    Toast.makeText(context, "Story Error: $errorMessage", Toast.LENGTH_LONG).show()
                }
                
                isCapturingImage = false
            }
        }
    }
    
    // Load walk data
    LaunchedEffect(walkId) {
        routeDrawn = false
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
                    Toast.makeText(context, "Failed to load walk", Toast.LENGTH_SHORT).show()
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
                    Column {
                        Text(
                            text = "Walk Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        walk?.let {
                            Text(
                                text = dateFormat.format(it.timestamp),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Toggle Stats Overlay
                    IconButton(
                        onClick = { showStatsOverlay = !showStatsOverlay }
                    ) {
                        Icon(
                            imageVector = if (showStatsOverlay) Icons.Default.Visibility 
                                         else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Stats",
                            tint = NeonGreen
                        )
                    }
                    
                    // Share Button - Choose Camera or Gallery
                    IconButton(
                        onClick = { 
                            if (walk != null) {
                                showImageSourceDialog = true
                            }
                        },
                        enabled = walk != null
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Walk",
                            tint = NeonGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Start Map (empty)
                    AndroidView(
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setTileSource(
                                    if (isDarkTheme) MapStyle.MAPBOX_DARK.tileSource
                                    else MapStyle.MAPBOX_STREETS.tileSource
                                )
                                controller.setZoom(15.0)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Show Skeleton at bottom
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp)
                    ) {
                        CompactStatsCardSkeleton()
                    }
                }
            } else if (walk == null) {
                EmptyWalkState()
            } else {
                // Map View
                AndroidView(
                    factory = { ctx ->
                        val map = MapView(ctx).apply {
                            setTileSource(
                                if (isDarkTheme) MapStyle.MAPBOX_DARK.tileSource
                                else MapStyle.MAPBOX_STREETS.tileSource
                            )
                            setMultiTouchControls(true)
                            controller.setZoom(15.0)
                        }
                        mapView = map
                        
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
                        map.setTileSource(
                            if (isDarkTheme) MapStyle.MAPBOX_DARK.tileSource
                            else MapStyle.MAPBOX_STREETS.tileSource
                        )
                    }
                )
                
                // Draw route with NEON GREEN
                LaunchedEffect(walk) {
                    walk?.let { walkData ->
                        mapView?.let { map ->
                            if (walkData.polylineCoordinates.isNotEmpty() && !routeDrawn) {
                                map.overlays.clear()
                                
                                val geoPoints = walkData.polylineCoordinates.map { firebaseGeoPoint ->
                                    GeoPoint(firebaseGeoPoint.latitude, firebaseGeoPoint.longitude)
                                }
                                
                                // Draw NEON GREEN polyline
                                val polyline = org.osmdroid.views.overlay.Polyline()
                                polyline.setPoints(geoPoints)
                                polyline.outlinePaint.color = "#C0F11C".toColorInt()
                                polyline.outlinePaint.strokeWidth = 14f
                                polyline.outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                                polyline.outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                                polyline.outlinePaint.isAntiAlias = true
                                polyline.outlinePaint.pathEffect = android.graphics.CornerPathEffect(50f)
                                map.overlays.add(polyline)

                                // Start marker
                                if (geoPoints.isNotEmpty()) {
                                    val startMarker = Marker(map)
                                    startMarker.position = geoPoints.first()
                                    startMarker.title = "Start"
                                    startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                    
                                    val outlineCol = if (isDarkTheme) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                                    val startIcon = MapUtils.createDotMarkerDrawable(
                                        map.context, "#EF4444".toColorInt(), outlineCol // Red
                                    )
                                    startMarker.icon = startIcon
                                    map.overlays.add(startMarker)

                                    // End marker
                                    if (geoPoints.size > 1) {
                                        val endMarker = Marker(map)
                                        endMarker.position = geoPoints.last()
                                        endMarker.title = "End"
                                        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                        val endIcon = MapUtils.createDotMarkerDrawable(
                                            map.context, "#22C55E".toColorInt(), outlineCol // Green
                                        )
                                        endMarker.icon = endIcon
                                        map.overlays.add(endMarker)
                                    }

                                    MapUtils.fitMapToPoints(map, geoPoints, padding = 150)
                                }
                                map.invalidate()
                                routeDrawn = true
                            }
                        }
                    }
                }
                
                // Compact Stats Overlay
                walk?.let { walkData ->
                    AnimatedVisibility(
                        visible = showStatsOverlay,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .zIndex(10f)
                    ) {
                        CompactStatsCard(
                            walk = walkData,
                            decimalFormat = decimalFormat,
                            timeFormat = timeFormat
                        )
                    }
                }
            }
        }
        
        // Processing indicator
        if (isCapturingImage) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .zIndex(100f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    CircularProgressIndicator(color = NeonGreen, strokeWidth = 4.dp)
                    Text(
                        "Creating your story...",
                        color = NeonGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "This will look amazing!",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        // Image Source Selection Dialog
        if (showImageSourceDialog) {
            AlertDialog(
                onDismissRequest = { showImageSourceDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            "Choose Image Source",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Select how you want to add a photo to your walk story:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Camera Option
                        Surface(
                            onClick = {
                                showImageSourceDialog = false
                                cameraLauncher.launch(null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        "Take Photo",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.Black
                                    )
                                    Text(
                                        "Use camera to capture a moment",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                        
                        // Gallery Option
                        Surface(
                            onClick = {
                                showImageSourceDialog = false
                                galleryLauncher.launch("image/*")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        "Choose from Gallery",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.Black
                                    )
                                    Text(
                                        "Select an existing photo",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showImageSourceDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun CompactStatsCard(
    walk: Walk,
    decimalFormat: DecimalFormat,
    timeFormat: SimpleDateFormat,
    modifier: Modifier = Modifier
) {
    val distanceKm = walk.distanceCovered / 1000.0
    val durationMinutes = walk.duration / 60000.0

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Neon Green accent bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                NeonGreen,
                                NeonGreen.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Compact Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Distance - Prominent
                Column(
                    modifier = Modifier.weight(1.3f)
                ) {
                    Text(
                        text = "DISTANCE",
                        fontSize = 9.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = decimalFormat.format(distanceKm),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonGreen
                        )
                        Text(
                            text = "km",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
                
                // Time
                CompactStatBox(
                    icon = Icons.Outlined.Timer,
                    label = "TIME",
                    value = formatDurationLocal(walk.duration),
                    modifier = Modifier.weight(1f)
                )
                
                // Speed
                val speedKmH = if (durationMinutes > 0) distanceKm / (durationMinutes / 60.0) else 0.0
                CompactStatBox(
                    icon = Icons.Outlined.Speed,
                    label = "SPEED",
                    value = String.format(Locale.US, "%.1f", speedKmH),                    unit = "km/h",
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Additional Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = NeonGreen
                    )
                    Text(
                        text = timeFormat.format(walk.timestamp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(NeonGreen)
                    )
                    Text(
                        text = "${walk.polylineCoordinates.size} pts",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun CompactStatBox(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    unit: String = ""
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = NeonGreen,
            modifier = Modifier.size(16.dp)
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (unit.isNotEmpty()) {
                    Text(
                        text = unit,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
            
            Text(
                text = label,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun EmptyWalkState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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
        Text(
            text = "This walk may have been deleted",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// Helper functions
fun formatDurationLocal(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = millis / (1000 * 60 * 60)

    return when {
        hours > 0 -> String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

// Native Android Share Sheet
fun shareWithNativeSheet(context: android.content.Context, bitmap: Bitmap) {
    try {
        val file = saveBitmapToFile(context, bitmap, "walkover_story.jpg")
        
        if (file != null) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Check out my walk! 🚶‍♂️💪 #WalkOver")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // Use Android's native share sheet
            context.startActivity(Intent.createChooser(intent, "Share your walk"))
        } else {
            Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun saveBitmapToFile(context: android.content.Context, bitmap: Bitmap, fileName: String): File? {
    return try {
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        file
    } catch (_: Exception) {
        null
    }
}