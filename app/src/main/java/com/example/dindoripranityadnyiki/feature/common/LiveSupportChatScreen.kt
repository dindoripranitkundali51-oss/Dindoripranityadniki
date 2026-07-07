package com.example.dindoripranityadnyiki.feature.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bot
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.LocalAppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveSupportChatScreen(
    navController: NavController,
    viewModel: LiveSupportChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isMarathi = LocalAppLanguage.current == "Marathi"
    var typedText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val primaryColor = Color(0xFFE05638) // Sacred Copper

    val faqOptions = if (isMarathi) {
        listOf("नमस्कार", "मदत", "बुकिंग कशी करावी?", "दक्षिणा ऑनलाईन पे कशी करावी?", "पावती मिळवा")
    } else {
        listOf("Hello", "Help", "How to book a Pooja?", "Online Payment", "Download Receipt")
    }

    DisposableEffect(Unit) {
        viewModel.startChatPolling()
        onDispose {
            viewModel.stopChatPolling()
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SupportAgent, null, tint = primaryColor, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isMarathi) "श्री स्वामी समर्थ हेल्पडेस्क" else "Swami Samarth Helpdesk",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (isMarathi) "थेट संपर्क आणि AI सहाय्यक" else "Live chat & automated support",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            
            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.messages.isEmpty() && !uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isMarathi) "सपोर्ट चॅट सुरू करण्यासाठी खाली प्रश्न विचारा." else "Ask a question below to start support chat.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                items(uiState.messages) { message ->
                    val isMe = message.senderId != "Admin" && message.senderId != "AI_Chatbot"
                    val isBot = message.senderId == "AI_Chatbot"

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isMe) 16.dp else 0.dp,
                                        bottomEnd = if (isMe) 0.dp else 16.dp
                                    )
                                )
                                .background(
                                    if (isMe) primaryColor else if (isBot) Color(0xFFFFF8E1) else Color(0xFFF1F5F9)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isBot) Color(0xFFFFE082) else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isBot) {
                                    Icon(
                                        imageVector = Icons.Default.Bot,
                                        contentDescription = null,
                                        tint = Color(0xFFF57C00),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = message.senderName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (isMe) Color.White.copy(alpha = 0.8f) else Color.Gray
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = message.message,
                                fontSize = 13.sp,
                                color = if (isMe) Color.White else Color.Black
                            )
                        }
                    }
                }
            }

            // Quick FAQs Row
            if (uiState.messages.isEmpty()) {
                Text(
                    text = if (isMarathi) "नेहमी विचारले जाणारे प्रश्न (FAQs):" else "Quick Help (FAQs):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    faqOptions.take(3).forEach { option ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFF3E0))
                                .border(1.dp, Color(0xFFFFE0B2), RoundedCornerShape(12.dp))
                                .clickable { viewModel.sendMessage(option) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(option, fontSize = 11.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Send Input Bar
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = typedText,
                        onValueChange = { typedText = it },
                        placeholder = { Text(if (isMarathi) "तुमचा संदेश टाईप करा..." else "Type message...") },
                        maxLines = 3,
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = primaryColor
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )

                    IconButton(
                        onClick = {
                            if (typedText.isNotBlank()) {
                                viewModel.sendMessage(typedText)
                                typedText = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = primaryColor),
                        enabled = typedText.isNotBlank() && !uiState.isSending,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
