package com.example.dindoripranityadnyiki

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.google.firebase.FirebaseApp

@HiltAndroidApp
class DindoriApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase for FCM only
        FirebaseApp.initializeApp(this)
    }
}
