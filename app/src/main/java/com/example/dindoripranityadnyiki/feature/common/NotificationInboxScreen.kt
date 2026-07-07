package com.example.dindoripranityadnyiki.feature.common

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import android.provider.Settings
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.core.data.AppLocaleStore
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import com.example.dindoripranityadnyiki.core.data.dataStore
import com.example.dindoripranityadnyiki.core.design.SocialUi
import com.example.dindoripranityadnyiki.core.navigation.Routes
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationInboxScreen(
    navController: NavController,
    viewModel: NotificationInboxViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val context = LocalContext.current
    val isMarathi = remember { AppLocaleStore.readLanguage(context) == "mr" }
    var userType by remember { mutableStateOf("user") }
    val notifications by viewModel.notifications.collectAsState(initial = emptyList())
    var permissionOk by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }

    LaunchedEffect(Unit) {
        userType = context.dataStore.data.first()[PrefKeys.USER_TYPE] ?: "user"
        viewModel.startPolling()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isMarathi) "नोटिफिकेशन्स" else "Notifications") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        },
        bottomBar = {
            NotificationBottomBar(navController = navController, userType = userType, isMarathi = isMarathi)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                permissionOk = NotificationManagerCompat.from(context).areNotificationsEnabled()
                if (!permissionOk) {
                    PermissionCard(
                        isMarathi = isMarathi,
                        onClick = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }

            if (notifications.isEmpty()) {
                item {
                    EmptyNotice(
                        message = if (isMarathi) "अजून नोटिफिकेशन्स नाहीत." else "No notifications yet."
                    )
                }
            } else {
                items(notifications, key = { it["id"]?.toString() ?: it.hashCode().toString() }) { item ->
                    NotificationRow(
                        item = item,
                        isMarathi = isMarathi,
                        onOpen = {
                            val id = (item["id"] as? Number)?.toInt()
                            if (id != null) {
                                viewModel.markAsRead(id)
                            }
                            notificationDestination(item)?.let { route -> navController.navigate(route) }
                        },
                        onMarkRead = {
                            val id = (item["id"] as? Number)?.toInt()
                            if (id != null) {
                                viewModel.markAsRead(id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationBottomBar(
    navController: NavController,
    userType: String,
    isMarathi: Boolean
) {
    val tabs = if (userType == "guruji") {
        listOf(
            BottomTab(Routes.GURUJI_DASHBOARD, if (isMarathi) "होम" else "Home", Icons.Default.Home),
            BottomTab(Routes.NOTIFICATION_INBOX, if (isMarathi) "सूचना" else "Notifications", Icons.Default.Notifications),
            BottomTab(Routes.GURUJI_WALLET, if (isMarathi) "मानधन" else "Mandhan", Icons.Default.AccountBalanceWallet)
        )
    } else {
        listOf(
            BottomTab(Routes.USER_HOME, if (isMarathi) "होम" else "Home", Icons.Default.Home),
            BottomTab(Routes.NOTIFICATION_INBOX, if (isMarathi) "सूचना" else "Notifications", Icons.Default.Notifications),
            BottomTab(Routes.USER_HISTORY, if (isMarathi) "इतिहास" else "History", Icons.Default.History)
        )
    }
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = tab.route == Routes.NOTIFICATION_INBOX,
                onClick = {
                    if (tab.route != Routes.NOTIFICATION_INBOX) {
                        navController.navigate(tab.route) { launchSingleTop = true }
                    }
                },
                icon = { Icon(tab.icon, null) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

@Composable
private fun EmptyNotice(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 56.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Notifications, contentDescription = message, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

@Composable
private fun NotificationPermissionRow(
    isMarathi: Boolean,
    onFix: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = if (isMarathi) "नोटिफिकेशन परवानगी बंद आहे" else "Notification permission is off",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = SocialUi.TitleColor)
            )
            Text(
                text = if (isMarathi) "OTP आणि booking updates लगेच दिसण्यासाठी ती सुरू करा." else "Turn it on so urgent OTP and booking updates appear instantly.",
                style = MaterialTheme.typography.bodySmall.copy(color = SocialUi.ValueColor),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            TextButton(onClick = onFix, contentPadding = PaddingValues(horizontal = 0.dp)) {
                Text(if (isMarathi) "सेटिंग्ज उघडा" else "Open Settings", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NotificationRow(
    item: Map<String, Any>,
    isMarathi: Boolean,
    onOpen: () -> Unit,
    onMarkRead: () -> Unit
) {
    val unread = item["read"] != true
    val priority = item["priority"] as? String ?: "normal"
    val color = if (priority == "high") MaterialTheme.colorScheme.primary else SocialUi.IconColor
    val destination = notificationDestination(item)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = color.copy(alpha = if (unread) 0.14f else 0.08f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Notifications, null, tint = color, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = localizedNotificationText(item["title"] as? String ?: "Notification", isMarathi),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold,
                    color = SocialUi.TitleColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = localizedNotificationText(item["body"] as? String ?: "", isMarathi),
                style = MaterialTheme.typography.bodySmall.copy(color = SocialUi.ValueColor),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${localizedDeliveryStatus(item["deliveryStatus"] as? String, isMarathi)} | ${localizedPriority(priority, isMarathi)}",
                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (destination != null) {
                    TextButton(onClick = onOpen, contentPadding = PaddingValues(horizontal = 0.dp)) {
                        Text(actionLabel(item, isMarathi), fontWeight = FontWeight.Bold)
                    }
                }
                if (unread) {
                    TextButton(onClick = onMarkRead, contentPadding = PaddingValues(horizontal = 0.dp)) {
                        Text(if (isMarathi) "वाचले" else "Mark read")
                    }
                }
            }
        }
    }
}



private fun notificationDestination(item: Map<String, Any>): String? {
    parseDeepLinkRoute(item["deepLink"] as? String)?.let { return it }
    val bookingId = (item["bookingId"] ?: item["booking_id"]) as? String ?: return null
    val action = ((item["action"] ?: item["type"] ?: item["route"]) as? String).orEmpty().uppercase()
    val role = ((item["role"] ?: item["targetRole"]) as? String).orEmpty().lowercase()
    return when {
        action.contains("PAY") -> Routes.poojaPayment(bookingId)
        action.contains("FEEDBACK") || action.contains("RATE") -> Routes.ratingFeedback(bookingId, item["gurujiName"] as? String ?: "")
        action.contains("RECEIPT") -> Routes.digitalReceipt(bookingId)
        action.contains("VERIFY") || action.contains("STATUS") -> Routes.bookingConfirmed(bookingId)
        role == "guruji" || action.contains("GURUJI") -> Routes.gurujiBookingDetails(bookingId)
        else -> Routes.bookingConfirmed(bookingId)
    }
}

private fun actionLabel(item: Map<String, Any>, isMarathi: Boolean): String {
    val action = ((item["action"] ?: item["type"] ?: item["route"]) as? String).orEmpty().uppercase()
    val deepLink = item["deepLink"] as? String
    return when {
        action.contains("PAY") -> if (isMarathi) "आता पेमेंट करा" else "Pay Now"
        action.contains("FEEDBACK") || action.contains("RATE") -> if (isMarathi) "अभिप्राय द्या" else "Give Feedback"
        action.contains("RECEIPT") -> if (isMarathi) "पावती पहा" else "View Receipt"
        action.contains("VERIFY") || action.contains("STATUS") -> if (isMarathi) "स्थिती पहा" else "View Status"
        deepLink?.contains("gurujiBookingDetails", ignoreCase = true) == true -> if (isMarathi) "सेवा उघडा" else "Open Seva"
        else -> if (isMarathi) "बुकिंग उघडा" else "Open Booking"
    }
}

private fun parseDeepLinkRoute(deepLink: String?): String? {
    if (deepLink.isNullOrBlank()) return null
    val uri = runCatching { deepLink.toUri() }.getOrNull() ?: return null
    if (uri.scheme != "dindori" || uri.host != "app") return null
    val segments = uri.pathSegments
    val screen = segments.getOrNull(0) ?: return null
    val id = segments.getOrNull(1)?.takeIf { it.isNotBlank() }
    return when (screen) {
        "poojaPayment" -> id?.let(Routes::poojaPayment)
        "digitalReceipt" -> id?.let(Routes::digitalReceipt)
        "ratingFeedback" -> id?.let { Routes.ratingFeedback(it, segments.getOrNull(2) ?: "Guruji") }
        "bookingConfirmed" -> id?.let(Routes::bookingConfirmed)
        "gurujiBookingDetails" -> id?.let(Routes::gurujiBookingDetails)
        "gurujiWallet" -> Routes.GURUJI_WALLET
        "notificationInbox" -> Routes.NOTIFICATION_INBOX
        else -> null
    }
}

private fun localizedDeliveryStatus(status: String?, isMarathi: Boolean): String {
    if (!isMarathi) return status ?: "-"
    return when (status) {
        "PushSent" -> "पाठवले"
        "InAppQueued", "InAppOnly" -> "अॅपमध्ये"
        "NoActiveAdmin" -> "प्रलंबित"
        else -> status ?: "-"
    }
}

private fun localizedPriority(priority: String, isMarathi: Boolean): String {
    if (!isMarathi) return priority.uppercase()
    return if (priority == "high") "तातडीचे" else "सामान्य"
}

private fun localizedNotificationText(text: String, isMarathi: Boolean): String {
    if (!isMarathi) return text
    return when (text) {
        "Notification" -> "नोटिफिकेशन"
        "Payment received" -> "पेमेंट प्राप्त झाले"
        "Your receipt is ready in the app." -> "à¤¤à¥à¤®à¤šà¥€ à¤ªà¤¾à¤µà¤¤à¥€ अॅपमध्ये à¤¤à¤¯à¤¾à¤° à¤†à¤¹à¥‡."
        "Dakshina credited" -> "दक्षिणा जमा झाली"
        "Booking created" -> "बुकिंग तयार झाले"
        "Your booking is assigned to a Guruji." -> "तुमचे बुकिंग गुरुजींना देण्यात आले आहे."
        "Your booking request is received." -> "तुमची बुकिंग विनंती प्राप्त झाली आहे."
        "New seva booking" -> "नवीन सेवा बुकिंग"
        "A new seva booking is assigned to you." -> "à¤¤à¥à¤®à¤šà¥à¤¯à¤¾à¤•à¤¡à¥‡ नवीन सेवा बुकिंग à¤†à¤²à¥‡ à¤†à¤¹à¥‡."
        "Completion OTP" -> "पूर्णता OTP"
        "Payment failed" -> "पेमेंट अयशस्वी"
        "Your payment could not be completed. Please try again from the app." -> "तुमचे पेमेंट पूर्ण झाले नाही. कृपया अॅपमधून पुन्हा प्रयत्न करा."
        "Payment status updated" -> "पेमेंट स्थिती अपडेट झाली"
        "Receipt sent again" -> "पावती पुन्हा पाठवली"
        "Your digital receipt is available again in the app." -> "à¤¤à¥à¤®à¤šà¥€ à¤¡à¤¿à¤œà¤¿à¤Ÿà¤² à¤ªà¤¾à¤µà¤¤à¥€ अॅपमध्ये à¤ªà¥à¤¨à¥à¤¹à¤¾ à¤‰à¤ªà¤²à¤¬à¥à¤§ à¤†à¤¹à¥‡."
        "Withdrawal request" -> "रक्कम काढण्याची विनंती"
        else -> text
    }
}

private fun priorityValue(item: Map<String, Any>): Int {
    val unreadBoost = if (item["read"] == true) 0 else 100
    val priority = if ((item["priority"] as? String) == "high") 50 else 0
    return unreadBoost + priority
}

private fun createdSeconds(item: Map<String, Any>): Long {
    val created = item["createdAt"]
    return when (created) {
        is String -> {
            try {
                java.time.Instant.parse(created).epochSecond
            } catch (e: Exception) {
                0L
            }
        }
        else -> 0L
    }
}

@Composable
fun PermissionCard(isMarathi: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isMarathi) "सूचना परवानगी नाकारली" else "Notification Permission Denied",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isMarathi) "कृपया सेटिंग्जमध्ये जाऊन नोटिफिकेशन्स सुरू करा जेणेकरून तुम्हाला अपडेट्स मिळतील." else "Please enable notifications in Settings to receive updates.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onClick) {
                Text(
                    text = if (isMarathi) "सेटिंग्ज उघडा" else "Open Settings",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

