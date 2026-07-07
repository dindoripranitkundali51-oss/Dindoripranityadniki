package com.example.dindoripranityadnyiki.feature.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.R
import com.example.dindoripranityadnyiki.core.design.*
import com.example.dindoripranityadnyiki.core.navigation.Routes
import com.example.dindoripranityadnyiki.core.util.SessionManager
import com.example.dindoripranityadnyiki.core.util.SessionState
import kotlinx.coroutines.delay

@Composable
fun AppSplashScreen(navController: NavController) {
    val scale = remember { Animatable(0.8f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1200, easing = DivineMotion.StandardEasing)
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
        
        delay(2200)

        when (val session = SessionManager.resolveSession(navController.context)) {
            is SessionState.LoggedIn -> {
                if (session.userType == "guruji") {
                    navController.navigate(Routes.GURUJI_DASHBOARD) { popUpTo(Routes.SPLASH) { inclusive = true } }
                } else {
                    navController.navigate(Routes.USER_HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
                }
            }
            SessionState.NotLoggedIn -> {
                navController.navigate(Routes.LOGIN) { popUpTo(Routes.SPLASH) { inclusive = true } }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier
                    .size(200.dp)
                    .scale(scale.value)
                    .alpha(alpha.value),
                shape = DivineShapes.extraLarge,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(120.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(DivineSpacing.ExtraLarge))
            
            Text(
                text = "दिंडोरी प्रणित यज्ञिकी",
                style = DivineTypography.displayLarge.copy(
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.alpha(alpha.value)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = DivineSpacing.Sacred)
        ) {
            Text(
                text = "YADNYIKI",
                style = DivineTypography.labelMedium.copy(
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                ),
                modifier = Modifier.alpha(alpha.value)
            )
        }
    }
}
