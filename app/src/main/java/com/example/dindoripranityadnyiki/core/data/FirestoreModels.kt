package com.example.dindoripranityadnyiki.core.data

/**
 * Domain Models for Dindori Pranit Yadnyiki.
 * Updated to remove all Firebase dependencies.
 */

data class UserProfile(
    val id: String = "",
    val uid: String = "",
    val fullName: String = "",
    val mobile: String = "",
    val email: String = "",
    val address: String = "",
    val district: String = "",
    val pincode: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val status: String = "Active",
    val fcmToken: String = "",
    val createdAt: String? = null
)

data class GurujiProfile(
    val id: String = "",
    val uid: String = "",
    val fullName: String = "",
    val name: String = "",
    val mobile: String = "",
    val email: String = "",
    val address: String = "",
    val district: String = "",
    val status: String = "Pending",
    val isAvailable: Boolean = false,
    val walletBalance: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val pendingWithdrawal: Double = 0.0,
    val availableDates: List<String> = emptyList(),
    val expertise: List<String> = emptyList(),
    val fcmToken: String = "",
    val createdAt: String? = null
)

data class BookingModel(
    val id: String = "",
    val displayId: String = "",
    val userId: String = "",
    val gurujiId: String = "",
    val gurujiName: String = "",
    val poojaId: String = "",
    val poojaName: String = "",
    val contactName: String = "",
    val contactPhone: String = "",
    val address: String = "",
    val district: String = "",
    val pincode: String = "",
    val date: String = "",
    val time: String = "",
    val status: String = "Pending",
    val paymentStatus: String = "Pending",
    val paymentMethod: String = "",
    val currentUserAction: String = "",
    val currentUserActionTitle: String = "",
    val currentUserActionPriority: Int? = null,
    val currentGurujiAction: String = "",
    val currentGurujiActionTitle: String = "",
    val currentGurujiActionPriority: Int? = null,
    val actualAmount: Double = 0.0,
    val gurujiShare: Double = 0.0,
    val trustShare: Double = 0.0,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val gurujiLat: Double? = null,
    val gurujiLng: Double? = null,
    val eta: String = "",
    val hasMarkedArrived: Boolean = false,
    val completionOtpAvailable: Boolean = false,
    val rating: Double? = null,
    val createdAt: String? = null,
    val lastUpdated: String? = null,
    val feedbackSubmittedAt: String? = null
)

data class PaymentTransaction(
    val id: String = "",
    val bookingId: String = "",
    val gurujiId: String = "",
    val type: String = "",
    val description: String = "",
    val status: String = "",
    val amount: Double = 0.0,
    val gurujiShare: Double = 0.0,
    val trustShare: Double = 0.0,
    val paymentId: String = "",
    val createdAt: String? = null,
    val timestamp: String? = null
)

data class PoojaService(
    val id: String = "",
    val name: String = "",
    val nameEn: String = "",
    val nameMr: String = "",
    val category: String = "General",
    val description: String = "",
    val status: String = "Active",
    val imageUrl: String = ""
)
