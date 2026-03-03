package com.sidhart.walkover.utils

import android.graphics.*
import com.sidhart.walkover.data.Walk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.toColorInt
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Creates STRAVA-STYLE overlays
 * User's photo/video as background + transparent route + stats + logo
 */
object StoryTemplateGenerator {
    
    private const val STORY_WIDTH = 1080
    private const val STORY_HEIGHT = 1920
    
    // Neon Green from InDrive theme
    private val NEON_GREEN = "#C0F11C".toColorInt()
    
    /**
     * Generate STRAVA-STYLE story
     * User photo as background, only overlay route + stats + logo
     */
    suspend fun generateStravaStyleStory(
        context: android.content.Context,
        userPhotoBitmap: Bitmap,
        walk: Walk,
        onError: ((String) -> Unit)? = null
    ): Bitmap? = withContext(Dispatchers.Default) {
        try {
            // Create final bitmap
            val storyBitmap = createBitmap(STORY_WIDTH, STORY_HEIGHT)
            val canvas = Canvas(storyBitmap)
            
            // 1. Draw background image using CENTER_CROP strategy
            val scale = maxOf(
                STORY_WIDTH.toFloat() / userPhotoBitmap.width.toFloat(),
                STORY_HEIGHT.toFloat() / userPhotoBitmap.height.toFloat()
            )
            val matrix = android.graphics.Matrix()
            matrix.postScale(scale, scale)
            val dx = (STORY_WIDTH - (userPhotoBitmap.width * scale)) / 2f
            val dy = (STORY_HEIGHT - (userPhotoBitmap.height * scale)) / 2f
            matrix.postTranslate(dx, dy)
            canvas.drawBitmap(userPhotoBitmap, matrix, Paint().apply { isFilterBitmap = true })
            
            // 2. Add gradient overlay for HUD style
            val overlayPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, STORY_HEIGHT.toFloat(),
                    intArrayOf("#E6000000".toColorInt(), "#33000000".toColorInt(), "#E6000000".toColorInt()),
                    floatArrayOf(0f, 0.4f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, STORY_WIDTH.toFloat(), STORY_HEIGHT.toFloat(), overlayPaint)
            
            // 3. Draw NEON GREEN route visualization
            drawRouteVisualization(canvas, walk)
            
            // 4. Load font and Draw HUD stats overlay
            val bebasFont = try {
                Typeface.createFromAsset(context.assets, "fonts/bebas_neue.ttf")
            } catch (e: Exception) {
                Typeface.DEFAULT_BOLD
            }
            drawHUDStats(canvas, walk, bebasFont)
            
            storyBitmap
        } catch (e: Exception) {
            android.util.Log.e("StoryGenerator", "Error generating story", e)
            onError?.invoke(e.message ?: e.toString())
            null
        }
    }
    
    /**
     * Draw NEON GREEN route path - STRAVA STYLE
     */
    private fun drawRouteVisualization(canvas: Canvas, walk: Walk) {
        if (walk.polylineCoordinates.isEmpty()) return
        
        // Calculate route bounds
        var minLat = Double.MAX_VALUE
        var maxLat = Double.MIN_VALUE
        var minLon = Double.MAX_VALUE
        var maxLon = Double.MIN_VALUE
        
        walk.polylineCoordinates.forEach { point ->
            minLat = minOf(minLat, point.latitude)
            maxLat = maxOf(maxLat, point.latitude)
            minLon = minOf(minLon, point.longitude)
            maxLon = maxOf(maxLon, point.longitude)
        }
        
        // Define safe drawing area - Top Right quadrant (HUD style)
        val padding = 80f
        val routeAreaLeft = STORY_WIDTH / 2f + 20f
        val routeAreaTop = 150f
        val routeAreaWidth = STORY_WIDTH / 2f - padding - 20f
        val routeAreaBottom = 850f
        val routeAreaHeight = routeAreaBottom - routeAreaTop
        
        // Calculate route dimensions
        val latRange = maxLat - minLat
        val lonRange = maxLon - minLon
        
        if (latRange == 0.0 || lonRange == 0.0) return
        
        // Add minimum size to prevent tiny routes
        val minRouteSize = 0.002 // ~200 meters minimum display size
        val effectiveLatRange = maxOf(latRange, minRouteSize)
        val effectiveLonRange = maxOf(lonRange, minRouteSize)
        
        // Calculate scale to fit route in safe area
        val scaleX = routeAreaWidth / effectiveLonRange
        val scaleY = routeAreaHeight / effectiveLatRange
        val scale = minOf(scaleX, scaleY).toFloat()
        
        // Center the route in the safe area
        val routeWidth = (effectiveLonRange * scale).toFloat()
        val routeHeight = (effectiveLatRange * scale).toFloat()
        val offsetX = routeAreaLeft + (routeAreaWidth - routeWidth) / 2
        val offsetY = routeAreaTop + (routeAreaHeight - routeHeight) / 2
        
        // Build path from coordinates
        val path = Path()
        var isFirst = true
        
        walk.polylineCoordinates.forEach { point ->
            // Use effective ranges for centering small routes
            val latCenter = minLat + effectiveLatRange / 2
            val lonCenter = minLon + effectiveLonRange / 2
            
            val x = offsetX + routeWidth / 2 + ((point.longitude - lonCenter) * scale).toFloat()
            val y = offsetY + routeHeight / 2 - ((point.latitude - latCenter) * scale).toFloat()
            
            if (isFirst) {
                path.moveTo(x, y)
                isFirst = false
            } else {
                path.lineTo(x, y)
            }
        }
        
        // Draw outer glow
        val glowPaint = Paint().apply {
            color = NEON_GREEN
            style = Paint.Style.STROKE
            strokeWidth = 30f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            pathEffect = android.graphics.CornerPathEffect(80f)
            maskFilter = BlurMaskFilter(25f, BlurMaskFilter.Blur.NORMAL)
            isAntiAlias = true
            alpha = 180
        }
        canvas.drawPath(path, glowPaint)
        
        // Draw middle glow
        val midGlowPaint = Paint().apply {
            color = NEON_GREEN
            style = Paint.Style.STROKE
            strokeWidth = 18f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            pathEffect = android.graphics.CornerPathEffect(80f)
            maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
            isAntiAlias = true
        }
        canvas.drawPath(path, midGlowPaint)
        
        // Draw solid neon line
        val linePaint = Paint().apply {
            color = NEON_GREEN
            style = Paint.Style.STROKE
            strokeWidth = 12f // slightly thicker aesthetic line
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            pathEffect = android.graphics.CornerPathEffect(80f)
            isAntiAlias = true
        }
        canvas.drawPath(path, linePaint)
        
        // Draw start marker
        if (walk.polylineCoordinates.isNotEmpty()) {
            val startPoint = walk.polylineCoordinates.first()
            val latCenter = minLat + effectiveLatRange / 2
            val lonCenter = minLon + effectiveLonRange / 2
            
            val startX = offsetX + routeWidth / 2 + ((startPoint.longitude - lonCenter) * scale).toFloat()
            val startY = offsetY + routeHeight / 2 - ((startPoint.latitude - latCenter) * scale).toFloat()
            
            // Glow around start
            val startGlowPaint = Paint().apply {
                color = NEON_GREEN
                style = Paint.Style.FILL
                maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
                isAntiAlias = true
            }
            canvas.drawCircle(startX, startY, 24f, startGlowPaint)
            
            // Start dot
            val startPaint = Paint().apply {
                color = NEON_GREEN
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(startX, startY, 14f, startPaint)
            
            // White border
            val borderPaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 3f
                isAntiAlias = true
            }
            canvas.drawCircle(startX, startY, 14f, borderPaint)
            
            // End marker (if different from start)
            if (walk.polylineCoordinates.size > 1) {
                val endPoint = walk.polylineCoordinates.last()
                val endX = offsetX + routeWidth / 2 + ((endPoint.longitude - lonCenter) * scale).toFloat()
                val endY = offsetY + routeHeight / 2 - ((endPoint.latitude - latCenter) * scale).toFloat()
                
                // Check if end is far enough from start to draw
                val distance = sqrt(
                    (endX - startX).toDouble().pow(2.0) +
                            (endY - startY).toDouble().pow(2.0)
                )
                
                if (distance > 30) { // Only draw if at least 30px apart
                    canvas.drawCircle(endX, endY, 24f, startGlowPaint)
                    canvas.drawCircle(endX, endY, 14f, startPaint)
                    canvas.drawCircle(endX, endY, 14f, borderPaint)
                }
            }
        }
    }
    
    /**
     * Draw HUD style overlay
     */
    private fun drawHUDStats(canvas: Canvas, walk: Walk, bebasFont: Typeface) {
        val distanceKm = walk.distanceCovered / 1000.0
        val durationMinutes = walk.duration / 60000.0
        val speedKmH = if (durationMinutes > 0) distanceKm / (durationMinutes / 60.0) else 0.0

        // Draw Corner Brackets (HUD overlay effect)
        val bracketPaint = Paint().apply {
            color = NEON_GREEN
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }
        val size = 60f
        val bPad = 60f
        // Top Left
        canvas.drawPath(Path().apply { moveTo(bPad, bPad + size); lineTo(bPad, bPad); lineTo(bPad + size, bPad) }, bracketPaint)
        // Top Right
        canvas.drawPath(Path().apply { moveTo(STORY_WIDTH - bPad - size, bPad); lineTo(STORY_WIDTH - bPad, bPad); lineTo(STORY_WIDTH - bPad, bPad + size) }, bracketPaint)
        // Bottom Left
        canvas.drawPath(Path().apply { moveTo(bPad, STORY_HEIGHT - bPad - size); lineTo(bPad, STORY_HEIGHT - bPad); lineTo(bPad + size, STORY_HEIGHT - bPad) }, bracketPaint)
        // Bottom Right
        canvas.drawPath(Path().apply { moveTo(STORY_WIDTH - bPad - size, STORY_HEIGHT - bPad); lineTo(STORY_WIDTH - bPad, STORY_HEIGHT - bPad); lineTo(STORY_WIDTH - bPad, STORY_HEIGHT - bPad - size) }, bracketPaint)

        // Top Left Text: Time of Day + Activity
        val hour = Calendar.getInstance().apply { time = walk.timestamp }.get(Calendar.HOUR_OF_DAY)
        val timeOfDay = when(hour) {
            in 5..11 -> "MORNING"
            in 12..16 -> "AFTERNOON"
            in 17..20 -> "EVENING"
            else -> "NIGHT"
        }
        val contextActivity = "$timeOfDay WALK"

        val smallGreenPaint = Paint().apply {
            color = NEON_GREEN
            textSize = 40f
            typeface = bebasFont
            isAntiAlias = true
            letterSpacing = 0.1f
            setShadowLayer(4f, 0f, 2f, Color.BLACK)
        }
        val largeWhitePaint = Paint().apply {
            color = Color.WHITE
            textSize = 90f
            typeface = bebasFont
            isAntiAlias = true
            letterSpacing = 0.05f
            setShadowLayer(8f, 0f, 4f, Color.BLACK)
        }

        canvas.drawText(contextActivity, bPad + 40f, bPad + 80f, smallGreenPaint)
        canvas.drawText("WALKOVER", bPad + 40f, bPad + 180f, largeWhitePaint)

        // Bottom Huge Distance
        val bottomBaseY = STORY_HEIGHT - 350f
        
        val hugeWhitePaint = Paint().apply {
            color = Color.WHITE
            textSize = 280f
            typeface = bebasFont
            isAntiAlias = true
            setShadowLayer(10f, 0f, 5f, Color.BLACK)
        }
        val distanceStr = String.format(Locale.getDefault(), "%.1f", distanceKm)
        val distWidth = hugeWhitePaint.measureText(distanceStr)
        canvas.drawText(distanceStr, bPad + 20f, bottomBaseY, hugeWhitePaint)
        
        val kmPaint = Paint().apply {
            color = NEON_GREEN
            textSize = 70f
            typeface = bebasFont
            isAntiAlias = true
            setShadowLayer(4f, 0f, 2f, Color.BLACK)
        }
        canvas.drawText("KM", bPad + 20f + distWidth + 10f, bottomBaseY, kmPaint)

        // Horizontal Line
        val lineY = bottomBaseY + 40f
        canvas.drawLine(bPad - 20f, lineY, STORY_WIDTH.toFloat(), lineY, bracketPaint)

        // 3 Stats Columns below the line
        val statsY = lineY + 120f
        val colWidth = STORY_WIDTH / 3f

        // Time
        val hoursStat = walk.duration / (1000 * 60 * 60)
        val minutesStat = (walk.duration / (1000 * 60)) % 60
        val secondsStat = (walk.duration / 1000) % 60
        val timeString = if (hoursStat > 0) String.format("%d:%02d:%02d", hoursStat, minutesStat, secondsStat) 
                         else String.format("%02d:%02d", minutesStat, secondsStat)
        
        drawHUDStat(canvas, bPad + 20f, statsY, "TIME", timeString, bebasFont, Paint.Align.LEFT)

        // Speed
        drawHUDStat(canvas, colWidth * 1.5f, statsY, "SPEED / KM", String.format(Locale.getDefault(), "%.1f", speedKmH), bebasFont, Paint.Align.CENTER)

        // Date (since we don't have BPM or Calories easily accessible)
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        drawHUDStat(canvas, STORY_WIDTH - bPad - 20f, statsY, "DATE", dateFormat.format(walk.timestamp).uppercase(), bebasFont, Paint.Align.RIGHT)
    }

    private fun drawHUDStat(
        canvas: Canvas,
        x: Float,
        y: Float,
        label: String,
        value: String,
        font: Typeface,
        align: Paint.Align
    ) {
        val valPaint = Paint().apply {
            color = Color.WHITE
            textSize = 80f
            typeface = font
            textAlign = align
            isAntiAlias = true
            setShadowLayer(8f, 0f, 4f, Color.BLACK)
        }
        canvas.drawText(value, x, y, valPaint)

        val labPaint = Paint().apply {
            color = NEON_GREEN
            textSize = 35f
            letterSpacing = 0.1f
            typeface = font
            textAlign = align
            isAntiAlias = true
            setShadowLayer(4f, 0f, 2f, Color.BLACK)
        }
        canvas.drawText(label, x, y + 50f, labPaint)
    }
    
    /**
     * Format duration helper
     */
    private fun formatDuration(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = millis / (1000 * 60 * 60)
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%d:%02d", minutes, seconds)
        }
    }
}