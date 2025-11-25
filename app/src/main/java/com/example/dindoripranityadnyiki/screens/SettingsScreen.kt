package com.example.dindoripranityadnyiki.screens

import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.viewModelScope
import com.example.dindoripranityadnyiki.data.dataStore
import com.example.dindoripranityadnyiki.data.PrefKeys
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException

// -----------------------------------------------------
// ENUMS
// -----------------------------------------------------

enum class AppLanguage { MARATHI, ENGLISH }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

// -----------------------------------------------------
// UI STATE
// -----------------------------------------------------

data class SettingsUiState(
    val isLoading: Boolean = true,
    val language: AppLanguage = AppLanguage.MARATHI,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val errorMessage: String? = null
)

// -----------------------------------------------------
// REPOSITORY - handles DataStore
// -----------------------------------------------------

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    val settingsFlow: Flow<SettingsUiState> = dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences())
            else throw e
        }
        .map { prefs ->
            val lang = when (prefs[PrefKeys.LANGUAGE]) {
                "en" -> AppLanguage.ENGLISH
                "mr" -> AppLanguage.MARATHI
                else -> AppLanguage.MARATHI
            }

            val theme = when (prefs[PrefKeys.THEME_MODE]) {
                "light" -> ThemeMode.LIGHT
                "dark" -> ThemeMode.DARK
                "system", null -> ThemeMode.SYSTEM
                else -> ThemeMode.SYSTEM
            }

            val notif = prefs[PrefKeys.NOTIFICATION_ENABLED] ?: true

            SettingsUiState(
                isLoading = false,
                language = lang,
                themeMode = theme,
                notificationsEnabled = notif,
                errorMessage = null
            )
        }

    suspend fun updateLanguage(language: AppLanguage) {
        withContext(ioDispatcher) {
            dataStore.edit {
                it[PrefKeys.LANGUAGE] = if (language == AppLanguage.MARATHI) "mr" else "en"
            }
        }
    }

    suspend fun updateTheme(theme: ThemeMode) {
        val value = when (theme) {
            ThemeMode.SYSTEM -> "system"
            ThemeMode.LIGHT -> "light"
            ThemeMode.DARK -> "dark"
        }
        withContext(ioDispatcher) {
            dataStore.edit { it[PrefKeys.THEME_MODE] = value }
        }
    }

    suspend fun updateNotifications(enabled: Boolean) {
        withContext(ioDispatcher) {
            dataStore.edit { it[PrefKeys.NOTIFICATION_ENABLED] = enabled }
        }
    }

    companion object {
        fun from(context: Context) = SettingsRepository(context.dataStore)
    }
}

// -----------------------------------------------------
// VIEWMODEL
// -----------------------------------------------------

class SettingsViewModel(
    private val repo: SettingsRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _ui.asStateFlow()

    init {
        // Observe DataStore changes
        viewModelScope.launch {
            repo.settingsFlow
                .onStart { _ui.update { it.copy(isLoading = true) } }
                .catch { e ->
                    _ui.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.localizedMessage ?: "Failed to load settings"
                        )
                    }
                }
                .collect { stateFromRepo ->
                    _ui.value = stateFromRepo
                }
        }
    }

    fun updateLanguage(lang: AppLanguage) {
        _ui.update { it.copy(language = lang) } // optimistic update
        viewModelScope.launch {
            runCatching { repo.updateLanguage(lang) }
                .onFailure { e ->
                    _ui.update {
                        it.copy(errorMessage = e.localizedMessage ?: "Unable to save language")
                    }
                }
        }
    }

    fun updateTheme(theme: ThemeMode) {
        _ui.update { it.copy(themeMode = theme) }
        viewModelScope.launch {
            runCatching { repo.updateTheme(theme) }
                .onFailure { e ->
                    _ui.update {
                        it.copy(errorMessage = e.localizedMessage ?: "Unable to save theme")
                    }
                }
        }
    }

    fun updateNotifications(enabled: Boolean) {
        _ui.update { it.copy(notificationsEnabled = enabled) }
        viewModelScope.launch {
            runCatching { repo.updateNotifications(enabled) }
                .onFailure { e ->
                    _ui.update {
                        it.copy(errorMessage = e.localizedMessage ?: "Unable to save notification setting")
                    }
                }
        }
    }

    fun clearError() {
        _ui.update { it.copy(errorMessage = null) }
    }

    class Factory(private val repo: SettingsRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repo) as T
        }
    }
}

// -----------------------------------------------------
// UI SCREEN
// -----------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {

    val ctx = LocalContext.current
    val vm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(SettingsRepository.from(ctx))
    )
    val ui by vm.uiState.collectAsState()

    val version = remember {
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    val bgBrush = remember {
        Brush.verticalGradient(listOf(Color.White, Color(0xFFF2F3F5)))
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Settings",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFB71C1C)
                )
            )
        }
    ) { inner ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            if (ui.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )
            }

            // --------------------------
            // LANGUAGE SECTION
            // --------------------------
            SectionCard(icon = Icons.Filled.Language, title = "App Language") {
                RadioRow(
                    label = "Marathi",
                    selected = ui.language == AppLanguage.MARATHI,
                    onClick = { vm.updateLanguage(AppLanguage.MARATHI) }
                )
                RadioRow(
                    label = "English",
                    selected = ui.language == AppLanguage.ENGLISH,
                    onClick = { vm.updateLanguage(AppLanguage.ENGLISH) }
                )
            }

            // --------------------------
            // THEME SECTION
            // --------------------------
            SectionCard(icon = Icons.Filled.Brightness4, title = "Theme") {
                RadioRow(
                    label = "System Default",
                    selected = ui.themeMode == ThemeMode.SYSTEM,
                    onClick = { vm.updateTheme(ThemeMode.SYSTEM) }
                )
                RadioRow(
                    label = "Light",
                    selected = ui.themeMode == ThemeMode.LIGHT,
                    onClick = { vm.updateTheme(ThemeMode.LIGHT) }
                )
                RadioRow(
                    label = "Dark",
                    selected = ui.themeMode == ThemeMode.DARK,
                    onClick = { vm.updateTheme(ThemeMode.DARK) }
                )
            }

            // --------------------------
            // NOTIFICATIONS SECTION
            // --------------------------
            SectionCard(icon = Icons.Filled.Notifications, title = "Notifications") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Push Notifications",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Receive updates about your puja bookings",
                            fontSize = 13.sp,
                            color = Color(0xFF607D8B)
                        )
                    }
                    Switch(
                        checked = ui.notificationsEnabled,
                        onCheckedChange = { vm.updateNotifications(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // --------------------------
            // FOOTER
            // --------------------------
            Text(
                text = "Version $version",
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        // Simple inline error handler – तुला नंतर Snackbar वापरायचा असेल तर integrate करू शकतो.
        ui.errorMessage?.let {
            LaunchedEffect(it) {
                vm.clearError()
            }
        }
    }
}

// -----------------------------------------------------
// Helpers
// -----------------------------------------------------

@Composable
fun SectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color(0xFF0D47A1),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0D47A1)
                )
            }
            Divider(modifier = Modifier.padding(vertical = 6.dp))
            content()
        }
    }
}

@Composable
fun RadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp
        )
        RadioButton(
            selected = selected,
            onClick = onClick
        )
    }
}
