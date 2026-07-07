package com.example.dindoripranityadnyiki.feature.guruji

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.LocalAppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KycUploadScreen(
    navController: NavController,
    viewModel: KycUploadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isMarathi = LocalAppLanguage.current == "Marathi"
    val context = LocalContext.current
    val coppersacred = Color(0xFFE05638)

    Scaffold(
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isMarathi) "KYC दस्तऐवज पडताळणी" else "KYC OCR Verification",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = coppersacred,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isMarathi) "डिजिटल केवायसी (KYC)" else "Instant Digital KYC Check",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.Black
            )

            Text(
                text = if (isMarathi) "तुमचे पॅन कार्ड आणि आधार नंबर टाकून झटपट OCR पडताळणी करा." else "Enter details to trigger instant AI OCR document verification.",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PAN Input
            OutlinedTextField(
                value = uiState.panNumber,
                onValueChange = { viewModel.updatePan(it) },
                label = { Text(if (isMarathi) "पॅन कार्ड क्रमांक" else "PAN Card Number") },
                leadingIcon = { Icon(Icons.Default.Description, null) },
                isError = uiState.panError != null,
                supportingText = uiState.panError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Aadhar Input
            OutlinedTextField(
                value = uiState.aadharNumber,
                onValueChange = { viewModel.updateAadhar(it) },
                label = { Text(if (isMarathi) "आधार कार्ड क्रमांक" else "Aadhar Card Number") },
                leadingIcon = { Icon(Icons.Default.Fingerprint, null) },
                isError = uiState.aadharError != null,
                supportingText = uiState.aadharError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(30.dp))

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            if (uiState.isSuccess) {
                Text(
                    text = if (isMarathi) "अभिनंदन! केवायसी यशस्वीरित्या मंजूर झाली." else "KYC Approved Successfully!",
                    color = Color(0xFF2E7D32),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = {
                    viewModel.submitKyc {
                        Toast.makeText(context, "OCR Verified successfully!", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = coppersacred),
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = if (isMarathi) "पडताळणी करा" else "Verify Documents",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
