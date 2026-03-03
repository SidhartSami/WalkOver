package com.sidhart.walkover.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onAcceptPrivacyPolicy: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Use a stable derived state with a generous threshold to avoid flickering.
    // Once true, stays true (one-way latch) so it never glitches back.
    var hasScrolledToBottom by remember { mutableStateOf(false) }
    val isScrolledToBottom by remember {
        derivedStateOf {
            if (scrollState.maxValue > 0 &&
                scrollState.value >= (scrollState.maxValue - 150)
            ) {
                hasScrolledToBottom = true
            }
            hasScrolledToBottom
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ─── Header ───────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon with tinted background circle
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PrivacyTip,
                    contentDescription = "Privacy Policy",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Privacy Policy",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Chip-style subtitle
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier.wrapContentWidth()
            ) {
                Text(
                    text = "Please read carefully before continuing",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ─── Scrollable Card ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(24.dp)
                ) {
                    PolicySectionContent(
                        title = "Information We Collect",
                        content = "WalkOver collects precise location data (GPS coordinates) only when you explicitly start a walking activity by using the \"Start Walk\" feature. Location tracking stops when you end the walk.\n\nIf a walk is not completed or the application closes unexpectedly, the walk data is not saved.\n\nWhile location permission is granted, the app may display your current location on the map, but this location is not stored unless an active walk is in progress."
                    )

                    PolicySectionContent(
                        title = "Walk Data",
                        content = "When a walk is completed, the following data is saved:\n• Route path (latitude and longitude points)\n• Date and time of the walk\n• Total distance traveled"
                    )

                    PolicySectionContent(
                        title = "Authentication Data",
                        content = "Firebase Authentication is used only to allow you to sign in and access your own walking data. No authentication data is used for advertising or tracking purposes."
                    )

                    PolicySectionContent(
                        title = "How We Use Your Data",
                        content = "Collected data is used solely to:\n• Display walking routes and history to you\n• Calculate distance and activity statistics\n• Allow you to access your saved walks after signing in"
                    )

                    PolicySectionContent(
                        title = "Data Storage",
                        content = "Walk data and account information are securely stored using Firebase services."
                    )

                    PolicySectionContent(
                        title = "Data Sharing",
                        content = "WalkOver does not sell, share, or rent your data to third parties. Data is not used for advertising or analytics."
                    )

                    PolicySectionContent(
                        title = "Data Deletion",
                        content = "You can delete individual walks within the app. When a walk is deleted, all associated data is permanently removed from our servers."
                    )

                    PolicySectionContent(
                        title = "Children's Privacy",
                        content = "WalkOver is not intended for children under the age of 13, and we do not knowingly collect personal data from children."
                    )

                    PolicySectionContent(
                        title = "Changes to This Privacy Policy",
                        content = "This Privacy Policy may be updated from time to time. Any changes will be reflected on this page.\n\nEffective date: January 14, 2026",
                        showDivider = false
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom status row inside card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isScrolledToBottom)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isScrolledToBottom)
                                Icons.Outlined.CheckCircle
                            else
                                Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (isScrolledToBottom)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isScrolledToBottom)
                                "You've reviewed the entire policy"
                            else
                                "Scroll to read the full policy",
                            fontSize = 12.sp,
                            color = if (isScrolledToBottom)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Fade gradient overlay at the bottom of the card (hidden when scrolled)
            if (!isScrolledToBottom) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                                )
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── Bottom CTA ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = !isScrolledToBottom,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { it },
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { it }
            ) {
                // Scroll hint pill
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scroll to read the full policy",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isScrolledToBottom,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { it },
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { it }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = { onAcceptPrivacyPolicy() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Accept & Continue",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "By continuing, you agree to our privacy practices",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun PolicySectionContent(
    title: String,
    content: String,
    showDivider: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp)
    ) {
        // Left accent bar + title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(18.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(50.dp)
                    )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = content,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp,
            textAlign = TextAlign.Justify,
            modifier = Modifier.padding(start = 14.dp)
        )

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(top = 18.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.8.dp
            )
        }
    }
}