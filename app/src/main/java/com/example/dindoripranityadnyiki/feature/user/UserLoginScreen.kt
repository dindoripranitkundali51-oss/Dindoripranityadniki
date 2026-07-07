package com.example.dindoripranityadnyiki.feature.user

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import androidx.datastore.preferences.core.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.LocalAppLanguage
import com.example.dindoripranityadnyiki.R
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import com.example.dindoripranityadnyiki.core.data.dataStore
import com.example.dindoripranityadnyiki.core.design.*
import com.example.dindoripranityadnyiki.core.navigation.Routes
import com.example.dindoripranityadnyiki.feature.common.*
import kotlinx.coroutines.launch

@Composable
fun UserLoginScreen(
    navController: NavController,
    viewModel: UserLoginViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isMarathi = LocalAppLanguage.current == "Marathi"
    val uiState by viewModel.uiState.collectAsState()

    var emailOrMobile by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showForgotDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val mobileRegex = "^[6-9]\\d{9}$".toRegex()
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]{2,}$".toRegex(RegexOption.IGNORE_CASE)

    val errorFillAll = stringResource(R.string.error_fill_all)
    val errorInvalidInput = stringResource(R.string.error_invalid_email_mobile)

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
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
                title = stringResource(R.string.login_welcome),
                subtitle = stringResource(R.string.login_subtitle), 
                chipText = if (isMarathi) "English" else "मराठी",
                onChipClick = {
                    scope.launch {
                        context.dataStore.edit { it[PrefKeys.LANGUAGE] = if (isMarathi) "en" else "mr" }
                        (context as? Activity)?.recreate()
                    }
                },
                height = 180,
                curveStart = true
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DivineTextField(
                    value = emailOrMobile,
                    onValueChange = { emailOrMobile = it.trim() },
                    label = stringResource(R.string.email_or_mobile),
                    icon = Icons.Filled.Person,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                )

                Spacer(Modifier.height(20.dp)) 

                DivineTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.password),
                    icon = Icons.Filled.Lock,
                    isPassword = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                )

                Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.CenterEnd) {
                    Text(
                        text = stringResource(R.string.forgot_password),
                        style = DivineTypography.labelLarge.copy(
                            color = SacredCopper,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        modifier = Modifier.clickable { showForgotDialog = true }
                    )
                }

                Spacer(Modifier.height(48.dp)) 

                PremiumAuthButton(
                    text = stringResource(R.string.login_button),
                    loading = uiState.isLoading,
                    enabled = !uiState.isLoading,
                    onClick = {
                        val input = emailOrMobile.trim()
                        val isValid = input.matches(emailRegex) || input.matches(mobileRegex)
                        
                        if (input.isBlank() || password.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar(errorFillAll) }
                            return@PremiumAuthButton
                        }
                        
                        if (!isValid) {
                            scope.launch { snackbarHostState.showSnackbar(errorInvalidInput) }
                            return@PremiumAuthButton
                        }

                        viewModel.login(context, input, password) { role ->
                            if (role == "guruji") {
                                navController.navigate(Routes.GURUJI_DASHBOARD) { popUpTo(0) }
                            } else {
                                navController.navigate(Routes.USER_HOME) { popUpTo(0) }
                            }
                        }
                    }
                )

                Spacer(Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.new_here_question),
                        style = DivineTypography.bodyLarge.copy(color = Color.Gray, fontSize = 14.sp)
                    )
                    Text(
                        text = " " + stringResource(R.string.create_account),
                        style = DivineTypography.bodyLarge.copy(
                            color = SacredCopper,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        ),
                        modifier = Modifier.clickable { navController.navigate(Routes.REGISTRATION) }
                    )
                }

                Spacer(Modifier.height(56.dp)) 

                SecondaryAuthButton(
                    text = stringResource(R.string.guruji_login_click),
                    onClick = { navController.navigate(Routes.GURUJI_LOGIN) }
                )
                
                Spacer(Modifier.height(32.dp))
            }
        }

        if (showForgotDialog) {
            DivineForgotPasswordDialog(
                onDismiss = { showForgotDialog = false },
                onReset = { input ->
                    val isValid = input.matches(emailRegex) || input.matches(mobileRegex)
                    if (isValid) {
                        viewModel.resetPassword(context, input) { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                            showForgotDialog = false
                        }
                    } else {
                        scope.launch { snackbarHostState.showSnackbar(errorInvalidInput) }
                    }
                }
            )
        }
    }
}
