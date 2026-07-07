package com.example.dindoripranityadnyiki.feature.guruji

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dindoripranityadnyiki.core.design.DivineCard
import com.example.dindoripranityadnyiki.core.design.DivineTypography
import com.example.dindoripranityadnyiki.core.design.SocialUi

@Composable
fun GurujiBookingInfoCard(
    booking: Map<String, Any>,
    status: String,
    eta: String,
    showArrived: Boolean,
    isMarathi: Boolean,
    sacredCopper: Color,
) {
    val context = LocalContext.current
    DivineCard {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            InfoRow(Icons.Default.Person, if (isMarathi) "यजमान नाव" else "Yajman name", booking["contactName"] as? String ?: "", sacredCopper)
            InfoRow(Icons.Default.CalendarMonth, if (isMarathi) "तारीख" else "Date", booking["date"] as? String ?: "", sacredCopper)
            InfoRow(Icons.Default.Info, if (isMarathi) "स्थिती" else "Status", status, sacredCopper)
            if (eta.isNotBlank()) {
                InfoRow(Icons.Default.Schedule, "ETA", eta, sacredCopper)
            }
            InfoRow(
                Icons.Default.CurrencyRupee,
                if (isMarathi) "रक्कम" else "Amount",
                (booking["actualAmount"] ?: booking["totalAmount"] ?: "").toString(),
                sacredCopper
            )
            if (showArrived) {
                ArrivalStatusCard(isMarathi = isMarathi)
            }
            if (status == "Assigned") {
                Text(
                    if (isMarathi) "सेवा स्वीकारल्यानंतर यजमानाचा पत्ता आणि संपर्क दिसेल." else "Info unlocks after accepting the seva.",
                    color = SocialUi.ValueColor,
                    style = DivineTypography.bodyMedium
                )
            } else {
                InfoRow(Icons.Default.Call, if (isMarathi) "फोन" else "Phone", booking["contactPhone"] as? String ?: "", sacredCopper)
                InfoRow(Icons.Default.Home, if (isMarathi) "पत्ता" else "Address", booking["address"] as? String ?: "", sacredCopper)
                Button(
                    onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${booking["contactPhone"]}"))) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = sacredCopper)
                ) {
                    Icon(Icons.Default.Call, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isMarathi) "यजमानाला कॉल करा" else "Call Now")
                }
            }
        }
    }
}

@Composable
fun GurujiStatusActions(
    status: String,
    canReview: Boolean,
    canStartSeva: Boolean,
    canRequestOtp: Boolean,
    canCompleteWithOtp: Boolean,
    isStatusLoading: Boolean,
    isMarathi: Boolean,
    sacredCopper: Color,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onRequestOtp: () -> Unit,
    onOpenCompletionDialog: () -> Unit,
) {
    when {
        isStatusLoading -> androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth(), color = sacredCopper)
        status == "Assigned" && canReview -> {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = sacredCopper)
                ) {
                    Text(if (isMarathi) "स्वीकारा" else "Accept")
                }
                OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) {
                    Text(if (isMarathi) "नकार द्या" else "Reject")
                }
            }
        }

        status == "Accepted" && canStartSeva -> {
            Text(
                text = if (isMarathi) {
                    "गुरुजी निघाल्यावर सेवा स्थिती आपोआप सुरू होईल."
                } else {
                    "Service status will start automatically once the Guruji leaves for the booking."
                },
                style = DivineTypography.bodyMedium.copy(color = SocialUi.ValueColor)
            )
        }

        status == "In Progress" && canCompleteWithOtp -> {
            Button(
                onClick = onOpenCompletionDialog,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = sacredCopper)
            ) {
                Text(if (isMarathi) "OTP आणि रक्कम टाका" else "Enter OTP & Amount")
            }
        }

        status == "In Progress" && canRequestOtp -> {
            Button(
                onClick = onRequestOtp,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = sacredCopper)
            ) {
                Text(if (isMarathi) "सेवा पूर्ण करा" else "Complete Seva")
            }
        }
    }
}

@Composable
fun GurujiCompletionDialog(
    show: Boolean,
    completionOtp: String,
    actualAmount: String,
    canResendOtp: Boolean,
    otpResendTimer: Int,
    isStatusLoading: Boolean,
    isMarathi: Boolean,
    sacredCopper: Color,
    onOtpChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onResendOtp: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
) {
    if (!show) return
    val canSubmitCompletion = completionOtp.length == 4 && actualAmount.toDoubleOrNull()?.let { it > 0.0 } == true
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isMarathi) "सेवा पूर्ण करा" else "Complete Pooja",
                style = DivineTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold, color = SocialUi.TitleColor)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (isMarathi) "यजमानाकडून मिळालेला OTP आणि प्रत्यक्ष दक्षिणा रक्कम टाका." else "Enter OTP from devotee and actual dakshina amount.",
                    style = DivineTypography.bodyMedium.copy(color = SocialUi.ValueColor)
                )
                OutlinedTextField(
                    value = completionOtp,
                    onValueChange = onOtpChange,
                    label = { Text("OTP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = actualAmount,
                    onValueChange = onAmountChange,
                    label = { Text(if (isMarathi) "दक्षिणा रक्कम" else "Dakshina Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (canResendOtp) {
                        TextButton(onClick = onResendOtp) {
                            Text(if (isMarathi) "OTP पुन्हा पाठवा" else "Resend OTP", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            if (isMarathi) "OTP पुन्हा पाठवण्यासाठी $otpResendTimer सेकंद प्रतीक्षा करा" else "Resend available in $otpResendTimer sec",
                            style = DivineTypography.bodySmall,
                            color = SocialUi.ValueColor
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canSubmitCompletion && !isStatusLoading,
                onClick = onSubmit,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = sacredCopper)
            ) {
                Text(if (isMarathi) "सबमिट" else "Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isMarathi) "रद्द करा" else "Cancel", color = SocialUi.ValueColor)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun ArrivalStatusCard(isMarathi: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SocialUi.SuccessGreen.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, SocialUi.SuccessGreen.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocationOn, null, tint = SocialUi.SuccessGreen, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isMarathi) "तुम्ही ठिकाणी पोहोचला आहात. OTP आणि completion flow साठी तयार." else "You have reached the destination. OTP/completion flow is ready.",
                style = DivineTypography.bodyMedium.copy(color = SocialUi.SuccessGreen, fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = CircleShape,
            color = color.copy(alpha = 0.08f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(20.dp), tint = color)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = DivineTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = SocialUi.TitleColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text.ifBlank { "-" },
                style = DivineTypography.bodySmall.copy(color = SocialUi.ValueColor),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RouteSummaryCard(
    distance: String,
    duration: String,
    isMarathi: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = color.copy(alpha = 0.12f)) {
                Icon(Icons.Default.Route, null, tint = color, modifier = Modifier.padding(10.dp).size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (isMarathi) "मार्गाचा अंदाज" else "Route estimate",
                    style = DivineTypography.labelMedium.copy(color = Color.Gray, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = listOf(distance, duration).filter { it.isNotBlank() }.joinToString(" • "),
                    style = DivineTypography.bodyLarge.copy(fontWeight = FontWeight.Black),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
