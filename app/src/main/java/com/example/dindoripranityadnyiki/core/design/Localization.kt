package com.example.dindoripranityadnyiki.core.design

import androidx.compose.runtime.Composable
import com.example.dindoripranityadnyiki.LocalAppLanguage

object AppStrings {
    private val Marathi = mapOf(
        "app_name" to "दिंडोरी प्रणीत यज्ञिकी",
        "home" to "मुख्य",
        "profile" to "प्रोफाईल",
        "wallet" to "वॉलेट",
        "settings" to "सेटिंग्स",
        "login" to "लॉगिन",
        "register" to "नोंदणी",
        "accept_seva" to "सेवा स्वीकारा",
        "on_the_way" to "मी निघालो आहे",
        "arrived" to "मी पोहचलो आहे",
        "start_seva" to "सेवा सुरू करा",
        "completed" to "सेवा पूर्ण झाली",
        "finding_guruji" to "उत्तम गुरुजींची नियुक्ती होत आहे...",
        "guruji_assigned" to "गुरुजी नियुक्त झाले आहेत",
        "track_service" to "सेवेची प्रगती",
        "total_amount" to "एकूण दक्षिणा",
        "payment_success" to "पेमेंट यशस्वी",
        "download_receipt" to "पावती डाऊनलोड करा",
        "offline_mode" to "आपण ऑफलाईन आहात",
        "online_mode" to "आपण ऑनलाईन आहात",
        "syncing" to "डेटा सिंक होत आहे..."
    )

    private val English = mapOf(
        "app_name" to "Dindori Pranit Yadnyiki",
        "home" to "Home",
        "profile" to "Profile",
        "wallet" to "Wallet",
        "settings" to "Settings",
        "login" to "Login",
        "register" to "Register",
        "accept_seva" to "Accept Seva",
        "on_the_way" to "On The Way",
        "arrived" to "I have arrived",
        "start_seva" to "Start Seva",
        "completed" to "Seva Completed",
        "finding_guruji" to "Finding best Guruji...",
        "guruji_assigned" to "Guruji Assigned",
        "track_service" to "Service Tracking",
        "total_amount" to "Total Amount",
        "payment_success" to "Payment Successful",
        "download_receipt" to "Download Receipt",
        "offline_mode" to "You are Offline",
        "online_mode" to "You are Online",
        "syncing" to "Syncing data..."
    )

    @Composable
    fun get(key: String): String {
        val isMarathi = LocalAppLanguage.current == "Marathi"
        return if (isMarathi) Marathi[key] ?: key else English[key] ?: key
    }
}
