package com.example.dindoripranityadnyiki.feature.guruji

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dindoripranityadnyiki.core.data.GurujiRepository
import com.example.dindoripranityadnyiki.core.data.toPresentationMap
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WalletUiState(
    val balance: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val pendingWithdrawal: Double = 0.0,
    val transactions: List<Map<String, Any>> = emptyList(),
    val isLoading: Boolean = true,
    val isWithdrawing: Boolean = false,
    val withdrawalSuccess: Boolean = false,
    val message: String? = null
)

class GurujiWalletViewModel(
    private val repository: GurujiRepository = GurujiRepository.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState = _uiState.asStateFlow()

    private var balanceListener: ListenerRegistration? = null
    private var transListener: ListenerRegistration? = null

    init {
        monitorWallet()
    }

    private fun monitorWallet() {
        // 1. Monitor Balance via Repository
        balanceListener = repository.listenToProfile { data ->
            val bal = data?.walletBalance ?: 0.0
            val total = data?.totalEarnings ?: 0.0
            val pending = data?.pendingWithdrawal ?: 0.0
            _uiState.value = _uiState.value.copy(
                balance = bal,
                totalEarnings = total,
                pendingWithdrawal = pending
            )
        }

        // 2. Monitor Transactions via Repository
        transListener = repository.listenToTransactions { list ->
            _uiState.value = _uiState.value.copy(transactions = list.map { it.toPresentationMap() }, isLoading = false)
        }
    }

    /**
     * Requests withdrawal through the backend so balance and KYC checks stay server-side.
     */
    fun requestWithdrawal(
        amount: Double,
        accountHolder: String,
        accountNumber: String,
        ifsc: String,
        upiId: String
    ) {
        if (amount < 500) {
            _uiState.value = _uiState.value.copy(message = "किमान विड्रॉवल ₹५०० आहे")
            return
        }

        if (amount > _uiState.value.balance) {
            _uiState.value = _uiState.value.copy(message = "अपुरेशी शिल्लक!")
            return
        }

        val hasUpi = upiId.trim().matches(Regex("^[\\w.-]+@[\\w.-]+$"))
        val hasBank = accountHolder.trim().length >= 3 &&
            accountNumber.trim().matches(Regex("^\\d{9,18}$")) &&
            ifsc.trim().uppercase().matches(Regex("^[A-Z]{4}0[A-Z0-9]{6}$"))
        if (!hasUpi && !hasBank) {
            _uiState.value = _uiState.value.copy(message = "UPI किंवा पूर्ण bank details भरा.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWithdrawing = true, withdrawalSuccess = false)
            
            repository.requestWithdrawal(
                amount = amount,
                accountHolder = accountHolder.trim(),
                accountNumber = accountNumber.trim(),
                ifsc = ifsc.trim().uppercase(),
                upiId = upiId.trim()
            )
                .onSuccess { msg ->
                    _uiState.value = _uiState.value.copy(isWithdrawing = false, withdrawalSuccess = true, message = msg)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isWithdrawing = false, 
                        message = "त्रुटी: ${e.localizedMessage}"
                    )
                }
        }
    }

    fun clearMessage() { _uiState.value = _uiState.value.copy(message = null, withdrawalSuccess = false) }

    override fun onCleared() {
        balanceListener?.remove()
        transListener?.remove()
        super.onCleared()
    }
}
