package com.example.dindoripranityadnyiki.core.design

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.LocalAppLanguage
import com.example.dindoripranityadnyiki.core.navigation.Routes

private data class BottomTabItem(
    val route: String,
    val labelMr: String,
    val labelEn: String,
    val icon: ImageVector
)

@Composable
fun UserBottomTabs(navController: NavController, selectedRoute: String) {
    AppBottomTabs(
        navController = navController,
        selectedRoute = selectedRoute,
        rootRoute = Routes.USER_HOME,
        tabs = listOf(
            BottomTabItem(Routes.USER_HOME, "होम", "Home", Icons.Default.Home),
            BottomTabItem(Routes.NOTIFICATION_INBOX, "सूचना", "Notifications", Icons.Default.Notifications),
            BottomTabItem(Routes.USER_HISTORY, "इतिहास", "History", Icons.Default.History),
        )
    )
}

@Composable
fun GurujiBottomTabs(navController: NavController, selectedRoute: String) {
    AppBottomTabs(
        navController = navController,
        selectedRoute = selectedRoute,
        rootRoute = Routes.GURUJI_DASHBOARD,
        tabs = listOf(
            BottomTabItem(Routes.GURUJI_DASHBOARD, "होम", "Home", Icons.Default.Home),
            BottomTabItem(Routes.NOTIFICATION_INBOX, "सूचना", "Notifications", Icons.Default.Notifications),
            BottomTabItem(Routes.GURUJI_WALLET, "मानधन", "Mandhan", Icons.Default.AccountBalanceWallet),
        )
    )
}

@Composable
private fun AppBottomTabs(
    navController: NavController,
    selectedRoute: String,
    rootRoute: String,
    tabs: List<BottomTabItem>
) {
    val isMarathi = LocalAppLanguage.current == "Marathi"
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        tabs.forEach { tab ->
            val selected = selectedRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(tab.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(rootRoute) {
                                saveState = true
                            }
                        }
                    }
                },
                icon = {
                    Icon(
                        tab.icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = if (isMarathi) tab.labelMr else tab.labelEn,
                        style = DivineTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
