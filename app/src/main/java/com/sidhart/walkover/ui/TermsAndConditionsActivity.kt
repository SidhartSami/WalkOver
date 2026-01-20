package com.sidhart.walkover.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sidhart.walkover.ui.theme.NeonGreen
import com.sidhart.walkover.ui.theme.WalkOverTheme
import com.sidhart.walkover.MainActivity

class TermsAndConditionsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("OnboardingFlow", "Step 1: Terms & Conditions Screen")
        
        setContent {
            WalkOverTheme {
                TermsAndConditionsScreen(
                    onAccept = {
                        // Save acceptance
                        getSharedPreferences("walkover_prefs", MODE_PRIVATE)
                            .edit()
                            .putBoolean("terms_accepted", true)
                            .apply()
                        
                        android.util.Log.d("OnboardingFlow", "Terms & Conditions accepted")
                        
                        // Move to onboarding slides
                        startActivity(Intent(this, OnboardingActivity::class.java))
                        finish()
                    },
                    onDecline = {
                        // User declined - close app
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun TermsAndConditionsScreen(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    var hasScrolledToBottom by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    
    // Check if scrolled to bottom
    LaunchedEffect(scrollState.value, scrollState.maxValue) {
        hasScrolledToBottom = scrollState.value >= (scrollState.maxValue - 50)
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    NeonGreen.copy(alpha = 0.2f),
                                    NeonGreen.copy(alpha = 0.05f)
                                )
                            ),
                            shape = RoundedCornerShape(40.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = "Terms",
                        modifier = Modifier.size(40.dp),
                        tint = NeonGreen
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Terms & Conditions",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Please read and accept to continue",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Terms Content
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(20.dp)
                ) {
                    TermsSection(
                        title = "1. Acceptance of Terms",
                        content = "By using WalkOver, you agree to these terms and conditions. If you do not agree, please do not use the app."
                    )
                    
                    TermsSection(
                        title = "2. Location Data",
                        content = "WalkOver collects and processes your location data to track your walks and provide route mapping. This data is stored securely and used only for app functionality."
                    )
                    
                    TermsSection(
                        title = "3. Privacy",
                        content = "Your privacy is important to us. We do not share your personal data with third parties without your consent. Location data remains private and is only used for tracking your activities."
                    )
                    
                    TermsSection(
                        title = "4. Data Storage",
                        content = "Walk data, including routes and statistics, is stored in Firebase. You can delete your data at any time through the app settings."
                    )
                    
                    TermsSection(
                        title = "5. User Responsibilities",
                        content = "You are responsible for maintaining the security of your account. Please use the app responsibly and be aware of your surroundings while walking."
                    )
                    
                    TermsSection(
                        title = "6. Accuracy",
                        content = "While we strive for accuracy, GPS tracking may vary based on device capabilities and environmental conditions. WalkOver is not liable for any inaccuracies."
                    )
                    
                    TermsSection(
                        title = "7. Changes to Terms",
                        content = "We reserve the right to modify these terms at any time. Continued use of the app constitutes acceptance of updated terms."
                    )
                    
                    TermsSection(
                        title = "8. Contact",
                        content = "For questions or concerns about these terms, please contact us through the app settings."
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Last updated: January 2026",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Scroll indicator
            if (!hasScrolledToBottom) {
                Text(
                    text = "↓ Please scroll to read all terms ↓",
                    fontSize = 12.sp,
                    color = NeonGreen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Accept Button
            Button(
                onClick = onAccept,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = hasScrolledToBottom,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    contentColor = Color.Black,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Accept & Continue",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Decline Button
            TextButton(
                onClick = onDecline,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Decline",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun TermsSection(title: String, content: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
    }
}
