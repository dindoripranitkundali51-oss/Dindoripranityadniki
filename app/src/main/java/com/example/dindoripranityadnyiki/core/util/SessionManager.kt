package com.example.dindoripranityadnyiki.core.util

import android.content.Context
import com.example.dindoripranityadnyiki.core.data.AppLocaleStore
import com.example.dindoripranityadnyiki.core.data.DataStoreManager
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import com.example.dindoripranityadnyiki.core.data.dataStore
import kotlinx.coroutines.flow.first

sealed class SessionState {
    data object NotLoggedIn : SessionState()
    data class LoggedIn(val userType: String) : SessionState()
}

object SessionManager {

    suspend fun resolveSession(context: Context): SessionState {
        val prefs = runCatching { context.dataStore.data.first() }.getOrNull()
        val token = prefs?.get(PrefKeys.JWT_TOKEN).orEmpty()
        
        if (token.isBlank()) {
            clearLocalSession(context)
            return SessionState.NotLoggedIn
        }

        val isLoggedIn = prefs?.get(PrefKeys.IS_LOGGED_IN) ?: false
        if (!isLoggedIn) {
            return SessionState.NotLoggedIn
        }

        val userType = prefs[PrefKeys.USER_TYPE] ?: "user"
        return SessionState.LoggedIn(userType)
    }

    suspend fun clearLocalSession(context: Context) {
        DataStoreManager(context).clearAll()
        AppLocaleStore.mirrorLanguage(context, AppLocaleStore.DEFAULT_LANGUAGE)
    }

    suspend fun signOut(context: Context) {
        clearLocalSession(context)
    }
}
