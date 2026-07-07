package com.example.dindoripranityadnyiki

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.dindoripranityadnyiki", appContext.packageName)
    }

    @Test
    fun deepLinkIntentResolvesToMainActivity() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("dindori://app/gurujiBookingDetails/test-booking"))
            .addCategory(Intent.CATEGORY_BROWSABLE)

        val matches = appContext.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        assertTrue(matches.any { it.activityInfo?.name?.contains("MainActivity") == true })
    }
}
