package com.example.dindoripranityadnyiki.feature.guruji

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.core.design.DivineCard
import com.example.dindoripranityadnyiki.core.design.DivineTypography
import com.example.dindoripranityadnyiki.core.design.SocialEmptyState
import com.example.dindoripranityadnyiki.core.design.SocialUi
import com.example.dindoripranityadnyiki.core.navigation.Routes

@Composable
fun NewAssignmentCard(
    request: Map<String, Any>,
    isMarathi: Boolean,
    isLoading: Boolean = false,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onViewDetails: () -> Unit
) {
    val sacredCopper = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape,
                    color = sacredCopper.copy(alpha = 0.08f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = if (isMarathi) "नवीन पूजा नियुक्ती" else "New pooja assignment",
                            tint = sacredCopper,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request["poojaName"] as? String ?: "Sacred Pooja",
                        style = DivineTypography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = SocialUi.TitleColor
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isMarathi) "तारीख: ${request["date"] ?: ""}" else "Date: ${request["date"] ?: ""}",
                        style = DivineTypography.bodySmall,
                        color = SocialUi.ValueColor
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isMarathi) "यजमान" else "Yajman",
                            style = DivineTypography.labelSmall,
                            color = SocialUi.ValueColor
                        )
                        Text(
                            text = request["contactName"] as? String ?: "-",
                            style = DivineTypography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = SocialUi.TitleColor
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = onViewDetails) {
                        Text(if (isMarathi) "तपशील पहा" else "View Details", color = sacredCopper)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                    border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.3f))
                ) {
                    Text(if (isMarathi) "नकार द्या" else "Reject")
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1.5f),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SocialUi.SuccessGreen)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isMarathi) "स्वीकारा" else "Accept")
                    }
                }
            }
        }
    }
}

@Composable
fun BookingItem(
    poojaName: String,
    yajmanName: String,
    status: String,
    isMarathi: Boolean,
    onClick: () -> Unit
) {
    val statusColor = when (status) {
        "Completed" -> SocialUi.SuccessGreen
        "Accepted" -> Color(0xFF2196F3)
        "Assigned" -> MaterialTheme.colorScheme.primary
        "In Progress" -> Color(0xFF9C27B0)
        "Payment Pending" -> Color(0xFFE65100)
        "Awaiting Verification" -> Color(0xFF6D4C41)
        else -> MaterialTheme.colorScheme.primary
    }
    val statusLabel = when {
        !isMarathi -> status
        status == "Completed" -> "पूर्ण झाले"
        status == "Accepted" -> "स्वीकारले"
        status == "Assigned" -> "नियुक्त"
        status == "In Progress" -> "चालू"
        status == "Payment Pending" -> "पेमेंट प्रलंबित"
        status == "Awaiting Verification" -> "पडताळणी प्रलंबित"
        else -> status
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = statusColor.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AutoAwesome, null, tint = statusColor, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = poojaName,
                    style = DivineTypography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = SocialUi.TitleColor
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${if (isMarathi) "यजमान" else "Yajman"}: $yajmanName",
                    style = DivineTypography.labelSmall,
                    color = SocialUi.ValueColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = statusLabel,
                style = DivineTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = statusColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
fun ApprovalPendingCard(isMarathi: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = if (isMarathi) "तुमचे प्रोफाइल verification प्रलंबित आहे." else "Profile verification pending.",
                style = DivineTypography.bodyMedium,
                color = SocialUi.TitleColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun EmptyGurujiBookingState(isMarathi: Boolean) {
    SocialEmptyState(
        icon = Icons.Default.Inbox,
        message = if (isMarathi) {
            "सध्या नवीन सेवा विनंती नाही. नवीन नियुक्त्या इथे दिसतील."
        } else {
            "No active seva requests right now. New assignments will appear here."
        }
    )
}

@Composable
fun PlanningWidget(isMarathi: Boolean, navController: NavController) {
    val deepOrange = Color(0xFFE65100)
    DivineCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { navController.navigate(Routes.GURUJI_AVAILABILITY) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = deepOrange.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CalendarMonth, null, tint = deepOrange)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (isMarathi) "सेवा नियोजन" else "Seva planning",
                    style = DivineTypography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = SocialUi.TitleColor
                    )
                )
                Text(
                    if (isMarathi) "उपलब्धता अपडेट करा आणि पुढील सेवा नीट सांभाळा." else "Update your availability and manage upcoming seva smoothly.",
                    style = DivineTypography.bodySmall,
                    color = SocialUi.ValueColor
                )
            }
            TextButton(onClick = { navController.navigate(Routes.GURUJI_AVAILABILITY) }) {
                Text(if (isMarathi) "उघडा" else "Open", color = deepOrange)
            }
        }
    }
}
