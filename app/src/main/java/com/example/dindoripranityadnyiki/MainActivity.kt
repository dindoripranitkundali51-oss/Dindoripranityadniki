// MainActivity.kt
package com.example.dindoripranityadnyiki

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.example.dindoripranityadnyiki.navigation.AppNavGraph
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Optional: edge-to-edge drawing (adjust as needed)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Initialize Firebase (safe-guarded)
        try {
            FirebaseApp.initializeApp(this)
            Log.d("MainActivity", "Firebase initialized.")
        } catch (e: Exception) {
            Log.e("MainActivity", "Firebase init failed: ${e.localizedMessage}")
        }

        setContent {
            MaterialTheme {
                Surface {
                    // create navController and pass to AppNavGraph
                    val navController = rememberNavController()
                    AppNavGraph(navController = navController)
                }
            }
        }
    }
}
