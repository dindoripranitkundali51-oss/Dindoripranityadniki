package com.example.dindoripranityadnyiki.core.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import android.widget.Toast
import androidx.core.app.RemoteInput
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import com.example.dindoripranityadnyiki.core.data.DataStoreManager
import com.example.dindoripranityadnyiki.MainActivity
import com.example.dindoripranityadnyiki.core.network.ApiService
import com.example.dindoripranityadnyiki.core.network.UpdateStatusRequest
import com.example.dindoripranityadnyiki.core.network.CreateTicketRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var apiService: ApiService

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val bookingId = intent.getStringExtra(EXTRA_BOOKING_ID).orEmpty()
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

        when (intent.action) {
            ACTION_OPEN -> openDeepLink(context, intent.getStringExtra(EXTRA_LINK))
            ACTION_COPY_OTP -> copyOtp(context, intent.getStringExtra(EXTRA_OTP).orEmpty())
            ACTION_ACCEPT -> updateBookingStatus(bookingId, "Accepted", pendingResult)
            ACTION_REJECT -> updateBookingStatus(bookingId, "Rejected", pendingResult)
            ACTION_REPLY -> saveReply(intent, bookingId, pendingResult)
            else -> pendingResult.finish()
        }

        if (notificationId != 0) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notificationId)
        }
    }

    private fun openDeepLink(context: Context, link: String?) {
        if (link.isNullOrBlank()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, link.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
    }

    private fun copyOtp(context: Context, otp: String) {
        if (otp.isBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("OTP", otp)
        clipboard.setPrimaryClip(clip)
        val isMr = isMarathiLanguage(context)
        Toast.makeText(context, if (isMr) "OTP कॉपी केला" else "OTP Copied", Toast.LENGTH_SHORT).show()
    }

    private fun isMarathiLanguage(context: Context): Boolean = runBlocking {
        val language = dataStoreManager.readString(PrefKeys.LANGUAGE).first()
        language == "mr" || language == "Marathi"
    }

    private fun updateBookingStatus(bookingId: String, status: String, pendingResult: PendingResult) {
        if (bookingId.isBlank()) {
            pendingResult.finish()
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = dataStoreManager.readString(PrefKeys.JWT_TOKEN).first()
                if (token.isNotBlank()) {
                    apiService.updateBookingStatus("Bearer $token", bookingId, UpdateStatusRequest(status))
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationAction", "Failed to update status: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun saveReply(intent: Intent, bookingId: String, pendingResult: PendingResult) {
        val text = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(KEY_TEXT_REPLY)?.toString().orEmpty()
        if (text.isBlank()) {
            pendingResult.finish()
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = dataStoreManager.readString(PrefKeys.JWT_TOKEN).first()
                if (token.isNotBlank()) {
                    apiService.createSupportTicket(
                        "Bearer $token",
                        CreateTicketRequest(
                            subject = "Reply to notification for booking $bookingId",
                            description = text.take(500),
                            category = "NotificationReply",
                            language = "mr"
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationAction", "Failed to save reply: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_OPEN = "com.example.dindoripranityadnyiki.notification.OPEN"
        const val ACTION_COPY_OTP = "com.example.dindoripranityadnyiki.notification.COPY_OTP"
        const val ACTION_ACCEPT = "com.example.dindoripranityadnyiki.notification.ACCEPT"
        const val ACTION_REJECT = "com.example.dindoripranityadnyiki.notification.REJECT"
        const val ACTION_REPLY = "com.example.dindoripranityadnyiki.notification.REPLY"
        const val EXTRA_BOOKING_ID = "bookingId"
        const val EXTRA_LINK = "link"
        const val EXTRA_OTP = "otp"
        const val EXTRA_NOTIFICATION_ID = "notificationId"
        const val KEY_TEXT_REPLY = "notification_reply"

        fun pendingIntent(context: Context, action: String, requestCode: Int, data: Map<String, String>): PendingIntent {
            val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_BOOKING_ID, data["bookingId"].orEmpty())
                putExtra(EXTRA_LINK, data["deepLink"] ?: data["link"].orEmpty())
                putExtra(EXTRA_OTP, data["otp"].orEmpty())
                putExtra(EXTRA_NOTIFICATION_ID, requestCode)
            }
            val mutable = if (action == ACTION_REPLY) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
            return PendingIntent.getBroadcast(context, requestCode + action.hashCode(), intent, mutable or PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}
