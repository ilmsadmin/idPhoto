package com.idphoto.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idphoto.app.ui.AppLanguage
import com.idphoto.app.ui.LocalLanguage
import com.idphoto.app.ui.LocalStrings

/**
 * Language selection bottom sheet — matching mockup.
 * 4 language options with flag, name, checkmark.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSheet(
    sheetState: SheetState,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE8EAED)),
            )
        },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                strings.chooseLang,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1A1C1E),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            )

            AppLanguage.entries.forEach { lang ->
                val isSelected = lang == currentLanguage
                LanguageOption(
                    flag = langFlags[lang.code] ?: "🌐",
                    name = lang.nativeName,
                    secondaryName = langSecondaryNames[lang.code] ?: "",
                    isSelected = isSelected,
                    onClick = { onLanguageSelected(lang) },
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LanguageOption(
    flag: String,
    name: String,
    secondaryName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected)
                    Modifier.border(2.dp, Color(0xFF1565C0), RoundedCornerShape(12.dp))
                else
                    Modifier.border(2.dp, Color.Transparent, RoundedCornerShape(12.dp))
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFFE8F0FE) else Color.Transparent,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Flag
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF4F6F9)),
                contentAlignment = Alignment.Center,
            ) {
                Text(flag, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1C1E),
                )
                if (secondaryName.isNotEmpty()) {
                    Text(
                        secondaryName,
                        fontSize = 12.sp,
                        color = Color(0xFF9AA0A6),
                    )
                }
            }

            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = Color(0xFF1565C0),
                )
            }
        }
    }
}

private val langFlags = mapOf(
    "vi" to "🇻🇳",
    "en" to "🇺🇸",
    "ja" to "🇯🇵",
    "zh" to "🇨🇳",
)

private val langSecondaryNames = mapOf(
    "vi" to "Vietnamese",
    "en" to "Tiếng Anh",
    "ja" to "Tiếng Nhật",
    "zh" to "Tiếng Trung",
)
