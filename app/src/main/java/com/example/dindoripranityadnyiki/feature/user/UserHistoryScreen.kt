package com.example.dindoripranityadnyiki.feature.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.LocalAppLanguage
import com.example.dindoripranityadnyiki.core.design.DivineTopBar
import com.example.dindoripranityadnyiki.core.design.DivineTypography
import com.example.dindoripranityadnyiki.core.design.UserBottomTabs
import com.example.dindoripranityadnyiki.core.navigation.Routes

@Composable
fun UserHistoryScreen(
    navController: NavController,
    viewModel: UserHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isMarathi = LocalAppLanguage.current == "Marathi"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            DivineTopBar(
                title = if (isMarathi) "बुकिंग इतिहास" else "Booking History",
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(Routes.USER_HOME) { launchSingleTop = true } }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        bottomBar = {
            UserBottomTabs(navController, Routes.USER_HISTORY)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            uiState.pastBookings.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.History, null, tint = Color.Gray.copy(alpha = 0.45f))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (isMarathi) "अजून कोणतीही बुकिंग दिसत नाही." else "No bookings yet.",
                        style = DivineTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (isMarathi) "तुमच्या अलीकडील सेवा अपडेट्स" else "Your recent seva updates",
                            style = DivineTypography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    items(uiState.pastBookings) { booking ->
                        HistoryItem(booking, isMarathi, navController)
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}
