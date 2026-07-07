package com.example.dindoripranityadnyiki.feature.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.dindoripranityadnyiki.core.design.*
import com.example.dindoripranityadnyiki.core.navigation.Routes
import com.example.dindoripranityadnyiki.feature.common.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserRegistrationScreen(
    navController: NavController,
    viewModel: UserRegistrationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isMarathi = LocalAppLanguage.current == "Marathi"
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    val mobileRegex = "^[6-9]\\d{9}$".toRegex()
    val nameRegex = "^[\\u0900-\\u097Fa-zA-Z\\s]{3,50}$".toRegex()
    val pincodeRegex = "^\\d{6}$".toRegex()

    LaunchedEffect(uiState.registrationSuccess) {
        if (uiState.registrationSuccess) {
            navController.navigate(Routes.USER_HOME) {
                popUpTo(Routes.LOGIN) { inclusive = true }
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
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
                title = stringResource(R.string.user_reg_title),
                subtitle = stringResource(R.string.user_reg_subtitle),
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
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // No section headers, just clean continuous fields (WhatsApp style)
                DivineTextField(
                    value = uiState.fullName,
                    onValueChange = { viewModel.updateField("fullName", it) },
                    label = stringResource(R.string.full_name),
                    icon = Icons.Filled.Person,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                
                DivineTextField(
                    value = uiState.mobile,
                    onValueChange = { viewModel.updateField("mobile", it) },
                    label = stringResource(R.string.mobile_number),
                    icon = Icons.Filled.Phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
                )

                DivineTextField(
                    value = uiState.email,
                    onValueChange = { viewModel.updateField("email", it) },
                    label = stringResource(R.string.email_id_optional),
                    icon = Icons.Filled.Email,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                )

                // No divider, continue directly to address
                SmartAddressField(
                    value = uiState.address,
                    onValueChange = { viewModel.updateField("address", it) },
                    label = stringResource(R.string.full_address),
                    onPlaceSelected = { viewModel.onPlaceSelected(it) },
                    onSmartAddressSelected = { viewModel.onSmartAddressSelected(it) }
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1.5f)) {
                        DivineTextField(
                            value = uiState.selectedDistrict,
                            onValueChange = { viewModel.updateField("district", it) },
                            label = stringResource(R.string.select_district),
                            icon = Icons.Filled.Map,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        DivineTextField(
                            value = uiState.pincode,
                            onValueChange = { viewModel.updateField("pincode", it) },
                            label = stringResource(R.string.pincode),
                            icon = Icons.Filled.LocationOn,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                        )
                    }
                }
                
                DivineTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.create_password),
                    icon = Icons.Filled.Lock,
                    isPassword = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next)
                )
                
                DivineTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = stringResource(R.string.confirm_password),
                    icon = Icons.Filled.Lock,
                    isPassword = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                )

                Spacer(Modifier.height(16.dp))

                PremiumAuthButton(
                    text = stringResource(R.string.register_now),
                    loading = uiState.isLoading,
                    enabled = !uiState.isLoading,
                    onClick = {
                        val state = uiState
                        val msg = when {
                            state.fullName.trim().length < 3 -> if(isMarathi) "नाव खूप लहान आहे" else "Name too short"
                            !state.fullName.trim().matches(nameRegex) -> if(isMarathi) "वैध नाव टाका" else "Invalid Name"
                            !state.mobile.matches(mobileRegex) -> if(isMarathi) "वैध १० अंकी मोबाईल नंबर टाका" else "Invalid 10-digit Mobile"
                            state.selectedDistrict.isBlank() -> if(isMarathi) "कृपया जिल्हा टाका" else "Please enter district"
                            !state.pincode.matches(pincodeRegex) -> if(isMarathi) "वैध ६ अंकी पिनकोड टाका" else "Invalid 6-digit Pincode"
                            state.address.trim().length < 8 -> if(isMarathi) "पत्ता अपूर्ण आहे" else "Address is incomplete"
                            state.lat == 0.0 || state.lng == 0.0 -> if(isMarathi) "कृपया लोकेशन अचूक निवडा" else "Please select accurate location"
                            password.length < 6 -> if(isMarathi) "पासवर्ड किमान ६ अक्षरी हवा" else "Password must be at least 6 chars"
                            password != confirmPassword -> if(isMarathi) "पासवर्ड जुळत नाही" else "Passwords do not match"
                            else -> null
                        }

                        if (msg != null) {
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        } else {
                            viewModel.registerUser(password)
                        }
                    }
                )

                Row(modifier = Modifier.padding(top = 8.dp, bottom = 40.dp)) {
                    Text(text = if (isMarathi) "आधीच खाते आहे? " else "Already have an account? ", color = Color.Gray, fontSize = 14.sp)
                    Text(
                        text = stringResource(R.string.login_button),
                        color = Color(0xFFF57C00),
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { navController.navigate(Routes.LOGIN) }
                    )
                }
            }
        }
    }
}
