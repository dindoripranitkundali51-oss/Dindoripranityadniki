package com.example.dindoripranityadnyiki.feature.user

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.example.dindoripranityadnyiki.LocalAppLanguage
import com.example.dindoripranityadnyiki.core.design.DivineTypography
import com.example.dindoripranityadnyiki.core.design.ShimmerLoadingEffect
import com.example.dindoripranityadnyiki.core.design.SocialEmptyState
import com.example.dindoripranityadnyiki.core.design.SocialUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoojaSelectionScreen(
    onPoojaSelected: (String, String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PoojaSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isMarathi = LocalAppLanguage.current == "Marathi"
    val allLabel = if (isMarathi) "सर्व" else "All"
    val dynamicCategories = remember(uiState.categories, isMarathi) {
        listOf(allLabel) + uiState.categories
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(0.dp),
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(bottom = 10.dp, top = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color(0xFF202020))
                        }
                        Text(
                            text = if (isMarathi) "पूजा निवडा" else "Select Pooja",
                            style = DivineTypography.headlineMedium.copy(
                                color = SocialUi.TitleColor,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            textAlign = TextAlign.Start,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 0.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                BasicTextField(
                                    value = uiState.searchQuery,
                                    onValueChange = { viewModel.onSearchQueryChanged(it, isMarathi) },
                                    modifier = Modifier.weight(1f),
                                    textStyle = DivineTypography.bodyLarge.copy(fontSize = 16.sp),
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        if (uiState.searchQuery.isEmpty()) {
                                            Text(
                                                if (isMarathi) "पूजा शोधा..." else "Search rituals or pooja...",
                                                style = DivineTypography.bodyLarge.copy(color = Color.Gray, fontSize = 16.sp)
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }
                        }
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        items(dynamicCategories) { category ->
                            val isSelected = uiState.selectedCategory == category ||
                                (category == allLabel && uiState.selectedCategory == "All") ||
                                (category == "All" && uiState.selectedCategory == allLabel)

                            Surface(
                                modifier = Modifier.clickable { viewModel.onCategoryChanged(category, isMarathi) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = category,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = DivineTypography.labelMedium.copy(
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            if (uiState.isLoading) {
                items(6) {
                    ShimmerLoadingEffect(
                        modifier = Modifier.fillMaxWidth().height(82.dp).padding(horizontal = 24.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                return@LazyColumn
            }

            if (uiState.poojaList.isEmpty()) {
                item {
                    SocialEmptyState(
                        message = uiState.error ?: if (isMarathi) "पूजा उपलब्ध नाहीत" else "No pooja available",
                        icon = Icons.Default.SearchOff
                    )
                }
            }

            items(uiState.poojaList) { pooja ->
                val poojaId = pooja["id"] as? String
                val poojaName = (if (isMarathi) pooja["name"] else pooja["nameEn"]) as? String ?: "Sacred Pooja"

                PoojaListRow(
                    name = poojaName,
                    imageUrl = pooja["imageUrl"] as? String ?: "",
                    isMarathi = isMarathi,
                    onClick = { poojaId?.let { onPoojaSelected(it, poojaName) } }
                )
            }
        }
    }
}

@Composable
fun PoojaListRow(
    name: String,
    imageUrl: String,
    isMarathi: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(58.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        ) {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { ShimmerLoadingEffect(modifier = Modifier.fillMaxSize(), shape = CircleShape) }
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = DivineTypography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = SocialUi.TitleColor
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isMarathi) "दक्षिणा सेवा पूर्ण झाल्यावर भरली जाईल" else "Dakshina is entered after completion",
                style = DivineTypography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 1
            )
        }
        Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
    }
}
