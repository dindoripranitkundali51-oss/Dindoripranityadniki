package com.example.dindoripranityadnyiki.feature.user

import android.R
import android.os.Parcelable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.parcelize.Parcelize
import java.util.Locale

// -----------------------------
// Route constants (use instead of hardcoded strings)
object Screens {
    const val BOOKING_FORM = "bookingForm"
}

// -----------------------------
// Models
// -----------------------------
enum class PoojaCategory(val label: String) {
    GRIHA_VASTU("गृह / वास्तु पूजा"),
    DEVSTHAPAN_YANTRA("देवस्थापन व यंत्र पूजन"),
    SHANTI_DOSHA("शांती / दोषनिवारण पूजा"),
    SANSKRITIK_UTSAV("सांस्कृतिक / धार्मिक उत्सव पूजा"),
    JEEVANSANSKAR("संस्कार / जीवनसंस्कार विधी"),
    PITRU_ANTYA("पितृ / अंत्यविधी पूजा"),
    ALL("सर्व")
}

@Parcelize
data class PoojaItem(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val description: String = "",
    val basePrice: Long = 0L
) : Parcelable

// -----------------------------
// Screen
// -----------------------------
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PoojaSelectionScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { FirebaseFirestore.getInstance() }

    // UI State
    var search by remember { mutableStateOf(TextFieldValue("")) }
    var selectedCategory by remember { mutableStateOf(PoojaCategory.ALL) }
    var selectedPoojaId by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf<String?>(null) }
    var poojaList by remember { mutableStateOf(listOf<PoojaItem>()) }

    // Soft gradient bg
    val bgBrush = remember {
        Brush.Companion.verticalGradient(
            listOf(Color(0xFFF8FAFF), Color(0xFFFFFFFF), Color(0xFFE3F2FD))
        )
    }

    // Intro animation
    val titleAlpha = remember { Animatable(0f) }
    val gridAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        titleAlpha.animateTo(1f, tween(700, easing = LinearEasing))
        gridAlpha.animateTo(1f, tween(700, 200, easing = FastOutSlowInEasing))
    }

    // Load from Firestore (with fallback to static list)
    LaunchedEffect(Unit) {
        isLoading = true
        isError = null
        try {
            val snap = db.collection("poojas")
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .await()

            val items = snap.documents.mapNotNull { d ->
                PoojaItem(
                    id = d.id,
                    name = d.getString("name") ?: "",
                    category = d.getString("category") ?: "",
                    imageUrl = d.getString("imageUrl") ?: "",
                    description = d.getString("description") ?: "",
                    basePrice = (d.getLong("basePrice") ?: 0L)
                )
            }

            poojaList = if (items.isNotEmpty()) items else fallbackPoojaList()
        } catch (e: Exception) {
            // network / rules / first-run: fallback
            poojaList = fallbackPoojaList()
            isError = e.localizedMessage
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            // Card-styled header replacing default top app bar
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 6.dp,
                shape = RectangleShape,
                color = Color(0xFFB71C1C), // adjust as needed
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                Column(
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.Companion.CenterHorizontally
                ) {
                    Text(
                        text = "Vedic ritual services",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Companion.ExtraBold,
                            color = Color.Companion.White
                        ),
                        modifier = Modifier.Companion.alpha(titleAlpha.value)
                    )
                    Spacer(modifier = Modifier.Companion.height(4.dp))
                    Text(
                        text = "Select Your Pooja",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Companion.White
                        ),
                        modifier = Modifier.Companion.alpha(titleAlpha.value)
                    )
                }
            }
        },
        bottomBar = {
            // Next button anchored at bottom so it stays visible
            AnimatedVisibility(
                visible = (selectedPoojaId != null),
                enter = fadeIn(tween(250)) +
                        slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(300)),
                exit = fadeOut(tween(200)) +
                        slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(200))
            ) {
                Box(
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    NextButton(
                        enabled = (selectedPoojaId != null),
                        onClick = {
                            val item = poojaList.firstOrNull { it.id == selectedPoojaId }
                            if (item != null) {
                                // Save minimal data in savedStateHandle; prefer id-only if item payload large
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("selectedPoojaId", item.id)

                                navController.navigate(Screens.BOOKING_FORM) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.Companion
                .fillMaxSize()
                .background(bgBrush)
                // apply the scaffold inner padding so content doesn't underlap topBar/bottomBar
                .padding(innerPadding)
        ) {
            if (isLoading) {
                LoadingState()
            } else {
                Column(
                    modifier = Modifier.Companion
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(Modifier.Companion.height(8.dp))

                    // Search
                    SearchBar(
                        value = search,
                        onValueChange = { search = it },
                        onClear = { search = TextFieldValue("") }
                    )

                    Spacer(Modifier.Companion.height(12.dp))

                    // Categories
                    CategoryRow(
                        selected = selectedCategory,
                        onSelected = { selectedCategory = it }
                    )

                    Spacer(Modifier.Companion.height(8.dp))

                    // Grid
                    val filtered = filteredPoojas(
                        list = poojaList,
                        query = search.text,
                        category = selectedCategory
                    )

                    AnimatedVisibility(visible = filtered.isNotEmpty()) {
                        PoojaGrid(
                            items = filtered,
                            selectedId = selectedPoojaId,
                            onSelect = { clickedId ->
                                // Toggle selection: if already selected → unselect (null), else select clickedId
                                selectedPoojaId =
                                    if (selectedPoojaId == clickedId) null else clickedId
                            },
                            gridAlpha = gridAlpha.value
                        )
                    }

                    if (filtered.isEmpty()) {
                        EmptyState(search.text, selectedCategory.label)
                    }

                    Spacer(Modifier.Companion.height(12.dp))

                    // Optional: Show any load-time error as a subtle hint (doesn't block flow)
                    isError?.let {
                        Text(
                            text = "Note: $it",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.Companion.fillMaxWidth(),
                            textAlign = TextAlign.Companion.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onClear: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val brandColor = Color(0xFF0D47A1)

    Surface(
        shape = shape,
        tonalElevation = 4.dp,
        shadowElevation = 10.dp,
        color = Color(0xFFFFE0B2), // थोडं transparent white
        border = BorderStroke(1.4.dp, brandColor.copy(alpha = 0.25f)), // elegant border
        modifier = Modifier.Companion
            .fillMaxWidth()
            .height(45.dp)
    ) {
        Row(
            modifier = Modifier.Companion
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = brandColor.copy(alpha = 0.8f),
                modifier = Modifier.Companion.size(22.dp)
            )

            Spacer(Modifier.Companion.width(10.dp))

            Box(modifier = Modifier.Companion.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = LocalTextStyle.current.copy(
                        color = Color(0xFF0F172A),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Companion.SemiBold
                    ),
                    cursorBrush = SolidColor(brandColor),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (value.text.isBlank()) {
                            Text(
                                text = "Search for a Pooja",
                                color = Color(0xFF607D8B),
                                fontWeight = FontWeight.Companion.Medium
                            )
                        }
                        innerTextField()
                    }
                )
            }

            AnimatedVisibility(visible = value.text.isNotEmpty()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.Companion
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(brandColor.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = brandColor
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    selected: PoojaCategory,
    onSelected: (PoojaCategory) -> Unit
) {
    val cats = listOf(
        PoojaCategory.ALL,
        PoojaCategory.GRIHA_VASTU,
        PoojaCategory.DEVSTHAPAN_YANTRA,
        PoojaCategory.SHANTI_DOSHA,
        PoojaCategory.SANSKRITIK_UTSAV,
        PoojaCategory.JEEVANSANSKAR,
        PoojaCategory.PITRU_ANTYA
    )
    val listState = rememberLazyListState()
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        // use item { } per category to avoid any items() overload/import issues
        cats.forEach { c ->
            item {
                val isActive = c == selected
                FilterChip(
                    label = c.label,
                    active = isActive,
                    onClick = { onSelected(c) }
                )
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
    val gradient = if (active)
        Brush.Companion.horizontalGradient(listOf(Color(0xFFFFE082), Color(0xFFFFCA28)))
    else Brush.Companion.horizontalGradient(listOf(Color.Companion.White, Color.Companion.White))

    Surface(
        onClick = onClick,
        shape = shape,
        border = BorderStroke(1.dp, if (active) Color(0xFFFFB300) else Color(0xFFE0E0E0)),
        shadowElevation = if (active) 6.dp else 2.dp,
        color = Color.Companion.Transparent
    ) {
        Box(
            modifier = Modifier.Companion
                .background(gradient, shape)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            contentAlignment = Alignment.Companion.Center
        ) {
            Text(
                text = label,
                color = if (active) Color(0xFF0D47A1) else Color(0xFF37474F),
                fontWeight = if (active) FontWeight.Companion.Bold else FontWeight.Companion.Medium,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun PoojaGrid(
    items: List<PoojaItem>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    gridAlpha: Float,
    modifier: Modifier = Modifier.Companion
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        modifier = modifier.alpha(gridAlpha)
    ) {
        items(items, key = { it.id.ifBlank { it.name } }) { item ->
            PoojaCard(
                item = item,
                selected = (item.id == selectedId),
                onClick = { onSelect(item.id) }
            )
        }
    }
}

@Composable
fun PoojaCard(
    item: PoojaItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val cardShape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    val borderColor = if (selected) Color(0xFF1565C0) else Color.Companion.Transparent
    val elevation = if (selected) 14.dp else 8.dp

    // Slightly taller card so image has breathing room
    Card(
        onClick = onClick,
        shape = cardShape,
        modifier = Modifier.Companion
            .height(200.dp)        // <- वाढवलेली उंची (पूर्वी 200.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Box(modifier = Modifier.Companion.fillMaxSize()) {

            // 1) Image area (give more height so important parts don't get cropped)
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.imageUrl.ifBlank { R.drawable.ic_menu_gallery })
                    .crossfade(true)
                    .build(),
                contentDescription = item.name,
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .height(140.dp)   // <- इमेजला पुरेसे vertical space (पूर्वी 92.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentScale = ContentScale.Companion.Crop,
                alignment = Alignment.Companion.Center // keep center; change to TopCenter if subject is top-aligned
            ) {
                val state = painter.state
                if (state is AsyncImagePainter.State.Loading) {
                    Box(
                        Modifier.Companion
                            .fillMaxSize()
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Companion.Center
                    ) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    }
                } else if (state is AsyncImagePainter.State.Error) {
                    Box(
                        Modifier.Companion
                            .fillMaxSize()
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Companion.Center
                    ) {
                        Text(text = "🕉️", fontSize = 28.sp)
                    }
                } else {
                    SubcomposeAsyncImageContent()
                }
            }

            // 2) subtle gradient overlay at image bottom so transition to info panel is smooth
            Box(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .height(36.dp)
                    .align(Alignment.Companion.TopStart)
                    .offset(y = 140.dp - 36.dp) // place at bottom of image (image height - overlay height)
                    .background(
                        Brush.Companion.verticalGradient(
                            colors = listOf(Color.Companion.Transparent, Color.Companion.White),
                        )
                    )
            )

            // 3) Bottom info panel (name) — reduced height so it doesn't fully cover image
            Box(
                modifier = Modifier.Companion
                    .align(Alignment.Companion.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Companion.White)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.Companion.fillMaxWidth(),
                    verticalAlignment = Alignment.Companion.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Companion.SemiBold),
                        maxLines = 2,
                        textAlign = TextAlign.Companion.Center,
                        color = Color(0xFF0D47A1)
                    )
                }
            }

            // 4) Selected badge top-end
            if (selected) {
                Box(
                    modifier = Modifier.Companion
                        .padding(8.dp)
                        .align(Alignment.Companion.TopEnd)
                        .border(
                            BorderStroke(2.dp, borderColor),
                            androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        )
                        .background(
                            Color.Companion.White.copy(alpha = 0.9f),
                            androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.Companion.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.Companion.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = Color(0xFF1565C0),
                            modifier = Modifier.Companion.size(18.dp)
                        )
                        Spacer(Modifier.Companion.width(6.dp))
                        Text(
                            text = "Selected",
                            fontSize = 12.sp,
                            color = Color(0xFF1565C0),
                            fontWeight = FontWeight.Companion.SemiBold
                        )
                    }
                }
            }

            // 5) subtle inner border
            Box(
                modifier = Modifier.Companion
                    .matchParentSize()
                    .padding(6.dp)
                    .border(BorderStroke(1.dp, borderColor.copy(alpha = 0.12f)), cardShape)
            )
        }
    }
}

@Composable
fun NextButton(enabled: Boolean, onClick: () -> Unit) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        modifier = Modifier.Companion
            .fillMaxWidth()
            .height(56.dp),
        shadowElevation = if (enabled) 12.dp else 0.dp,
        color = Color.Companion.Transparent
    ) {
        Box(
            modifier = Modifier.Companion
                .fillMaxSize()
                .background(
                    brush = if (enabled)
                        Brush.Companion.horizontalGradient(
                            listOf(
                                Color(0xFF0D47A1),
                                Color(0xFF1976D2)
                            )
                        )
                    else Brush.Companion.horizontalGradient(
                        listOf(
                            Color(0xFFF1F5F9),
                            Color(0xFFF1F5F9)
                        )
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
                )
                .clickable(enabled = enabled) { if (enabled) onClick() },
            contentAlignment = Alignment.Companion.Center
        ) {
            Row(verticalAlignment = Alignment.Companion.CenterVertically) {
                Text(
                    text = if (enabled) "Proceed to Booking" else "Select a Pooja",
                    color = if (enabled) Color.Companion.White else Color(0xFF94A3B8),
                    fontWeight = FontWeight.Companion.Bold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.Companion.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (enabled) Color.Companion.White else Color(0xFF94A3B8)
                )
            }
        }
    }
}


@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.Companion
            .fillMaxSize()
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Companion.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.Companion.height(12.dp))
        Text("Loading poojas…", color = Color(0xFF475569))
    }
}

@Composable
private fun EmptyState(query: String, category: String) {
    Column(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.Companion.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.ic_menu_help),
            contentDescription = null,
            modifier = Modifier.Companion.size(100.dp)
        )
        Spacer(Modifier.Companion.height(12.dp))
        Text(
            "No results",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Companion.Bold)
        )
        val q = if (query.isBlank()) "—" else "\"$query\""
        Text(
            "Try a different search or category.\nQuery: $q · Category: $category",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)),
            textAlign = TextAlign.Companion.Center
        )
    }
}

// -----------------------------
// Helpers
// -----------------------------
private fun filteredPoojas(
    list: List<PoojaItem>,
    query: String,
    category: PoojaCategory
): List<PoojaItem> {
    val q = query.trim().lowercase(Locale.getDefault())
    return list.asSequence()
        .filter { item ->
            val catOk = when (category) {
                PoojaCategory.ALL -> true
                else -> item.category.trim().lowercase(Locale.getDefault()) ==
                        category.label.trim().lowercase(Locale.getDefault())
            }
            val qOk = if (q.isBlank()) true else {
                item.name.lowercase(Locale.getDefault()).contains(q) ||
                        item.category.lowercase(Locale.getDefault()).contains(q)
            }
            catOk && qOk
        }
        .toList()
}

private fun fallbackPoojaList(): List<PoojaItem> {
    fun items(cat: PoojaCategory, names: List<String>) =
        names.mapIndexed { idx, n ->
            PoojaItem(
                id = "${cat.name}_$idx",
                name = n,
                category = cat.label,
                imageUrl = "",
                description = "",
                basePrice = 0
            )
        }

    val grahVastu = listOf(
        "वास्तुशांती","नवचंडीसह वास्तुशांती","गृह प्रवेश","भूमी पूजन",
        "पायाभरणी","जलपूजन","भूकंपशांती यंत्र पूजन","वातक्षोभ शांती यंत्र पूजन"
    )

    val devSthapan = listOf(
        "देव प्रतिष्ठाण","देवगृह यज्ञप्रतिष्ठा","मूर्ती प्राणप्रतिष्ठा","श्रीयंत्र पूजन",
        "श्रीयंत्र आवरण पूजा","रुद्र यंत्र पूजन","कुबेर यंत्र पूजन","गुरुपिथावरील स्थापित यंत्र पूजन"
    )

    val shanti = listOf(
        "नवचंडी","नक्षत्र शांती","करण / योग शांती","कालसर्प","उपसर्ग शांती","उदक शांती"
    )

    val utsav = listOf(
        "सत्यनारायण","अनंत चतुर्दशी","लक्ष्मी पूजन","मल्हारी याग","श्रमकीलक","सप्ताह"
    )

    val sanskar = listOf(
        "विवाह","कुंभविवाह / अर्क","पुसंवन विधी","मुंज","साखरपुडा",
        "पुत्रकामेष्टी","कलशपूजन","पिंपळपूजन / औदुंबरमुंज"
    )

    val pitru = listOf(
        "अंत्यविधी","पितृपूजन / महालयश्राध्द","सहस्त्रचंद्र दर्शन"
    )

    return buildList {
        addAll(items(PoojaCategory.GRIHA_VASTU, grahVastu))
        addAll(items(PoojaCategory.DEVSTHAPAN_YANTRA, devSthapan))
        addAll(items(PoojaCategory.SHANTI_DOSHA, shanti))
        addAll(items(PoojaCategory.SANSKRITIK_UTSAV, utsav))
        addAll(items(PoojaCategory.JEEVANSANSKAR, sanskar))
        addAll(items(PoojaCategory.PITRU_ANTYA, pitru))
    }
}