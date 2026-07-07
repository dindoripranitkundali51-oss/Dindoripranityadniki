package com.example.dindoripranityadnyiki.core.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserProfile(
    val id: String = "",
    val uid: String = "",
    val fullName: String = "",
    val mobile: String = "",
    val email: String = "",
    val authEmail: String = "",
    val address: String = "",
    val district: String = "",
    val pincode: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val status: String = "Active",
    val fcmToken: String = "",
    val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
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
    val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
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
    val eta: String = "",
    val hasMarkedArrived: Boolean = false,
    val gurujiLocation: GeoPoint? = null,
    val completionOtpAvailable: Boolean = false,
    val rating: Double? = null,
    val createdAt: Timestamp? = null,
    val lastUpdated: Timestamp? = null,
    val feedbackSubmittedAt: Timestamp? = null
)

@IgnoreExtraProperties
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
    val createdAt: Timestamp? = null,
    val timestamp: Timestamp? = null
)

@IgnoreExtraProperties
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

data class BookingCreateRequest(
    val poojaId: String,
    val poojaName: String,
    val contactName: String,
    val contactPhone: String,
    val address: String,
    val district: String,
    val pincode: String,
    val date: String,
    val time: String,
    val lat: Double,
    val lng: Double,
    val instructions: String,
    val clientRequestId: String
) {
    fun asCallablePayload(): Map<String, Any> = mapOf(
        "poojaId" to poojaId,
        "poojaName" to poojaName,
        "contactName" to contactName,
        "contactPhone" to contactPhone,
        "address" to address,
        "district" to district,
        "pincode" to pincode,
        "date" to date,
        "time" to time,
        "lat" to lat,
        "lng" to lng,
        "instructions" to instructions,
        "clientRequestId" to clientRequestId
    )
}

data class AdminProfile(
    val uid: String = "",
    val email: String = "",
    val active: Boolean = true,
    val status: String = "Active"
)

fun UserProfile.toPresentationMap(): Map<String, Any> = mapOf(
    "id" to id, "uid" to uid, "fullName" to fullName, "mobile" to mobile,
    "email" to email, "address" to address, "district" to district,
    "pincode" to pincode, "status" to status
)

fun GurujiProfile.toPresentationMap(): Map<String, Any> = mapOf(
    "id" to id, "uid" to uid, "fullName" to fullName, "name" to name,
    "mobile" to mobile, "email" to email, "address" to address,
    "district" to district, "status" to status, "isAvailable" to isAvailable,
    "walletBalance" to walletBalance, "totalEarnings" to totalEarnings,
    "pendingWithdrawal" to pendingWithdrawal, "availableDates" to availableDates,
    "expertise" to expertise
)

fun BookingModel.toPresentationMap(): Map<String, Any> = buildMap {
    put("id", id); put("displayId", displayId); put("userId", userId); put("gurujiId", gurujiId)
    put("gurujiName", gurujiName); put("poojaId", poojaId); put("poojaName", poojaName)
    put("contactName", contactName); put("contactPhone", contactPhone); put("address", address)
    put("district", district); put("pincode", pincode); put("date", date); put("time", time)
    put("status", status); put("paymentStatus", paymentStatus); put("paymentMethod", paymentMethod)
    put("currentUserAction", currentUserAction); put("currentUserActionTitle", currentUserActionTitle)
    currentUserActionPriority?.let { put("currentUserActionPriority", it) }
    put("currentGurujiAction", currentGurujiAction.ifBlank { currentUserAction })
    put("currentGurujiActionTitle", currentGurujiActionTitle.ifBlank { currentUserActionTitle })
    (currentGurujiActionPriority ?: currentUserActionPriority)?.let { put("currentGurujiActionPriority", it) }
    if (actualAmount > 0.0) put("actualAmount", actualAmount)
    if (gurujiShare > 0.0) put("gurujiShare", gurujiShare)
    if (trustShare > 0.0) put("trustShare", trustShare)
    put("lat", lat); put("lng", lng)
    put("userLat", lat); put("userLng", lng)
    put("eta", eta); put("hasMarkedArrived", hasMarkedArrived)
    gurujiLocation?.let { put("gurujiLocation", it) }
    put("completionOtpAvailable", completionOtpAvailable); rating?.let { put("rating", it) }
    createdAt?.let { put("createdAt", it) }; lastUpdated?.let { put("lastUpdated", it) }
    feedbackSubmittedAt?.let { put("feedbackSubmittedAt", it) }
}

fun PaymentTransaction.toPresentationMap(): Map<String, Any> = buildMap {
    put("id", id); put("bookingId", bookingId); put("gurujiId", gurujiId); put("type", type)
    put("description", description); put("status", status); put("amount", amount)
    put("gurujiShare", gurujiShare); put("trustShare", trustShare); put("paymentId", paymentId)
    createdAt?.let { put("createdAt", it) }; timestamp?.let { put("timestamp", it) }
}

fun PoojaService.toPresentationMap(): Map<String, Any> = mapOf(
    "id" to id, "name" to name, "nameEn" to nameEn, "nameMr" to nameMr,
    "category" to category, "description" to description, "status" to status, "imageUrl" to imageUrl
)
