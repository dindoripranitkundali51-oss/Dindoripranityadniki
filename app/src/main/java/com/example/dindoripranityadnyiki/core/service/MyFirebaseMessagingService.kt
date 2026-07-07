package com.example.dindoripranityadnyiki.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.example.dindoripranityadnyiki.MainActivity
import com.example.dindoripranityadnyiki.R
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import com.example.dindoripranityadnyiki.core.data.dataStore
import com.example.dindoripranityadnyiki.core.network.ApiService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val link = remoteMessage.data["deepLink"] ?: remoteMessage.data["link"]
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "श्री स्वामी समर्थ"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: ""
        val payload = remoteMessage.data
        val isUrgent = remoteMessage.data["action"] == "REVIEW_ASSIGNMENT" ||
            remoteMessage.data["priority"] == "high"

        sendNotification(title, body, link, isUrgent, payload)
    }

    private fun sendNotification(
        title: String,
        messageBody: String,
        link: String?,
        isUrgent: Boolean,
        payload: Map<String, String>
    ) {
        val isMarathi = isMarathiLanguage()
        val localizedTitle = localizedNotificationText(title, isMarathi)
        val localizedBody = localizedNotificationText(messageBody, isMarathi)
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (link != null && link.startsWith("dindori://")) {
                data = Uri.parse(link)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = if (isUrgent) "urgent_seva_channel" else "seva_updates_channel"
        val soundUri = if (isUrgent) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
        val notificationId = System.currentTimeMillis().toInt()
        val urgentPrefix = if (isMarathi) "तातडीची सेवा" else "Urgent Seva"

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle(if (isUrgent) "$urgentPrefix: $localizedTitle" else localizedTitle)
            .setContentText(localizedBody)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setPriority(if (isUrgent) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (isUrgent) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(localizedBody))

        addSmartActions(notificationBuilder, payload, notificationId, isMarathi)

        if (isUrgent) {
            notificationBuilder.setFullScreenIntent(pendingIntent, true)
            notificationBuilder.setVibrate(longArrayOf(0, 500, 200, 500, 200, 500, 200, 800))
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = if (isUrgent) {
                if (isMarathi) "तातडीची सेवा सूचना" else "Urgent Seva Alerts"
            } else {
                if (isMarathi) "सेवा अपडेट्स" else "Seva Updates"
            }
            val importance = if (isUrgent) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = if (isMarathi) {
                    "दिंडोरी सेवा अपडेट्ससाठी सूचना"
                } else {
                    "Notifications for Dindori Seva updates"
                }
                if (isUrgent) {
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500, 200, 800)
                    val attributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    setSound(soundUri, attributes)
                }
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun addSmartActions(
        builder: NotificationCompat.Builder,
        data: Map<String, String>,
        notificationId: Int,
        isMarathi: Boolean
    ) {
        val action = data["action"].orEmpty()
        val openAction = NotificationCompat.Action.Builder(
            R.drawable.app_logo,
            if (isMarathi) "उघडा" else "Open",
            NotificationActionReceiver.pendingIntent(this, NotificationActionReceiver.ACTION_OPEN, notificationId, data)
        ).build()

        when (action) {
            "REVIEW_ASSIGNMENT" -> {
                builder.addAction(
                    R.drawable.app_logo,
                    if (isMarathi) "स्वीकारा" else "Accept",
                    NotificationActionReceiver.pendingIntent(this, NotificationActionReceiver.ACTION_ACCEPT, notificationId + 1, data)
                )
                builder.addAction(
                    R.drawable.app_logo,
                    if (isMarathi) "नकार द्या" else "Reject",
                    NotificationActionReceiver.pendingIntent(this, NotificationActionReceiver.ACTION_REJECT, notificationId + 2, data)
                )
                builder.addAction(openAction)
            }
            "CHECK_OTP_NOTIFICATION" -> {
                builder.addAction(
                    R.drawable.app_logo,
                    if (isMarathi) "OTP कॉपी करा" else "Copy OTP",
                    NotificationActionReceiver.pendingIntent(this, NotificationActionReceiver.ACTION_COPY_OTP, notificationId + 3, data)
                )
                builder.addAction(openAction)
                builder.addAction(replyAction(notificationId, data, isMarathi))
            }
            "PAY_NOW" -> {
                builder.addAction(
                    R.drawable.app_logo,
                    if (isMarathi) "पेमेंट करा" else "Pay Now",
                    NotificationActionReceiver.pendingIntent(this, NotificationActionReceiver.ACTION_OPEN, notificationId + 4, data)
                )
                builder.addAction(replyAction(notificationId, data, isMarathi))
            }
            "RATE_SEVA" -> {
                builder.addAction(
                    R.drawable.app_logo,
                    if (isMarathi) "रेटिंग द्या" else "Rate Now",
                    NotificationActionReceiver.pendingIntent(this, NotificationActionReceiver.ACTION_OPEN, notificationId + 5, data)
                )
                builder.addAction(replyAction(notificationId, data, isMarathi))
            }
            else -> {
                builder.addAction(openAction)
                builder.addAction(replyAction(notificationId, data, isMarathi))
            }
        }
    }

    private fun replyAction(notificationId: Int, data: Map<String, String>, isMarathi: Boolean): NotificationCompat.Action {
        val remoteInput = RemoteInput.Builder(NotificationActionReceiver.KEY_TEXT_REPLY)
            .setLabel(if (isMarathi) "उत्तर द्या" else "Reply")
            .build()
        return NotificationCompat.Action.Builder(
            R.drawable.app_logo,
            if (isMarathi) "उत्तर" else "Reply",
            NotificationActionReceiver.pendingIntent(this, NotificationActionReceiver.ACTION_REPLY, notificationId + 6, data)
        ).addRemoteInput(remoteInput).build()
    }

    override fun onNewToken(token: String) {
        saveTokenToBackend(token)
    }

    private fun saveTokenToBackend(token: String) {
        val dataStoreManager = com.example.dindoripranityadnyiki.core.data.DataStoreManager(applicationContext)
        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl("https://dindoripranitapi.somee.com/api/v1/")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)

        runBlocking {
            try {
                val jwt = dataStoreManager.readString(PrefKeys.JWT_TOKEN).first()
                if (jwt.isNotBlank()) {
                    apiService.updateFcmToken("Bearer $jwt", com.example.dindoripranityadnyiki.core.network.UpdateFcmTokenRequest(token))
                }
            } catch (e: Exception) {
                android.util.Log.e("MyFCM", "Failed to upload token to backend: ${e.message}")
            }
        }
    }

    private fun isMarathiLanguage(): Boolean = runBlocking {
        val language = dataStore.data.first()[PrefKeys.LANGUAGE] ?: "mr"
        language == "mr" || language == "Marathi"
    }

    private fun localizedNotificationText(text: String, isMarathi: Boolean): String {
        if (!isMarathi) return text
        return when (text) {
            "Notification" -> "नोटिफिकेशन"
            "Payment received" -> "पेमेंट प्राप्त झाले"
            "Your receipt is ready in the app." -> "तुमची पावती ॲपमध्ये तयार आहे."
            "Dakshina credited" -> "दक्षिणा जमा झाली"
            "Booking created" -> "बुकिंग तयार झाले"
            "Your booking is assigned to a Guruji." -> "तुमचे बुकिंग गुरुजींना देण्यात आले आहे."
            "Your booking request is received." -> "तुमची बुकिंग विनंती प्राप्त झाली आहे."
            "New seva booking" -> "नवीन सेवा बुकिंग"
            "A new seva booking is assigned to you." -> "तुमच्याकडे नवीन सेवा बुकिंग आले आहे."
            "Completion OTP" -> "पूजा पूर्णता OTP"
            "Payment failed" -> "पेमेंट अयशस्वी"
            "Your payment could not be completed. Please try again from the app." -> "तुमचे पेमेंट पूर्ण झाले नाही. कृपया ॲपमधून पुन्हा प्रयत्न करा."
            "Payment status updated" -> "पेमेंट स्थिती अपडेट झाली"
            "Receipt sent again" -> "पावती पुन्हा पाठवली"
            "Your digital receipt is available again in the app." -> "तुमची डिजिटल पावती ॲपमध्ये पुन्हा उपलब्ध आहे."
            "Withdrawal request" -> "रक्कम काढण्याची विनंती"
            else -> text
        }
    }
}
