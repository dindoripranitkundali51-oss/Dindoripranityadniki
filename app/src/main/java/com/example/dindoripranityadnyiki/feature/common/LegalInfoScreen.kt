package com.example.dindoripranityadnyiki.feature.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.LocalAppLanguage
import com.example.dindoripranityadnyiki.core.design.DivineScreen
import com.example.dindoripranityadnyiki.core.design.DivineTopBar
import com.example.dindoripranityadnyiki.core.design.DivineTypography
import com.example.dindoripranityadnyiki.core.design.SocialUi

@Composable
fun LegalInfoScreen(navController: NavController, type: String) {
    val isMarathi = LocalAppLanguage.current == "Marathi"
    val isPrivacy = type == "privacy"
    val title = when {
        isPrivacy && isMarathi -> "गोपनीयता धोरण"
        isPrivacy -> "Privacy Policy"
        isMarathi -> "सेवा अटी"
        else -> "Terms of Service"
    }
    val sections = if (isPrivacy) privacySections(isMarathi) else termsSections(isMarathi)

    DivineScreen(
        topBar = {
            DivineTopBar(
                title = title,
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = SocialUi.ScreenHorizontal)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(24.dp))
            sections.forEach { section ->
                Text(
                    text = section.title,
                    style = DivineTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = SocialUi.TitleColor
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = section.body,
                    style = DivineTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private data class LegalSection(val title: String, val body: String)

private fun privacySections(isMarathi: Boolean) = if (isMarathi) {
    listOf(
        LegalSection(
            "आम्ही कोणती माहिती घेतो",
            "सेवा बुकिंग आणि व्यवस्थापनासाठी खाते, संपर्क, बुकिंग, पेमेंट पुष्टी, लोकेशन, सपोर्ट, नोटिफिकेशन आणि फीडबॅक माहिती वापरली जाते."
        ),
        LegalSection(
            "संवेदनशील कागदपत्रे",
            "गुरुजींची KYC कागदपत्रे फक्त पडताळणी आणि कार्यवाहीसाठी वापरली जातात. प्रवेश फक्त अधिकृत ॲडमिन आणि संबंधित गुरुजी खात्यापुरता मर्यादित असतो."
        ),
        LegalSection(
            "पेमेंट आणि पावत्या",
            "व्यवहार पडताळणी, पावती तयार करणे आणि आर्थिक नोंदी ठेवण्यासाठी ऑनलाइन पेमेंट आयडी, पावती snapshot आणि वाटप तपशील साठवले जातात."
        ),
        LegalSection(
            "डेटा नियंत्रण",
            "दुरुस्ती, खाते मदत किंवा deletion विनंतीसाठी युजर सपोर्टशी संपर्क करू शकतात, जिथे कायदेशीर आणि कार्यवाहीदृष्ट्या परवानगी असेल."
        )
    )
} else {
    listOf(
        LegalSection(
            "Information we collect",
            "We collect account, contact, booking, payment confirmation, location, support, notification, and feedback details needed to provide seva booking and administration."
        ),
        LegalSection(
            "Sensitive documents",
            "Guruji KYC documents are used only for verification and operational compliance. Access is restricted to authorized administrators and the concerned Guruji account."
        ),
        LegalSection(
            "Payments and receipts",
            "Online payment identifiers, receipt snapshots, and settlement split details are stored to verify transactions, generate receipts, and maintain financial records."
        ),
        LegalSection(
            "Data control",
            "Users may contact support for corrections, account help, or deletion requests where legally and operationally permitted."
        )
    )
}

private fun termsSections(isMarathi: Boolean) = if (isMarathi) {
    listOf(
        LegalSection(
            "सेवा बुकिंग",
            "नेमलेले गुरुजी सेवा नीट पूर्ण करू शकतील यासाठी युजरने संपर्क, पत्ता, तारीख आणि लोकेशन तपशील अचूक द्यावेत."
        ),
        LegalSection(
            "ऑनलाइन पेमेंट",
            "पेमेंट गेटवे आणि backend पडताळणी यशस्वी झाल्यानंतरच बुकिंग पावती आणि settlement साठी पुष्टी केले जाते."
        ),
        LegalSection(
            "गुरुजी जबाबदाऱ्या",
            "गुरुजींनी availability, profile, service area आणि completion status अचूक ठेवणे आवश्यक आहे. Completion OTP सेवा प्रत्यक्ष पूर्ण झाल्यानंतरच वापरावा."
        ),
        LegalSection(
            "सपोर्ट आणि तक्रारी",
            "कार्यवाही, पेमेंट failure, rescheduling, cancellation किंवा पावती समस्या सपोर्टमधून administrative review साठी नोंदवाव्यात."
        )
    )
} else {
    listOf(
        LegalSection(
            "Seva bookings",
            "Users must provide accurate contact, address, date, and location details so that the assigned Guruji can complete the seva smoothly."
        ),
        LegalSection(
            "Online payment",
            "Bookings are confirmed for receipt and settlement only after online payment verification succeeds through the payment gateway and backend verification."
        ),
        LegalSection(
            "Guruji responsibilities",
            "Gurujis must keep availability, profile, service area, and completion status accurate. Completion OTP must be used only after the seva is actually completed."
        ),
        LegalSection(
            "Support and disputes",
            "Operational issues, payment failures, rescheduling, cancellation, and receipt problems should be raised through support for administrative review."
        )
    )
}
