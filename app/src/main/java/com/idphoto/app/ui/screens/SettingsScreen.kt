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
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.review.ReviewManagerFactory
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.outlined.StarBorder
import com.idphoto.app.ui.AppLanguage
import com.idphoto.app.ui.LocalLanguage
import com.idphoto.app.ui.LocalStrings
import com.idphoto.app.ui.components.SurfaceIconButton
import com.idphoto.app.ui.theme.LocalAppColors
import com.idphoto.app.ui.theme.ThemeMode

/**
 * Get app version name from BuildConfig / PackageManager.
 */
@Composable
private fun getAppVersionName(): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }
}

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
    onPrivacyClick: () -> Unit = {},
) {
    val strings = LocalStrings.current
    val language = LocalLanguage.current
    val colors = LocalAppColors.current
    val context = androidx.compose.ui.platform.LocalContext.current

    // DPI picker dialog state
    var showDpiDialog by remember { mutableStateOf(false) }
    // Format picker dialog state
    var showFormatDialog by remember { mutableStateOf(false) }
    // Rate app dialog state
    var showRateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        // ── Top Bar ──
        Surface(
            modifier = Modifier
                .fillMaxWidth(),
            color = colors.surface,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 14.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SurfaceIconButton(
                    icon = Icons.Default.ArrowBackIosNew,
                    contentDescription = strings.back,
                    onClick = onBack,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    strings.settingsTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.onSurface,
                    letterSpacing = (-0.3).sp,
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
                        onClick = onPrivacyClick,
                        trailing = { SettingsArrow() },
                    )
                    HorizontalDivider(color = colors.divider, modifier = Modifier.padding(start = 68.dp))
                    SettingsItem(
                        icon = Icons.Default.BarChart,
                        iconBg = colors.iconBgOrange,
                        iconTint = Color(0xFFD84315),
                        title = strings.sRate,
                        subtitle = strings.fiveStars,
                        onClick = { showRateDialog = true },
                        trailing = { SettingsArrow() },
                    )
                    HorizontalDivider(color = colors.divider, modifier = Modifier.padding(start = 68.dp))
                    SettingsItem(
                        icon = Icons.Default.Info,
                        iconBg = colors.iconBgGreen,
                        iconTint = colors.success,
                        title = strings.sVersion,
                        subtitle = getAppVersionName(),
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

    // ── Rate Dialog ──
    if (showRateDialog) {
        RateAppDialog(
            onDismiss = { showRateDialog = false },
            onRatingSubmit = { rating ->
                showRateDialog = false
                if (rating in 1..3) {
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:zenixhq.com@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Feedback for ID Photo Pro")
                    }
                    try {
                        context.startActivity(emailIntent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else if (rating in 4..5) {
                    val reviewManager = ReviewManagerFactory.create(context)
                    val request = reviewManager.requestReviewFlow()
                    request.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val reviewInfo = task.result
                            var activity: android.app.Activity? = null
                            var currentContext = context
                            while (currentContext is android.content.ContextWrapper) {
                                if (currentContext is android.app.Activity) {
                                    activity = currentContext
                                    break
                                }
                                currentContext = currentContext.baseContext
                            }
                            if (activity == null && context is android.app.Activity) {
                                activity = context
                            }
                            if (activity != null) {
                                reviewManager.launchReviewFlow(activity, reviewInfo)
                            }
                        }
                    }
                }
            }
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
                        72 -> strings.dpiWebScreen
                        150 -> strings.dpiDraft
                        200 -> strings.dpiStandard
                        300 -> strings.dpiHighQuality
                        450 -> strings.dpiProfessional
                        600 -> strings.dpiUltraHd
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
        FormatOption(strings.formatJpeg, strings.formatJpegDesc),
        FormatOption(strings.formatPng, strings.formatPngDesc),
        FormatOption(strings.formatWebp, strings.formatWebpDesc),
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

// ── Rate App Dialog ──

@Composable
private fun RateAppDialog(
    onDismiss: () -> Unit,
    onRatingSubmit: (Int) -> Unit
) {
    val colors = LocalAppColors.current
    val strings = LocalStrings.current
    var rating by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(
                strings.sRate,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "How would you rate your experience?",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= rating) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = if (i <= rating) Color(0xFFFFC107) else colors.textTertiary,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { rating = i }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (rating > 0) onRatingSubmit(rating) },
                enabled = rating > 0
            ) {
                Text("Submit", color = if (rating > 0) colors.primary else colors.textTertiary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = colors.textSecondary)
            }
        }
    )
}
