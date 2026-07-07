package com.example.dindoripranityadnyiki.feature.user

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.core.design.DivineShapes
import com.example.dindoripranityadnyiki.core.design.DivineSpacing
import com.example.dindoripranityadnyiki.core.design.DivineTypography
import com.example.dindoripranityadnyiki.core.design.SocialEmptyState
import com.example.dindoripranityadnyiki.core.design.SocialUi
import com.example.dindoripranityadnyiki.core.navigation.Routes

@Composable
fun BookingCTA(isMarathi: Boolean, onClick: () -> Unit) {
    val sacredCopper = MaterialTheme.colorScheme.primary
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
            Text(
                text = if (isMarathi) "\u0928\u0935\u0940\u0928 \u092a\u0942\u091c\u093e \u092c\u0941\u0915 \u0915\u0930\u093e" else "Book New Pooja",
                style = DivineTypography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = DivineSpacing.Large, end = 76.dp)
            )
            Surface(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 20.dp).size(40.dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, null, tint = sacredCopper, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
fun UserNextActionCard(isMarathi: Boolean, onClick: () -> Unit) {
    val sacredCopper = MaterialTheme.colorScheme.primary
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = sacredCopper.copy(alpha = 0.08f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.EventAvailable, null, tint = sacredCopper, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isMarathi) "\u0928\u0935\u0940\u0928 \u092a\u0942\u091c\u093e \u092c\u0941\u0915 \u0915\u0930\u093e" else "Book a new pooja",
                    style = DivineTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, color = SocialUi.TitleColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isMarathi) "\u0938\u0927\u094d\u092f\u093e \u091a\u093e\u0932\u0942 \u0915\u0943\u0924\u0940 \u0928\u093e\u0939\u0940. \u092a\u0941\u0922\u0940\u0932 \u092a\u0942\u091c\u093e \u0928\u093f\u0935\u0921\u0942\u0928 \u0924\u093e\u0930\u0940\u0916 \u0920\u0930\u0935\u093e." else "No active action now. Choose the next pooja and date.",
                    style = DivineTypography.bodySmall,
                    color = SocialUi.ValueColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = sacredCopper, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = DivineTypography.labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
            color = SocialUi.ValueColor,
            letterSpacing = 0.5.sp
        ),
        modifier = Modifier.padding(top = DivineSpacing.Medium, bottom = DivineSpacing.Small)
    )
}

@Composable
fun ActiveServiceCard(booking: Map<String, Any>, isMarathi: Boolean, navController: NavController) {
    val bookingId = booking["id"] as? String ?: ""
    val userAction = booking["currentUserAction"] as? String ?: ""
    val status = booking["status"] as? String ?: ""
    val buttonText = when (userAction) {
        "PAY_NOW" -> if (isMarathi) "\u092a\u0947\u092e\u0947\u0902\u091f \u0915\u0930\u093e" else "Pay Now"
        "CHECK_OTP_NOTIFICATION" -> if (isMarathi) "\u0913\u091f\u0940\u092a\u0940 \u0928\u094b\u091f\u093f\u092b\u093f\u0915\u0947\u0936\u0928 \u092a\u0939\u093e" else "Check OTP Notification"
        "WAIT_PAYMENT_VERIFICATION" -> if (isMarathi) "\u0938\u094d\u0925\u093f\u0924\u0940 \u092a\u0939\u093e" else "View Status"
        "RATE_SEVA" -> if (isMarathi) "\u092b\u0940\u0921\u092c\u0945\u0915 \u0926\u094d\u092f\u093e" else "Rate Seva"
        "VIEW_RECEIPT" -> if (isMarathi) "\u092a\u093e\u0935\u0924\u0940 \u092a\u0939\u093e" else "View Receipt"
        else -> if (isMarathi) "\u092a\u0939\u093e" else "View"
    }
    val destination = when (userAction) {
        "PAY_NOW" -> Routes.poojaPayment(bookingId)
        "CHECK_OTP_NOTIFICATION" -> Routes.NOTIFICATION_INBOX
        "RATE_SEVA" -> Routes.ratingFeedback(bookingId, booking["gurujiName"] as? String ?: "")
        "VIEW_RECEIPT" -> Routes.digitalReceipt(bookingId)
        else -> Routes.bookingConfirmed(bookingId)
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = DivineShapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.EventAvailable, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    booking["poojaName"] as? String ?: "",
                    fontWeight = FontWeight.SemiBold,
                    color = SocialUi.TitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val actionTitle = booking["currentUserActionTitle"] as? String
                val actionDescription = booking["currentUserActionDescription"] as? String
                Text(
                    actionTitle ?: if (isMarathi) "स्थिती: ${localizedBookingStatus(status, true)}" else "Status: ${localizedBookingStatus(status, false)}",
                    style = DivineTypography.bodySmall,
                    color = SocialUi.ValueColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!actionDescription.isNullOrBlank()) {
                    Text(
                        actionDescription,
                        style = DivineTypography.bodySmall,
                        color = SocialUi.ValueColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            TextButton(onClick = { navController.navigate(destination) }) {
                Text(buttonText, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun HistoryItem(booking: Map<String, Any>, isMarathi: Boolean, navController: NavController) {
    val bookingId = booking["id"] as? String ?: ""
    val receiptNo = booking["receiptNo"] as? String ?: "REC-${bookingId.take(8).uppercase()}"
    val status = booking["status"] as? String ?: ""
    val paymentStatus = booking["paymentStatus"] as? String ?: ""
    val isReceiptReady = status == "Paid" || paymentStatus == "Paid" || status == "Completed"
    val needsFeedback = isReceiptReady &&
        booking["feedbackSubmittedAt"] == null &&
        booking["rating"] == null
    Column(
        modifier = Modifier.fillMaxWidth().clickable {
            if (needsFeedback) {
                navController.navigate(Routes.ratingFeedback(bookingId, booking["gurujiName"] as? String ?: ""))
            } else if (status == "Cancelled" || !isReceiptReady) {
                navController.navigate(Routes.bookingConfirmed(bookingId))
            } else {
                navController.navigate(Routes.digitalReceipt(bookingId))
            }
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.EventAvailable, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    booking["poojaName"] as? String ?: "",
                    style = DivineTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = SocialUi.TitleColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(booking["date"] as? String ?: "", style = DivineTypography.bodySmall, color = SocialUi.ValueColor)
                Text(
                    localizedBookingStatus(status, isMarathi),
                    style = DivineTypography.bodySmall,
                    color = if (status == "Cancelled") Color(0xFFDC2626) else SocialUi.ValueColor
                )
                if (status == "Paid" || paymentStatus == "Paid") {
                    Text(receiptNo, style = DivineTypography.bodySmall, color = SocialUi.SuccessGreen)
                }
                if (status == "Cancelled") {
                    Text(
                        if (isMarathi) "\u092c\u0941\u0915\u093f\u0902\u0917 \u0930\u0926\u094d\u0926 \u091d\u093e\u0932\u0947" else "Booking cancelled",
                        style = DivineTypography.bodySmall,
                        color = Color(0xFFDC2626)
                    )
                }
                if (needsFeedback) {
                    Text(
                        if (isMarathi) "\u092b\u0940\u0921\u092c\u0945\u0915 \u092c\u093e\u0915\u0940 \u0906\u0939\u0947" else "Feedback pending",
                        style = DivineTypography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
        }
    }
}

private fun localizedBookingStatus(status: String, isMarathi: Boolean): String {
    if (!isMarathi) return status.ifBlank { "Pending" }
    return when (status) {
        "Pending" -> "प्रलंबित"
        "Assigned" -> "गुरुजी नेमले"
        "Accepted" -> "स्वीकारले"
        "In Progress" -> "सेवा सुरू"
        "Payment Pending" -> "पेमेंट बाकी"
        "Awaiting Verification" -> "तपासणी बाकी"
        "Completed" -> "पूर्ण"
        "Paid" -> "पैसे भरले"
        "Cancelled" -> "रद्द"
        else -> status.ifBlank { "प्रलंबित" }
    }
}

@Composable
fun EmptyHistoryState(isMarathi: Boolean) {
    SocialEmptyState(
        icon = Icons.Default.History,
        message = if (isMarathi) "\u0905\u091c\u0942\u0928 \u0924\u0941\u092e\u091a\u0940 \u092c\u0941\u0915\u093f\u0902\u0917 \u0907\u0925\u0947 \u0926\u093f\u0938\u0924 \u0928\u093e\u0939\u0940\u0924." else "No bookings found yet. Your booking updates will appear here."
    )
}
