package com.example.dindoripranityadnyiki.feature.user

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.R
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import com.example.dindoripranityadnyiki.core.data.dataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException

@Composable
fun OnboardingScreen(navController: NavController) {
    val context = LocalContext.current
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
                    if (e is IOException) emit(emptyPreferences())
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

    val animatedBrush = Brush.Companion.linearGradient(
        colors = listOf(
            Color(0xFFBBDEFB),
            Color(0xFFE3F2FD),
            Color(0xFF90CAF9)
        ),
        start = Offset(0f, offset),
        end = Offset(offset, 0f)
    )

    // 🌼 Main UI
    Box(
        modifier = Modifier.Companion
            .fillMaxSize()
            .background(animatedBrush)
    ) {
        Column(
            modifier = Modifier.Companion
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Companion.CenterHorizontally
        ) {
            // Scrollable section
            Column(
                horizontalAlignment = Alignment.Companion.CenterHorizontally,
                modifier = Modifier.Companion
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.Companion.height(30.dp))

                // 🪔 Logo
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.Companion
                        .size(180.dp)
                        .alpha(alphaAnim)
                )

                Spacer(modifier = Modifier.Companion.height(18.dp))

                // ✨ Title
                Text(
                    text = "✨ Welcome to ✨\nDindori Pranit Yadnyiki",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Companion.ExtraBold,
                    textAlign = TextAlign.Companion.Center,
                    style = TextStyle(
                        color = Color(0xFF0D47A1),
                        shadow = Shadow(
                            color = Color(0xFF64B5F6),
                            blurRadius = 10f
                        )
                    ),
                    modifier = Modifier.Companion.alpha(alphaAnim)
                )

                Spacer(modifier = Modifier.Companion.height(16.dp))

                Text(
                    text = "भाविकांसाठी अधिकृत आध्यात्मिक व्यासपीठ — दिंडोरी प्रणित सेवा मार्गांतर्गत पारदर्शक पूजा बुकिंग आणि विश्वासार्ह गुरुजींची सेवा.",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Companion.Medium,
                    color = Color(0xFF263238),
                    textAlign = TextAlign.Companion.Center,
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .alpha(alphaAnim)
                        .padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.Companion.height(20.dp))

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

                Spacer(modifier = Modifier.Companion.height(20.dp))
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
                colors = ButtonDefaults.buttonColors(containerColor = Color.Companion.Transparent),
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .height(58.dp)
                    .alpha(alphaAnim)
                    .shadow(10.dp, androidx.compose.foundation.shape.RoundedCornerShape(40.dp))
                    .background(
                        brush = Brush.Companion.horizontalGradient(
                            listOf(Color(0xFF1976D2), Color(0xFF0D47A1))
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(40.dp)
                    )
            ) {
                Text(
                    text = "Continue →",
                    color = Color.Companion.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Companion.Bold
                )
            }

            Spacer(modifier = Modifier.Companion.height(24.dp))
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
        modifier = Modifier.Companion
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .alpha(alpha),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Companion.White.copy(alpha = 0.85f))
    ) {
        Row(
            verticalAlignment = Alignment.Companion.CenterVertically,
            modifier = Modifier.Companion.padding(16.dp)
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.Companion.size(100.dp)
            )
            Spacer(modifier = Modifier.Companion.width(16.dp))
            Column {
                Text(
                    text = titleEn,
                    fontWeight = FontWeight.Companion.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0D47A1)
                )
                Text(
                    text = descEn,
                    fontSize = 13.sp,
                    color = Color(0xFF37474F)
                )
                Spacer(modifier = Modifier.Companion.height(6.dp))
                Text(
                    text = titleMr,
                    fontWeight = FontWeight.Companion.Bold,
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