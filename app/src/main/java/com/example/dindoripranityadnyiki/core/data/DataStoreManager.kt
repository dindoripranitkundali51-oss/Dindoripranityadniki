package com.example.dindoripranityadnyiki.core.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Shared application DataStore instance.
val Context.dataStore by preferencesDataStore(name = "PoojaAppPrefs")

object AppLocaleStore {
    private const val PREF_NAME = "AppLocalePrefs"
    private const val KEY_LANGUAGE = "language"
    const val DEFAULT_LANGUAGE = "mr"

    fun readLanguage(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, DEFAULT_LANGUAGE)
            ?: DEFAULT_LANGUAGE
    }

    fun mirrorLanguage(context: Context, language: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.ifBlank { DEFAULT_LANGUAGE })
            .apply()
    }
}

/**
 * Manager class for handling DataStore operations.
 * Provides a central point for reading and writing app preferences.
 */
class DataStoreManager(private val context: Context) {

    /**
     * Saves a boolean preference to DataStore.
     */
    suspend fun savePreference(key: Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[key] = value
        }
    }

    /**
     * Saves a string preference to DataStore.
     * If the key is for language, it also mirrors the value to SharedPreferences.
     */
    suspend fun saveStringPreference(key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { prefs ->
            prefs[key] = value
        }
        if (key == PrefKeys.LANGUAGE) {
            AppLocaleStore.mirrorLanguage(context, value)
        }
    }

    /**
     * Reads a boolean preference from DataStore.
     * Safely handles IOExceptions by emitting empty preferences.
     */
    fun readBoolean(key: Preferences.Key<Boolean>): Flow<Boolean> =
        context.dataStore.data
            .catch { e ->
                if (e is IOException) emit(emptyPreferences())
                else throw e
            }
            .map { it[key] ?: false }

    /**
     * Reads a string preference from DataStore.
     * Safely handles IOExceptions by emitting empty preferences.
     */
    fun readString(key: Preferences.Key<String>): Flow<String> =
        context.dataStore.data
            .catch { e ->
                if (e is IOException) emit(emptyPreferences())
                else throw e
            }
            .map { it[key] ?: "" }

    /**
     * Clears all preferences from DataStore and resets local language mirror.
     */
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
        AppLocaleStore.mirrorLanguage(context, AppLocaleStore.DEFAULT_LANGUAGE)
    }
}
