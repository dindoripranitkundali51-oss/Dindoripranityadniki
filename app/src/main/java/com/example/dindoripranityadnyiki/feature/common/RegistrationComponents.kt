@file:Suppress("DEPRECATION")

package com.example.dindoripranityadnyiki.feature.common

import android.location.Geocoder
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.window.Popup
import com.example.dindoripranityadnyiki.core.design.*
import com.example.dindoripranityadnyiki.core.util.RateLimiters
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class SmartAddressResult(
    val address: String,
    val district: String,
    val pincode: String,
    val lat: Double,
    val lng: Double,
    val place: Place
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val sacredCopper = Color(0xFFE65100)
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick?.invoke() },
        label = { Text(label, fontSize = 14.sp) },
        leadingIcon = { 
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                tint = sacredCopper, 
                modifier = Modifier.size(20.dp) 
            ) 
        },
        readOnly = readOnly,
        enabled = onClick == null,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = sacredCopper,
            unfocusedBorderColor = Color.LightGray,
            focusedLabelColor = sacredCopper
        )
    )
}

/**
 * 🛰️ SMART AUTOMATIC ADDRESS FIELD
 * Uses Google Places API to auto-fill District, Pincode, and Lat/Lng.
 */
@Composable
fun SmartAddressField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    onPlaceSelected: (Place) -> Unit,
    onSmartAddressSelected: ((SmartAddressResult) -> Unit)? = null,
    error: String? = null
) {
    val context = LocalContext.current
    val sacredCopper = Color(0xFFE65100)
    val placesClient = remember { Places.createClient(context) }
    val sessionToken = remember { AutocompleteSessionToken.newInstance() }
    val scope = rememberCoroutineScope()
    
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var showDropdown by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(value) {
        val query = value.trim()
        if (query.isBlank()) {
            predictions = emptyList()
            showDropdown = false
            isSearching = false
            searchError = null
            return@LaunchedEffect
        }
        delay(180)
        
        // Check rate limit before making API call
        val canProceed = RateLimiters.placesAutocomplete.tryAcquire()
        if (!canProceed) {
            val waitTime = RateLimiters.placesAutocomplete.getTimeUntilNextRequest()
            delay(waitTime)
            // Retry after waiting
            if (!RateLimiters.placesAutocomplete.tryAcquire()) {
                predictions = emptyList()
                showDropdown = false
                isSearching = false
                searchError = "Too many requests. Please wait a moment."
                return@LaunchedEffect
            }
        }
        
        isSearching = true
        searchError = null
        val request = FindAutocompletePredictionsRequest.builder()
            .setCountries("IN")
            .setSessionToken(sessionToken)
            .setQuery(query)
            .build()
        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                predictions = response.autocompletePredictions.take(8)
                showDropdown = predictions.isNotEmpty()
                isSearching = false
            }
            .addOnFailureListener { err ->
                predictions = emptyList()
                showDropdown = false
                isSearching = false
                searchError = err.localizedMessage ?: "Address search failed. Please try again."
            }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        DivineTextField(
            value = value,
            onValueChange = { query ->
                onValueChange(query)
            },
            label = label,
            icon = Icons.Default.Home,
            error = error
        )

        if (isSearching) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                color = sacredCopper
            )
        }
        if (!searchError.isNullOrBlank()) {
            Text(
                text = "Address search failed. Please type more details.",
                color = MaterialTheme.colorScheme.error,
                style = DivineTypography.labelSmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (showDropdown) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Popup(
                    alignment = Alignment.TopStart,
                    onDismissRequest = { showDropdown = false }
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .heightIn(max = 250.dp),
                        elevation = CardDefaults.cardElevation(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                    ) {
                        LazyColumn {
                            items(predictions) { prediction ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val placeFields = listOf(
                                                Place.Field.ID, 
                                                Place.Field.NAME, 
                                                Place.Field.ADDRESS, 
                                                Place.Field.LAT_LNG,
                                                Place.Field.ADDRESS_COMPONENTS
                                            )
                                            val fetchRequest = FetchPlaceRequest.newInstance(prediction.placeId, placeFields)
                                            placesClient.fetchPlace(fetchRequest).addOnSuccessListener { response ->
                                                val place = response.place
                                                scope.launch {
                                                    val resolved = resolveSmartAddress(context, place)
                                                    if (onSmartAddressSelected != null) {
                                                        onSmartAddressSelected(resolved)
                                                    } else {
                                                        onPlaceSelected(place)
                                                    }
                                                    showDropdown = false
                                                    predictions = emptyList()
                                                }
                                            }
                                        }
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = prediction.getPrimaryText(null).toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = prediction.getSecondaryText(null).toString(),
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun resolveSmartAddress(context: android.content.Context, place: Place): SmartAddressResult {
    val lat = place.latLng?.latitude ?: 0.0
    val lng = place.latLng?.longitude ?: 0.0
    var district = ""
    var pincode = ""

    place.addressComponents?.asList()?.forEach { component ->
        when {
            component.types.contains("postal_code") -> pincode = component.name.filter { it.isDigit() }.take(6)
            component.types.contains("administrative_area_level_2") -> district = component.name
            component.types.contains("locality") && district.isBlank() -> district = component.name
            component.types.contains("administrative_area_level_3") && district.isBlank() -> district = component.name
        }
    }

    val addressText = place.address ?: place.name ?: ""
    if (pincode.isBlank()) {
        pincode = Regex("\\b[1-9][0-9]{5}\\b").find(addressText)?.value.orEmpty()
    }

    if ((pincode.isBlank() || district.isBlank()) && lat != 0.0 && lng != 0.0) {
        withContext(Dispatchers.IO) {
            runCatching {
                val geocoder = Geocoder(context, Locale("en", "IN"))
                val address = geocoder.getFromLocation(lat, lng, 5).orEmpty().firstOrNull {
                    !it.postalCode.isNullOrBlank() || !it.subAdminArea.isNullOrBlank()
                }
                if (address != null) {
                    if (pincode.isBlank()) pincode = address.postalCode.orEmpty().filter { it.isDigit() }.take(6)
                    if (district.isBlank()) district = address.subAdminArea ?: address.locality ?: address.adminArea ?: ""
                }
            }
        }
    }

    return SmartAddressResult(
        address = addressText,
        district = district,
        pincode = pincode,
        lat = lat,
        lng = lng,
        place = place
    )
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = DivineTypography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE65100),
            letterSpacing = 0.5.sp
        ),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun TactileSelectionTrigger(
    label: String,
    value: String,
    placeholder: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sacredCopper = Color(0xFFE65100)
    val hasValue = value.isNotEmpty()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = DivineTypography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            ),
            modifier = Modifier.padding(start = 4.dp)
        )
        
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (hasValue) sacredCopper else Color.LightGray.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (hasValue) sacredCopper else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                
                Text(
                    text = if (hasValue) value else placeholder,
                    style = DivineTypography.bodyLarge.copy(
                        color = if (hasValue) Color(0xFF212121) else Color.Gray.copy(alpha = 0.7f),
                        fontSize = 15.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
                
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = if (hasValue) sacredCopper else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDistrictSelectionSheet(
    title: String,
    options: List<String>,
    selectedValue: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = title,
                style = DivineTypography.titleLarge.copy(fontWeight = FontWeight.Black),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Box(modifier = Modifier.heightIn(max = 400.dp)) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(options) { option ->
                        val isSelected = option == selectedValue
                        
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(option) },
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = option,
                                    style = DivineTypography.bodyLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected option",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 📄 WhatsApp-style compact Document component - combines number input and upload
 */
@Composable
fun DocumentUploadWithNumber(
    label: String,
    numberValue: String,
    onNumberChange: (String) -> Unit,
    isUploaded: Boolean,
    onUploadClick: () -> Unit,
    icon: ImageVector,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    numberPlaceholder: String = "",
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isUploaded) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Label + Upload Status in one row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = label,
                        style = DivineTypography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    )
                }
                Surface(
                    onClick = onUploadClick,
                    shape = RoundedCornerShape(20.dp),
                    color = if (isUploaded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isUploaded) Icons.Outlined.CheckCircle else Icons.Outlined.FileUpload,
                            contentDescription = if (isUploaded) "Already uploaded" else "Upload document",
                            tint = if (isUploaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isUploaded) "Uploaded" else "Upload",
                            color = if (isUploaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            // Single clean number input field
            DivineTextField(
                value = numberValue,
                onValueChange = onNumberChange,
                label = numberPlaceholder.ifBlank { "$label Number" },
                icon = icon,
                keyboardOptions = keyboardOptions
            )
        }
    }
}
