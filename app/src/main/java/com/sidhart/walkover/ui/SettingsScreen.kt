package com.sidhart.walkover.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Import the enums from MainActivity
import com.sidhart.walkover.ThemePreference
import com.sidhart.walkover.LabelMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    currentMapStyle: MapStyle = MapStyle.CARTODB_POSITRON,
    onMapStyleChange: (MapStyle) -> Unit = {},
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    onThemePreferenceChange: (ThemePreference) -> Unit = {},
    selectedLabelMode: LabelMode = LabelMode.WITH_LABELS,
    onLabelModeChange: (LabelMode) -> Unit = {}
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

        Spacer(modifier = Modifier.height(24.dp))
        
        // Theme Selection Container
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "App Theme",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Choose how the map appears",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                var themeExpanded by remember { mutableStateOf(false) }
                
                Box {
                    OutlinedTextField(
                        value = themePreference.displayName,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Select Theme") },
                        trailingIcon = {
                            Icon(
                                imageVector = if (themeExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Dropdown"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                android.util.Log.d("SettingsScreen", "Theme dropdown clicked, expanding: $themeExpanded")
                                themeExpanded = !themeExpanded 
                            },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    
                    DropdownMenu(
                        expanded = themeExpanded,
                        onDismissRequest = { themeExpanded = false }
                    ) {
                        ThemePreference.values().forEach { preference ->
                            DropdownMenuItem(
                                text = { Text(preference.displayName) },
                                onClick = {
                                    android.util.Log.d("SettingsScreen", "Theme preference changed to: ${preference.displayName}")
                                    onThemePreferenceChange(preference)
                                    themeExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Label Selection Container
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Map Labels",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Show or hide street names and labels",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                var labelExpanded by remember { mutableStateOf(false) }
                
                Box {
                    OutlinedTextField(
                        value = selectedLabelMode.displayName,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Select Label Option") },
                        trailingIcon = {
                            Icon(
                                imageVector = if (labelExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Dropdown"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                android.util.Log.d("SettingsScreen", "Label dropdown clicked, expanding: $labelExpanded")
                                labelExpanded = !labelExpanded 
                            },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    
                    DropdownMenu(
                        expanded = labelExpanded,
                        onDismissRequest = { labelExpanded = false }
                    ) {
                        LabelMode.values().forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.displayName) },
                                onClick = {
                                    android.util.Log.d("SettingsScreen", "Label mode changed to: ${mode.displayName}")
                                    onLabelModeChange(mode)
                                    labelExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Current Map Style Display
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Current Map Style",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = currentMapStyle.displayName,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "This style will update automatically based on your theme and label preferences",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // App Info Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
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