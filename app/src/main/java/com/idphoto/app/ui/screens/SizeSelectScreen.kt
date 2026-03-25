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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idphoto.app.processing.PhotoSize
import com.idphoto.app.processing.PhotoSizeManager
import com.idphoto.app.ui.LocalStrings
import com.idphoto.app.ui.theme.LocalAppColors

/**
 * Size Selection Screen — matching mockup.
 *
 * Top bar with back + title, search box, horizontal country tabs,
 * vertical list of sizes, custom size item at bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SizeSelectScreen(
    onBack: () -> Unit,
    onSizeSelected: (PhotoSize) -> Unit,
    onCustomSize: () -> Unit = {},
) {
    val strings = LocalStrings.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf("VN") }
    val colors = LocalAppColors.current

    // Custom size dialog state
    var showCustomSizeDialog by remember { mutableStateOf(false) }

    val countries = PhotoSizeManager.countries
    val sizes = remember(selectedCountry, searchQuery) {
        if (searchQuery.isBlank()) {
            PhotoSizeManager.getSizesByCountry(selectedCountry)
        } else {
            PhotoSizeManager.searchSizes(searchQuery)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        // ── Top Bar ──
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.surface,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 52.dp, bottom = 14.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    color = colors.background,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.ArrowBackIosNew,
                            contentDescription = strings.back,
                            modifier = Modifier.size(22.dp),
                            tint = colors.textPrimary,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    strings.selectSize,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                )
            }
        }

        // ── Search Box ──
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceElevated,
            shadowElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            strings.searchPlaceholder,
                            color = colors.textTertiary,
                            fontSize = 14.sp,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                )
            }
        }

        // ── Country Tabs ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            countries.forEach { countryTab ->
                val isSelected = selectedCountry == countryTab.code
                Surface(
                    onClick = { selectedCountry = countryTab.code },
                    shape = RoundedCornerShape(24.dp),
                    color = if (isSelected) colors.primary else colors.surface,
                    border = if (isSelected) null else ButtonDefaults.outlinedButtonBorder(enabled = true),
                ) {
                    Text(
                        countryTab.name,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color.White else colors.textSecondary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Size List ──
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            items(sizes) { size ->
                SizeListItem(
                    name = size.name,
                    description = size.description,
                    onClick = { onSizeSelected(size) },
                )
            }
            // Custom size item
            item {
                CustomSizeItem(
                    title = strings.customSize,
                    subtitle = strings.customSizeSub,
                    onClick = { showCustomSizeDialog = true },
                )
            }
        }
    }

    // ── Custom Size Dialog ──
    if (showCustomSizeDialog) {
        CustomSizeDialog(
            onDismiss = { showCustomSizeDialog = false },
            onConfirm = { customSize ->
                showCustomSizeDialog = false
                onSizeSelected(customSize)
            },
        )
    }
}

@Composable
private fun SizeListItem(
    name: String,
    description: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = LocalAppColors.current.surface,
        shadowElevation = 1.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LocalAppColors.current.primaryLight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = LocalAppColors.current.primary,
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocalAppColors.current.textPrimary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    description,
                    fontSize = 12.sp,
                    color = LocalAppColors.current.textTertiary,
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = LocalAppColors.current.textTertiary,
            )
        }
    }
}

@Composable
private fun CustomSizeItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = LocalAppColors.current.surface,
        shadowElevation = 1.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LocalAppColors.current.primaryLight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = LocalAppColors.current.primary,
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocalAppColors.current.textPrimary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = LocalAppColors.current.textTertiary,
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = LocalAppColors.current.textTertiary,
            )
        }
    }
}

// ── Custom Size Dialog ──

@Composable
private fun CustomSizeDialog(
    onDismiss: () -> Unit,
    onConfirm: (PhotoSize) -> Unit,
) {
    val colors = LocalAppColors.current
    val strings = LocalStrings.current

    var widthText by remember { mutableStateOf("") }
    var heightText by remember { mutableStateOf("") }
    var nameText by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(
                strings.customSize,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Name field (optional)
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Tên (tuỳ chọn)", fontSize = 13.sp) },
                    placeholder = { Text("Ví dụ: Ảnh thẻ 3x4", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                )

                // Width & Height row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = widthText,
                        onValueChange = {
                            widthText = it.filter { c -> c.isDigit() || c == '.' }
                            hasError = false
                        },
                        label = { Text("Rộng (mm)", fontSize = 13.sp) },
                        placeholder = { Text("30", fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = hasError && widthText.toFloatOrNull() == null,
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    )
                    OutlinedTextField(
                        value = heightText,
                        onValueChange = {
                            heightText = it.filter { c -> c.isDigit() || c == '.' }
                            hasError = false
                        },
                        label = { Text("Cao (mm)", fontSize = 13.sp) },
                        placeholder = { Text("40", fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = hasError && heightText.toFloatOrNull() == null,
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    )
                }

                if (hasError) {
                    Text(
                        "Vui lòng nhập chiều rộng và cao hợp lệ",
                        fontSize = 12.sp,
                        color = Color(0xFFC62828),
                    )
                }

                // Preview info
                val w = widthText.toFloatOrNull()
                val h = heightText.toFloatOrNull()
                if (w != null && h != null && w > 0 && h > 0) {
                    val dpi = 300
                    val pxW = (w / 25.4f * dpi).toInt()
                    val pxH = (h / 25.4f * dpi).toInt()
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.primary.copy(alpha = 0.08f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = colors.primary,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "${w.toInt()}×${h.toInt()} mm → ${pxW}×${pxH} px (300 DPI)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.primary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = widthText.toFloatOrNull()
                    val h = heightText.toFloatOrNull()
                    if (w != null && h != null && w > 0 && h > 0) {
                        val dpi = 300
                        val pxW = (w / 25.4f * dpi).toInt()
                        val pxH = (h / 25.4f * dpi).toInt()
                        val displayName = if (nameText.isNotBlank()) nameText
                            else "${w.toInt()}x${h.toInt()} mm"
                        val customSize = PhotoSize(
                            name = displayName,
                            widthMm = w,
                            heightMm = h,
                            description = strings.customSize,
                            widthPx = pxW,
                            heightPx = pxH,
                            country = "CUSTOM",
                        )
                        onConfirm(customSize)
                    } else {
                        hasError = true
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            ) {
                Text(strings.done, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = colors.textSecondary)
            }
        },
    )
}
