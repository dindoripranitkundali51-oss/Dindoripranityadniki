package com.example.dindoripranityadnyiki.feature.guruji

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.LocalAppLanguage
import com.example.dindoripranityadnyiki.core.design.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun GurujiAvailabilityScreen(
    navController: NavController,
    viewModel: GurujiAvailabilityViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isMarathi = LocalAppLanguage.current == "Marathi"
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val currentRealMonth = remember { YearMonth.now() }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    DivineScreen(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DivineTopBar(
                title = if (isMarathi) "उपलब्धता नियोजन" else "Availability",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = SocialUi.TitleColor)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
                }
            } else {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (isMarathi) "उपलब्ध तारखा" else "Available dates",
                            style = DivineTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold, color = SocialUi.TitleColor)
                        )
                        Text(
                            text = if (isMarathi) "ज्या दिवशी सेवा देऊ शकता त्या तारखा निवडा." else "Select the dates you can serve.",
                            style = DivineTypography.bodySmall.copy(color = SocialUi.ValueColor)
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (currentMonth > currentRealMonth) currentMonth = currentMonth.minusMonths(1) },
                        enabled = currentMonth > currentRealMonth
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            null,
                            tint = if (currentMonth > currentRealMonth) SocialUi.ValueColor else SocialUi.ValueColor.copy(alpha = 0.35f)
                        )
                    }
                    Text(
                        text = "${currentMonth.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                        style = DivineTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = SocialUi.TitleColor)
                    )
                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                        Icon(Icons.Default.ChevronRight, null, tint = SocialUi.ValueColor)
                    }
                }

                        AvailabilityCalendarGrid(
                            yearMonth = currentMonth,
                            selectedDates = uiState.selectedDates,
                            bookedDates = uiState.bookedDates,
                            onDateToggle = { viewModel.toggleDate(it) },
                            isMarathi = isMarathi
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(modifier = Modifier.size(16.dp), shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary) {}
                                Spacer(Modifier.width(8.dp))
                                Text(text = if (isMarathi) "उपलब्ध" else "Available", style = DivineTypography.bodySmall, color = SocialUi.TitleColor)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(modifier = Modifier.size(16.dp), shape = RoundedCornerShape(6.dp), color = SocialUi.Error) {}
                                Spacer(Modifier.width(8.dp))
                                Text(text = if (isMarathi) "बुक केलेली" else "Booked", style = DivineTypography.bodySmall, color = SocialUi.TitleColor)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (isMarathi) "निवडलेल्या तारखांनाच नवीन सेवा मिळतील." else "New services will be assigned only on selected dates.",
                            style = DivineTypography.bodySmall.copy(color = SocialUi.ValueColor, fontWeight = FontWeight.Medium)
                        )
                    }
                }

                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { viewModel.saveAvailability() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = DivineShapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = !uiState.isUpdating
                ) {
                    if (uiState.isUpdating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CloudDone, null, modifier = Modifier.size(20.dp), tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isMarathi) "बदल जतन करा" else "Save changes",
                            style = DivineTypography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun AvailabilityCalendarGrid(
    yearMonth: YearMonth,
    selectedDates: List<String>,
    bookedDates: List<String>,
    onDateToggle: (String) -> Unit,
    isMarathi: Boolean
) {
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDay = yearMonth.atDay(1).dayOfWeek.value % 7
    val today = LocalDate.now()
    val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
            val weekdays = if (isMarathi) {
                listOf("रवि", "सोम", "मंगळ", "बुध", "गुरु", "शुक्र", "शनि")
            } else {
                listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
            }
            weekdays.forEach {
                Text(
                    text = it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = DivineTypography.labelSmall.copy(color = SocialUi.ValueColor, fontWeight = FontWeight.SemiBold)
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(280.dp),
            userScrollEnabled = false
        ) {
            items(firstDay) { Box(Modifier.aspectRatio(1f)) }

            items(daysInMonth) { index ->
                val day = index + 1
                val date = yearMonth.atDay(day)
                val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val isSelected = selectedDates.contains(dateStr)
                val isBooked = bookedDates.contains(dateStr)
                val isToday = dateStr == todayStr
                val isPast = date.isBefore(today)
                val isClickable = !isPast && !isBooked

                val scale by animateFloatAsState(
                    targetValue = if (isSelected && !isPast) 1.08f else 1.0f,
                    animationSpec = DivineMotion.snappySpring(),
                    label = "availability_day_scale"
                )
                val bgColor by animateColorAsState(
                    targetValue = when {
                        isBooked && !isPast -> SocialUi.Error
                        isSelected && !isPast -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        else -> Color.Transparent
                    },
                    animationSpec = DivineMotion.pageTween(),
                    label = "availability_day_color"
                )

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(4.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColor)
                        .clickable(enabled = isClickable) { onDateToggle(dateStr) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.toString(),
                        style = DivineTypography.bodyMedium.copy(
                            color = when {
                                isPast -> SocialUi.ValueColor.copy(alpha = 0.35f)
                                isSelected || isBooked -> Color.White
                                isToday -> MaterialTheme.colorScheme.primary
                                else -> SocialUi.TitleColor
                            },
                            fontWeight = if ((isSelected && !isPast) || isToday || isBooked) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                }
            }
        }
    }
}
