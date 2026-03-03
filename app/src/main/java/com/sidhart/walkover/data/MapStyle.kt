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
        XYTileSource("DarkMatter", 1, 19, 256, ".png", 
            arrayOf("https://a.basemaps.cartocdn.com/dark_all/", 
                    "https://b.basemaps.cartocdn.com/dark_all/", 
                    "https://c.basemaps.cartocdn.com/dark_all/"))
    )
}
