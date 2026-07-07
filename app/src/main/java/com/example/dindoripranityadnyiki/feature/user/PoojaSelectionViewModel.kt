package com.example.dindoripranityadnyiki.feature.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dindoripranityadnyiki.core.data.SacredSevaRepository
import com.example.dindoripranityadnyiki.core.data.PoojaService
import com.example.dindoripranityadnyiki.core.data.toPresentationMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.*

/**
 * 🌳 TRIE DATA STRUCTURE FOR ULTRA-FAST SEARCH
 */
class PoojaTrie {
    private class Node {
        val children = mutableMapOf<Char, Node>()
        var isEndOfWord = false
        val poojaIndices = mutableListOf<Int>()
    }

    private val root = Node()

    fun insert(text: String, index: Int) {
        var current = root
        text.lowercase().forEach { char ->
            current = current.children.getOrPut(char) { Node() }
            current.poojaIndices.add(index)
        }
        current.isEndOfWord = true
    }

    fun search(prefix: String): List<Int> {
        var current = root
        prefix.lowercase().forEach { char ->
            current = current.children[char] ?: return emptyList()
        }
        return current.poojaIndices.distinct()
    }
}

data class PoojaSelectionUiState(
    val poojaList: List<Map<String, Any>> = emptyList(),
    val categories: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val suggestions: List<String> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class PoojaSelectionViewModel @Inject constructor(
    private val repository: SacredSevaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PoojaSelectionUiState())
    val uiState = _uiState.asStateFlow()

    private var fullPoojaList: List<PoojaService> = emptyList()
    private val trie = PoojaTrie()

    private val categoryMap = mapOf(
        "🏗️ वास्तू आणि बांधकाम" to listOf("भूमी पूजन", "पायाभरणी", "भूकंपशांती", "वातक्षोभ", "गृह प्रवेश", "वास्तुशांती", "जलपूजन"),
        "🛕 देवघर आणि प्रतिष्ठापना" to listOf("देवघर", "मूर्ती प्रतिष्ठापना", "देव प्रतिष्ठाण", "देवगृह", "प्राणप्रतिष्ठा"),
        "🔱 यंत्र आणि विशिष्ट पूजा" to listOf("यंत्र", "श्रीयंत्र", "रुद्र यंत्र", "स्वयंवर", "कालासिद्धी", "कुबेर यंत्र", "गुरुपिथावरील", "पादुका पूजन"),
        "🌙 व्रत, उत्सव आणि नियमित" to listOf("सत्यनारायण", "लक्ष्मी पूजन", "अनंत चतुर्दशी", "अनघा अष्टमी", "मल्हारी याग", "सप्ताह", "श्रमकीलक"),
        "🌌 दोष निवारण आणि शांती" to listOf("नक्षत्र", "शांतीकरण", "कालसर्प", "उपसर्ग", "उदक शांती", "नवचंडी"),
        "💍 कौटुंबिक संस्कार" to listOf("साखरपुडा", "विवाह", "कुंभविवाह", "अर्क", "पुसंवन", "पुत्रकामेष्टी", "मुंज", "पिंपळपूजन", "औदुंबरमुंज", "सहस्त्रचंद्र", "कलश"),
        "🕯️ पितृ विधी (उत्तरकार्य)" to listOf("अंत्यविधी", "पितृपूजन", "महालय", "श्राध्द")
    )

    init {
        loadPoojas()
    }

    private fun loadPoojas() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                fullPoojaList = repository.getSacredServices()
                
                fullPoojaList.forEachIndexed { index, pooja ->
                    val name = pooja.name
                    val nameEn = pooja.nameEn
                    trie.insert(name, index)
                    trie.insert(nameEn, index)
                    name.split(" ").forEach { trie.insert(it, index) }
                    nameEn.split(" ").forEach { trie.insert(it, index) }
                }

                _uiState.value = _uiState.value.copy(
                    categories = categoryMap.keys.toList()
                )
                applyFilters()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    fun onSearchQueryChanged(query: String, isMarathi: Boolean) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters(isMarathi)
    }

    fun onCategoryChanged(category: String, isMarathi: Boolean) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        applyFilters(isMarathi)
    }

    private fun applyFilters(isMarathi: Boolean = true) {
        val rawQuery = _uiState.value.searchQuery.lowercase().trim()
        val selectedCat = _uiState.value.selectedCategory
        val allLabel = if (isMarathi) "सर्व" else "All"
        
        val trieIndices = if (rawQuery.isNotEmpty()) trie.search(rawQuery) else null

        val filtered = fullPoojaList.filterIndexed { index, pooja ->
            val name = pooja.name.lowercase()
            val nameEn = pooja.nameEn.lowercase()
            
            val matchesSearch = if (rawQuery.isEmpty()) {
                true
            } else {
                trieIndices?.contains(index) == true || 
                isFuzzyMatch(rawQuery, name) || isFuzzyMatch(rawQuery, nameEn)
            }
            
            val matchesCategory = if (selectedCat == allLabel || selectedCat == "All" || selectedCat == "सर्व") {
                true
            } else {
                val keywords = categoryMap[selectedCat] ?: emptyList()
                keywords.any { name.contains(it.lowercase()) }
            }
            
            matchesSearch && matchesCategory
        }
        _uiState.value = _uiState.value.copy(poojaList = filtered.map { it.toPresentationMap() }, isLoading = false)
    }

    private fun getLevenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, minOf(dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost))
            }
        }
        return dp[s1.length][s2.length]
    }

    private fun isFuzzyMatch(query: String, target: String): Boolean {
        if (query.length < 3) return false
        val targetWords = target.split(" ", "-", "/").filter { it.length >= 3 }
        for (word in targetWords) {
            val distance = getLevenshteinDistance(query, word)
            val threshold = if (query.length <= 4) 1 else 2
            if (distance <= threshold) return true
        }
        return false
    }
}
