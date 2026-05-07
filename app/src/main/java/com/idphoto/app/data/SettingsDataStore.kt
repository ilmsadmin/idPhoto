package com.idphoto.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.idphoto.app.ui.AppLanguage
import com.idphoto.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * DataStore Preferences — lưu trữ cài đặt app bền vững.
 *
 * Các giá trị được lưu:
 * - Ngôn ngữ (language)
 * - Chế độ tối (themeMode)
 * - DPI ảnh (photoDpi)
 * - Định dạng output (outputFormat)
 * - Watermark bật/tắt (watermarkEnabled)
 * - Onboarding đã hoàn tất (onboardingCompleted)
 * - Khổ giấy in (paperSize)
 * - Đường cắt bật/tắt (cutLinesEnabled)
 */

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "id_photo_settings")

class SettingsDataStore(private val context: Context) {

    // ── Keys ──

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PHOTO_DPI = intPreferencesKey("photo_dpi")
        val OUTPUT_FORMAT = stringPreferencesKey("output_format")
        val WATERMARK_ENABLED = booleanPreferencesKey("watermark_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val PAPER_SIZE = stringPreferencesKey("paper_size")
        val CUT_LINES_ENABLED = booleanPreferencesKey("cut_lines_enabled")
    }

    // ── Data class for all settings ──

    data class Settings(
        val language: AppLanguage = AppLanguage.fromSystemLocale(),
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
        val photoDpi: Int = 300,
        val outputFormat: String = "JPEG",
        val watermarkEnabled: Boolean = false,
        val onboardingCompleted: Boolean = false,
        val paperSize: String = "4×6 inch",
        val cutLinesEnabled: Boolean = true,
    )

    // ── Read all settings as Flow ──

    val settingsFlow: Flow<Settings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            Settings(
                language = prefs[Keys.LANGUAGE]?.let { code ->
                    AppLanguage.entries.find { it.code == code }
                } ?: AppLanguage.fromSystemLocale(),
                themeMode = prefs[Keys.THEME_MODE]?.let { name ->
                    try { ThemeMode.valueOf(name) } catch (_: Exception) { ThemeMode.SYSTEM }
                } ?: ThemeMode.SYSTEM,
                photoDpi = prefs[Keys.PHOTO_DPI] ?: 300,
                outputFormat = prefs[Keys.OUTPUT_FORMAT] ?: "JPEG",
                watermarkEnabled = prefs[Keys.WATERMARK_ENABLED] ?: false,
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
                paperSize = prefs[Keys.PAPER_SIZE] ?: "4×6 inch",
                cutLinesEnabled = prefs[Keys.CUT_LINES_ENABLED] ?: true,
            )
        }

    // ── Write individual settings ──

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.code
        }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun setPhotoDpi(dpi: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PHOTO_DPI] = dpi
        }
    }

    suspend fun setOutputFormat(format: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OUTPUT_FORMAT] = format
        }
    }

    suspend fun setWatermarkEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WATERMARK_ENABLED] = enabled
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setPaperSize(paperSize: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PAPER_SIZE] = paperSize
        }
    }

    suspend fun setCutLinesEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CUT_LINES_ENABLED] = enabled
        }
    }
}
