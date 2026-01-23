package com.sidhart.walkover.ui

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onAcceptPrivacyPolicy: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Calculate scroll percentage to determine if user has read content
    val isScrolledToBottom = scrollState.value >= (scrollState.maxValue - 50)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header - Fixed at top
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.PrivacyTip,
                contentDescription = "Privacy Policy",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Privacy Policy",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Please review before continuing",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // Scrollable Content in Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
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
                    content = "This Privacy Policy may be updated from time to time. Any changes will be reflected on this page.\n\nEffective date: January 14, 2026"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Scroll Indicator - Inside the card at bottom
                if (!isScrolledToBottom) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scroll to continue",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    // Show completion message when scrolled to bottom
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "You've reviewed the entire policy",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Button Section - Always at bottom, button disabled/enabled based on scroll
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { onAcceptPrivacyPolicy() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = isScrolledToBottom
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 8.dp)
                )
                Text(
                    text = "Accept & Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isScrolledToBottom)
                    "By continuing, you agree to our privacy practices"
                else
                    "Please read the entire policy before accepting",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PolicySectionContent(
    title: String,
    content: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp)
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = content,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp,
            textAlign = TextAlign.Justify
        )
    }
}