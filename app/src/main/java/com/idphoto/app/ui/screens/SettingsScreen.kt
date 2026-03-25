package com.idphoto.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idphoto.app.ui.AppLanguage
import com.idphoto.app.ui.LocalLanguage
import com.idphoto.app.ui.LocalStrings
import com.idphoto.app.ui.theme.LocalAppColors
import com.idphoto.app.ui.theme.ThemeMode

/**
 * Settings Screen — matching mockup.
 *
 * Sections: Language, Appearance, Photo & Quality, Other.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    photoDpi: Int,
    outputFormat: String,
    watermarkEnabled: Boolean,
    onBack: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLanguageClick: () -> Unit,
    onPhotoDpiChange: (Int) -> Unit,
    onOutputFormatChange: (String) -> Unit,
    onWatermarkToggle: (Boolean) -> Unit,
) {
    val strings = LocalStrings.current
    val language = LocalLanguage.current
    val colors = LocalAppColors.current

    // DPI picker dialog state
    var showDpiDialog by remember { mutableStateOf(false) }
    // Format picker dialog state
    var showFormatDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        // ── Top Bar ──
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.surface,
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
                    strings.settingsTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Language Section ──
            SettingsSection(title = strings.sLangSection) {
                SettingsGroup {
                    SettingsItem(
                        icon = Icons.Outlined.Language,
                        iconBg = colors.iconBgBlue,
                        iconTint = colors.primary,
                        title = strings.sLanguage,
                        subtitle = language.nativeName,
                        onClick = onLanguageClick,
                        trailing = { SettingsArrow() },
                    )
                }
            }

            // ── Appearance / Dark Mode Section ──
            SettingsSection(title = strings.sAppearance) {
                SettingsGroup {
                    SettingsItem(
                        icon = Icons.Default.DarkMode,
                        iconBg = colors.iconBgIndigo,
                        iconTint = Color(0xFF3949AB),
                        title = strings.sDarkMode,
                        subtitle = when (themeMode) {
                            ThemeMode.SYSTEM -> strings.sDarkModeAuto
                            ThemeMode.LIGHT -> strings.sDarkModeLight
                            ThemeMode.DARK -> strings.sDarkModeDark
                        },
                        trailing = {},
                    )
                    // Theme mode options row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 68.dp, end = 16.dp, bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val label = when (mode) {
                                ThemeMode.SYSTEM -> strings.sDarkModeAuto
                                ThemeMode.LIGHT -> strings.sDarkModeLight
                                ThemeMode.DARK -> strings.sDarkModeDark
                            }
                            val isSelected = themeMode == mode
                            Surface(
                                onClick = { onThemeModeChange(mode) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) colors.primary else colors.background,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    label,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) colors.textOnPrimary else colors.textSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }

            // ── Photo & Quality Section ──
            SettingsSection(title = strings.sPhotoSection) {
                SettingsGroup {
                    SettingsItem(
                        icon = Icons.Default.Timer,
                        iconBg = colors.iconBgBlue,
                        iconTint = colors.primary,
                        title = strings.sQuality,
                        subtitle = "$photoDpi DPI",
                        onClick = { showDpiDialog = true },
                        trailing = { SettingsArrow() },
                    )
                    HorizontalDivider(color = colors.divider, modifier = Modifier.padding(start = 68.dp))
                    SettingsItem(
                        icon = Icons.Default.Image,
                        iconBg = colors.iconBgGreen,
                        iconTint = colors.success,
                        title = strings.sFormat,
                        subtitle = outputFormat,
                        onClick = { showFormatDialog = true },
                        trailing = { SettingsArrow() },
                    )
                    HorizontalDivider(color = colors.divider, modifier = Modifier.padding(start = 68.dp))
                    SettingsItem(
                        icon = Icons.Default.BrokenImage,
                        iconBg = colors.iconBgOrange,
                        iconTint = Color(0xFFE65100),
                        title = strings.sWatermark,
                        subtitle = if (watermarkEnabled) "" else strings.sWatermarkOff,
                        trailing = {
                            Switch(
                                checked = watermarkEnabled,
                                onCheckedChange = onWatermarkToggle,
                                colors = SwitchDefaults.colors(checkedTrackColor = colors.primary),
                            )
                        },
                    )
                }
            }

            // ── Other Section ──
            SettingsSection(title = strings.sOther) {
                SettingsGroup {
                    SettingsItem(
                        icon = Icons.Default.Shield,
                        iconBg = colors.iconBgIndigo,
                        iconTint = Color(0xFF3949AB),
                        title = strings.sPrivacy,
                        subtitle = strings.sPrivacySub,
                        trailing = { SettingsArrow() },
                    )
                    HorizontalDivider(color = colors.divider, modifier = Modifier.padding(start = 68.dp))
                    SettingsItem(
                        icon = Icons.AutoMirrored.Filled.Help,
                        iconBg = colors.iconBgOrange,
                        iconTint = Color(0xFFE65100),
                        title = strings.sHelp,
                        subtitle = "FAQ",
                        trailing = { SettingsArrow() },
                    )
                    HorizontalDivider(color = colors.divider, modifier = Modifier.padding(start = 68.dp))
                    SettingsItem(
                        icon = Icons.Default.BarChart,
                        iconBg = colors.iconBgOrange,
                        iconTint = Color(0xFFD84315),
                        title = strings.sRate,
                        subtitle = "★★★★★",
                        trailing = { SettingsArrow() },
                    )
                    HorizontalDivider(color = colors.divider, modifier = Modifier.padding(start = 68.dp))
                    SettingsItem(
                        icon = Icons.Default.Info,
                        iconBg = colors.iconBgGreen,
                        iconTint = colors.success,
                        title = strings.sVersion,
                        subtitle = "1.0.0",
                        trailing = { SettingsArrow() },
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // ── DPI Picker Dialog ──
    if (showDpiDialog) {
        DpiPickerDialog(
            currentDpi = photoDpi,
            onDpiSelected = { dpi ->
                onPhotoDpiChange(dpi)
                showDpiDialog = false
            },
            onDismiss = { showDpiDialog = false },
        )
    }

    // ── Format Picker Dialog ──
    if (showFormatDialog) {
        FormatPickerDialog(
            currentFormat = outputFormat,
            onFormatSelected = { format ->
                onOutputFormatChange(format)
                showFormatDialog = false
            },
            onDismiss = { showFormatDialog = false },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = LocalAppColors.current.textTertiary,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        content()
    }
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = LocalAppColors.current.surface,
        shadowElevation = 1.dp,
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    Surface(
        onClick = onClick ?: {},
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconTint,
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LocalAppColors.current.textPrimary,
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        subtitle,
                        fontSize = 11.sp,
                        color = LocalAppColors.current.textTertiary,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
            }
            trailing()
        }
    }
}

@Composable
private fun SettingsArrow() {
    Icon(
        Icons.Default.ChevronRight,
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = LocalAppColors.current.textTertiary,
    )
}

// ── DPI Picker Dialog ──

@Composable
private fun DpiPickerDialog(
    currentDpi: Int,
    onDpiSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    val strings = LocalStrings.current
    val dpiOptions = listOf(72, 150, 200, 300, 450, 600)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(
                strings.sQuality,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                dpiOptions.forEach { dpi ->
                    val isSelected = dpi == currentDpi
                    val description = when (dpi) {
                        72 -> "Web / Screen"
                        150 -> "Draft"
                        200 -> "Standard"
                        300 -> "High Quality ★"
                        450 -> "Professional"
                        600 -> "Ultra HD"
                        else -> ""
                    }
                    Surface(
                        onClick = { onDpiSelected(dpi) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) colors.primary.copy(alpha = 0.1f) else Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "$dpi DPI",
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) colors.primary else colors.textPrimary,
                                )
                                if (description.isNotEmpty()) {
                                    Text(
                                        description,
                                        fontSize = 12.sp,
                                        color = colors.textTertiary,
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = colors.primary,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = colors.textSecondary)
            }
        },
    )
}

// ── Format Picker Dialog ──

@Composable
private fun FormatPickerDialog(
    currentFormat: String,
    onFormatSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    val strings = LocalStrings.current

    data class FormatOption(val name: String, val description: String)

    val formatOptions = listOf(
        FormatOption("JPEG", "Smaller file, good quality"),
        FormatOption("PNG", "Lossless, transparent BG"),
        FormatOption("WEBP", "Modern, small & high quality"),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(
                strings.sFormat,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                formatOptions.forEach { format ->
                    val isSelected = format.name == currentFormat
                    Surface(
                        onClick = { onFormatSelected(format.name) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) colors.primary.copy(alpha = 0.1f) else Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    format.name,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) colors.primary else colors.textPrimary,
                                )
                                Text(
                                    format.description,
                                    fontSize = 12.sp,
                                    color = colors.textTertiary,
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = colors.primary,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = colors.textSecondary)
            }
        },
    )
}
