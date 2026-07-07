package com.example.dindoripranityadnyiki.feature.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.LocalAppLanguage
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import com.example.dindoripranityadnyiki.core.data.dataStore
import com.example.dindoripranityadnyiki.core.design.DivineTypography
import com.example.dindoripranityadnyiki.core.design.UserBottomTabs
import com.example.dindoripranityadnyiki.core.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHomeScreen(
    navController: NavController,
    viewModel: UserHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isMarathi = LocalAppLanguage.current == "Marathi"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    val sacredCopper = MaterialTheme.colorScheme.primary
    val snackbarHostState = remember { SnackbarHostState() }

    val handleLogout = {
        viewModel.logout(context) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    LaunchedEffect(uiState.isBlocked) {
        if (uiState.isBlocked) handleLogout()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
        bottomBar = { UserBottomTabs(navController, Routes.USER_HOME) },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 4.dp, top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = if (isMarathi) "मेनू" else "Menu",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isMarathi) "English मध्ये बदला" else "Switch to Marathi") },
                                    onClick = {
                                        showMenu = false
                                        scope.launch {
                                            context.dataStore.edit {
                                                it[PrefKeys.LANGUAGE] = if (isMarathi) "en" else "mr"
                                            }
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.Language, null, tint = sacredCopper) }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isMarathi) "मदत आणि सपोर्ट" else "Help & Support") },
                                    onClick = {
                                        showMenu = false
                                        navController.navigate(Routes.support("user"))
                                    },
                                    leadingIcon = { Icon(Icons.Default.HeadsetMic, null, tint = sacredCopper) }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isMarathi) "लॉगआउट" else "Logout") },
                                    onClick = {
                                        showMenu = false
                                        handleLogout()
                                    },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.Red) }
                                )
                            }
                        }
                        Text(
                            text = if (isMarathi) "दिंडोरी प्रणित याज्ञिकी" else "Dindori Pranit Yadnyiki",
                            style = DivineTypography.headlineSmall.copy(
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Black,
                                fontSize = 19.sp
                            ),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        IconButton(onClick = { navController.navigate(Routes.USER_PROFILE) }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = if (isMarathi) "प्रोफाईल" else "Profile", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = sacredCopper)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            item { Spacer(Modifier.height(24.dp)) }

            if (uiState.upcomingBooking != null) {
                item {
                    SectionHeader(if (isMarathi) "चालू कृती" else "Current Action")
                    uiState.upcomingBooking?.let { booking ->
                        ActiveServiceCard(booking, isMarathi, navController)
                    }
                }
            }

            if (uiState.upcomingBooking == null) {
                item {
                    SectionHeader(if (isMarathi) "पुढील काम" else "Suggested Next Step")
                    UserNextActionCard(isMarathi) {
                        navController.navigate(Routes.POOJA_SELECTION)
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
