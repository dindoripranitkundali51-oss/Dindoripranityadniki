package com.example.dindoripranityadnyiki.feature.guruji

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.LocalAppLanguage
import com.example.dindoripranityadnyiki.core.design.DivineCard
import com.example.dindoripranityadnyiki.core.design.DivineScreen
import com.example.dindoripranityadnyiki.core.design.DivineTextField
import com.example.dindoripranityadnyiki.core.design.DivineTopBar
import com.example.dindoripranityadnyiki.core.design.DivineTypography
import com.example.dindoripranityadnyiki.core.design.GurujiBottomTabs
import com.example.dindoripranityadnyiki.core.design.ShimmerLoadingEffect
import com.example.dindoripranityadnyiki.core.design.SocialEmptyState
import com.example.dindoripranityadnyiki.core.design.SocialUi
import com.example.dindoripranityadnyiki.core.navigation.Routes
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun GurujiWalletScreen(
    navController: NavController,
    viewModel: GurujiWalletViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isMarathi = LocalAppLanguage.current == "Marathi"
    var showWithdrawDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val sacredCopper = MaterialTheme.colorScheme.primary

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            if (uiState.withdrawalSuccess) showWithdrawDialog = false
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    DivineScreen(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { GurujiBottomTabs(navController, Routes.GURUJI_WALLET) },
        topBar = {
            DivineTopBar(
                title = if (isMarathi) "मानधन" else "Wallet",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color(0xFF202020))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                WalletBalanceHeader(
                    balance = uiState.balance,
                    isMarathi = isMarathi,
                    onWithdraw = { showWithdrawDialog = true },
                    enabled = uiState.balance > 0
                )
            }

            item {
                val credited = uiState.transactions
                    .filter { (it["type"] as? String ?: "CREDIT") == "CREDIT" }
                    .sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 }
                    .ifZero(uiState.totalEarnings)
                val withdrawals = uiState.transactions
                    .filter { (it["type"] as? String ?: "").contains("WITHDRAWAL") }
                    .sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 }

                WalletMetricRow(Icons.Default.Savings, if (isMarathi) "शिल्लक" else "Balance", money(uiState.balance), sacredCopper)
                WalletMetricRow(Icons.AutoMirrored.Filled.TrendingUp, if (isMarathi) "जमा" else "Credits", money(credited), Color(0xFF008069))
                WalletMetricRow(Icons.Default.PendingActions, if (isMarathi) "प्रलंबित" else "Pending", money(uiState.pendingWithdrawal), Color(0xFFE65100))
                WalletMetricRow(Icons.AutoMirrored.Filled.CallMade, if (isMarathi) "विनंत्या" else "Requested", money(withdrawals), Color(0xFFDC2626))
            }

            item {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = if (isMarathi) "व्यवहार" else "Transactions",
                    style = DivineTypography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = SocialUi.TitleColor
                    ),
                    modifier = Modifier.padding(horizontal = SocialUi.ScreenHorizontal)
                )
                Spacer(Modifier.height(8.dp))
            }

            when {
                uiState.isLoading -> {
                    items(4) {
                        ShimmerLoadingEffect(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .padding(horizontal = SocialUi.ScreenHorizontal, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                uiState.transactions.isEmpty() -> {
                    item {
                        SocialEmptyState(
                            message = if (isMarathi) "अजून कोणतेही व्यवहार नाहीत." else "No transactions yet.",
                            icon = Icons.Default.History
                        )
                    }
                }
                else -> {
                    items(uiState.transactions, key = { it["id"]?.toString() ?: it.hashCode().toString() }) { tx ->
                        WalletTransactionRow(
                            tx = tx,
                            isMarathi = isMarathi,
                            highlightRecentWithdrawal = uiState.withdrawalSuccess
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(28.dp)) }
        }

        if (showWithdrawDialog) {
            WithdrawalDialog(
                isMarathi = isMarathi,
                max = uiState.balance,
                isLoading = uiState.isWithdrawing,
                onDismiss = { showWithdrawDialog = false },
                onConfirm = { amount, accountHolder, accountNumber, ifsc, upiId ->
                    viewModel.requestWithdrawal(amount, accountHolder, accountNumber, ifsc, upiId)
                },
                sacredCopper = sacredCopper
            )
        }
    }
}

@Composable
private fun WalletBalanceHeader(
    balance: Double,
    isMarathi: Boolean,
    onWithdraw: () -> Unit,
    enabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 26.dp, bottom = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(86.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(42.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = money(balance),
            style = DivineTypography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                color = SocialUi.TitleColor
            ),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (isMarathi) "उपलब्ध शिल्लक" else "Available balance",
            style = DivineTypography.bodyMedium.copy(color = SocialUi.ValueColor)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (isMarathi) "रक्कम काढण्याची विनंती" else "Request Withdrawal",
            style = DivineTypography.bodyMedium.copy(
                color = if (enabled) SocialUi.SuccessGreen else SocialUi.ValueColor,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.clickable(enabled = enabled) { onWithdraw() }
        )
    }
}

@Composable
private fun WalletMetricRow(icon: ImageVector, label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SocialUi.RowHorizontal, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, tint = SocialUi.IconColor, modifier = Modifier.size(SocialUi.IconSize).padding(top = 2.dp))
        Spacer(Modifier.size(SocialUi.IconGap))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = DivineTypography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = SocialUi.TitleColor
                )
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = DivineTypography.bodyMedium.copy(color = color, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WalletTransactionRow(
    tx: Map<String, Any>,
    isMarathi: Boolean,
    highlightRecentWithdrawal: Boolean
) {
    val amount = (tx["amount"] as? Number)?.toDouble() ?: 0.0
    val type = tx["type"] as? String ?: "CREDIT"
    val isWithdrawal = type.contains("WITHDRAWAL")
    val status = tx["status"] as? String ?: if (isWithdrawal) "Pending" else "Success"
    val poojaName = tx["poojaName"] as? String ?: if (isWithdrawal) {
        if (isMarathi) "रक्कम काढण्याची विनंती" else "Withdrawal Request"
    } else {
        if (isMarathi) "सेवा दक्षिणा" else "Seva Credit"
    }
    val accent = if (isWithdrawal) Color(0xFFDC2626) else SocialUi.SuccessGreen
    val meta = listOfNotNull(formatLedgerDate(tx), status).joinToString(" | ")
    val createdAt = tx["createdAt"] as? Timestamp
    val isFreshWithdrawal = highlightRecentWithdrawal &&
        isWithdrawal &&
        (createdAt?.let { System.currentTimeMillis() - it.toDate().time <= 5 * 60 * 1000 } == true)

    Surface(
        modifier = Modifier.padding(horizontal = 10.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isFreshWithdrawal) accent.copy(alpha = 0.08f) else Color.Transparent,
        border = if (isFreshWithdrawal) androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.24f)) else null
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
                color = accent.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isWithdrawal) Icons.AutoMirrored.Filled.CallMade else Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                if (isFreshWithdrawal) {
                    Text(
                        text = if (isMarathi) "नवीन विनंती" else "New request",
                        style = DivineTypography.labelSmall.copy(fontWeight = FontWeight.Bold, color = accent)
                    )
                }
                Text(
                    text = poojaName,
                    style = DivineTypography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = SocialUi.TitleColor
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = meta,
                    style = DivineTypography.bodySmall.copy(color = SocialUi.ValueColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${if (isWithdrawal) "-" else "+"}${money(amount)}",
                style = DivineTypography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = accent),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun WithdrawalDialog(
    isMarathi: Boolean,
    max: Double,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String, String, String) -> Unit,
    sacredCopper: Color
) {
    var amt by remember { mutableStateOf("") }
    var accountHolder by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var ifsc by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    val amount = amt.toDoubleOrNull() ?: 0.0
    val hasUpi = upiId.trim().matches(Regex("^[\\w.-]+@[\\w.-]+$"))
    val hasBank = accountHolder.trim().length >= 3 &&
        accountNumber.trim().matches(Regex("^\\d{9,18}$")) &&
        ifsc.trim().uppercase().matches(Regex("^[A-Z]{4}0[A-Z0-9]{6}$"))
    val isValid = amount >= 500.0 && amount <= max && (hasUpi || hasBank)

    Dialog(onDismissRequest = onDismiss) {
        DivineCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = if (isMarathi) "रक्कम काढण्याची विनंती" else "Withdrawal Request",
                    style = DivineTypography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (isMarathi) "उपलब्ध: ${money(max)}" else "Available: ${money(max)}",
                    style = DivineTypography.bodySmall.copy(color = SocialUi.ValueColor)
                )
                DivineTextField(
                    value = amt,
                    onValueChange = {
                        val cleaned = it.filter { char -> char.isDigit() || char == '.' }
                        amt = if (cleaned.count { char -> char == '.' } <= 1) cleaned.take(10) else amt
                    },
                    label = if (isMarathi) "रक्कम" else "Amount",
                    icon = Icons.Default.CurrencyRupee
                )
                DivineTextField(
                    value = upiId,
                    onValueChange = { upiId = it.trim().take(80) },
                    label = "UPI ID",
                    icon = Icons.Default.QrCode
                )
                DivineTextField(
                    value = accountHolder,
                    onValueChange = { accountHolder = it.take(80) },
                    label = if (isMarathi) "खातेदाराचे नाव" else "Account Holder Name",
                    icon = Icons.Default.Person
                )
                DivineTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it.filter { char -> char.isDigit() }.take(18) },
                    label = if (isMarathi) "खाते क्रमांक" else "Account Number",
                    icon = Icons.Default.Numbers
                )
                DivineTextField(
                    value = ifsc,
                    onValueChange = { ifsc = it.uppercase().filter { char -> char.isLetterOrDigit() }.take(11) },
                    label = if (isMarathi) "IFSC कोड" else "IFSC Code",
                    icon = Icons.Default.AccountBalance
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), enabled = !isLoading) {
                        Text(if (isMarathi) "रद्द" else "Cancel")
                    }
                    Button(
                        onClick = { onConfirm(amount, accountHolder, accountNumber, ifsc, upiId) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = sacredCopper),
                        enabled = !isLoading && isValid
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text(if (isMarathi) "विनंती" else "Request")
                        }
                    }
                }
            }
        }
    }
}

private fun money(value: Double): String {
    return "₹${String.format(Locale.getDefault(), "%.2f", value)}"
}

private fun Double.ifZero(fallback: Double): Double = if (this == 0.0) fallback else this

private fun formatLedgerDate(tx: Map<String, Any>): String? {
    val timestamp = tx["timestamp"] as? Timestamp ?: tx["createdAt"] as? Timestamp ?: return null
    return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(timestamp.toDate())
}
