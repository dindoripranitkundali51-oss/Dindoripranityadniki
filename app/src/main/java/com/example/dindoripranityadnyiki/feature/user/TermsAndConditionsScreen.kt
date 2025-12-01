package com.example.dindoripranityadnyiki.feature.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.core.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndConditionsScreen(navController: NavController) {

    val scroll = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Terms & Conditions",
                        color = Color.Companion.White,
                        fontWeight = FontWeight.Companion.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Companion.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFB71C1C),
                    titleContentColor = Color.Companion.White
                )
            )
        }
    ) { inner ->

        Column(
            modifier = Modifier.Companion
                .padding(inner)
                .fillMaxSize()
                .background(Color(0xFFF7F9FC))
                .verticalScroll(scroll)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Header Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Companion.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.Companion.fillMaxWidth()
            ) {
                Column(modifier = Modifier.Companion.padding(16.dp)) {
                    Text(
                        "Dindori Pranit Yadnyiki – Official App",
                        fontWeight = FontWeight.Companion.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0D47A1)
                    )
                    Text(
                        "Last updated: 01 Nov 2025",
                        fontSize = 12.sp,
                        color = Color(0xFF607D8B),
                        modifier = Modifier.Companion.padding(top = 2.dp)
                    )
                    Text(
                        "कृपया हा मजकूर काळजीपूर्वक वाचा. ॲप वापरणे म्हणजे तुम्ही या नियमांना सहमती देता.",
                        color = Color(0xFF37474F),
                        fontSize = 13.sp,
                        modifier = Modifier.Companion.padding(top = 6.dp)
                    )
                }
            }

            // TERMS SECTIONS
            TermsSection(
                "1. App Purpose",
                "हे ॲप भाविकांसाठी पूजा बुकिंग आणि गुरुजी सेवा व्यवस्थापनासाठी आहे. " +
                        "अधिकृत वेद-विज्ञान संशोधन विभागाच्या मार्गदर्शनाखाली पूजा बुकिंग याच ॲपद्वारे करावी."
            )

            TermsSection(
                "2. User Account & Authenticity",
                "नोंदणीवेळी दिलेली माहिती अचूक असणे आवश्यक आहे. " +
                        "तुमच्या लॉगिन माहितीची सुरक्षा तुमच्या जबाबदारीवर आहे."
            )

            TermsSection(
                "3. Pooja Booking & Confirmation",
                "बुकिंग Pending → Approved झाल्यावरच अंतिम मानले जाते. " +
                        "तांत्रिक किंवा प्रशासनिक कारणास्तव बुकिंग नाकारले जाऊ शकते."
            )

            TermsSection(
                "4. Payments (जर लागू असेल तर)",
                "शुल्क वेळोवेळी बदलू शकते. पेमेंट गेटवे संबंधित अडचणींसाठी ॲप जबाबदार नाही."
            )

            TermsSection(
                "5. Cancellation & Rescheduling",
                "Cancel Request फक्त ॲपमधून पाठवावी. " +
                        "प्रत्येक केंद्राची स्वतःची रद्द धोरणे लागू असतील."
            )

            TermsSection(
                "6. Puja Execution Responsibility",
                "पूजा अधिकृत गुरुजींकडून करण्याचा प्रयत्न केला जातो. धार्मिक किंवा आध्यात्मिक परिणामाची हमी दिली जात नाही."
            )

            TermsSection(
                "7. Data Privacy",
                "तुमची माहिती फक्त सेवा पुरवण्यासाठीच वापरली जाईल. तृतीय पक्षाला विकली जाणार नाही."
            )

            TermsSection(
                "8. Notifications",
                "बुकिंग अपडेट्स, स्मरणपत्रे आणि सूचना देण्यासाठी SMS/E-mail/Push Notifications पाठवले जातील."
            )

            TermsSection(
                "9. Prohibited Usage",
                "खोटी माहिती, गैरवापर, आणि धार्मिक भावना दुखावणारे वर्तन निषिद्ध आहे. अकाउंट निलंबित होऊ शकते."
            )

            TermsSection(
                "10. Liability",
                "तांत्रिक अडचणी, नेटवर्क समस्या किंवा तृतीय पक्ष सेवांमुळे होणाऱ्या नुकसानीसाठी ॲप जबाबदार नाही."
            )

            TermsSection(
                "11. Changes to Terms",
                "या अटी वेळोवेळी बदलल्या जाऊ शकतात; ॲप वापर चालू ठेवणे म्हणजे तुम्ही बदल मान्य केलेत."
            )

            TermsSection(
                "12. Contact & Grievance",
                "तक्रारींसाठी Help & Support वापरा किंवा Support ई-मेलद्वारे संपर्क साधा."
            )

            // ACTION BUTTONS
            Row(
                modifier = Modifier.Companion.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { navController.navigate(Routes.HELP_SUPPORT) },
                    modifier = Modifier.Companion.weight(1f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(30.dp)
                ) {
                    Icon(
                        Icons.Default.MailOutline,
                        contentDescription = null,
                        modifier = Modifier.Companion.size(18.dp)
                    )
                    Spacer(Modifier.Companion.width(6.dp))
                    Text("Support", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = { navController.navigate(Routes.PRIVACY) },
                    modifier = Modifier.Companion.weight(1f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(30.dp)
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.Companion.size(18.dp)
                    )
                    Spacer(Modifier.Companion.width(6.dp))
                    Text("Privacy Policy", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.Companion.height(18.dp))

            Text(
                "App वापरत राहून तुम्ही वरील सर्व अटी मान्य करत आहात.",
                fontSize = 12.sp,
                color = Color(0xFF607D8B),
                textAlign = TextAlign.Companion.Center,
                modifier = Modifier.Companion.fillMaxWidth()
            )

            Spacer(modifier = Modifier.Companion.height(20.dp))
        }
    }
}

@Composable
private fun TermsSection(title: String, body: String) {

    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Companion.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.Companion.fillMaxWidth()
    ) {
        Column(modifier = Modifier.Companion.padding(14.dp)) {

            Text(
                text = title,
                fontWeight = FontWeight.Companion.SemiBold,
                fontSize = 15.sp,
                color = Color(0xFF0D47A1)
            )

            Spacer(modifier = Modifier.Companion.height(6.dp))

            Text(
                text = body,
                fontSize = 13.sp,
                color = Color(0xFF37474F),
                lineHeight = 18.sp
            )
        }
    }
}