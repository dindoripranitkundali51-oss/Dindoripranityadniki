// File: ProfileScreen.kt
package com.example.dindoripranityadnyiki.screens

import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.dindoripranityadnyiki.R
import com.example.dindoripranityadnyiki.data.PrefKeys
import com.example.dindoripranityadnyiki.data.dataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.navigation.NavController
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

// --------------------
// Data Model
// --------------------
data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val mobile: String = "",
    val district: String = "",
    val address: String = "",
    val pincode: String = "",
    val role: String = "",
    val photoUrl: String = ""
)

// --------------------
// Validation helpers
// --------------------
object ProfileValidation {
    fun validName(name: String) = name.trim().length >= 2
    fun validMobile(mobile: String) = mobile.trim().length == 10 && mobile.all { it.isDigit() }
    fun validEmail(email: String) = email.isBlank() || android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    fun validPincode(pin: String) = pin.isBlank() || (pin.length == 6 && pin.all { it.isDigit() })
}

// --------------------
// Repository interface + Firebase implementation
// --------------------
interface UserRepository {
    suspend fun getUserProfile(uid: String): UserProfile?
    suspend fun updateUserProfile(uid: String, updates: Map<String, Any>): Boolean
    suspend fun uploadProfileImage(uid: String, fileUri: Uri): String?
}

class FirebaseUserRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : UserRepository {
    private val usersCol get() = firestore.collection("users")

    override suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            val doc = usersCol.document(uid).get().awaitSafe()
            if (doc == null || !doc.exists()) return null
            UserProfile(
                uid = uid,
                fullName = doc.getString("fullName") ?: "",
                email = doc.getString("email") ?: "",
                mobile = doc.getString("mobile") ?: "",
                district = doc.getString("district") ?: "",
                address = doc.getString("address") ?: "",
                pincode = doc.getString("pincode") ?: "",
                role = doc.getString("role") ?: "",
                photoUrl = doc.getString("photoUrl") ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateUserProfile(uid: String, updates: Map<String, Any>): Boolean {
        return try {
            usersCol.document(uid)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .awaitSafe()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun uploadProfileImage(uid: String, fileUri: Uri): String? {
        return try {
            val ref = storage.reference.child("users/$uid/profile.jpg")
            ref.putFile(fileUri).awaitSafe()
            ref.downloadUrl.awaitSafe()?.toString()
        } catch (e: Exception) {
            null
        }
    }
}

// --------------------
// Task -> suspend helper
// --------------------
suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitSafe(): T? {
    return try {
        suspendCancellableCoroutine { cont ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    cont.resume(task.result)
                } else {
                    cont.resume(null)
                }
            }
            cont.invokeOnCancellation {
                try {
                    if (!isComplete) {
                        // no-op
                    }
                } catch (_: Exception) {
                }
            }
        }
    } catch (e: Exception) {
        null
    }
}

// --------------------
// ViewState & ViewModel
// --------------------
data class ProfileUiState(
    val loading: Boolean = true,
    val profile: UserProfile = UserProfile(),
    val inEditMode: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class ProfileViewModel(
    private val repo: UserRepository,
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _ui = MutableStateFlow(ProfileUiState())
    val ui: StateFlow<ProfileUiState> = _ui.asStateFlow()

    init {
        viewModelScopeLoad()
    }

    private fun viewModelScopeLoad() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            val uid = auth.currentUser?.uid
            if (uid == null) {
                val prefs = withContext(Dispatchers.IO) { context.dataStore.data.firstOrNullSafe() }
                val role = prefs?.get(PrefKeys.USER_ROLE) ?: ""
                val district = prefs?.get(PrefKeys.USER_DISTRICT) ?: ""
                val address = prefs?.get(PrefKeys.USER_ADDRESS) ?: ""
                val pincode = prefs?.get(PrefKeys.USER_PINCODE) ?: ""
                val profile = UserProfile(
                    uid = "",
                    fullName = "",
                    email = "",
                    mobile = "",
                    district = district,
                    address = address,
                    pincode = pincode,
                    role = role,
                    photoUrl = ""
                )
                _ui.value = _ui.value.copy(loading = false, profile = profile)
                return@launch
            }

            val p = withContext(Dispatchers.IO) { repo.getUserProfile(uid) }
            if (p != null) _ui.value = _ui.value.copy(loading = false, profile = p)
            else _ui.value = _ui.value.copy(loading = false, error = "Unable to load profile")
        }
    }

    fun refresh() = viewModelScope.launch { viewModelScopeLoad() }

    fun toggleEdit(enable: Boolean) {
        _ui.value = _ui.value.copy(inEditMode = enable, error = null, successMessage = null)
    }

    fun onProfileChange(update: UserProfile) {
        _ui.value = _ui.value.copy(profile = update)
    }

    fun saveProfile() = viewModelScope.launch {
        val cur = _ui.value.profile

        if (!ProfileValidation.validName(cur.fullName)) {
            _ui.value = _ui.value.copy(error = "Enter a valid name (min 2 chars)")
            return@launch
        }
        if (!ProfileValidation.validMobile(cur.mobile)) {
            _ui.value = _ui.value.copy(error = "Enter a valid 10-digit mobile")
            return@launch
        }
        if (!ProfileValidation.validEmail(cur.email)) {
            _ui.value = _ui.value.copy(error = "Enter a valid email")
            return@launch
        }
        if (!ProfileValidation.validPincode(cur.pincode)) {
            _ui.value = _ui.value.copy(error = "Enter a valid 6-digit pincode (or leave empty)")
            return@launch
        }

        _ui.value = _ui.value.copy(saving = true, error = null)
        val uid = auth.currentUser?.uid
        val updates = mapOf(
            "fullName" to cur.fullName,
            "mobile" to cur.mobile,
            "email" to cur.email,
            "district" to cur.district,
            "address" to cur.address,
            "pincode" to cur.pincode,
            "role" to cur.role
        )

        val ok = if (uid == null) {
            withContext(Dispatchers.IO) {
                context.dataStore.edit { prefs ->
                    prefs[PrefKeys.USER_DISTRICT] = cur.district
                    prefs[PrefKeys.USER_ADDRESS] = cur.address
                    prefs[PrefKeys.USER_PINCODE] = cur.pincode
                    prefs[PrefKeys.USER_ROLE] = cur.role
                }
            }
            true
        } else {
            withContext(Dispatchers.IO) { repo.updateUserProfile(uid, updates) }
        }

        if (ok) {
            _ui.value = _ui.value.copy(
                saving = false,
                inEditMode = false,
                successMessage = "Profile saved"
            )
            refresh()
        } else {
            _ui.value = _ui.value.copy(saving = false, error = "Save failed. Try again.")
        }
    }

    fun uploadImage(uri: Uri) = viewModelScope.launch {
        _ui.value = _ui.value.copy(saving = true, error = null)
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _ui.value = _ui.value.copy(saving = false, error = "Login required to upload photo")
            return@launch
        }
        val url = withContext(Dispatchers.IO) { repo.uploadProfileImage(uid, uri) }
        if (url != null) {
            val ok =
                withContext(Dispatchers.IO) { repo.updateUserProfile(uid, mapOf("photoUrl" to url)) }
            if (ok) {
                _ui.value = _ui.value.copy(saving = false, successMessage = "Photo uploaded")
                refresh()
            } else {
                _ui.value = _ui.value.copy(saving = false, error = "Failed to save photo url")
            }
        } else {
            _ui.value = _ui.value.copy(saving = false, error = "Image upload failed")
        }
    }

    fun logout() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            context.dataStore.edit { prefs ->
                prefs[PrefKeys.IS_LOGGED_IN] = false
            }
        }
        try {
            auth.signOut()
        } catch (_: Exception) {
        }
    }

    fun clearMessages() {
        _ui.value = _ui.value.copy(error = null, successMessage = null)
    }
}

// --------------------
// Compose UI (simple, clean)
// --------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenHost(
    viewModel: ProfileViewModel
) {
    val ctx = LocalContext.current
    val ui by remember { viewModel.ui }.collectAsState(initial = ProfileUiState())

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { viewModel.uploadImage(it) }
        }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Profile",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFB71C1C)
                ),
                actions = {
                    IconButton(onClick = {
                        viewModel.logout()
                        Toast.makeText(ctx, "Logged out", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF3F4F6))
                .padding(padding)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Avatar + name
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFFB71C1C), CircleShape)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (ui.profile.photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = ui.profile.photoUrl,
                            contentDescription = "Profile image",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_profile_user

                            ),
                            contentDescription = "avatar",
                            modifier = Modifier.size(70.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (ui.profile.fullName.isNotBlank()) ui.profile.fullName else "Guest User",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                Spacer(modifier = Modifier.height(18.dp))

                // Details card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProfileRow(
                            label = "Full name",
                            value = ui.profile.fullName,
                            editable = ui.inEditMode,
                            onChange = { viewModel.onProfileChange(ui.profile.copy(fullName = it)) }
                        )
                        ProfileRow(
                            label = "Email",
                            value = ui.profile.email,
                            editable = ui.inEditMode,
                            onChange = { viewModel.onProfileChange(ui.profile.copy(email = it)) }
                        )
                        ProfileRow(
                            label = "Mobile",
                            value = ui.profile.mobile,
                            editable = ui.inEditMode,
                            onChange = { viewModel.onProfileChange(ui.profile.copy(mobile = it)) }
                        )
                        ProfileRow(
                            label = "Address",
                            value = ui.profile.address,
                            editable = ui.inEditMode,
                            onChange = { viewModel.onProfileChange(ui.profile.copy(address = it)) },
                            multiline = true
                        )
                        ProfileRow(
                            label = "Pincode",
                            value = ui.profile.pincode,
                            editable = ui.inEditMode,
                            onChange = { viewModel.onProfileChange(ui.profile.copy(pincode = it)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Edit / Save buttons
                Crossfade(targetState = ui.inEditMode) { edit ->
                    if (edit) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.saveProfile() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Save")
                            }
                            OutlinedButton(
                                onClick = { viewModel.toggleEdit(false) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Cancel")
                            }
                        }
                    } else {
                        Button(
                            onClick = { viewModel.toggleEdit(true) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Edit Profile")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Messages & progress
                ui.error?.let {
                    Text(text = it, color = Color(0xFFB91C1C))
                }
                ui.successMessage?.let {
                    Text(text = it, color = Color(0xFF15803D))
                }

                if (ui.loading || ui.saving) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ProfileRow(
    label: String,
    value: String,
    editable: Boolean,
    onChange: (String) -> Unit,
    multiline: Boolean = false
) {
    Column {
        Text(text = label, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
        if (editable) {
            if (multiline) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onChange,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            } else {
                OutlinedTextField(
                    value = value,
                    onValueChange = onChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        } else {
            Text(
                text = if (value.isNotBlank()) value else "—",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                color = Color(0xFF111827)
            )
        }
    }
}

// --------------------
// Public wrapper
// --------------------
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val repo = FirebaseUserRepository(
        FirebaseFirestore.getInstance(),
        FirebaseStorage.getInstance()
    )
    val vm: ProfileViewModel = viewModel(factory = ProfileVMFactory(repo, context))
    ProfileScreenHost(viewModel = vm)
}

class ProfileVMFactory(
    private val repo: UserRepository,
    private val ctx: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ProfileViewModel(repo, ctx) as T
    }
}

// --------------------
// DataStore helper
// --------------------
suspend fun kotlinx.coroutines.flow.Flow<androidx.datastore.preferences.core.Preferences>.firstOrNullSafe():
        androidx.datastore.preferences.core.Preferences? {
    return try {
        this.first()
    } catch (e: Exception) {
        null
    }
}
