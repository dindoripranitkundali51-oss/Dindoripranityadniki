package com.example.dindoripranityadnyiki.core.data

object Constants {
    const val AFFIDAVIT_URL = "https://raw.githubusercontent.com/dindoripranitkundali51-oss/affidavit-files/main/affidavit.pdf"
    const val POOJA_SEPARATOR = "--- इतर सेवा ---"

    val DISTRICTS = listOf(
        "Ahilyanagar" to "अहिल्यानगर",
        "Akola" to "अकोला",
        "Amravati" to "अमरावती",
        "Beed" to "बीड",
        "Bhandara" to "भंडारा",
        "Buldhana" to "बुलढाणा",
        "Chandrapur" to "चंद्रपूर",
        "Chhatrapati Sambhajinagar" to "छत्रपती संभाजीनगर",
        "Dharashiv" to "धाराशिव",
        "Dhule" to "धुळे",
        "Gadchiroli" to "गडचिरोली",
        "Gondia" to "गोंदिया",
        "Hingoli" to "हिंगोली",
        "Jalgaon" to "जळगाव",
        "Jalna" to "जालना",
        "Kolhapur" to "कोल्हापूर",
        "Latur" to "लातूर",
        "Mumbai City" to "मुंबई शहर",
        "Mumbai Suburban" to "मुंबई उपनगर",
        "Nagpur" to "नागपूर",
        "Nanded" to "नांदेड",
        "Nandurbar" to "नंदुरबार",
        "Nashik" to "नाशिक",
        "Palghar" to "पालघर",
        "Parbhani" to "परभणी",
        "Pune" to "पुणे",
        "Raigad" to "रायगड",
        "Ratnagiri" to "रत्नागिरी",
        "Sangli" to "सांगली",
        "Satara" to "सातारा",
        "Sindhudurg" to "सिंधुदुर्ग",
        "Solapur" to "सोलापूर",
        "Thane" to "ठाणे",
        "Wardha" to "वर्धा",
        "Washim" to "वाशिम",
        "Yavatmal" to "यवतमाळ"
    ).sortedBy { it.first }

    private val DISTRICT_ALIASES = mapOf(
        "Ahmednagar" to "Ahilyanagar",
        "Ahemadnagar" to "Ahilyanagar",
        "Aurangabad" to "Chhatrapati Sambhajinagar",
        "Osmanabad" to "Dharashiv",
        "Bombay" to "Mumbai City",
        "Mumbai" to "Mumbai City",
        "Suburban Mumbai" to "Mumbai Suburban"
    )

    private val DISTRICT_LOOKUP: Map<String, String> = buildMap {
        DISTRICTS.forEach { (english, marathi) ->
            put(english.normalizedKey(), english)
            put(marathi.normalizedKey(), english)
        }
        DISTRICT_ALIASES.forEach { (alias, canonical) ->
            put(alias.normalizedKey(), canonical)
        }
    }

    fun normalizeDistrict(value: String): String {
        val clean = value.trim()
        if (clean.isBlank()) return ""
        return DISTRICT_LOOKUP[clean.normalizedKey()] ?: clean
    }

    fun districtNameForLanguage(value: String, isMarathi: Boolean): String {
        val canonical = normalizeDistrict(value)
        if (!isMarathi) return canonical
        return DISTRICTS.firstOrNull { it.first == canonical }?.second ?: canonical
    }

    private fun String.normalizedKey(): String = trim()
        .lowercase()
        .replace(".", "")
        .replace("-", " ")
        .replace(Regex("\\s+"), " ")

    val MAIN_POOJA_LIST = listOf(
        "नवचंडी", "वास्तुशांती", "नवचंडीसह वास्तुशांती", "सत्यनारायण"
    )

    val OTHER_POOJA_LIST = listOf(
        "गृह प्रवेश", "भूमी पूजन", "पायाभरणी", "जलपूजन", "भूकंपशांती यंत्र पूजन", 
        "वातक्षोभ शांती यंत्र पूजन", "देव प्रतिष्ठाण", "देवगृह यज्ञप्रतिष्ठा", 
        "मूर्ती प्राणप्रतिष्ठा", "श्रीयंत्र पूजन", "श्रीयंत्र आवरण पूजा", "रुद्र यंत्र पूजन", 
        "श्री स्वयंवर कालासिद्धी यंत्र पूजन", "कुबेर यंत्र पूजन", 
        "गुरुपिथावरील स्थापित यंत्र पूजन", "सिद्धामंगल पादुका पूजन", "अनघा अष्टमी पूजन",
        "नक्षत्र शांती", "करण / योग शांती", "कालसर्प", "उपसर्ग शांती", "उदक शांती",
        "अनंत चतुर्दशी", "लक्ष्मी पूजन", "मल्हारी याग", "श्रमकीलक", "सप्ताह", 
        "विवाह", "कुंभविवाह / अर्क", "पुसंवन विधी", "मुंज", "साखरपुडा", 
        "पुत्रकामेष्टी", "कलशपूजन", "पिंपळपूजन / औदुंबरमुंज", "अंत्यविधी", 
        "पितृपूजन / महालयश्राध्द", "सहस्त्रचंद्र दर्शन", "इतर"
    )

    val NEIGHBOR_MAP = mapOf(
        "Nashik" to listOf("Ahilyanagar", "Dhule", "Jalgaon", "Palghar", "Thane"),
        "Pune" to listOf("Ahilyanagar", "Raigad", "Satara", "Solapur", "Thane"),
        "Mumbai City" to listOf("Mumbai Suburban", "Thane"),
        "Mumbai Suburban" to listOf("Mumbai City", "Thane", "Palghar"),
        "Thane" to listOf("Mumbai Suburban", "Palghar", "Pune", "Ahilyanagar", "Nashik", "Raigad"),
        "Ahilyanagar" to listOf("Nashik", "Pune", "Thane", "Beed", "Dharashiv", "Solapur", "Chhatrapati Sambhajinagar"),
        "Chhatrapati Sambhajinagar" to listOf("Jalgaon", "Nashik", "Ahilyanagar", "Beed", "Jalna"),
        "Nagpur" to listOf("Wardha", "Bhandara", "Chandrapur", "Amravati"),
        "Amravati" to listOf("Nagpur", "Wardha", "Yavatmal", "Washim", "Akola", "Buldhana"),
        "Kolhapur" to listOf("Sangli", "Ratnagiri", "Sindhudurg"),
        "Satara" to listOf("Pune", "Raigad", "Ratnagiri", "Sangli", "Solapur"),
        "Solapur" to listOf("Ahilyanagar", "Pune", "Satara", "Sangli", "Dharashiv"),
        "Sangli" to listOf("Kolhapur", "Satara", "Solapur", "Ratnagiri"),
        "Ratnagiri" to listOf("Raigad", "Satara", "Sangli", "Kolhapur", "Sindhudurg"),
        "Sindhudurg" to listOf("Ratnagiri", "Kolhapur"),
        "Raigad" to listOf("Mumbai Suburban", "Thane", "Pune", "Satara", "Ratnagiri"),
        "Jalgaon" to listOf("Dhule", "Nashik", "Buldhana", "Chhatrapati Sambhajinagar"),
        "Dhule" to listOf("Nandurbar", "Nashik", "Jalgaon"),
        "Nandurbar" to listOf("Dhule"),
        "Jalna" to listOf("Chhatrapati Sambhajinagar", "Beed", "Parbhani", "Buldhana"),
        "Beed" to listOf("Ahilyanagar", "Chhatrapati Sambhajinagar", "Jalna", "Parbhani", "Latur", "Dharashiv"),
        "Latur" to listOf("Beed", "Parbhani", "Nanded", "Dharashiv"),
        "Dharashiv" to listOf("Ahilyanagar", "Solapur", "Beed", "Latur"),
        "Nanded" to listOf("Latur", "Parbhani", "Hingoli", "Yavatmal"),
        "Parbhani" to listOf("Jalna", "Beed", "Latur", "Nanded", "Hingoli", "Buldhana"),
        "Hingoli" to listOf("Washim", "Parbhani", "Nanded", "Yavatmal"),
        "Buldhana" to listOf("Jalgaon", "Akola", "Washim", "Parbhani", "Jalna", "Amravati"),
        "Akola" to listOf("Amravati", "Buldhana", "Washim"),
        "Washim" to listOf("Akola", "Amravati", "Yavatmal", "Hingoli", "Buldhana"),
        "Yavatmal" to listOf("Amravati", "Wardha", "Chandrapur", "Nanded", "Hingoli", "Washim"),
        "Wardha" to listOf("Nagpur", "Amravati", "Yavatmal", "Chandrapur"),
        "Chandrapur" to listOf("Nagpur", "Wardha", "Yavatmal", "Gadchiroli", "Bhandara"),
        "Gadchiroli" to listOf("Chandrapur", "Gondia", "Bhandara"),
        "Bhandara" to listOf("Nagpur", "Gondia", "Chandrapur", "Gadchiroli"),
        "Gondia" to listOf("Bhandara", "Gadchiroli")
    )
}
