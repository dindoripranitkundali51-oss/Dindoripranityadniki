package com.example.dindoripranityadnyiki.feature.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.LocalAppLanguage
import com.example.dindoripranityadnyiki.core.design.DivineScreen
import com.example.dindoripranityadnyiki.core.design.DivineTypography
import com.example.dindoripranityadnyiki.core.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    navController: NavController,
    viewModel: UserProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isMarathi = LocalAppLanguage.current == "Marathi"
    val sacredCopper = MaterialTheme.colorScheme.primary
    val whatsappGreen = Color(0xFF008069)

    DivineScreen(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color(0xFF202020))
                    }
                    Text(
                        text = if (isMarathi) "प्रोफाइल" else "Profile",
                        style = DivineTypography.headlineSmall.copy(
                            color = Color(0xFF202020),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp
                        ),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start
                    )
                    Spacer(Modifier.width(48.dp))
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = sacredCopper)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(28.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(128.dp),
                        shape = CircleShape,
                        color = sacredCopper.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = if (isMarathi) "प्रोफाइल फोटो" else "Profile photo",
                                tint = sacredCopper,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = if (isMarathi) "बदला" else "Edit",
                        style = DivineTypography.bodyMedium.copy(
                            color = whatsappGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Spacer(Modifier.height(48.dp))

                ProfileInfoRow(
                    label = if (isMarathi) "नाव" else "Name",
                    value = uiState.fullName.ifBlank { if (isMarathi) "यजमान" else "User" },
                    icon = Icons.Default.Person
                )
                ProfileInfoRow(
                    label = if (isMarathi) "फोन" else "Phone",
                    value = uiState.mobile,
                    icon = Icons.Default.PhoneAndroid
                )
                ProfileInfoRow(
                    label = if (isMarathi) "ईमेल" else "Email",
                    value = uiState.email.ifEmpty { if (isMarathi) "दिलेला नाही" else "Not Provided" },
                    icon = Icons.Default.AlternateEmail
                )
                ProfileInfoRow(
                    label = if (isMarathi) "पत्ता" else "Address",
                    value = uiState.address,
                    icon = Icons.Default.HomeWork
                )
                LegalLinkRow(
                    label = "Privacy Policy",
                    icon = Icons.Default.Security,
                    onClick = { navController.navigate(Routes.legalInfo("privacy")) }
                )
                LegalLinkRow(
                    label = "Terms of Service",
                    icon = Icons.Default.Description,
                    onClick = { navController.navigate(Routes.legalInfo("terms")) }
                )

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun LegalLinkRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    val whatsappGreen = Color(0xFF008069)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 28.dp, vertical = 18.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF707070),
            modifier = Modifier.size(24.dp).padding(top = 2.dp)
        )
        Spacer(Modifier.width(28.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = DivineTypography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF202020)
                )
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Open",
                style = DivineTypography.bodyMedium.copy(color = whatsappGreen)
            )
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 18.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF707070),
            modifier = Modifier.size(24.dp).padding(top = 2.dp)
        )
        Spacer(Modifier.width(28.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = DivineTypography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF202020)
                )
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = DivineTypography.bodyMedium.copy(color = Color(0xFF6F6F6F)),
                lineHeight = 20.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
