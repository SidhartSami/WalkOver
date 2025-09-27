package com.sidhart.walkover.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    currentMapStyle: MapStyle = MapStyle.CARTODB_POSITRON,
    onMapStyleChange: (MapStyle) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(32.dp))
        
        // Map Style Selection Container
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Map Style",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Choose your preferred map appearance",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Map Style Selection Boxes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Positron Style Box
                    MapStyleBox(
                        style = MapStyle.CARTODB_POSITRON,
                        isSelected = currentMapStyle == MapStyle.CARTODB_POSITRON,
                        onClick = { onMapStyleChange(MapStyle.CARTODB_POSITRON) },
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Voyager Style Box
                    MapStyleBox(
                        style = MapStyle.CARTO_VOYAGER,
                        isSelected = currentMapStyle == MapStyle.CARTO_VOYAGER,
                        onClick = { onMapStyleChange(MapStyle.CARTO_VOYAGER) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // App Info Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "App Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "WalkOver - Fitness Gamified App",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Version 1.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Track your walking routes, compete on the leaderboard, and explore different map styles.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MapStyleBox(
    style: MapStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Placeholder for map preview image
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (style == MapStyle.CARTODB_POSITRON) 
                            Color(0xFFF5F5F5) // Light gray for Positron
                        else 
                            Color(0xFFE8F4FD) // Light blue for Voyager
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (style == MapStyle.CARTODB_POSITRON) "🗺️" else "🌍",
                    fontSize = 24.sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = style.displayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}