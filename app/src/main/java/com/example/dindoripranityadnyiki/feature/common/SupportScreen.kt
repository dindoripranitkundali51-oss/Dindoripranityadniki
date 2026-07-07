package com.example.dindoripranityadnyiki.feature.common

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.LocalAppLanguage
import com.example.dindoripranityadnyiki.core.design.DivineCard
import com.example.dindoripranityadnyiki.core.design.DivineScreen
import com.example.dindoripranityadnyiki.core.design.DivineShapes
import com.example.dindoripranityadnyiki.core.design.DivineSpacing
import com.example.dindoripranityadnyiki.core.design.DivineTextField
import com.example.dindoripranityadnyiki.core.design.DivineTopBar
import com.example.dindoripranityadnyiki.core.design.DivineTypography
import com.example.dindoripranityadnyiki.core.design.SocialUi
import com.example.dindoripranityadnyiki.core.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    navController: NavController,
    userRole: String,
    viewModel: SupportViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val context = LocalContext.current
    val isMarathi = LocalAppLanguage.current == "Marathi"
    val scope = rememberCoroutineScope()

    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()
    val createSuccess by viewModel.createSuccess.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

    LaunchedEffect(createSuccess) {
        if (createSuccess) {
            Toast.makeText(
                context,
                if (isMarathi) "तुमची विनंती पाठवली गेली." else "Request sent.",
                Toast.LENGTH_SHORT
            ).show()
            viewModel.resetSuccess()
            navController.navigate(Routes.supportTickets(userRole)) {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    DivineScreen(
        topBar = {
            DivineTopBar(
                title = if (isMarathi) "मदत आणि पाठबळ" else "Support",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = SocialUi.TitleColor)
                    }
                },
                actions = {
                    TextButton(onClick = { navController.navigate(Routes.supportTickets(userRole)) }) {
                        Text(
                            if (isMarathi) "माझी तिकिटे" else "My Tickets",
                            fontWeight = FontWeight.Bold
                        )
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
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.HeadsetMic,
                        null,
                        modifier = Modifier.size(42.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (isMarathi) "आम्ही तुमची कशी मदत करू शकतो?" else "How can we help?",
                style = DivineTypography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = SocialUi.TitleColor
                ),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate(Routes.SUPPORT_CHAT) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Message, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isMarathi) "थेट चॅट सपोर्ट (Live Chat)" else "Live Chat Support",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(Modifier.height(16.dp))

            DivineTextField(
                value = subject,
                onValueChange = { subject = it },
                label = if (isMarathi) "विषय" else "Subject",
                icon = Icons.Default.Topic
            )

            Spacer(Modifier.height(14.dp))

            DivineTextField(
                value = message,
                onValueChange = { message = it },
                label = if (isMarathi) "तुमचा संदेश" else "Message",
                icon = Icons.AutoMirrored.Filled.Message,
                singleLine = false,
                minLines = 3,
                maxLines = 5
            )

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    if (subject.isBlank() || message.isBlank()) return@Button
                    viewModel.submitTicket(
                        subject = subject,
                        description = message,
                        category = "Support",
                        language = if (isMarathi) "mr" else "en"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isMarathi) "विनंती पाठवा" else "Send request",
                        style = DivineTypography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
            TextButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text(
                    text = if (isMarathi) "माझे खाते बंद करा" else "Request account deletion",
                    style = DivineTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Spacer(Modifier.height(DivineSpacing.Sacred))
        }

        if (showDeleteConfirm) {
            BasicAlertDialog(onDismissRequest = { showDeleteConfirm = false }) {
                DivineCard(modifier = Modifier.fillMaxWidth(0.92f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(DivineSpacing.Medium)) {
                        Text(
                            text = if (isMarathi) "खाते डिलीट करायचे?" else "Deletion Request",
                            style = DivineTypography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        )
                        Text(
                            text = if (isMarathi) {
                                "तुमचा सर्व डेटा कायमचा काढून टाकला जाईल. ही प्रक्रिया 7 दिवसांत पूर्ण होईल."
                            } else {
                                "Your data will be permanently removed. This process completes in 7 days."
                            },
                            style = DivineTypography.bodyLarge.copy(color = SocialUi.ValueColor)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(DivineSpacing.Medium)
                        ) {
                            OutlinedButton(
                                onClick = { showDeleteConfirm = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = DivineShapes.medium
                            ) {
                                Text(if (isMarathi) "रद्द करा" else "Cancel")
                            }
                            Button(
                                onClick = {
                                    viewModel.requestAccountDeletion()
                                    showDeleteConfirm = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = DivineShapes.medium,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(if (isMarathi) "हो, डिलीट करा" else "Confirm", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
