package com.example.dindoripranityadnyiki.feature.guruji

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.LocalAppLanguage
import com.example.dindoripranityadnyiki.R
import com.example.dindoripranityadnyiki.core.data.Constants
import com.example.dindoripranityadnyiki.core.design.DivineScreen
import com.example.dindoripranityadnyiki.core.design.DivineTypography
import com.example.dindoripranityadnyiki.core.design.SacredCopper
import com.example.dindoripranityadnyiki.core.design.DivineTextField
import com.example.dindoripranityadnyiki.feature.common.PremiumAuthButton
import com.example.dindoripranityadnyiki.feature.common.PremiumAuthHeader
import com.example.dindoripranityadnyiki.feature.common.DocumentUploadWithNumber
import com.example.dindoripranityadnyiki.feature.common.SmartAddressField
import com.example.dindoripranityadnyiki.feature.common.SmartAddressResult
import com.example.dindoripranityadnyiki.feature.common.TactileSelectionTrigger
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GurujiRegistrationScreen(
    onAffidavitDownloadClick: () -> Unit,
    onSubmitSuccess: () -> Unit,
    navController: NavController,
    viewModel: GurujiRegistrationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isMarathi = LocalAppLanguage.current == "Marathi"
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var isAgreed by rememberSaveable { mutableStateOf(false) }
    var activeDocKey by rememberSaveable { mutableStateOf<String?>(null) }
    var showExpertiseSheet by rememberSaveable { mutableStateOf(false) }
    var showWorkDistrictSheet by rememberSaveable { mutableStateOf(false) }

    val mobileRegex = "^[6-9]\\d{9}$".toRegex()
    val nameRegex = "^[\\u0900-\\u097Fa-zA-Z\\s]{3,50}$".toRegex()
    val pincodeRegex = "^\\d{6}$".toRegex()
    val aadhaarRegex = "^\\d{12}$".toRegex()
    val panRegex = "^[A-Z]{5}\\d{4}[A-Z]$".toRegex()

    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { resultUri -> activeDocKey?.let { key -> viewModel.selectedDocs[key] = resultUri } }
    }

    LaunchedEffect(uiState.success) {
        if (uiState.success) onSubmitSuccess()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    DivineScreen(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            PremiumAuthHeader(
                title = stringResource(R.string.guruji_reg_title),
                subtitle = stringResource(R.string.guruji_reg_subtitle),
                chipText = "",
                showChip = false,
                showBack = true,
                onBack = { navController.popBackStack() },
                height = 180,
                curveStart = false
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                GurujiPersonalInfoSection(
                    uiState = uiState,
                    password = password,
                    confirmPassword = confirmPassword,
                    onPasswordChange = { password = it },
                    onConfirmPasswordChange = { confirmPassword = it },
                    onFieldChange = viewModel::updateField,
                    onPlaceSelected = viewModel::onPlaceSelected,
                    onSmartAddressSelected = viewModel::onSmartAddressSelected
                )

                GurujiWorkInfoSection(
                    experience = uiState.experience,
                    selectedDistrictCount = viewModel.selectedDistricts.size,
                    selectedExpertiseCount = viewModel.selectedExpertises.count { it != Constants.POOJA_SEPARATOR },
                    isMarathi = isMarathi,
                    onExperienceChange = { viewModel.updateField("experience", it) },
                    onOpenWorkDistricts = { showWorkDistrictSheet = true },
                    onOpenExpertise = { showExpertiseSheet = true }
                )

                GurujiDocumentsSection(
                    uiState = uiState,
                    isMarathi = isMarathi,
                    selectedDocs = viewModel.selectedDocs,
                    onAffidavitDownloadClick = onAffidavitDownloadClick,
                    onFieldChange = viewModel::updateField,
                    onPickDocument = { key, mimeType ->
                        activeDocKey = key
                        docPicker.launch(mimeType)
                    }
                )

                GurujiRegistrationSubmitSection(
                    isAgreed = isAgreed,
                    isLoading = uiState.isLoading,
                    loadingMessage = uiState.loadingMessage,
                    onAgreementChange = { isAgreed = it },
                    onSubmit = {
                        val validationMessage = validateGurujiRegistration(
                            uiState = uiState,
                            password = password,
                            confirmPassword = confirmPassword,
                            isAgreed = isAgreed,
                            selectedDistricts = viewModel.selectedDistricts,
                            selectedExpertises = viewModel.selectedExpertises,
                            selectedDocs = viewModel.selectedDocs,
                            mobileRegex = mobileRegex,
                            nameRegex = nameRegex,
                            pincodeRegex = pincodeRegex,
                            aadhaarRegex = aadhaarRegex,
                            panRegex = panRegex,
                            isMarathi = isMarathi
                        )
                        if (validationMessage != null) {
                            scope.launch { snackbarHostState.showSnackbar(validationMessage) }
                        } else {
                            viewModel.registerGuruji(context, password) { }
                        }
                    }
                )

                Spacer(Modifier.width(40.dp))
            }
        }
    }

    if (showWorkDistrictSheet) {
        SelectionSheet(
            title = if (isMarathi) "सेवेचे जिल्हे निवडा" else "Select Work Districts",
            options = Constants.DISTRICTS.map { if (isMarathi) it.second else it.first },
            selectedItems = viewModel.selectedDistricts,
            onDismiss = { showWorkDistrictSheet = false },
            onDone = { showWorkDistrictSheet = false }
        )
    }

    if (showExpertiseSheet) {
        SelectionSheet(
            title = stringResource(R.string.select_pooja),
            options = uiState.poojaList,
            isLoading = uiState.isPoojaLoading,
            selectedItems = viewModel.selectedExpertises,
            onDismiss = { showExpertiseSheet = false },
            onDone = { showExpertiseSheet = false },
            onRetry = { viewModel.loadPoojas() }
        )
    }
}

@Composable
private fun GurujiPersonalInfoSection(
    uiState: GurujiRegistrationUiState,
    password: String,
    confirmPassword: String,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onFieldChange: (String, String) -> Unit,
    onPlaceSelected: (com.google.android.libraries.places.api.model.Place) -> Unit,
    onSmartAddressSelected: (SmartAddressResult) -> Unit
) {
    RegistrationSection(stringResource(R.string.section_1_personal)) {
        DivineTextField(
            value = uiState.fullName,
            onValueChange = { onFieldChange("fullName", it) },
            label = stringResource(R.string.full_name),
            icon = Icons.Filled.Person
        )
        DivineTextField(
            value = uiState.mobile,
            onValueChange = {
                if (it.length <= 10) onFieldChange("mobile", it)
            },
            label = stringResource(R.string.mobile_number),
            icon = Icons.Filled.Phone,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
        )
        DivineTextField(
            value = uiState.email,
            onValueChange = { onFieldChange("email", it) },
            label = stringResource(R.string.email_address),
            icon = Icons.Filled.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        )
        SmartAddressField(
            value = uiState.address,
            onValueChange = { onFieldChange("address", it) },
            label = stringResource(R.string.full_address),
            onPlaceSelected = onPlaceSelected,
            onSmartAddressSelected = onSmartAddressSelected
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1.5f)) {
                DivineTextField(
                    value = uiState.district,
                    onValueChange = { },
                    label = stringResource(R.string.select_district),
                    icon = Icons.Filled.Map,
                    readOnly = true
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                DivineTextField(
                    value = uiState.pincode,
                    onValueChange = { },
                    label = stringResource(R.string.pincode),
                    icon = Icons.Filled.LocationOn,
                    readOnly = true
                )
            }
        }
        DivineTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.create_password),
            icon = Icons.Filled.Lock,
            isPassword = true
        )
        DivineTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = stringResource(R.string.confirm_password),
            icon = Icons.Filled.Lock,
            isPassword = true
        )
    }
}

@Composable
private fun GurujiWorkInfoSection(
    experience: String,
    selectedDistrictCount: Int,
    selectedExpertiseCount: Int,
    isMarathi: Boolean,
    onExperienceChange: (String) -> Unit,
    onOpenWorkDistricts: () -> Unit,
    onOpenExpertise: () -> Unit
) {
    RegistrationSection(stringResource(R.string.section_2_work)) {
        DivineTextField(
            value = experience,
            onValueChange = onExperienceChange,
            label = stringResource(R.string.experience_years),
            icon = Icons.Filled.Work
        )
        TactileSelectionTrigger(
            label = if (isMarathi) "सेवेचे जिल्हे" else "Work Districts",
            value = if (selectedDistrictCount == 0) "" else "$selectedDistrictCount Selected",
            placeholder = if (isMarathi) "जिल्हे निवडा" else "Select Districts...",
            icon = Icons.Filled.Language,
            onClick = onOpenWorkDistricts
        )
        TactileSelectionTrigger(
            label = stringResource(R.string.select_pooja),
            value = if (selectedExpertiseCount == 0) "" else "$selectedExpertiseCount Selected",
            placeholder = if (isMarathi) "पूजा निवडा" else "Select Pooja...",
            icon = Icons.AutoMirrored.Filled.FactCheck,
            onClick = onOpenExpertise
        )
    }
}

@Composable
private fun GurujiDocumentsSection(
    uiState: GurujiRegistrationUiState,
    isMarathi: Boolean,
    selectedDocs: Map<String, android.net.Uri>,
    onAffidavitDownloadClick: () -> Unit,
    onFieldChange: (String, String) -> Unit,
    onPickDocument: (String, String) -> Unit
) {
    RegistrationSection(
        title = stringResource(R.string.section_3_docs),
        action = {
            TextButton(onClick = onAffidavitDownloadClick) {
                Icon(Icons.Outlined.Download, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (isMarathi) "डाउनलोड प्रतिज्ञापत्र" else "Download Affidavit",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) {
        DocumentUploadWithNumber(
            label = stringResource(R.string.aadhaar_card),
            numberValue = uiState.aadhaarNumber,
            onNumberChange = { onFieldChange("aadhaar", it) },
            isUploaded = selectedDocs.containsKey("aadhaar"),
            onUploadClick = { onPickDocument("aadhaar", "image/*") },
            icon = Icons.Filled.Badge,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            numberPlaceholder = if (isMarathi) "आधार क्रमांक" else "Aadhaar Number"
        )
        DocumentUploadWithNumber(
            label = stringResource(R.string.pan_card),
            numberValue = uiState.panNumber,
            onNumberChange = { onFieldChange("pan", it) },
            isUploaded = selectedDocs.containsKey("pan"),
            onUploadClick = { onPickDocument("pan", "image/*") },
            icon = Icons.Filled.CreditCard,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            numberPlaceholder = if (isMarathi) "PAN क्रमांक" else "PAN Number"
        )
        listOf(
            "affidavit" to stringResource(R.string.affidavit),
            "photo" to stringResource(R.string.photo)
        ).forEach { (key, label) ->
            DocUploadRow(label, selectedDocs.containsKey(key)) {
                onPickDocument(key, if (key == "affidavit") "application/pdf" else "image/*")
            }
        }
    }
}

@Composable
private fun GurujiRegistrationSubmitSection(
    isAgreed: Boolean,
    isLoading: Boolean,
    loadingMessage: String,
    onAgreementChange: (Boolean) -> Unit,
    onSubmit: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { onAgreementChange(!isAgreed) }
        ) {
            Checkbox(checked = isAgreed, onCheckedChange = onAgreementChange)
            Text(stringResource(R.string.i_accept_terms), fontSize = 14.sp)
        }

        if (isLoading && loadingMessage.isNotEmpty()) {
            Text(
                text = loadingMessage,
                color = SacredCopper,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        PremiumAuthButton(
            text = stringResource(R.string.submit_reg_app),
            loading = isLoading,
            enabled = !isLoading,
            onClick = onSubmit
        )
    }
}

private fun validateGurujiRegistration(
    uiState: GurujiRegistrationUiState,
    password: String,
    confirmPassword: String,
    isAgreed: Boolean,
    selectedDistricts: List<String>,
    selectedExpertises: List<String>,
    selectedDocs: Map<String, android.net.Uri>,
    mobileRegex: Regex,
    nameRegex: Regex,
    pincodeRegex: Regex,
    aadhaarRegex: Regex,
    panRegex: Regex,
    isMarathi: Boolean
): String? {
    return when {
        !isAgreed -> if (isMarathi) "कृपया नियम आणि अटी मान्य करा" else "Please accept terms and conditions"
        !uiState.fullName.trim().matches(nameRegex) -> if (isMarathi) "वैध नाव टाका" else "Invalid name"
        !uiState.mobile.matches(mobileRegex) -> if (isMarathi) "वैध मोबाईल नंबर टाका" else "Invalid mobile number"
        uiState.address.trim().length < 8 || uiState.lat == 0.0 || uiState.lng == 0.0 -> if (isMarathi) "कृपया नकाशावरून अचूक पत्ता निवडा" else "Select exact address from map/search"
        uiState.district.isBlank() -> if (isMarathi) "जिल्हा निवडा" else "Select district"
        !uiState.pincode.matches(pincodeRegex) -> if (isMarathi) "वैध पिनकोड टाका" else "Invalid pincode"
        selectedDistricts.isEmpty() -> if (isMarathi) "सेवेचे जिल्हे निवडा" else "Select work districts"
        selectedExpertises.none { it != Constants.POOJA_SEPARATOR } -> if (isMarathi) "किमान एक पूजा निवडा" else "Select at least one pooja"
        (uiState.experience.toIntOrNull() ?: -1) < 0 -> if (isMarathi) "अनुभव तपासा" else "Check experience"
        !uiState.aadhaarNumber.matches(aadhaarRegex) -> if (isMarathi) "वैध 12 अंकी आधार क्रमांक टाका" else "Enter valid 12 digit Aadhaar number"
        !uiState.panNumber.matches(panRegex) -> if (isMarathi) "वैध PAN क्रमांक टाका" else "Enter valid PAN number"
        !selectedDocs.containsKey("aadhaar") || !selectedDocs.containsKey("pan") || !selectedDocs.containsKey("affidavit") -> if (isMarathi) "सर्व KYC कागदपत्रे अपलोड करा" else "Upload all KYC documents"
        password.length < 6 -> if (isMarathi) "पासवर्ड किमान 6 अक्षरी हवा" else "Password must be at least 6 characters"
        password != confirmPassword -> if (isMarathi) "पासवर्ड जुळत नाही" else "Passwords do not match"
        else -> null
    }
}

@Composable
private fun RegistrationSection(title: String, action: @Composable (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.Black, color = SacredCopper, fontSize = 17.sp)
            action?.invoke()
        }
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) { content() }
    }
}

@Composable
private fun DocUploadRow(label: String, isUploaded: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isUploaded) Color(0xFFDCFCE7) else Color(0xFFE5E7EB))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isUploaded) Color(0xFFDCFCE7) else Color(0xFFF3F4F6)
                ) {
                    Icon(
                        imageVector = if (isUploaded) Icons.Outlined.CheckCircle else Icons.Outlined.FileUpload,
                        contentDescription = null,
                        tint = if (isUploaded) Color(0xFF16A34A) else Color.Gray,
                        modifier = Modifier.padding(8.dp).size(24.dp)
                    )
                }
                Text(
                    label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1F2937)
                )
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isUploaded) Color(0xFFDCFCE7) else SacredCopper.copy(alpha = 0.1f)
            ) {
                Text(
                    text = if (isUploaded) stringResource(R.string.uploaded) else stringResource(R.string.upload),
                    color = if (isUploaded) Color(0xFF16A34A) else SacredCopper,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionSheet(
    title: String,
    options: List<String>,
    isLoading: Boolean = false,
    selectedItems: MutableList<String>,
    onDismiss: () -> Unit,
    onDone: (List<String>) -> Unit,
    onRetry: (() -> Unit)? = null
) {
    val isMarathi = LocalAppLanguage.current == "Marathi"
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val localSelection = remember { mutableStateListOf<String>().apply { addAll(selectedItems) } }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding()) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 16.dp))

            Box(
                modifier = Modifier
                    .heightIn(min = 150.dp, max = 500.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SacredCopper)
                        Spacer(Modifier.heightIn(min = 12.dp))
                        Text(if (isMarathi) "पूजा लोड होत आहेत..." else "Loading poojas...", color = Color.Gray)
                    }
                } else if (options.isEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isMarathi) "पूजा उपलब्ध नाहीत." else "No poojas available.",
                            color = Color.Gray,
                            style = DivineTypography.bodyMedium
                        )
                        if (onRetry != null) {
                            TextButton(onClick = onRetry) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (isMarathi) "पुन्हा प्रयत्न करा" else "Retry")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(options) { item ->
                            val isSeparator = item == Constants.POOJA_SEPARATOR
                            val isSelected = localSelection.contains(item)

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isSeparator) {
                                        if (isSelected) localSelection.remove(item) else localSelection.add(item)
                                    },
                                color = when {
                                    isSeparator -> Color(0xFFF8FAFC)
                                    isSelected -> SacredCopper.copy(alpha = 0.1f)
                                    else -> Color.Transparent
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = if (isSelected && !isSeparator) androidx.compose.foundation.BorderStroke(1.dp, SacredCopper) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = if (isSeparator) Arrangement.Center else Arrangement.Start
                                ) {
                                    Text(
                                        text = item,
                                        modifier = if (isSeparator) Modifier else Modifier.weight(1f),
                                        fontWeight = if (isSeparator || isSelected) FontWeight.Black else FontWeight.Normal,
                                        color = when {
                                            isSeparator -> Color.Gray
                                            isSelected -> SacredCopper
                                            else -> Color.Black
                                        },
                                        fontSize = if (isSeparator) 13.sp else 16.sp
                                    )
                                    if (isSelected && !isSeparator) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = SacredCopper
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Button(
                onClick = {
                    selectedItems.clear()
                    selectedItems.addAll(localSelection)
                    onDone(localSelection)
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SacredCopper),
                enabled = !isLoading
            ) {
                Text(if (isMarathi) "पूर्ण झाले" else "Done", fontWeight = FontWeight.Bold)
            }
        }
    }
}
