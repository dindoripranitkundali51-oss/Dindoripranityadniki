package com.example.dindoripranityadnyiki.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.dindoripranityadnyiki.data.PrefKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// ✅ हेच तुझं एकमेव DataStore (Singleton)
val Context.dataStore by preferencesDataStore(name = "PoojaAppPrefs")

// ✅ Helper class — save, read, clear साठी
class DataStoreManager(private val context: Context) {

    // 🧾 Boolean value save
    suspend fun savePreference(key: Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[key] = value
        }
    }

    // 🧾 String value save
    suspend fun saveStringPreference(key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { prefs ->
            prefs[key] = value
        }
    }

    // 🧾 Boolean read — crash safe
    fun readBoolean(key: Preferences.Key<Boolean>): Flow<Boolean> =
        context.dataStore.data
            .catch { e ->
                if (e is IOException) emit(emptyPreferences()) // safety: IO error आल्यास app crash होणार नाही
                else throw e
            }
            .map { it[key] ?: false }

    // 🧾 String read — crash safe
    fun readString(key: Preferences.Key<String>): Flow<String> =
        context.dataStore.data
            .catch { e ->
                if (e is IOException) emit(emptyPreferences())
                else throw e
            }
            .map { it[key] ?: "" }

    // 🧾 सर्व Preferences clear करण्यासाठी (Logout / Reset scenario)
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
