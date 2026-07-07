package com.example.dindoripranityadnyiki.feature.common

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dindoripranityadnyiki.LocalAppLanguage
import com.example.dindoripranityadnyiki.core.design.*

@Composable
fun PremiumAuthHeader(
    title: String,
    subtitle: String,
    chipText: String,
    modifier: Modifier = Modifier,
    showBack: Boolean = false,
    onBack: (() -> Unit)? = null,
    onChipClick: (() -> Unit)? = null,
    showLogo: Boolean = false,
    showChip: Boolean = true,
    height: Int = 160,
    curveStart: Boolean = true
) {
    val cornerShape = RoundedCornerShape(
        bottomStart = if (curveStart) 40.dp else 0.dp,
        bottomEnd = if (!curveStart) 40.dp else 0.dp
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .shadow(4.dp, cornerShape)
            .clip(cornerShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(SacredCopperLight, SacredCopper)
                )
            )
    ) {
        if (showChip && onChipClick != null) {
            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .statusBarsPadding()
                    .clickable { onChipClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Language, null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Text(chipText, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        if (showBack && onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart).padding(top = 8.dp, start = 4.dp).statusBarsPadding()
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )
            
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun PremiumAuthButton(
    text: String,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SacredCopper,
            contentColor = Color.White
        ),
        enabled = enabled && !loading
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp
            )
        }
    }
}

@Composable
fun SecondaryAuthButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, DivineTeal)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Icon(icon, null, modifier = Modifier.size(18.dp))
            Text(text, color = DivineTeal, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun PremiumAuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    DivineTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        icon = icon,
        isPassword = isPassword,
        keyboardOptions = keyboardOptions
    )
}

@Composable
fun DivineForgotPasswordDialog(
    onDismiss: () -> Unit,
    onReset: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    val isMarathi = LocalAppLanguage.current == "Marathi"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isMarathi) "पासवर्ड विसरलात?" else "Forgot Password?",
                style = DivineTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold, color = SocialUi.TitleColor)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isMarathi) 
                        "तुमचा नोंदणीकृत ईमेल किंवा मोबाईल नंबर टाका. आम्ही तुम्हाला पासवर्ड रीसेट करण्यासाठी लिंक पाठवू." 
                        else "Enter your registered email or mobile number. We will send you a reset link.",
                    style = DivineTypography.bodyMedium.copy(color = SocialUi.ValueColor)
                )
                DivineTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = if (isMarathi) "ईमेल किंवा मोबाईल" else "Email or Mobile",
                    icon = Icons.Default.Person
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onReset(input) }
            ) {
                Text(
                    text = if (isMarathi) "रीसेट करा" else "Reset",
                    color = SacredCopper,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (isMarathi) "रद्द करा" else "Cancel",
                    color = SocialUi.ValueColor
                )
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}
