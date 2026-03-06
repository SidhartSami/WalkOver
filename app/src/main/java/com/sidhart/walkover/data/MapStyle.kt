package com.sidhart.walkover.data

import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource

enum class MapStyle(val displayName: String, val emoji: String, val tileSource: ITileSource) {
    OUTDOORS("Outdoors", "🏔️", TileSourceFactory.MAPNIK),
    LIGHT("Light", "☀️", TileSourceFactory.MAPNIK),
    DARK("Dark", "🌙", 
        XYTileSource("DarkMatter", 1, 19, 256, ".png", 
            arrayOf("https://a.basemaps.cartocdn.com/dark_all/", 
                    "https://b.basemaps.cartocdn.com/dark_all/", 
                    "https://c.basemaps.cartocdn.com/dark_all/"))
    ),
    MAPBOX_STREETS("Streets", "🗺️", TileSourceFactory.MAPNIK),
    MAPBOX_DARK("Dark", "🌙", 
        XYTileSource("DarkMatterBlue", 1, 19, 256, ".png", 
            arrayOf("https://a.basemaps.cartocdn.com/dark_all/", 
                    "https://b.basemaps.cartocdn.com/dark_all/", 
                    "https://c.basemaps.cartocdn.com/dark_all/"))
    );

    companion object {
        /**
         * Blue-navy color filter for dark mode map tiles.
         * Apply this to mapView.overlayManager.tilesOverlay.setColorFilter(...)
         * to get the distinctive dark navy-blue look.
         */
        fun getDarkBlueColorFilter(): android.graphics.ColorMatrixColorFilter {
            val matrix = android.graphics.ColorMatrix(floatArrayOf(
                0.55f, 0.0f, 0.0f, 0.0f, 5f,    // Red: reduce
                0.0f,  0.65f, 0.0f, 0.0f, 15f,   // Green: slight
                0.0f,  0.0f,  0.9f, 0.0f, 40f,   // Blue: boost
                0.0f,  0.0f,  0.0f, 1.0f, 0f     // Alpha: untouched
            ))
            return android.graphics.ColorMatrixColorFilter(matrix)
        }
    }
}
