package com.example.dindoripranityadnyiki.core.design

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared status screen layout.
 * Transitions between states using cinematic cross-fades and vertical offsets.
 * Designed to elevate the user's emotional state during success, error, or empty states.
 */
@Composable
fun DivineStatusScreen(
    icon: ImageVector,
    title: String,
    description: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(DivineSpacing.ExtraLarge),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                color = color.copy(alpha = 0.05f),
                shape = DivineShapes.extraLarge
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(32.dp).fillMaxSize(),
                    tint = color
                )
            }
            
            Spacer(Modifier.height(DivineSpacing.Large))
            
            Text(
                text = title,
                style = DivineTypography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(Modifier.height(DivineSpacing.Small))
            
            Text(
                text = description,
                style = DivineTypography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    lineHeight = 22.sp
                ),
                textAlign = TextAlign.Center
            )
            
            if (actionText != null && onAction != null) {
                Spacer(Modifier.height(DivineSpacing.ExtraLarge))
                Button(
                    onClick = onAction,
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth(0.8f),
                    shape = DivineShapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Text(
                        text = actionText,
                        style = DivineTypography.labelMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}
