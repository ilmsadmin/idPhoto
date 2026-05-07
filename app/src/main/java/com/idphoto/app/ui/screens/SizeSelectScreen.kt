package com.idphoto.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idphoto.app.processing.PhotoSize
import com.idphoto.app.processing.PhotoSizeManager
import com.idphoto.app.ui.LocalStrings
import com.idphoto.app.ui.localizedCountry
import com.idphoto.app.ui.localizedSizeDescription
import com.idphoto.app.ui.components.SizeThumb
import com.idphoto.app.ui.components.SurfaceIconButton
import com.idphoto.app.ui.components.TonalChip
import com.idphoto.app.ui.theme.LocalAppColors

/**
 * Size Selection Screen — Material 3 Expressive (Redesign 2.0).
 */
@Composable
fun SizeSelectScreen(
    onBack: () -> Unit,
    onSizeSelected: (PhotoSize) -> Unit,
    @Suppress("UNUSED_PARAMETER") onCustomSize: () -> Unit = {},
) {
    val strings = LocalStrings.current
    val colors = LocalAppColors.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf("VN") }
    var showCustomDialog by remember { mutableStateOf(false) }

    val countries = PhotoSizeManager.countries
    val sizes = remember(selectedCountry, searchQuery) {
        if (searchQuery.isBlank()) PhotoSizeManager.getSizesByCountry(selectedCountry)
        else PhotoSizeManager.searchSizes(searchQuery)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface),
    ) {
        // ── Topbar ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SurfaceIconButton(
                Icons.AutoMirrored.Filled.ArrowBackIos,
                onClick = onBack,
                contentDescription = strings.back,
                background = Color.Transparent,
            )
            Text(
                strings.selectSize,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.onSurface,
                modifier = Modifier.weight(1f),
                letterSpacing = (-0.2).sp,
            )
            SurfaceIconButton(Icons.Filled.Tune, onClick = {}, contentDescription = "Filter")
        }

        // ── Search ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surfaceContainer)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(color = colors.onSurface, fontSize = 14.sp),
                cursorBrush = Brush.verticalGradient(listOf(colors.primary, colors.primary)),
                decorationBox = { inner ->
                    if (searchQuery.isEmpty()) {
                        Text(
                            strings.searchPlaceholder,
                            color = colors.onSurfaceVariant,
                            fontSize = 14.sp,
                        )
                    }
                    inner()
                },
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── Country tabs ─────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            countries.forEach { country ->
                CountryTab(
                    name = strings.localizedCountry(country.code),
                    flag = countryFlagEmoji(country.code),
                    active = selectedCountry == country.code,
                    onClick = { selectedCountry = country.code },
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── Size list ─────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            // First item: featured highlight if current country has a featured size
            val featuredFirst = sizes.firstOrNull()
            itemsIndexed(sizes, featuredFirst) { index, size ->
                SizeRow(
                    size = size,
                    featured = (index == 0 && size == featuredFirst && searchQuery.isBlank()),
                    onClick = { onSizeSelected(size) },
                )
            }
            item {
                CustomSizeRow(onClick = { showCustomDialog = true })
            }
        }
    }

    if (showCustomDialog) {
        CustomSizeDialog(
            onDismiss = { showCustomDialog = false },
            onConfirm = { customSize ->
                showCustomDialog = false
                onSizeSelected(customSize)
            },
        )
    }
}

// Helper because LazyListScope.items doesn't take an extra param; use itemsIndexed.
private inline fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexed(
    items: List<PhotoSize>,
    @Suppress("UNUSED_PARAMETER") featured: PhotoSize?,
    crossinline content: @androidx.compose.runtime.Composable (Int, PhotoSize) -> Unit,
) {
    items(items.size) { i -> content(i, items[i]) }
}

// ────────────────────── Tabs / Rows ──────────────────────

@Composable
private fun CountryTab(name: String, flag: String, active: Boolean, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val bg = if (active) colors.onSurface else colors.surfaceContainer
    val fg = if (active) colors.surface else colors.onSurfaceVariant
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(flag, fontSize = 14.sp)
        Text(name, color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun countryFlagEmoji(code: String): String = when (code.uppercase()) {
    "VN" -> "🇻🇳"; "US" -> "🇺🇸"; "JP" -> "🇯🇵"; "EU" -> "🇪🇺"; "KR" -> "🇰🇷"; "CN" -> "🇨🇳"
    "GB", "UK" -> "🇬🇧"; "FR" -> "🇫🇷"; "DE" -> "🇩🇪"; "IN" -> "🇮🇳"
    else -> "🌐"
}

@Composable
private fun SizeRow(size: PhotoSize, featured: Boolean, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val containerMod = if (featured) {
        Modifier
            .background(
                Brush.linearGradient(
                    listOf(
                        colors.primary.copy(alpha = if (colors.isDark) 0.18f else 0.08f),
                        colors.primaryGrad2.copy(alpha = if (colors.isDark) 0.18f else 0.10f),
                    )
                )
            )
            .border(1.5.dp, colors.primary.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
    } else {
        Modifier
            .background(colors.surfaceContainerLowest)
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(20.dp))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(containerMod)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SizeThumb(width = 36.dp, height = 50.dp, accent = colors.primary)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    size.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )
                if (featured) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        LocalStrings.current.mostPopularSuffix,
                        fontSize = 12.sp,
                        color = colors.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(LocalStrings.current.localizedSizeDescription(size.description), fontSize = 12.sp, color = colors.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${size.widthPx}×${size.heightPx}px",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.primary,
            )
            if (featured) {
                Spacer(Modifier.height(4.dp))
                TonalChip(
                    text = LocalStrings.current.suggestedBadge,
                    background = if (colors.isDark) Color(0x4463CEC5) else Color(0xFFDDF4EF),
                    contentColor = if (colors.isDark) Color(0xFF9EEDE5) else Color(0xFF0E625B),
                )
            }
        }
    }
}

@Composable
private fun CustomSizeRow(onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        colors.secondary.copy(alpha = 0.10f),
                        colors.primary.copy(alpha = 0.06f),
                    )
                )
            )
            .border(1.5.dp, colors.secondary, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SizeThumb(width = 36.dp, height = 48.dp, accent = colors.secondary)
        Column(modifier = Modifier.weight(1f)) {
            Text(strings.customSize, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(strings.customSizeSub, fontSize = 12.sp, color = colors.onSurfaceVariant)
        }
        Icon(Icons.Filled.AddCircle, null, tint = colors.secondary, modifier = Modifier.size(28.dp))
    }
}

// ────────────────────── Custom size dialog ──────────────────────

@Composable
private fun CustomSizeDialog(onDismiss: () -> Unit, onConfirm: (PhotoSize) -> Unit) {
    val colors = LocalAppColors.current
    val strings = LocalStrings.current

    var width by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceContainerLow,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(strings.customSize, fontWeight = FontWeight.ExtraBold, color = colors.onSurface)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.customNameLabel, fontSize = 13.sp) },
                    placeholder = { Text(strings.customNameExample, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = width,
                        onValueChange = { width = it.filter { c -> c.isDigit() || c == '.' }; hasError = false },
                        label = { Text(strings.customWidthMmLabel) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = hasError && width.toFloatOrNull() == null,
                    )
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it.filter { c -> c.isDigit() || c == '.' }; hasError = false },
                        label = { Text(strings.customHeightMmLabel) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = hasError && height.toFloatOrNull() == null,
                    )
                }
                val w = width.toFloatOrNull(); val h = height.toFloatOrNull()
                if (w != null && h != null && w > 0 && h > 0) {
                    val pxW = (w / 25.4f * 300).toInt(); val pxH = (h / 25.4f * 300).toInt()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.primary.copy(alpha = 0.08f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Info, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${w.toInt()}×${h.toInt()} mm → ${pxW}×${pxH} px (300 DPI)",
                            fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.primary,
                        )
                    }
                }
                if (hasError) Text(strings.customInvalidSize, fontSize = 12.sp, color = colors.error)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = width.toFloatOrNull(); val h = height.toFloatOrNull()
                    if (w != null && h != null && w > 0 && h > 0) {
                        val pxW = (w / 25.4f * 300).toInt(); val pxH = (h / 25.4f * 300).toInt()
                        val displayName = if (name.isNotBlank()) name else "${w.toInt()}x${h.toInt()} mm"
                        onConfirm(
                            PhotoSize(
                                name = displayName, widthMm = w, heightMm = h,
                                description = strings.customSize,
                                widthPx = pxW, heightPx = pxH, country = "CUSTOM",
                            )
                        )
                    } else hasError = true
                },
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            ) { Text(strings.done, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel, color = colors.onSurfaceVariant) }
        },
    )
}
