# 🛡️ PRODUCTION PROGUARD RULES - Dindori Pranit Yadnyiki

# 1. Standard Android rules
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*
-renamesourcefileattribute SourceFile
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 2. Firebase Rules
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.internal.** { *; }
-dontwarn com.google.firebase.**

# 3. Retrofit & OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# 4. Razorpay Rules (Critical for Payments)
-keep class com.razorpay.** {*;}
-dontwarn com.razorpay.**
-keepattributes *Annotation*
-keep class com.google.android.gms.wallet.** {*;}

# 5. Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# 6. Keep Data Models (IMPORTANT: Prevent JSON parsing errors)
# Keep Firestore models and all data classes
-keep class com.example.dindoripranityadnyiki.core.data.** { *; }
-keep class com.example.dindoripranityadnyiki.core.data.FirestoreModels$** { *; }
-keep class com.example.dindoripranityadnyiki.core.data.FirestoreModels.** { *; }

# 7. iText PDF Rules
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**
-dontwarn org.slf4j.impl.StaticLoggerBinder

# 8. ML Kit / Play Services release shrinker compatibility
-dontwarn com.google.mlkit.common.sdkinternal.LibraryVersion

# 9. Hilt Dependency Injection
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-keepclasseswithmembernames class * {
    @dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper <fields>;
}

# 10. Jetpack Compose
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }

# 11. Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# 12. Google Maps
-keep class com.google.android.gms.maps.** { *; }
-keep class com.google.maps.android.** { *; }

# 13. Coil Image Loading
-keep class coil.** { *; }
-dontwarn coil.**
