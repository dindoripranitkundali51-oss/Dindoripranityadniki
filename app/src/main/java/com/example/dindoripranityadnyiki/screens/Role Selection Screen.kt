package com.example.dindoripranityadnyiki.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.R
import androidx.datastore.preferences.core.edit
import com.example.dindoripranityadnyiki.data.PrefKeys
import com.example.dindoripranityadnyiki.data.dataStore
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

    val backgroundGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFE3F2FD),
            Color(0xFFBBDEFB),
            Color(0xFF90CAF9)
        ),
        start = androidx.compose.ui.geometry.Offset(0f, offset),
        end = androidx.compose.ui.geometry.Offset(offset, 0f)
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
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 20.dp)
        ) {

            // 🔆 App Logo + Title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                            alpha = logoAlpha.value
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(340.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFC107).copy(alpha = glowAlpha),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                            .blur(80.dp)
                    )
                    Image(
                        painter = painterResource(id = R.drawable.app_role),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(260.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Choose Your Role",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0D47A1),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(logoAlpha.value)
                )
                Text(
                    text = "(Select how you wish to continue)",
                    fontSize = 14.sp,
                    color = Color(0xFF546E7A),
                    modifier = Modifier.alpha(logoAlpha.value)
                )
            }

            // 🔹 Role Buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier
                    .alpha(fadeCards.value)
                    .padding(bottom = 30.dp)
            ) {
                RoleChoiceCardModernClean(
                    title = "USER",
                    subtitle = "Book Poojas & Manage Requests",
                    gradient = Brush.horizontalGradient(
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
                    gradient = Brush.horizontalGradient(
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
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp)
                )
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }
            }
        }
    }
}
