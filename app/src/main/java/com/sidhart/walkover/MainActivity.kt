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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.sidhart.walkover.data.LocationPoint
import com.sidhart.walkover.data.Walk
import com.sidhart.walkover.service.FirebaseService
import com.sidhart.walkover.service.LocationService
import com.sidhart.walkover.ui.LeaderboardActivity
import com.sidhart.walkover.ui.theme.WalkOverTheme
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
                    Toast.makeText(this@MainActivity, "❌ No internet connection", Toast.LENGTH_LONG).show()
                    return@launch
                }
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
                if (networkCapabilities == null || !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    Toast.makeText(this@MainActivity, "❌ No internet connection", Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                // Check if Firebase is properly initialized
                if (!firebaseService.isUserAuthenticated()) {
                    Toast.makeText(this@MainActivity, "🔄 Signing in to Firebase...", Toast.LENGTH_SHORT).show()
                    firebaseService.signInAnonymously().fold(
                        onSuccess = { 
                            Toast.makeText(this@MainActivity, "✅ Signed in anonymously", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { error ->
                            Toast.makeText(this@MainActivity, "❌ Firebase sign-in failed: ${error.message}", Toast.LENGTH_LONG).show()
                            // Log the full error for debugging
                            android.util.Log.e("FirebaseError", "Sign-in failed", error)
                        }
                    )
                } else {
                    Toast.makeText(this@MainActivity, "✅ Already signed in", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "❌ App initialization error: ${e.message}", Toast.LENGTH_LONG).show()
                android.util.Log.e("AppError", "Initialization failed", e)
            }
        }
        
        setContent {
            WalkOverTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FitnessMapScreen(
                        modifier = Modifier.padding(innerPadding),
                        locationService = locationService,
                        firebaseService = firebaseService,
                        context = this@MainActivity
                    )
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessMapScreen(
    modifier: Modifier = Modifier,
    locationService: LocationService,
    firebaseService: FirebaseService,
    context: Context
) {
    var isTracking by remember { mutableStateOf(false) }
    var locationPoints by remember { mutableStateOf<List<LocationPoint>>(emptyList()) }
    var distance by remember { mutableStateOf(0.0) }
    var area by remember { mutableStateOf(0.0) }
    
    val decimalFormat = remember { DecimalFormat("#.##") }
    
    // Map functionality removed - will be added later
    
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
    
    // Map functionality removed - will be added later
    
    Box(modifier = modifier.fillMaxSize()) {
        // Map functionality removed - will be added later
        
        // Stats Card
        Card(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Distance: ${decimalFormat.format(distance)} m",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Area: ${decimalFormat.format(area)} m²",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Control Buttons
        Row(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.BottomStart),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
        
        // Leaderboard Button
        Button(
            onClick = {
                context.startActivity(Intent(context, LeaderboardActivity::class.java))
            },
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.BottomEnd)
        ) {
            Text("Leaderboard")
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