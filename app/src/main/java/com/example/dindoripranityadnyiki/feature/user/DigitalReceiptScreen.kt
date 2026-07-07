package com.example.dindoripranityadnyiki.feature.user

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.LocalAppLanguage
import com.example.dindoripranityadnyiki.R
import com.example.dindoripranityadnyiki.core.design.DivineCard
import com.example.dindoripranityadnyiki.core.design.DivineScreen
import com.example.dindoripranityadnyiki.core.design.DivineShapes
import com.example.dindoripranityadnyiki.core.design.DivineSpacing
import com.example.dindoripranityadnyiki.core.design.DivineStatusScreen
import com.example.dindoripranityadnyiki.core.design.DivineTopBar
import com.example.dindoripranityadnyiki.core.design.DivineTypography
import com.example.dindoripranityadnyiki.core.design.SocialUi
import com.example.dindoripranityadnyiki.core.navigation.Routes
import com.example.dindoripranityadnyiki.core.service.PdfService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalReceiptScreen(
    bookingId: String,
    navController: NavController,
    viewModel: DigitalReceiptViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val context = LocalContext.current
    val isMarathi = LocalAppLanguage.current == "Marathi"

    val bookingState by viewModel.booking.collectAsState()
    val receiptState by viewModel.receiptSnapshot.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessageState by viewModel.errorMessage.collectAsState()

    val booking = bookingState
    val receiptSnapshot = receiptState
    val errorMessage = errorMessageState

    var hasPromptedFeedback by remember(bookingId) { mutableStateOf(false) }
    var showFeedbackPrompt by remember(bookingId) { mutableStateOf(false) }
    val pdfService = remember { PdfService(context) }

    LaunchedEffect(bookingId) {
        viewModel.loadReceiptDetails(bookingId)
    }

    LaunchedEffect(bookingId, booking) {
        val bookingData = booking ?: return@LaunchedEffect
        val shouldPromptFeedback =
            bookingData["feedbackSubmittedAt"] == null &&
                bookingData["rating"] == null &&
                ((bookingData["status"] as? String) in listOf("Completed", "Paid") ||
                    (bookingData["paymentStatus"] as? String) == "Paid")
        if (shouldPromptFeedback && !hasPromptedFeedback) {
            hasPromptedFeedback = true
            showFeedbackPrompt = true
        }
    }

    DivineScreen(
        topBar = {
            DivineTopBar(
                title = if (isMarathi) "डिजिटल पावती" else "Digital Receipt",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    val receiptReady = booking?.let {
                        val status = it["status"] as? String ?: ""
                        val paymentStatus = it["paymentStatus"] as? String ?: ""
                        status in listOf("Completed", "Paid") || paymentStatus == "Paid"
                    } == true
                    if (receiptReady) {
                        IconButton(
                            onClick = {
                                try {
                                    val data = booking ?: return@IconButton
                                    val amount = (data["actualAmount"] as? Number)?.toDouble() ?: 0.0
                                    val receipt = receiptSnapshot.orEmpty()
                                    val receiptNo = receipt["receiptNo"] as? String ?: "REC-${bookingId.take(8).uppercase()}"
                                    val file = pdfService.generateReceipt(
                                        bookingId = bookingId,
                                        receiptNo = receiptNo,
                                        poojaName = data["poojaName"] as? String ?: "Seva",
                                        amount = amount,
                                        date = (receipt["date"] as? String) ?: data["date"] as? String ?: "",
                                        yajmanName = data["contactName"] as? String ?: "",
                                        mobile = data["contactPhone"] as? String ?: "",
                                        address = data["address"] as? String ?: "",
                                        gurujiName = data["gurujiName"] as? String ?: "",
                                        paymentId = receipt["gatewayPaymentId"] as? String ?: data["gatewayPaymentId"] as? String ?: "",
                                        verifyUrl = receipt["verifyUrl"] as? String
                                            ?: "https://dindori-pranit-yadnyiki.web.app/verify-receipt?id=$receiptNo"
                                    )
                                    if (file != null && file.exists()) {
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, if (isMarathi) "पावती शेअर करा" else "Share Receipt"))
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Export Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )

                errorMessage != null -> DivineStatusScreen(
                    icon = Icons.Default.Info,
                    title = if (isMarathi) "पावती त्रुटी" else "Receipt Error",
                    description = errorMessage ?: "Unknown error",
                    actionText = if (isMarathi) "परत जा" else "Go Back",
                    onAction = { navController.popBackStack() },
                    color = MaterialTheme.colorScheme.error
                )

                booking != null -> ReceiptContent(
                    bookingId = bookingId,
                    bookingData = booking.orEmpty(),
                    receiptData = receiptSnapshot.orEmpty(),
                    isMarathi = isMarathi,
                    navController = navController
                )
            }
        }
    }

    if (showFeedbackPrompt && booking != null) {
        AlertDialog(
            onDismissRequest = { showFeedbackPrompt = false },
            title = {
                Text(if (isMarathi) "अभिप्राय द्यायचा आहे" else "Feedback pending")
            },
            text = {
                Text(
                    if (isMarathi) {
                        "तुमची पावती तयार आहे. सेवा अनुभवासाठी आता अभिप्राय द्यायचा का?"
                    } else {
                        "Your receipt is ready. Do you want to rate this seva now?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFeedbackPrompt = false
                        navController.navigate(Routes.ratingFeedback(bookingId, booking?.get("gurujiName") as? String ?: ""))
                    }
                ) {
                    Text(if (isMarathi) "आता द्या" else "Rate now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeedbackPrompt = false }) {
                    Text(if (isMarathi) "नंतर" else "Later")
                }
            }
        )
    }
}

@Composable
private fun ReceiptContent(
    bookingId: String,
    bookingData: Map<String, Any>,
    receiptData: Map<String, Any>,
    isMarathi: Boolean,
    navController: NavController
) {
    val receiptNo = receiptData["receiptNo"] as? String ?: "REC-${bookingId.take(8).uppercase()}"
    val amount = (bookingData["actualAmount"] as? Number)?.toDouble() ?: 0.0
    val needsFeedback = bookingData["feedbackSubmittedAt"] == null && bookingData["rating"] == null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = DivineSpacing.Large)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(DivineSpacing.Medium))

        DivineCard(modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ReceiptHeader()
                Spacer(Modifier.height(DivineSpacing.Large))

                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = SocialUi.SuccessGreen.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SocialUi.SuccessGreen.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SocialUi.SuccessGreen,
                        modifier = Modifier.padding(20.dp).fillMaxSize()
                    )
                }

                Spacer(Modifier.height(DivineSpacing.Large))
                Text(
                    text = if (isMarathi) "दक्षिणा यशस्वी" else "Dakshina Confirmed",
                    style = DivineTypography.headlineMedium.copy(
                        color = SocialUi.SuccessGreen,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(Modifier.height(DivineSpacing.ExtraLarge))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Spacer(Modifier.height(DivineSpacing.Medium))

                ReceiptItem(if (isMarathi) "बुकिंग आयडी" else "Ref ID", "#$bookingId")
                ReceiptItem(if (isMarathi) "पावती क्र." else "Receipt No.", receiptNo)
                ReceiptItem(if (isMarathi) "यजमान नाव" else "Yajman Name", bookingData["contactName"] as? String ?: "---")
                ReceiptItem(if (isMarathi) "मोबाईल नं." else "Mobile No.", bookingData["contactPhone"] as? String ?: "---")
                ReceiptItem(if (isMarathi) "पत्ता" else "Address", bookingData["address"] as? String ?: "---")
                ReceiptItem(if (isMarathi) "सेवा" else "Sacred Seva", bookingData["poojaName"] as? String ?: "---")
                ReceiptItem(if (isMarathi) "दिनांक" else "Date", bookingData["date"] as? String ?: "---")
                ReceiptItem(if (isMarathi) "गुरुजी" else "Guruji", bookingData["gurujiName"] as? String ?: "---")

                Spacer(Modifier.height(DivineSpacing.Medium))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Spacer(Modifier.height(DivineSpacing.Medium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isMarathi) "एकूण दक्षिणा" else "Total Settlement",
                        style = DivineTypography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    )
                    Text(
                        text = "₹$amount",
                        style = DivineTypography.displayLarge.copy(fontSize = 26.sp, color = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        if (needsFeedback) {
            Spacer(Modifier.height(DivineSpacing.Large))
            Button(
                onClick = {
                    navController.navigate(Routes.ratingFeedback(bookingId, bookingData["gurujiName"] as? String ?: ""))
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = DivineShapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text(
                    if (isMarathi) "सेवेचा अभिप्राय द्या" else "Rate Your Seva",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(DivineSpacing.ExtraLarge))
        Text(
            text = if (isMarathi) "|| श्री स्वामी समर्थ ||" else "|| Shri Swami Samarth ||",
            style = DivineTypography.titleLarge.copy(color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
        )
        Text(
            text = if (isMarathi) "दिंडोरी प्रणीत याज्ञिकी सेवा मार्ग" else "Dindori Pranit Yadnyiki Seva Marg",
            style = DivineTypography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(Modifier.height(DivineSpacing.Sacred))
    }
}

@Composable
private fun ReceiptHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFE8F5D0),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86B94B).copy(alpha = 0.45f))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.swami_samarth_receipt),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(68.dp)
            )
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "|| Shri Swami Samarth ||",
                    style = DivineTypography.labelMedium.copy(
                        color = Color(0xFFD35400),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
                Text(
                    text = if (LocalAppLanguage.current == "Marathi") "डिजिटल दक्षिणा पावती" else "Digital Dakshina Receipt",
                    style = DivineTypography.titleMedium.copy(
                        color = Color(0xFF1F5E31),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
            }
            Spacer(Modifier.weight(1f))
            Image(
                painter = painterResource(id = R.drawable.gurumauli_receipt),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(68.dp)
            )
        }
    }
}

@Composable
private fun ReceiptItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.padding(horizontal = 7.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = DivineTypography.bodyMedium.copy(color = SocialUi.TitleColor, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = DivineTypography.bodySmall.copy(color = SocialUi.ValueColor),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
