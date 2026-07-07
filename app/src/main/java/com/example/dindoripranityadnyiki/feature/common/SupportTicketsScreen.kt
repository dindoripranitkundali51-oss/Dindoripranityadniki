package com.example.dindoripranityadnyiki.feature.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.LocalAppLanguage
import com.example.dindoripranityadnyiki.core.design.DivineCard
import com.example.dindoripranityadnyiki.core.design.DivineScreen
import com.example.dindoripranityadnyiki.core.design.DivineTopBar
import com.example.dindoripranityadnyiki.core.design.DivineTypography
import com.example.dindoripranityadnyiki.core.design.SocialUi
import com.example.dindoripranityadnyiki.core.navigation.Routes
import java.time.Instant

@Composable
fun SupportTicketsScreen(
    navController: NavController,
    userRole: String,
    viewModel: SupportViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val isMarathi = LocalAppLanguage.current == "Marathi"
    val tickets by viewModel.tickets.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadTickets()
    }

    DivineScreen(
        topBar = {
            DivineTopBar(
                title = if (isMarathi) "माझी तिकिटे" else "My Tickets",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = SocialUi.TitleColor)
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.support(userRole)) }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            tickets.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.HelpOutline,
                        null,
                        modifier = Modifier.size(80.dp),
                        tint = SocialUi.ValueColor.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isMarathi) "अजून कोणतेही तिकीट नाही" else "No tickets yet",
                        style = DivineTypography.titleMedium,
                        color = SocialUi.TitleColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isMarathi) {
                            "नवीन तिकीट तयार करण्यासाठी वरील उजव्या बाजूचे मदत चिन्ह उघडा"
                        } else {
                            "Use the help button in the top right to create a new ticket"
                        },
                        style = DivineTypography.bodyMedium,
                        color = SocialUi.ValueColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tickets) { ticket ->
                        TicketCard(ticket = ticket, isMarathi = isMarathi)
                    }
                }
            }
        }
    }
}

@Composable
fun TicketCard(ticket: Map<String, Any>, isMarathi: Boolean) {
    val status = ticket["status"] as? String ?: "Open"
    val subject = ticket["subject"] as? String ?: ""
    val message = ticket["message"] as? String ?: ""
    val createdAtStr = ticket["createdAt"] as? String
    val createdAtMs = try {
        createdAtStr?.let { Instant.parse(it).toEpochMilli() }
    } catch (e: Exception) {
        null
    }
    val isFreshTicket = createdAtMs?.let {
        System.currentTimeMillis() - it <= 2 * 60 * 1000
    } == true

    val statusColor = when (status.lowercase()) {
        "open" -> MaterialTheme.colorScheme.primary
        "in progress" -> SocialUi.DeepOrange
        "resolved" -> SocialUi.SuccessGreen
        "closed" -> SocialUi.ValueColor
        else -> SocialUi.ValueColor
    }
    val statusLabel = when {
        isMarathi && status.equals("open", ignoreCase = true) -> "उघडे"
        isMarathi && status.equals("in progress", ignoreCase = true) -> "प्रगतीअधीन"
        isMarathi && status.equals("resolved", ignoreCase = true) -> "सोडवले"
        isMarathi && status.equals("closed", ignoreCase = true) -> "बंद"
        else -> status
    }

    DivineCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isFreshTicket) {
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = if (isMarathi) "नवीन तिकीट" else "New ticket",
                        style = DivineTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subject,
                    style = DivineTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = SocialUi.TitleColor
                )
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = DivineTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            if (message.isNotBlank()) {
                Text(
                    text = message,
                    style = DivineTypography.bodyMedium,
                    color = SocialUi.ValueColor,
                    maxLines = 3
                )
            }

            if (status.equals("resolved", ignoreCase = true)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        tint = SocialUi.SuccessGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isMarathi) "हे तिकीट सोडवले गेले आहे" else "This ticket has been resolved",
                        style = DivineTypography.bodySmall,
                        color = SocialUi.SuccessGreen
                    )
                }
            }
        }
    }
}
