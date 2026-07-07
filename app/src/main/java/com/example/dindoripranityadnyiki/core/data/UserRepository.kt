package com.example.dindoripranityadnyiki.core.data

import com.example.dindoripranityadnyiki.core.network.ApiService
import com.example.dindoripranityadnyiki.core.network.LoginRequest
import com.example.dindoripranityadnyiki.core.network.UserRegisterRequest
import com.example.dindoripranityadnyiki.core.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager
) {

    fun getUid(): String {
        // Since we are using REST, we can store and return the logged-in user's mobile or unique ID.
        // We will read the unique ID or mobile from DataStore.
        return runBlocking {
            dataStoreManager.readString(PrefKeys.USER_MOBILE).first()
        }
    }

    suspend fun saveUserRegistration(profile: UserProfile): Result<Boolean> = runCatching {
        // In the REST API, registering is done via ApiService.registerUser.
        // This function is kept for signature compatibility.
        val request = UserRegisterRequest(
            fullName = profile.fullName,
            mobile = profile.mobile,
            email = profile.email,
            address = profile.address,
            district = profile.district,
            pincode = profile.pincode,
            lat = profile.lat,
            lng = profile.lng
        )
        val response = apiService.registerUser(request)
        if (response.isSuccessful && response.body()?.success == true) {
            val body = response.body()!!
            body.token?.let { dataStoreManager.saveStringPreference(PrefKeys.JWT_TOKEN, it) }
            dataStoreManager.saveStringPreference(PrefKeys.USER_NAME, profile.fullName)
            dataStoreManager.saveStringPreference(PrefKeys.USER_MOBILE, profile.mobile)
            dataStoreManager.saveStringPreference(PrefKeys.USER_EMAIL, profile.email)
            dataStoreManager.saveStringPreference(PrefKeys.USER_ADDRESS, profile.address)
            dataStoreManager.saveStringPreference(PrefKeys.USER_DISTRICT, profile.district)
            dataStoreManager.saveStringPreference(PrefKeys.USER_PINCODE, profile.pincode)
            true
        } else {
            throw Exception(response.errorBody()?.string() ?: "Registration failed")
        }
    }

    suspend fun registerNewUser(uid: String, profile: UserProfile): Result<Boolean> =
        saveUserRegistration(profile.copy(id = "", uid = uid))

    suspend fun getUserProfile(): UserProfile? {
        val mobile = getUid()
        if (mobile.isBlank()) return null
        return try {
            val name = dataStoreManager.readString(PrefKeys.USER_NAME).first()
            val email = dataStoreManager.readString(PrefKeys.USER_EMAIL).first()
            val address = dataStoreManager.readString(PrefKeys.USER_ADDRESS).first()
            val district = dataStoreManager.readString(PrefKeys.USER_DISTRICT).first()
            val pincode = dataStoreManager.readString(PrefKeys.USER_PINCODE).first()
            
            UserProfile(
                fullName = name,
                mobile = mobile,
                email = email,
                address = address,
                district = district,
                pincode = pincode,
                uid = mobile,
                id = mobile
            )
        } catch (e: Exception) {
            null
        }
    }

    fun getUserProfileFlow(): Flow<Resource<UserProfile>> = flow {
        emit(Resource.Loading())
        try {
            val name = dataStoreManager.readString(PrefKeys.USER_NAME).first()
            val mobile = dataStoreManager.readString(PrefKeys.USER_MOBILE).first()
            val email = dataStoreManager.readString(PrefKeys.USER_EMAIL).first()
            val address = dataStoreManager.readString(PrefKeys.USER_ADDRESS).first()
            val district = dataStoreManager.readString(PrefKeys.USER_DISTRICT).first()
            val pincode = dataStoreManager.readString(PrefKeys.USER_PINCODE).first()
            
            if (mobile.isBlank()) {
                emit(Resource.Error("User not logged in"))
            } else {
                emit(Resource.Success(UserProfile(
                    fullName = name,
                    mobile = mobile,
                    email = email,
                    address = address,
                    district = district,
                    pincode = pincode,
                    uid = mobile,
                    id = mobile
                )))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to read user profile"))
        }
    }
}
