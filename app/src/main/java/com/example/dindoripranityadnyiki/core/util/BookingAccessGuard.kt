package com.example.dindoripranityadnyiki.core.util

enum class BookingAccess {
    USER_OWNER,
    GURUJI_ASSIGNED,
    DENIED
}

object BookingAccessGuard {

    suspend fun verify(
        uid: String,
        bookingId: String,
        expectGuruji: Boolean
    ): BookingAccess {
        if (uid.isBlank() || bookingId.isBlank()) return BookingAccess.DENIED
        return if (expectGuruji) BookingAccess.GURUJI_ASSIGNED else BookingAccess.USER_OWNER
    }
}
