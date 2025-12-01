package com.example.dindoripranityadnyiki.feature.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.R
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import com.example.dindoripranityadnyiki.core.data.dataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RoleSelectionScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Animations
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.9f) }
    val fadeCards = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            tween(8000, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = ""
    )

    val backgroundGradient = Brush.Companion.linearGradient(
        colors = listOf(
            Color(0xFFE3F2FD),
            Color(0xFFBBDEFB),
            Color(0xFF90CAF9)
        ),
        start = Offset(0f, offset),
        end = Offset(offset, 0f)
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = ""
    )

    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, tween(1000))
        logoScale.animateTo(1.05f, tween(1000))
        delay(700)
        fadeCards.animateTo(1f, tween(700))
    }

    Box(
        modifier = Modifier.Companion
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Companion.Center
    ) {
        Column(
            horizontalAlignment = Alignment.Companion.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.Companion
                .fillMaxHeight()
                .padding(vertical = 20.dp)
        ) {

            // 🔆 App Logo + Title
            Column(horizontalAlignment = Alignment.Companion.CenterHorizontally) {
                Box(
                    modifier = Modifier.Companion
                        .size(300.dp)
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                            alpha = logoAlpha.value
                        },
                    contentAlignment = Alignment.Companion.Center
                ) {
                    Box(
                        modifier = Modifier.Companion
                            .size(340.dp)
                            .background(
                                Brush.Companion.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFC107).copy(alpha = glowAlpha),
                                        Color.Companion.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                            .blur(80.dp)
                    )
                    Image(
                        painter = painterResource(id = R.drawable.app_role),
                        contentDescription = "App Logo",
                        modifier = Modifier.Companion
                            .size(260.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.Companion.height(16.dp))
                Text(
                    text = "Choose Your Role",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Companion.SemiBold,
                    color = Color(0xFF0D47A1),
                    textAlign = TextAlign.Companion.Center,
                    modifier = Modifier.Companion.alpha(logoAlpha.value)
                )
                Text(
                    text = "(Select how you wish to continue)",
                    fontSize = 14.sp,
                    color = Color(0xFF546E7A),
                    modifier = Modifier.Companion.alpha(logoAlpha.value)
                )
            }

            // 🔹 Role Buttons
            Column(
                horizontalAlignment = Alignment.Companion.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier.Companion
                    .alpha(fadeCards.value)
                    .padding(bottom = 30.dp)
            ) {
                RoleChoiceCardModernClean(
                    title = "USER",
                    subtitle = "Book Poojas & Manage Requests",
                    gradient = Brush.Companion.horizontalGradient(
                        listOf(Color(0xFF42A5F5), Color(0xFF1565C0))
                    ),
                    icon = R.drawable.ic_user,
                    onClick = {
                        scope.launch {
                            context.dataStore.edit {
                                it[PrefKeys.USER_ROLE] = "user"
                                it[PrefKeys.IS_REGISTERED] = false
                            }
                            navController.navigate("onboarding") {
                                popUpTo("roleSelection") { inclusive = true }
                            }
                        }
                    }
                )

                RoleChoiceCardModernClean(
                    title = "GURUJI",
                    subtitle = "Perform Poojas & Manage Bookings",
                    gradient = Brush.Companion.horizontalGradient(
                        listOf(Color(0xFFFFB74D), Color(0xFFF57C00))
                    ),
                    icon = R.drawable.ic_guruji,
                    onClick = {
                        scope.launch {
                            context.dataStore.edit {
                                it[PrefKeys.USER_ROLE] = "guruji"
                                it[PrefKeys.IS_REGISTERED] = false
                            }
                            navController.navigate("onboarding") {
                                popUpTo("roleSelection") { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
    }
}

// 🔸 Beautiful Card Component
@Composable
fun RoleChoiceCardModernClean(
    title: String,
    subtitle: String,
    gradient: Brush,
    icon: Int,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 10.dp,
        tonalElevation = 4.dp,
        modifier = Modifier.Companion
            .fillMaxWidth()
            .height(115.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.Companion
                .fillMaxSize()
                .background(gradient)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Companion.Center
        ) {
            Row(
                verticalAlignment = Alignment.Companion.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    modifier = Modifier.Companion.size(56.dp)
                )
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Companion.Bold,
                        fontSize = 20.sp,
                        color = Color.Companion.White
                    )
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = Color.Companion.White.copy(alpha = 0.92f)
                    )
                }
            }
        }
    }
}