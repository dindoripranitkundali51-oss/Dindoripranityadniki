package com.example.dindoripranityadnyiki.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*

/* -----------------------------------------------------------
   MODEL + DATA PROVIDER (STATIC PRIVACY POLICY SECTIONS)
----------------------------------------------------------- */

data class PolicySection(
    val title: String,
    val body: String
)

fun getPrivacyPolicySections(): List<PolicySection> = listOf(
    PolicySection(
        title = "1. परिचय (Introduction)",
        body = """
            ही अधिकृत मोबाइल अ‍ॅप सेवा दिंडोरी प्रणित वेद–विज्ञान संशोधन विभागाच्या आध्यात्मिक 
            सेवांसाठी तयार केली आहे. अ‍ॅप वापरताना हे गोपनीयता धोरण लागू होते.
        """.trimIndent()
    ),
    PolicySection(
        title = "2. आम्ही कोणती माहिती गोळा करतो? (Data We Collect)",
        body = """
            • तुमचे नाव, मोबाइल, ई-मेल  
            • बुकिंग माहिती  
            • तांत्रिक माहिती (डिव्हाइस माहिती, अ‍ॅप व्हर्जन)  
            • Support किंवा Help तिकीटमध्ये दिलेली माहिती  
        """.trimIndent()
    ),
    PolicySection(
        title = "3. माहितीचा उपयोग (How We Use Your Data)",
        body = """
            • पूजा बुकिंग प्रक्रिया करण्यासाठी  
            • गुरुजींना आवश्यक माहिती देण्यासाठी  
            • Notifications / Updates पाठवण्यासाठी  
            • सुरक्षा आणि fraud prevention साठी  
            • सेवा सुधारण्यासाठी (anonymous analytics)  
        """.trimIndent()
    ),
    PolicySection(
        title = "4. डेटा शेअरिंग (Data Sharing)",
        body = """
            • केवळ अधिकृत गुरुजी/सेवा सहयोगींना  
            • Google Firebase (cloud storage/processing)  
            • कायद्यानुसार आवश्यक असल्यास सरकारी संस्थांना  
            • कधीही तृतीय पक्षाला data विकला जाणार नाही  
        """.trimIndent()
    ),
    PolicySection(
        title = "5. सुरक्षा (Security)",
        body = """
            • डेटा Google Firebase वर सुरक्षितपणे संग्रहित केला जातो  
            • सर्व नेटवर्क ट्रॅफिक HTTPS/SSL द्वारे encrypted असते  
            • तथापि इंटरनेटवरील कोणतीही माहिती 100% सुरक्षित हमी देता येत नाही  
        """.trimIndent()
    ),
    PolicySection(
        title = "6. तुमचे अधिकार (Your Rights)",
        body = """
            • तुमची प्रोफाइल माहिती पाहणे / अपडेट करणे  
            • अकाउंट बंद करण्याची विनंती  
            • Notifications चालू/बंद करण्याचा पर्याय  
        """.trimIndent()
    ),
    PolicySection(
        title = "7. डेटा Retention",
        body = """
            • कायद्याने आवश्यक कालावधीपर्यंत बुकिंग/पेमेंट नोंदी ठेवल्या जातील  
            • गरज नसलेला डेटा delete किंवा anonymize केला जाईल  
        """.trimIndent()
    ),
    PolicySection(
        title = "8. धोरणातील बदल (Changes)",
        body = """
            • हे धोरण वेळोवेळी अपडेट केले जाऊ शकते  
            • बदल केल्यास अ‍ॅपमध्ये सूचना दिली जाईल  
        """.trimIndent()
    ),
    PolicySection(
        title = "9. Contact",
        body = """
            गोपनीयता धोरणाबाबत प्रश्न असल्यास Help & Support विभागाद्वारे संपर्क करा.
        """.trimIndent()
    )
)

/* -----------------------------------------------------------
   PRIMARY UI SCREEN
----------------------------------------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    navController: NavController
) {
    val scrollState = rememberScrollState()
    val lastUpdated = Date()
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val sections = getPrivacyPolicySections()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Privacy Policy",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFB71C1C)
                )
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F9FC))
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {

            // Header
            Text(
                "Dindori Pranit Yadnyiki — Official App",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = Color(0xFF0D47A1)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                "Last updated: ${formatter.format(lastUpdated)}",
                fontSize = 13.sp,
                color = Color(0xFF616161)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                "कृपया हे गोपनीयता धोरण काळजीपूर्वक वाचा. या अ‍ॅपचा वापर केल्यास आपण हे धोरण स्वीकारता.",
                fontSize = 14.sp,
                color = Color(0xFF37474F)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Sections
            sections.forEach { section ->
                PolicyCard(section)
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Disclaimer: ही माहिती केवळ मार्गदर्शनासाठी आहे. आवश्यक तेव्हा कायदेशीर सल्ला घ्या.",
                color = Color(0xFF757575),
                fontSize = 13.sp
            )
        }
    }
}

/* -----------------------------------------------------------
   CARD COMPOSABLE
----------------------------------------------------------- */

@Composable
fun PolicyCard(section: PolicySection) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Text(
            text = section.title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = Color(0xFF0D47A1)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = section.body,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            color = Color(0xFF37474F)
        )
    }
}
