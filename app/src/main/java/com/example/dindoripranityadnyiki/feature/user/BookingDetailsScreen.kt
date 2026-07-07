@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.dindoripranityadnyiki.feature.user

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.dindoripranityadnyiki.R
import com.example.dindoripranityadnyiki.core.design.*
import com.example.dindoripranityadnyiki.core.navigation.Routes
import com.example.dindoripranityadnyiki.feature.common.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun BookingDetailsScreen(
    poojaId: String,
    poojaName: String,
    navController: NavController,
    viewModel: BookingDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val sacredCopper = MaterialTheme.colorScheme.primary
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showMapDialog by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }
    var showMuhurtPicker by remember { mutableStateOf(false) }

    val locationOptions = remember {
        listOf(
            "Home" to R.string.loc_home,
            "Office" to R.string.loc_office,
            "Shop" to R.string.loc_shop,
            "Factory" to R.string.loc_factory,
            "Hotel" to R.string.loc_hotel,
            "Warehouse" to R.string.loc_warehouse,
            "Farm" to R.string.loc_farm,
            "Marriage Hall" to R.string.loc_marriage_hall,
            "Temple" to R.string.loc_temple,
            "Ashram" to R.string.loc_ashram,
            "Other" to R.string.loc_other
        )
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.bookingId) {
        uiState.bookingId?.let { id ->
            navController.navigate(Routes.bookingConfirmed(id)) {
                popUpTo(Routes.USER_HOME) { inclusive = false }
            }
        }
    }

    LaunchedEffect(poojaId) {
        viewModel.refreshProfile()
        viewModel.loadPoojaImage(poojaId)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { BookingDetailsTopBar(onBack = { navController.popBackStack() }) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = sacredCopper)
            }
        } else {
            BookingDetailsFormContent(
                uiState = uiState,
                poojaName = poojaName,
                sacredCopper = sacredCopper,
                locationOptions = locationOptions,
                showTypeMenu = showTypeMenu,
                onTypeMenuToggle = { showTypeMenu = it },
                onOpenMap = { showMapDialog = true },
                onOpenMuhurtPicker = { showMuhurtPicker = true },
                onNameChange = viewModel::updateName,
                onMobileChange = viewModel::updateMobile,
                onEmailChange = viewModel::updateEmail,
                onAddressChange = viewModel::updateAddress,
                onPlaceSelected = viewModel::onPlaceSelected,
                onSmartAddressSelected = viewModel::onSmartAddressSelected,
                onLocationTypeSelected = viewModel::updateLocationType,
                onSpecialInstructionsChange = viewModel::updateSpecialInstructions,
                onConfirm = { viewModel.confirmBooking(poojaId, poojaName) },
                modifier = Modifier.padding(padding)
            )
        }
    }

    BookingMuhurtPickerDialog(
        show = showMuhurtPicker,
        availableDates = uiState.availableDates,
        sacredCopper = sacredCopper,
        onDismiss = { showMuhurtPicker = false },
        onDateSelected = viewModel::onDateSelected
    )

    BookingLocationMapDialog(
        show = showMapDialog,
        userLat = uiState.userLat,
        userLng = uiState.userLng,
        onDismiss = { showMapDialog = false }
    )
}

@Composable
private fun BookingDetailsTopBar(onBack: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(bottom = 8.dp, top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = stringResource(R.string.booking_details_title),
                    style = DivineTypography.headlineSmall.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.size(48.dp))
            }
            Text(
                text = stringResource(R.string.booking_details_subtitle),
                style = DivineTypography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun BookingDetailsFormContent(
    uiState: BookingDetailsUiState,
    poojaName: String,
    sacredCopper: Color,
    locationOptions: List<Pair<String, Int>>,
    showTypeMenu: Boolean,
    onTypeMenuToggle: (Boolean) -> Unit,
    onOpenMap: () -> Unit,
    onOpenMuhurtPicker: () -> Unit,
    onNameChange: (String) -> Unit,
    onMobileChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onPlaceSelected: (com.google.android.libraries.places.api.model.Place) -> Unit,
    onSmartAddressSelected: (SmartAddressResult) -> Unit,
    onLocationTypeSelected: (String) -> Unit,
    onSpecialInstructionsChange: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BookingPoojaHeaderCard(poojaName = poojaName, imageUrl = uiState.poojaImageUrl)

        Text(
            text = stringResource(R.string.devotee_info_title),
            style = DivineTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = SocialUi.TitleColor
        )

        BookingDevoteeFields(
            uiState = uiState,
            onNameChange = onNameChange,
            onMobileChange = onMobileChange,
            onEmailChange = onEmailChange,
            onAddressChange = onAddressChange,
            onPlaceSelected = onPlaceSelected,
            onSmartAddressSelected = onSmartAddressSelected
        )

        BookingLocationPreviewCard(
            isVisible = uiState.isLocationFetched,
            userLat = uiState.userLat,
            userLng = uiState.userLng,
            onOpenMap = onOpenMap
        )

        BookingAreaFields(uiState = uiState)

        BookingLocationTypeField(
            selectedLocationType = uiState.selectedLocationType,
            locationOptions = locationOptions,
            sacredCopper = sacredCopper,
            expanded = showTypeMenu,
            onExpandedChange = onTypeMenuToggle,
            onLocationTypeSelected = onLocationTypeSelected
        )

        BookingDateField(
            dateText = uiState.selectedDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "",
            error = uiState.dateError,
            onOpenPicker = onOpenMuhurtPicker
        )

        DivineTextField(
            value = uiState.specialInstructions,
            onValueChange = onSpecialInstructionsChange,
            label = stringResource(R.string.special_instructions_hint),
            icon = Icons.Default.RateReview,
            singleLine = false,
            minLines = 2,
            maxLines = 3
        )

        Spacer(Modifier.height(16.dp))

        BookingConfirmButton(
            isProcessing = uiState.isProcessing,
            enabled = uiState.isPoojaDetailsLoaded,
            sacredCopper = sacredCopper,
            onConfirm = onConfirm
        )

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun BookingPoojaHeaderCard(
    poojaName: String,
    imageUrl: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .height(96.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.size(58.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = poojaName,
                    style = DivineTypography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = SocialUi.TitleColor,
                        fontSize = 18.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.shri_swami_samarth),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = SocialUi.ValueColor
                )
            }
        }
    }
}

@Composable
private fun BookingDevoteeFields(
    uiState: BookingDetailsUiState,
    onNameChange: (String) -> Unit,
    onMobileChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onPlaceSelected: (com.google.android.libraries.places.api.model.Place) -> Unit,
    onSmartAddressSelected: (SmartAddressResult) -> Unit
) {
    DivineTextField(
        value = uiState.fullName,
        onValueChange = onNameChange,
        label = stringResource(R.string.full_name),
        icon = Icons.Default.Person,
        error = uiState.nameError
    )

    DivineTextField(
        value = uiState.mobile,
        onValueChange = onMobileChange,
        label = stringResource(R.string.mobile_number),
        icon = Icons.Default.Phone,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        error = uiState.mobileError
    )

    DivineTextField(
        value = uiState.email,
        onValueChange = onEmailChange,
        label = stringResource(R.string.email_id_optional),
        icon = Icons.Default.Email,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
    )

    SmartAddressField(
        value = uiState.address,
        onValueChange = onAddressChange,
        label = stringResource(R.string.full_address),
        onPlaceSelected = onPlaceSelected,
        onSmartAddressSelected = onSmartAddressSelected,
        error = uiState.addressError
    )
}

@Composable
private fun BookingLocationPreviewCard(
    isVisible: Boolean,
    userLat: Double,
    userLng: Double,
    onOpenMap: () -> Unit
) {
    AnimatedVisibility(visible = isVisible) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(Modifier.fillMaxSize()) {
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(LatLng(userLat, userLng), 15f)
                }
                LaunchedEffect(userLat, userLng) {
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(userLat, userLng), 15f)
                }

                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        myLocationButtonEnabled = false,
                        scrollGesturesEnabled = false,
                        zoomGesturesEnabled = false
                    ),
                    properties = MapProperties(isMyLocationEnabled = false)
                ) {
                    Marker(state = MarkerState(position = LatLng(userLat, userLng)))
                }

                Box(modifier = Modifier.fillMaxSize().clickable(onClick = onOpenMap))
            }
        }
    }
}

@Composable
private fun BookingAreaFields(uiState: BookingDetailsUiState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        DivineTextField(
            value = uiState.district,
            onValueChange = { },
            label = stringResource(R.string.district),
            icon = Icons.Default.Map,
            readOnly = true,
            modifier = Modifier.weight(1f),
            error = uiState.districtError
        )

        DivineTextField(
            value = uiState.pincode,
            onValueChange = { },
            label = stringResource(R.string.pincode),
            icon = Icons.Default.PushPin,
            readOnly = true,
            modifier = Modifier.weight(1f),
            error = uiState.pincodeError
        )
    }
}

@Composable
private fun BookingLocationTypeField(
    selectedLocationType: String,
    locationOptions: List<Pair<String, Int>>,
    sacredCopper: Color,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onLocationTypeSelected: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        val selectedOption = locationOptions.find { it.first == selectedLocationType }
        val currentTypeDisplay = selectedOption?.let { stringResource(it.second) } ?: selectedLocationType

        DivineTextField(
            value = currentTypeDisplay,
            onValueChange = {},
            label = stringResource(R.string.place_of_pooja),
            icon = Icons.Default.HomeWork,
            readOnly = true,
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = sacredCopper) }
        )
        Box(modifier = Modifier.matchParentSize().clickable { onExpandedChange(true) })

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.fillMaxWidth(0.85f).background(Color.White)
        ) {
            locationOptions.forEach { (key, resId) ->
                DropdownMenuItem(
                    text = { Text(stringResource(resId), fontSize = 14.sp) },
                    onClick = {
                        onLocationTypeSelected(key)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun BookingDateField(
    dateText: String,
    error: String?,
    onOpenPicker: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        DivineTextField(
            value = dateText,
            onValueChange = {},
            label = stringResource(R.string.select_date),
            icon = Icons.Default.CalendarMonth,
            readOnly = true,
            error = error
        )
        Box(modifier = Modifier.matchParentSize().clickable(onClick = onOpenPicker))
    }
}

@Composable
private fun BookingConfirmButton(
    isProcessing: Boolean,
    enabled: Boolean,
    sacredCopper: Color,
    onConfirm: () -> Unit
) {
    Button(
        onClick = onConfirm,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = sacredCopper),
        enabled = !isProcessing && enabled
    ) {
        if (isProcessing) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
        } else {
            Text(stringResource(R.string.confirm_booking_btn), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun BookingMuhurtPickerDialog(
    show: Boolean,
    availableDates: List<String>,
    sacredCopper: Color,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    if (!show) return

    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.ofEpochMilli(utcTimeMillis)
                    .atZone(ZoneId.of("UTC"))
                    .toLocalDate()
                if (date.isBefore(LocalDate.now())) return false
                val dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                return availableDates.contains(dateString)
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let {
                    onDateSelected(
                        Instant.ofEpochMilli(it)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                    )
                }
                onDismiss()
            }) {
                Text("OK", color = sacredCopper, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = Color.Gray)
            }
        },
        shape = RoundedCornerShape(16.dp)
    ) {
        DatePicker(
            state = datePickerState,
            colors = DatePickerDefaults.colors(
                selectedDayContainerColor = sacredCopper,
                todayContentColor = sacredCopper
            )
        )
    }
}

@Composable
private fun BookingLocationMapDialog(
    show: Boolean,
    userLat: Double,
    userLng: Double,
    onDismiss: () -> Unit
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxSize(),
        text = {
            Box(Modifier.fillMaxSize()) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(LatLng(userLat, userLng), 17f)
                    }
                ) {
                    Marker(state = MarkerState(position = LatLng(userLat, userLng)))
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd).padding(24.dp).background(Color.White, CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.Black)
                }
            }
        },
        confirmButton = {},
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
