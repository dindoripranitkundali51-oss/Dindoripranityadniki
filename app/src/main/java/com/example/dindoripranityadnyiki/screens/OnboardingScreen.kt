package com.example.dindoripranityadnyiki.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.R
import androidx.datastore.preferences.core.edit
import com.example.dindoripranityadnyiki.data.dataStore
import com.example.dindoripranityadnyiki.data.PrefKeys
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException

@Composable
fun OnboardingScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    // 🌟 State
    var userRole by remember { mutableStateOf("") }
    var alphaAnim by remember { mutableStateOf(0f) }

    // 🌈 Smooth fade animation
    LaunchedEffect(Unit) {
        delay(100)
        repeat(10) {
            alphaAnim = it / 10f
            delay(25)
        }
    }

    // 🧠 Read role safely
    LaunchedEffect(Unit) {
        try {
            val prefs = context.dataStore.data
                .catch { e ->
                    if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
                    else throw e
                }
                .first()

            userRole = prefs[PrefKeys.USER_ROLE] ?: ""

            // Smooth automatic redirection for Guruji
            if (userRole == "guruji") {
                delay(300)
                navController.navigate("gurujiOnboarding") {
                    popUpTo("onboarding") { inclusive = true }
                }
            }
        } catch (_: Exception) {
            // fallback (safe)
        }
    }

    // 🔄 Background gradient animation
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            tween(9000, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = ""
    )

    val animatedBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFBBDEFB),
            Color(0xFFE3F2FD),
            Color(0xFF90CAF9)
        ),
        start = androidx.compose.ui.geometry.Offset(0f, offset),
        end = androidx.compose.ui.geometry.Offset(offset, 0f)
    )

    // 🌼 Main UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Scrollable section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(30.dp))

                // 🪔 Logo
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(180.dp)
                        .alpha(alphaAnim)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // ✨ Title
                Text(
                    text = "✨ Welcome to ✨\nDindori Pranit Yadnyiki",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        color = Color(0xFF0D47A1),
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color(0xFF64B5F6),
                            blurRadius = 10f
                        )
                    ),
                    modifier = Modifier.alpha(alphaAnim)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "भाविकांसाठी अधिकृत आध्यात्मिक व्यासपीठ — दिंडोरी प्रणित सेवा मार्गांतर्गत पारदर्शक पूजा बुकिंग आणि विश्वासार्ह गुरुजींची सेवा.",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF263238),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(alphaAnim)
                        .padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 🪄 Feature Cards
                FeatureCardModern(
                    iconRes = R.drawable.ic_pandit,
                    titleEn = "Verified Guruji Services",
                    descEn = "All Poojas conducted only by official & verified Guruji",
                    titleMr = "विश्वासार्ह गुरुजी सेवा",
                    descMr = "सर्व पूजा फक्त अधिकृत व सत्यापित गुरुजींकडूनच केली जाईल",
                    alpha = alphaAnim
                )

                FeatureCardModern(
                    iconRes = R.drawable.ic_service,
                    titleEn = "Transparency & Trust",
                    descEn = "Secured bookings with digital receipts & live status",
                    titleMr = "पारदर्शक व विश्वासार्ह सेवा",
                    descMr = "प्रत्येक बुकिंग डिजिटल पावती व थेट स्टेटस अपडेटसह सुरक्षित",
                    alpha = alphaAnim
                )

                FeatureCardModern(
                    iconRes = R.drawable.ic_puja,
                    titleEn = "Official Booking Only",
                    descEn = "Book Pooja only via this official app – avoid unauthorized services",
                    titleMr = "फक्त अधिकृत ॲप बुकिंग",
                    descMr = "पूजा बुकिंग फक्त या अधिकृत ॲपद्वारेच करा – अनधिकृत सेवांना टाळा",
                    alpha = alphaAnim
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // 🚀 Continue Button
            Button(
                onClick = {
                    scope.launch {
                        context.dataStore.edit {
                            it[PrefKeys.IS_ONBOARDING_DONE] = true
                        }
                        navController.navigate("registration") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                },
                shape = RoundedCornerShape(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .alpha(alphaAnim)
                    .shadow(10.dp, RoundedCornerShape(40.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF1976D2), Color(0xFF0D47A1))
                        ),
                        shape = RoundedCornerShape(40.dp)
                    )
            ) {
                Text(
                    text = "Continue →",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// 🌟 Feature Card UI
@Composable
private fun FeatureCardModern(
    iconRes: Int,
    titleEn: String,
    descEn: String,
    titleMr: String,
    descMr: String,
    alpha: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .alpha(alpha),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = titleEn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0D47A1)
                )
                Text(
                    text = descEn,
                    fontSize = 13.sp,
                    color = Color(0xFF37474F)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = titleMr,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1565C0)
                )
                Text(
                    text = descMr,
                    fontSize = 13.sp,
                    color = Color(0xFF1A237E)
                )
            }
        }
    }
}
