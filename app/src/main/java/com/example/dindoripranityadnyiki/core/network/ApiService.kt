package com.example.dindoripranityadnyiki.core.network

import retrofit2.Response
import retrofit2.http.*

/**
 * Dindori Pranit API Retrofit Interface
 * Replaces the old Firebase Https Callable Functions with clean REST calls.
 */
interface ApiService {

    // ==========================================
    // १. AUTHENTICATION APIs
    // ==========================================

    @POST("v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("v1/auth/register/user")
    suspend fun registerUser(
        @Body request: UserRegisterRequest
    ): Response<RegisterResponse>

    @POST("v1/auth/register/expert")
    suspend fun registerExpert(
        @Body request: ExpertRegisterRequest
    ): Response<RegisterResponse>

    // ==========================================
    // २. BOOKING & OTP APIs
    // ==========================================

    @POST("v1/booking")
    suspend fun createBooking(
        @Header("Authorization") token: String, // format: "Bearer {jwt_token}"
        @Body request: CreateBookingRequest
    ): Response<BookingResponse>

    @GET("v1/booking/{id}")
    suspend fun getBookingDetails(
        @Header("Authorization") token: String,
        @Path("id") bookingId: String
    ): Response<BookingDetailResponse>

    @GET("v1/booking/user")
    suspend fun getUserBookings(
        @Header("Authorization") token: String
    ): Response<BookingListResponse>

    @GET("v1/booking/expert")
    suspend fun getExpertBookings(
        @Header("Authorization") token: String
    ): Response<BookingListResponse>

    @PUT("v1/booking/{id}/status")
    suspend fun updateBookingStatus(
        @Header("Authorization") token: String,
        @Path("id") bookingId: String,
        @Body request: UpdateStatusRequest
    ): Response<StatusResponse>

    @POST("v1/booking/{id}/otp/request")
    suspend fun requestCompletionOtp(
        @Header("Authorization") token: String,
        @Path("id") bookingId: String
    ): Response<OtpRequestResponse>

    @POST("v1/booking/{id}/otp/verify")
    suspend fun verifyCompletionOtp(
        @Header("Authorization") token: String,
        @Path("id") bookingId: String,
        @Body request: VerifyOtpRequest
    ): Response<StatusResponse>

    // ==========================================
    // ३. PAYMENT & WITHDRAWALS APIs
    // ==========================================

    @POST("v1/payment/create-order")
    suspend fun createRazorpayOrder(
        @Header("Authorization") token: String,
        @Body request: CreateOrderRequest
    ): Response<OrderResponse>

    @POST("v1/payment/verify")
    suspend fun verifyPayment(
        @Header("Authorization") token: String,
        @Body request: VerifyPaymentRequest
    ): Response<PaymentVerifyResponse>

    @POST("v1/payment/withdrawal")
    suspend fun requestWithdrawal(
        @Header("Authorization") token: String,
        @Body request: WithdrawalRequest
    ): Response<StatusResponse>

    @POST("v1/booking/{id}/feedback")
    suspend fun submitFeedback(
        @Header("Authorization") token: String,
        @Path("id") bookingId: String,
        @Body request: SubmitFeedbackRequest
    ): Response<StatusResponse>

    @PUT("v1/booking/location")
    suspend fun updateLocation(
        @Header("Authorization") token: String,
        @Body request: UpdateLocationRequest
    ): Response<StatusResponse>

    @PUT("v1/auth/fcm-token")
    suspend fun updateFcmToken(
        @Header("Authorization") token: String,
        @Body request: UpdateFcmTokenRequest
    ): Response<StatusResponse>

    @POST("v1/support/ticket")
    suspend fun createSupportTicket(
        @Header("Authorization") token: String,
        @Body request: CreateTicketRequest
    ): Response<StatusResponse>

    @GET("v1/support/tickets")
    suspend fun getSupportTickets(
        @Header("Authorization") token: String
    ): Response<SupportTicketsListResponse>

    @GET("v1/notifications")
    suspend fun getUserNotifications(
        @Header("Authorization") token: String
    ): Response<NotificationListResponse>

    @PUT("v1/notifications/{id}/read")
    suspend fun markNotificationRead(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<StatusResponse>

    @POST("v1/booking/{id}/request")
    suspend fun submitBookingRequest(
        @Header("Authorization") token: String,
        @Path("id") bookingId: String,
        @Body request: SubmitServiceRequestRequest
    ): Response<StatusResponse>

    @GET("v1/payment/receipt/{bookingId}")
    suspend fun getPaymentReceipt(
        @Header("Authorization") token: String,
        @Path("bookingId") bookingId: String
    ): Response<ReceiptSnapshotResponse>

    @POST("v1/kyc/submit")
    suspend fun submitKyc(
        @Header("Authorization") token: String,
        @Body request: SubmitKycRequest
    ): Response<StatusResponse>

    @POST("v1/chat/send")
    suspend fun sendChatMessage(
        @Header("Authorization") token: String,
        @Body request: SendMessageRequest
    ): Response<StatusResponse>

    @GET("v1/chat/history/{receiverId}")
    suspend fun getChatHistory(
        @Header("Authorization") token: String,
        @Path("receiverId") receiverId: String
    ): Response<ChatHistoryResponse>

    @POST("v1/booking/expert/availability")
    suspend fun saveAvailability(
        @Header("Authorization") token: String,
        @Body request: GurujiAvailabilityRequest
    ): Response<StatusResponse>

    @GET("v1/booking/expert/availability")
    suspend fun getAvailability(
        @Header("Authorization") token: String
    ): Response<GurujiAvailabilityResponse>

    @GET("v1/booking/available-dates")
    suspend fun getAvailableDates(
        @Header("Authorization") token: String
    ): Response<GurujiAvailabilityResponse>

    @GET("v1/payment/transactions")
    suspend fun getTransactions(
        @Header("Authorization") token: String
    ): Response<TransactionListResponse>
}

// DTO Data Models for Retrofit (Kotlin)
data class LoginRequest(val mobile: String)
data class LoginResponse(val success: Boolean, val token: String, val role: String, val profile: Any)

data class UserRegisterRequest(
    val fullName: String,
    val mobile: String,
    val email: String?,
    val address: String?,
    val district: String?,
    val pincode: String?,
    val lat: Double?,
    val lng: Double?
)

data class ExpertRegisterRequest(
    val fullName: String,
    val mobile: String,
    val email: String?,
    val address: String?,
    val district: String?,
    val pincode: String?,
    val lat: Double?,
    val lng: Double?,
    val expertises: String,
    val expertType: String, // "Guruji" or "VastuExpert"
    val panNumber: String,
    val aadharNumber: String
)

data class RegisterResponse(val success: Boolean, val token: String?, val role: String?, val uid: String)

data class CreateBookingRequest(
    val clientRequestId: String,
    val poojaId: String,
    val date: String, // ISO Format Date
    val contactName: String?,
    val contactPhone: String?,
    val address: String?,
    val district: String?,
    val pincode: String?,
    val userLat: Double?,
    val userLng: Double?
)

data class BookingResponse(val success: Boolean, val assigned: Boolean, val assignedId: String?, val data: BookingDto)
data class BookingDetailResponse(val success: Boolean, val data: BookingDto)
data class BookingListResponse(val success: Boolean, val data: List<BookingDto>)
data class UpdateStatusRequest(val status: String)
data class StatusResponse(val success: Boolean, val status: String?, val message: String?)
data class OtpRequestResponse(val success: Boolean, val message: String, val debugOtp: String?)
data class VerifyOtpRequest(val otp: String, val dakshina: Double)
data class CreateOrderRequest(val bookingId: String)
data class OrderResponse(val success: Boolean, val orderId: String, val amount: Double, val keyId: String)
data class VerifyPaymentRequest(val bookingId: String, val razorpayPaymentId: String, val razorpaySignature: String)
data class PaymentVerifyResponse(val success: Boolean, val message: String, val receiptNo: String?)
data class WithdrawalRequest(val amount: Double, val bankAccount: String?, val ifsc: String?, val upi: String?)

data class SubmitFeedbackRequest(val rating: Int, val comment: String)
data class UpdateLocationRequest(val lat: Double, val lng: Double)

data class UpdateFcmTokenRequest(val fcmToken: String)
data class CreateTicketRequest(val subject: String, val description: String, val category: String?, val language: String?)
data class SupportTicketDto(val id: String, val userId: String, val subject: String, val description: String, val category: String, val language: String, val status: String, val priority: String, val createdAt: String, val updatedAt: String)
data class SupportTicketsListResponse(val success: Boolean, val data: List<SupportTicketDto>)
data class UserNotificationDto(val id: Int, val userId: String, val title: String, val body: String, val action: String?, val bookingId: String?, val deepLink: String?, val priority: String, val isRead: Boolean, val createdAt: String)
data class NotificationListResponse(val success: Boolean, val data: List<UserNotificationDto>)

data class SubmitServiceRequestRequest(val type: String, val requestedDate: String?, val reason: String?)

data class ReceiptSnapshotDto(val bookingId: String, val receiptNo: String, val amount: Double, val paymentStatus: String, val createdAt: String)
data class ReceiptSnapshotResponse(val success: Boolean, val data: ReceiptSnapshotDto)

data class SubmitKycRequest(val panNumber: String, val aadharNumber: String)
data class SendMessageRequest(val receiverId: String, val message: String)
data class ChatMessageDto(val id: Int, val senderId: String, val senderName: String, val receiverId: String, val message: String, val isRead: Boolean, val createdAt: String)
data class ChatHistoryResponse(val success: Boolean, val data: List<ChatMessageDto>)

data class GurujiAvailabilityRequest(val dates: List<String>)
data class GurujiAvailabilityResponse(val success: Boolean, val data: List<String>)

data class TransactionListResponse(val success: Boolean, val data: List<PaymentTransactionDto>)
data class PaymentTransactionDto(
    val id: Int,
    val bookingId: String?,
    val amount: Double,
    val transactionType: String,
    val referenceNo: String?,
    val description: String?,
    val createdAt: String
)

fun PaymentTransactionDto.toDomain(): com.example.dindoripranityadnyiki.core.data.PaymentTransaction {
    return com.example.dindoripranityadnyiki.core.data.PaymentTransaction(
        id = this.id.toString(),
        bookingId = this.bookingId.orEmpty(),
        type = this.transactionType,
        description = this.description.orEmpty(),
        amount = this.amount,
        createdAt = null
    )
}

data class BookingDto(
    val id: String,
    val displayId: String,
    val userId: String,
    val gurujiId: String?,
    val gurujiName: String?,
    val poojaId: String,
    val poojaName: String?,
    val contactName: String?,
    val contactPhone: String?,
    val address: String?,
    val district: String?,
    val pincode: String?,
    val date: String?,
    val time: String?,
    val status: String,
    val paymentStatus: String,
    val paymentMethod: String?,
    val razorpayPaymentId: String?,
    val currentUserAction: String?,
    val currentUserActionTitle: String?,
    val currentUserActionPriority: Int?,
    val currentGurujiAction: String?,
    val currentGurujiActionTitle: String?,
    val currentGurujiActionPriority: Int?,
    val amount: Double,
    val actualAmount: Double?,
    val gurujiShare: Double?,
    val trustShare: Double?,
    val userLat: Double?,
    val userLng: Double?,
    val lat: Double?,
    val lng: Double?,
    val gurujiLat: Double?,
    val gurujiLng: Double?,
    val eta: String?,
    val hasMarkedArrived: Boolean?,
    val completionOtpAvailable: Boolean?,
    val rating: Double?,
    val createdAt: String?,
    val lastUpdated: String?,
    val feedbackSubmittedAt: String?
)

fun BookingDto.toBookingModel(): com.example.dindoripranityadnyiki.core.data.BookingModel {
    return com.example.dindoripranityadnyiki.core.data.BookingModel(
        id = this.id,
        displayId = this.displayId,
        userId = this.userId,
        gurujiId = this.gurujiId.orEmpty(),
        gurujiName = this.gurujiName.orEmpty(),
        poojaId = this.poojaId,
        poojaName = this.poojaName.orEmpty(),
        contactName = this.contactName.orEmpty(),
        contactPhone = this.contactPhone.orEmpty(),
        address = this.address.orEmpty(),
        district = this.district.orEmpty(),
        pincode = this.pincode.orEmpty(),
        date = this.date.orEmpty(),
        time = this.time.orEmpty(),
        status = this.status,
        paymentStatus = this.paymentStatus,
        paymentMethod = this.paymentMethod.orEmpty(),
        currentUserAction = this.currentUserAction.orEmpty(),
        currentUserActionTitle = this.currentUserActionTitle.orEmpty(),
        currentUserActionPriority = this.currentUserActionPriority,
        currentGurujiAction = this.currentGurujiAction.orEmpty(),
        currentGurujiActionTitle = this.currentGurujiActionTitle.orEmpty(),
        currentGurujiActionPriority = this.currentGurujiActionPriority,
        actualAmount = this.actualAmount ?: this.amount,
        gurujiShare = this.gurujiShare ?: 0.0,
        trustShare = this.trustShare ?: 0.0,
        lat = this.userLat ?: this.lat ?: 0.0,
        lng = this.userLng ?: this.lng ?: 0.0,
        eta = this.eta.orEmpty(),
        hasMarkedArrived = this.hasMarkedArrived ?: false,
        completionOtpAvailable = this.completionOtpAvailable ?: false,
        rating = this.rating,
        createdAt = null,
        lastUpdated = null,
        feedbackSubmittedAt = null
    )
}
