package com.example.dindoripranityadnyiki.feature.user

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.LocalAppLanguage
import com.example.dindoripranityadnyiki.core.design.DivineMotion
import com.example.dindoripranityadnyiki.core.design.DivineScreen
import com.example.dindoripranityadnyiki.core.design.DivineShapes
import com.example.dindoripranityadnyiki.core.design.DivineTextField
import com.example.dindoripranityadnyiki.core.design.DivineTopBar
import com.example.dindoripranityadnyiki.core.design.DivineTypography
import com.example.dindoripranityadnyiki.core.design.SocialUi
import com.example.dindoripranityadnyiki.core.navigation.Routes
import com.google.firebase.functions.FirebaseFunctions

@Composable
fun RatingFeedbackScreen(
    navController: NavController,
    bookingId: String,
    gurujiName: String
) {
    val context = LocalContext.current
    val isMarathi = LocalAppLanguage.current == "Marathi"
    val functions = remember { FirebaseFunctions.getInstance() }
    val displayGurujiName = gurujiName.ifBlank { if (isMarathi) "\u0917\u0941\u0930\u0941\u091c\u0940" else "Guruji" }

    var rating by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    val trimmedFeedback = feedback.trim()
    val minCommentLength = if (rating in 1..3) 30 else 10
    val isFeedbackValid = rating > 0 && trimmedFeedback.length >= minCommentLength

    DivineScreen(
        topBar = {
            DivineTopBar(
                title = if (isMarathi) "\u0905\u092d\u093f\u092a\u094d\u0930\u093e\u092f" else "Feedback",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = SocialUi.TitleColor)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))

            Surface(
                modifier = Modifier.size(88.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (isMarathi) "$displayGurujiName \u0938\u094b\u092c\u0924 \u0905\u0928\u0941\u092d\u0935 \u0915\u0938\u093e \u0939\u094b\u0924\u093e?" else "How was your experience with $displayGurujiName?",
                style = DivineTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold, color = SocialUi.TitleColor),
                textAlign = TextAlign.Center
            )
            Text(
                text = if (isMarathi) "\u0930\u0947\u091f\u093f\u0902\u0917 \u0906\u0923\u093f \u0925\u094b\u0921\u0915\u094d\u092f\u093e\u0924 \u0905\u092d\u093f\u092a\u094d\u0930\u093e\u092f \u0926\u094d\u092f\u093e." else "Give a rating and short review.",
                style = DivineTypography.bodySmall.copy(color = SocialUi.ValueColor),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(5) { index ->
                    val starIndex = index + 1
                    val isSelected = starIndex <= rating
                    val starScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.16f else 1.0f,
                        animationSpec = DivineMotion.snappySpring(),
                        label = "star_scale"
                    )
                    val starAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.35f,
                        label = "star_alpha"
                    )

                    Icon(
                        imageVector = if (isSelected) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else SocialUi.ValueColor,
                        modifier = Modifier
                            .size(48.dp)
                            .graphicsLayer {
                                scaleX = starScale
                                scaleY = starScale
                                alpha = starAlpha
                            }
                            .clip(CircleShape)
                            .clickable { if (!isSubmitting) rating = starIndex }
                    )
                }
            }

            AnimatedVisibility(
                visible = rating > 0,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val ratingLabel = when (rating) {
                    1 -> if (isMarathi) "\u0938\u0941\u0927\u093e\u0930\u0923\u0947\u091a\u0940 \u0917\u0930\u091c" else "Requires improvement"
                    2 -> if (isMarathi) "\u0920\u0940\u0915" else "Fair"
                    3 -> if (isMarathi) "\u091a\u093e\u0902\u0917\u0932\u0947" else "Good"
                    4 -> if (isMarathi) "\u0916\u0942\u092a \u091a\u093e\u0902\u0917\u0932\u0947" else "Very good"
                    else -> if (isMarathi) "\u0909\u0924\u094d\u0915\u0943\u0937\u094d\u091f" else "Excellent"
                }
                Text(
                    text = ratingLabel,
                    style = DivineTypography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(Modifier.height(28.dp))
            DivineTextField(
                value = feedback,
                onValueChange = { feedback = it },
                label = if (isMarathi) "\u0924\u0941\u092e\u091a\u093e \u0905\u0928\u0941\u092d\u0935 \u0932\u093f\u0939\u093e" else "Write your experience",
                icon = Icons.Default.Star,
                singleLine = false,
                minLines = 3,
                maxLines = 5
            )
            Text(
                text = if (rating in 1..3) {
                    if (isMarathi) "\u0915\u0943\u092a\u092f\u093e \u0938\u0935\u093f\u0938\u094d\u0924\u0930 comment \u0932\u093f\u0939\u093e. \u0915\u093f\u092e\u093e\u0928 30 \u0905\u0915\u094d\u0937\u0930\u0947." else "Please write a detailed comment. Minimum 30 characters."
                } else {
                    if (isMarathi) "\u0915\u0943\u092a\u092f\u093e \u091b\u094b\u091f\u093e comment \u0932\u093f\u0939\u093e. \u0915\u093f\u092e\u093e\u0928 10 \u0905\u0915\u094d\u0937\u0930\u0947." else "Please write a short comment. Minimum 10 characters."
                },
                style = DivineTypography.labelSmall.copy(color = SocialUi.ValueColor),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    if (!isFeedbackValid) {
                        Toast.makeText(
                            context,
                            if (isMarathi) "\u0930\u0947\u091f\u093f\u0902\u0917 \u0906\u0923\u093f comment \u0926\u094b\u0928\u094d\u0939\u0940 \u0906\u0935\u0936\u094d\u092f\u0915 \u0906\u0939\u0947\u0924." else "Rating and comment are required.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }
                    isSubmitting = true
                    functions.getHttpsCallable("submitFeedback").call(
                        mapOf(
                            "bookingId" to bookingId,
                            "rating" to rating,
                            "review" to trimmedFeedback
                        )
                    ).addOnSuccessListener {
                        Toast.makeText(context, if (isMarathi) "\u0905\u092d\u093f\u092a\u094d\u0930\u093e\u092f \u0928\u094b\u0902\u0926\u0935\u0932\u093e, \u0927\u0928\u094d\u092f\u0935\u093e\u0926!" else "Feedback submitted. Thank you.", Toast.LENGTH_SHORT).show()
                        navController.navigate(Routes.USER_HOME) {
                            popUpTo(0) { inclusive = true }
                        }
                    }.addOnFailureListener {
                        isSubmitting = false
                        Toast.makeText(context, "Error: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = DivineShapes.medium,
                enabled = !isSubmitting && isFeedbackValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = if (isMarathi) "\u0905\u092d\u093f\u092a\u094d\u0930\u093e\u092f \u0938\u092c\u092e\u093f\u091f \u0915\u0930\u093e" else "Submit feedback",
                        style = DivineTypography.labelMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
