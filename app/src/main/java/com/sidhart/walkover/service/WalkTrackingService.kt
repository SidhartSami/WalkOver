package com.sidhart.walkover.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sidhart.walkover.MainActivity
import com.sidhart.walkover.R
import com.sidhart.walkover.data.LiveWalkState
import com.sidhart.walkover.data.LocationPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class WalkTrackingService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var locationService: LocationService
    private lateinit var notificationManager: NotificationManager

    val _walkState = MutableStateFlow(LiveWalkState())
    val walkState: StateFlow<LiveWalkState> = _walkState.asStateFlow()
    
    // Method to sync state from Activity
    fun syncState(state: LiveWalkState) {
        val wasTracking = _walkState.value.isTracking
        _walkState.value = state
        
        // If Activity started tracking, ensure service is also tracking location
        if (state.isTracking && !wasTracking && !state.isPaused) {
            startLocationTracking()
        } else if (!state.isTracking && wasTracking) {
            locationJob?.cancel()
        } else if (state.isPaused && !_walkState.value.isPaused) {
            locationJob?.cancel()
        } else if (!state.isPaused && _walkState.value.isPaused) {
            startLocationTracking()
        }
        
        updateNotification()
    }

    private var locationJob: Job? = null
    private var timerJob: Job? = null

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "walk_tracking_channel"
        private const val NOTIFICATION_ID = 1

        const val ACTION_START_TRACKING = "ACTION_START_TRACKING"
        const val ACTION_PAUSE_TRACKING = "ACTION_PAUSE_TRACKING"
        const val ACTION_RESUME_TRACKING = "ACTION_RESUME_TRACKING"
        const val ACTION_STOP_TRACKING = "ACTION_STOP_TRACKING"
    }

    inner class LocalBinder : Binder() {
        fun getService(): WalkTrackingService = this@WalkTrackingService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("WalkTrackingService", "Service created")
        locationService = LocationService(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d("WalkTrackingService", "Service bound")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("WalkTrackingService", "onStartCommand: ${intent?.action}")

        when (intent?.action) {
            ACTION_START_TRACKING -> startTracking()
            ACTION_PAUSE_TRACKING -> pauseTracking()
            ACTION_RESUME_TRACKING -> resumeTracking()
            ACTION_STOP_TRACKING -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        Log.d("WalkTrackingService", "Starting tracking...")
        
        // Check if location service has permission
        if (!locationService.hasLocationPermission()) {
            Log.e("WalkTrackingService", "Location permission not granted")
            return
        }

        // Only initialize state if not already tracking (Activity might have started it)
        if (!_walkState.value.isTracking) {
            _walkState.value = LiveWalkState(
                isTracking = true,
                isPaused = false,
                startTime = System.currentTimeMillis(),
                points = emptyList()
            )
        }

        startForeground(NOTIFICATION_ID, createNotification())
        
        // Only start location tracking if Activity isn't handling it
        // For now, Activity handles tracking, service just shows notifications
        // startLocationTracking() // Commented out - Activity handles tracking
        startTimer()

        Log.d("WalkTrackingService", "Tracking started successfully")
    }

    private fun pauseTracking() {
        Log.d("WalkTrackingService", "Pausing tracking")
        _walkState.value = _walkState.value.copy(
            isPaused = true,
            pauseStartTime = System.currentTimeMillis()
        )
        locationJob?.cancel()
        updateNotification()
    }

    private fun resumeTracking() {
        Log.d("WalkTrackingService", "Resuming tracking")
        val pauseDuration = System.currentTimeMillis() - _walkState.value.pauseStartTime
        _walkState.value = _walkState.value.copy(
            isPaused = false,
            totalPausedTime = _walkState.value.totalPausedTime + pauseDuration
        )
        startLocationTracking()
        updateNotification()
    }

    private fun stopTracking() {
        Log.d("WalkTrackingService", "Stopping tracking")
        _walkState.value = _walkState.value.copy(isTracking = false, isPaused = false)
        locationJob?.cancel()
        timerJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startLocationTracking() {
        locationJob?.cancel()

        Log.d("WalkTrackingService", "Starting location tracking...")

        locationJob = serviceScope.launch {
            try {
                locationService.getLocationUpdates()
                    .catch { e ->
                        Log.e("WalkTrackingService", "Location tracking error", e)
                    }
                    .collect { location ->
                        val currentState = _walkState.value
                        if (!currentState.isPaused && currentState.isTracking) {
                            val updatedPoints = currentState.points + location
                            val distance = calculateTotalDistance(updatedPoints)

                            Log.d("WalkTrackingService", "Location update: ${location.latitude}, ${location.longitude}, Points: ${updatedPoints.size}, Distance: $distance")

                            _walkState.value = currentState.copy(points = updatedPoints)
                                .updateStats(distance, 0.0, updatedPoints.size)

                            // Update notification every 10 points to reduce overhead
                            if (updatedPoints.size % 10 == 0) {
                                updateNotification()
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e("WalkTrackingService", "Fatal location tracking error", e)
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                val currentState = _walkState.value
                if (!currentState.isPaused && currentState.isTracking) {
                    // Update elapsed time in state while preserving distance and other stats
                    val elapsedTime = System.currentTimeMillis() - currentState.startTime - currentState.totalPausedTime
                    val distance = calculateTotalDistance(currentState.points)
                    val updatedState = currentState.updateStats(distance, 0.0, currentState.points.size)
                    _walkState.value = updatedState
                    updateNotification()
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Walk Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows walk tracking progress"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
            Log.d("WalkTrackingService", "Notification channel created")
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val state = _walkState.value
        val statusText = if (state.isPaused) "Paused" else "Active"
        val distance = String.format("%.2f km", state.stats.distanceKm)
        val time = state.stats.formatElapsedTime()

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Walk in Progress - $statusText")
            .setContentText("$distance • $time • ${state.points.size} points")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation) // Fallback icon
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(createPauseResumeAction())
            .addAction(createStopAction())
            .build()
    }

    private fun createPauseResumeAction(): NotificationCompat.Action {
        val state = _walkState.value
        val actionIntent = Intent(this, WalkTrackingService::class.java).apply {
            action = if (state.isPaused) ACTION_RESUME_TRACKING else ACTION_PAUSE_TRACKING
        }
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(
            if (state.isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
            if (state.isPaused) "Resume" else "Pause",
            pendingIntent
        ).build()
    }

    private fun createStopAction(): NotificationCompat.Action {
        val stopIntent = Intent(this, WalkTrackingService::class.java).apply {
            action = ACTION_STOP_TRACKING
        }
        val pendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Stop",
            pendingIntent
        ).build()
    }

    private fun updateNotification() {
        try {
            notificationManager.notify(NOTIFICATION_ID, createNotification())
        } catch (e: Exception) {
            Log.e("WalkTrackingService", "Failed to update notification", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("WalkTrackingService", "Service destroyed")
        locationJob?.cancel()
        timerJob?.cancel()
        serviceScope.cancel()
    }
}