package com.idphoto.app.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.idphoto.app.data.SettingsDataStore
import com.idphoto.app.processing.*
import com.idphoto.app.ui.screens.EditTool
import com.idphoto.app.ui.screens.PipelineStepUi
import com.idphoto.app.ui.screens.StepStatus
import com.idphoto.app.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * New MainViewModel — integrates IDPhotoPipeline, navigation-based flow, i18n.
 */

data class AppUiState(
    // Photos
    val originalBitmap: Bitmap? = null,
    val processedBitmap: Bitmap? = null,      // After pipeline (segmented)
    val compositeBitmap: Bitmap? = null,      // After bg + outfit
    val croppedBitmap: Bitmap? = null,        // After size crop
    val printLayoutBitmap: Bitmap? = null,

    // Pipeline state
    val pipelineSteps: List<PipelineStepUi> = emptyList(),
    val pipelineMessage: String = "",
    val pipelineError: String? = null,
    val isProcessing: Boolean = false,

    // Selected size
    val selectedSizeIndex: Int = 1, // Default 3x4
    val selectedSize: PhotoSize = PhotoSizeManager.standardSizes[1],

    // Edit state
    val selectedBgColor: Color = Color(0xFFF0F0F0),
    val selectedBgIndex: Int = 0,
    val brightnessLevel: Float = 0f,          // -1f..1f, 0 = original
    val selectedOutfitIndex: Int = 0,
    val activeEditTool: EditTool? = null,     // Which tool panel is open

    // Print state
    val printQuantity: Int = 9,
    val paperSize: String = "4×6 inch",
    val cutLinesEnabled: Boolean = true,

    // Settings
    val language: AppLanguage = AppLanguage.VI,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val photoDpi: Int = 300,
    val outputFormat: String = "JPEG",
    val watermarkEnabled: Boolean = false,

    // Language sheet
    val showLanguageSheet: Boolean = false,

    // General
    val modnetAvailable: Boolean = false,
    val errorMessage: String? = null,
    val noFaceDetected: Boolean = false,
    val saveSuccess: Boolean = false,
    val savedUri: Uri? = null,
    val pipelineRunId: Int = 0,          // Incremented each pipeline run
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val pipeline = IDPhotoPipeline(application)
    private val modNetProcessor = MODNetProcessor(application)
    private val settingsStore = SettingsDataStore(application)

    init {
        _uiState.value = _uiState.value.copy(
            modnetAvailable = modNetProcessor.isModelAvailable()
        )

        // Load persisted settings on startup
        viewModelScope.launch {
            try {
                val saved = settingsStore.settingsFlow.first()
                _uiState.value = _uiState.value.copy(
                    language = saved.language,
                    themeMode = saved.themeMode,
                    photoDpi = saved.photoDpi,
                    outputFormat = saved.outputFormat,
                    watermarkEnabled = saved.watermarkEnabled,
                    paperSize = saved.paperSize,
                    cutLinesEnabled = saved.cutLinesEnabled,
                )
            } catch (_: Exception) {
                // Use defaults if DataStore fails
            }
        }
    }

    // ─── Language ──────────────────────────────────

    fun setLanguage(lang: AppLanguage) {
        _uiState.value = _uiState.value.copy(
            language = lang,
            showLanguageSheet = false,
        )
        viewModelScope.launch { settingsStore.setLanguage(lang) }
    }

    fun showLanguageSheet() {
        _uiState.value = _uiState.value.copy(showLanguageSheet = true)
    }

    fun hideLanguageSheet() {
        _uiState.value = _uiState.value.copy(showLanguageSheet = false)
    }

    // ─── Theme Mode ────────────────────────────────

    fun setThemeMode(mode: ThemeMode) {
        _uiState.value = _uiState.value.copy(themeMode = mode)
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    // ─── Size Selection ────────────────────────────

    fun onSizeSelected(index: Int) {
        val size = PhotoSizeManager.standardSizes.getOrElse(index) { PhotoSizeManager.standardSizes[1] }
        _uiState.value = _uiState.value.copy(
            selectedSizeIndex = index,
            selectedSize = size,
        )
    }

    fun onSizeSelected(size: PhotoSize) {
        val index = PhotoSizeManager.standardSizes.indexOf(size).coerceAtLeast(0)
        _uiState.value = _uiState.value.copy(
            selectedSizeIndex = index,
            selectedSize = size,
        )
    }

    // ─── Photo Input ───────────────────────────────

    fun onPhotoCaptured(bitmap: Bitmap) {
        startPipeline(bitmap)
    }

    fun onImageSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                    BitmapFactory.decodeStream(inputStream).also { inputStream?.close() }
                }
                if (bitmap != null) {
                    startPipeline(bitmap)
                } else {
                    _uiState.value = _uiState.value.copy(errorMessage = "Cannot read image")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Error: ${e.message}")
            }
        }
    }

    // ─── Pipeline ──────────────────────────────────

    private fun startPipeline(bitmap: Bitmap) {
        val strings = getStrings(_uiState.value.language)

        _uiState.value = _uiState.value.copy(
            originalBitmap = bitmap,
            processedBitmap = null,
            compositeBitmap = null,
            croppedBitmap = null,
            printLayoutBitmap = null,
            savedUri = null,
            errorMessage = null,
            noFaceDetected = false,
            isProcessing = true,
            pipelineError = null,
            pipelineMessage = strings.processing,
            pipelineRunId = _uiState.value.pipelineRunId + 1,
            pipelineSteps = listOf(
                PipelineStepUi(strings.pipelineFaceMesh, StepStatus.PENDING),
                PipelineStepUi(strings.pipelineAlignment, StepStatus.PENDING),
                PipelineStepUi(strings.pipelineQuality, StepStatus.PENDING),
                PipelineStepUi(strings.pipelineSegmentation, StepStatus.PENDING),
                PipelineStepUi(strings.pipelineExport, StepStatus.PENDING),
            ),
        )

        viewModelScope.launch {
            try {
                pipeline.process(bitmap, object : IDPhotoPipeline.PipelineCallback {
                    override fun onStepStarted(stepName: String, stepIndex: Int, totalSteps: Int) {
                        updatePipelineStep(stepIndex, StepStatus.RUNNING, stepName)
                    }

                    override fun onStepCompleted(stepName: String, stepIndex: Int, status: IDPhotoPipeline.StepStatus) {
                        val uiStatus = when (status) {
                            IDPhotoPipeline.StepStatus.SUCCESS -> StepStatus.DONE
                            IDPhotoPipeline.StepStatus.SKIPPED -> StepStatus.DONE
                            IDPhotoPipeline.StepStatus.FAILED -> StepStatus.ERROR
                            else -> StepStatus.DONE
                        }
                        updatePipelineStep(stepIndex, uiStatus, stepName)
                    }

                    override fun onPreviewReady(previewBitmap: Bitmap) {
                        // Show fast ML Kit preview immediately
                        _uiState.value = _uiState.value.copy(processedBitmap = previewBitmap)
                    }

                    override fun onComplete(result: IDPhotoPipeline.PipelineResult) {
                        // Mark final export step done
                        updatePipelineStep(4, StepStatus.DONE, "")

                        _uiState.value = _uiState.value.copy(
                            processedBitmap = result.segmentedBitmap ?: result.alignedBitmap,
                            isProcessing = false,
                        )

                        // Build composite
                        updateComposite()
                    }

                    override fun onError(error: Exception) {
                        val isNoFace = error is NoFaceDetectedException
                        val strings = getStrings(_uiState.value.language)
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            noFaceDetected = isNoFace,
                            pipelineError = if (isNoFace) strings.noFaceDetected else (error.message ?: "Unknown error"),
                            errorMessage = if (isNoFace) strings.noFaceMessage else error.message,
                        )
                    }
                })

            } catch (e: Exception) {
                val isNoFace = e is NoFaceDetectedException
                val strings = getStrings(_uiState.value.language)
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    noFaceDetected = isNoFace,
                    pipelineError = if (isNoFace) strings.noFaceDetected else (e.message ?: "Unknown error"),
                    errorMessage = if (isNoFace) strings.noFaceMessage else e.message,
                )
            }
        }
    }

    private fun updatePipelineStep(stepIndex: Int, status: StepStatus, message: String) {
        val steps = _uiState.value.pipelineSteps.toMutableList()
        if (stepIndex in steps.indices) {
            steps[stepIndex] = steps[stepIndex].copy(status = status)
        }
        _uiState.value = _uiState.value.copy(
            pipelineSteps = steps,
            pipelineMessage = if (message.isNotEmpty()) message else _uiState.value.pipelineMessage,
        )
    }

    fun retryPipeline() {
        val bitmap = _uiState.value.originalBitmap ?: return
        startPipeline(bitmap)
    }

    // ─── Background Selection ──────────────────────

    fun onBgColorSelected(color: Color) {
        _uiState.value = _uiState.value.copy(selectedBgColor = color)
        updateComposite()
    }

    fun onBgOptionSelected(index: Int) {
        val option = ImageUtils.backgroundOptions.getOrNull(index) ?: return
        val color = if (option.gradientColors != null) {
            // Use display color for Compose UI background
            Color(option.displayColor)
        } else {
            Color(option.displayColor)
        }
        _uiState.value = _uiState.value.copy(
            selectedBgColor = color,
            selectedBgIndex = index,
        )
        updateComposite()
    }

    // ─── Outfit ────────────────────────────────────

    fun onOutfitSelected(index: Int) {
        _uiState.value = _uiState.value.copy(selectedOutfitIndex = index)
        updateComposite()
    }

    // ─── Edit Tool Actions ─────────────────────────

    fun onEditToolClick(tool: EditTool) {
        when (tool) {
            EditTool.ROTATE -> {
                rotatePhoto()
                // Don't open a panel, just rotate immediately
            }
            EditTool.CHANGE_BG -> {
                toggleToolPanel(tool)
            }
            EditTool.BRIGHTNESS -> {
                toggleToolPanel(tool)
            }
        }
    }

    fun toggleToolPanel(tool: EditTool) {
        val current = _uiState.value.activeEditTool
        _uiState.value = _uiState.value.copy(
            activeEditTool = if (current == tool) null else tool
        )
    }

    fun closeToolPanel() {
        _uiState.value = _uiState.value.copy(activeEditTool = null)
    }

    private fun reRunSegmentation() {
        val bitmap = _uiState.value.originalBitmap ?: return
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isProcessing = true)
                pipeline.process(bitmap, object : IDPhotoPipeline.PipelineCallback {
                    override fun onStepStarted(stepName: String, stepIndex: Int, totalSteps: Int) {}
                    override fun onStepCompleted(stepName: String, stepIndex: Int, status: IDPhotoPipeline.StepStatus) {}
                    override fun onPreviewReady(previewBitmap: Bitmap) {
                        _uiState.value = _uiState.value.copy(processedBitmap = previewBitmap)
                    }
                    override fun onComplete(result: IDPhotoPipeline.PipelineResult) {
                        _uiState.value = _uiState.value.copy(
                            processedBitmap = result.segmentedBitmap ?: result.alignedBitmap,
                            isProcessing = false,
                        )
                        updateComposite()
                    }
                    override fun onError(error: Exception) {
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            errorMessage = "Lỗi xóa phông: ${error.message}",
                        )
                    }
                })
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = "Lỗi xóa phông: ${e.message}",
                )
            }
        }
    }

    fun onBrightnessLevelChanged(level: Float) {
        _uiState.value = _uiState.value.copy(brightnessLevel = level)
        applyEditEffectsDebounced()
    }

    /**
     * Unified debounced edit effects pipeline.
     * Brightness changes are debounced then rebuild composite.
     */
    private var editEffectsJob: Job? = null

    private fun applyEditEffectsDebounced() {
        editEffectsJob?.cancel()
        editEffectsJob = viewModelScope.launch {
            kotlinx.coroutines.delay(150)
            rebuildComposite()
        }
    }

    /**
     * Rebuild compositeBitmap from processedBitmap by applying:
     * 1. Brightness adjustment on foreground pixels
     * 2. Background compositing (color/gradient)
     * 3. Outfit overlay
     *
     * processedBitmap is NEVER mutated — it's the immutable segmented base.
     */
    private suspend fun rebuildComposite() {
        var foreground = _uiState.value.processedBitmap ?: return

        val brightness = _uiState.value.brightnessLevel

        // Step 1: Apply brightness (non-destructive, on copy)
        if (brightness != 0f) {
            val factor = 1f + brightness * 0.5f // -1→0.5x, 0→1.0x, 1→1.5x
            foreground = withContext(Dispatchers.Default) {
                adjustBitmapBrightness(foreground, factor)
            }
        }

        // Step 2: Composite on background
        val bgIndex = _uiState.value.selectedBgIndex
        val bgOption = ImageUtils.backgroundOptions.getOrNull(bgIndex)

        var composite = withContext(Dispatchers.Default) {
            if (bgOption != null && bgOption.gradientColors != null) {
                ImageUtils.compositeOnGradientBackground(foreground, bgOption)
            } else {
                val bgColor = _uiState.value.selectedBgColor
                val androidColor = android.graphics.Color.rgb(
                    (bgColor.red * 255).toInt(),
                    (bgColor.green * 255).toInt(),
                    (bgColor.blue * 255).toInt()
                )
                ImageUtils.compositeOnBackground(foreground, androidColor)
            }
        }

        // Step 3: Apply outfit overlay
        val outfitIndex = _uiState.value.selectedOutfitIndex
        if (outfitIndex > 0 && outfitIndex < OutfitOverlay.outfitOptions.size) {
            val outfit = OutfitOverlay.outfitOptions[outfitIndex]
            composite = withContext(Dispatchers.Default) {
                OutfitOverlay.applyOutfit(composite, null, outfit)
            }
        }

        _uiState.value = _uiState.value.copy(compositeBitmap = composite)
    }

    private fun adjustBitmapBrightness(bitmap: Bitmap, factor: Float): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val width = result.width
        val height = result.height
        val pixels = IntArray(width * height)
        result.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = (pixel shr 24) and 0xFF
            val r = (((pixel shr 16) and 0xFF) * factor).toInt().coerceIn(0, 255)
            val g = (((pixel shr 8) and 0xFF) * factor).toInt().coerceIn(0, 255)
            val b = ((pixel and 0xFF) * factor).toInt().coerceIn(0, 255)
            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    private fun cropToSelectedSize() {
        // Crop is only for preview — the actual crop is done at save/print time.
        // Just show a snackbar or visual feedback that crop will be applied on save.
        val bitmap = _uiState.value.compositeBitmap ?: _uiState.value.processedBitmap ?: return
        val size = _uiState.value.selectedSize
        viewModelScope.launch {
            val cropped = withContext(Dispatchers.Default) {
                PhotoSizeManager.cropToSize(bitmap, size)
            }
            // Only update croppedBitmap for preview, do NOT overwrite compositeBitmap
            _uiState.value = _uiState.value.copy(croppedBitmap = cropped)
        }
    }

    private fun rotatePhoto() {
        val bitmap = _uiState.value.processedBitmap ?: return
        viewModelScope.launch {
            val rotated = withContext(Dispatchers.Default) {
                val matrix = android.graphics.Matrix().apply { postRotate(90f) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
            _uiState.value = _uiState.value.copy(processedBitmap = rotated)
            updateComposite()
        }
    }

    // ─── Composite ─────────────────────────────────

    /**
     * Rebuild composite from processedBitmap with all current edit settings.
     * Called when background, outfit, or any parameter changes.
     * Uses the unified rebuildComposite pipeline.
     */
    private fun updateComposite() {
        viewModelScope.launch {
            rebuildComposite()
        }
    }

    // ─── Print ─────────────────────────────────────

    fun onPrintQuantityChange(qty: Int) {
        _uiState.value = _uiState.value.copy(printQuantity = qty.coerceIn(1, 20))
    }

    fun onPaperSizeChange(paperSize: String) {
        _uiState.value = _uiState.value.copy(paperSize = paperSize)
        viewModelScope.launch { settingsStore.setPaperSize(paperSize) }
        // Re-generate print layout with new paper size
        preparePrintLayout()
    }

    fun onCutLinesToggle(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(cutLinesEnabled = enabled)
        viewModelScope.launch { settingsStore.setCutLinesEnabled(enabled) }
    }

    fun preparePrintLayout() {
        // Use compositeBitmap (already has beauty + brightness + bg + outfit applied at original res)
        val composite = _uiState.value.compositeBitmap ?: return
        val size = _uiState.value.selectedSize

        viewModelScope.launch {
            val cropped = withContext(Dispatchers.Default) {
                // Crop to selected photo size — only at print/save time
                PhotoSizeManager.cropToSize(composite, size)
            }
            val layout = withContext(Dispatchers.Default) {
                PhotoSizeManager.createPrintLayout(cropped, size)
            }
            _uiState.value = _uiState.value.copy(
                croppedBitmap = cropped,
                printLayoutBitmap = layout,
            )
        }
    }

    // ─── Save ──────────────────────────────────────

    /**
     * Save photo with user's zoom/pan adjustments applied.
     *
     * Dùng processedBitmap (ảnh tách nền gốc, chưa qua beauty) làm foreground,
     * render lên background với đúng vị trí zoom/pan user đã chỉnh trên preview.
     *
     * renderFramedBitmap sẽ output ở resolution cao (dựa trên kích thước foreground gốc),
     * KHÔNG dùng resolution của preview frame (200dp×260dp).
     *
     * Flow: processedBitmap → brightness → renderFramedBitmap (high-res) → outfit → crop → save
     */
    fun savePhotoWithTransform(
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        frameWidth: Int,
        frameHeight: Int,
    ) {
        viewModelScope.launch {
            try {
                val size = _uiState.value.selectedSize
                var foreground = _uiState.value.processedBitmap ?: return@launch

                val bgIndex = _uiState.value.selectedBgIndex
                val bgOption = ImageUtils.backgroundOptions.getOrNull(bgIndex)
                val bgColor = _uiState.value.selectedBgColor
                val brightness = _uiState.value.brightnessLevel

                withContext(Dispatchers.Default) {
                    // Chỉ apply brightness (KHÔNG apply beauty — đã bỏ)
                    if (brightness != 0f) {
                        val factor = 1f + brightness * 0.5f
                        foreground = adjustBitmapBrightness(foreground, factor)
                    }

                    // Render foreground lên background với user transform
                    // Output ở resolution cao (tự tính từ foreground size, KHÔNG phải frame size)
                    val framed = if (bgOption != null && bgOption.gradientColors != null) {
                        ImageUtils.renderFramedBitmapWithOption(
                            foreground = foreground,
                            bgOption = bgOption,
                            scale = scale,
                            offsetX = offsetX,
                            offsetY = offsetY,
                            frameWidth = frameWidth,
                            frameHeight = frameHeight,
                        )
                    } else {
                        val androidBgColor = android.graphics.Color.rgb(
                            (bgColor.red * 255).toInt(),
                            (bgColor.green * 255).toInt(),
                            (bgColor.blue * 255).toInt()
                        )
                        ImageUtils.renderFramedBitmap(
                            foreground = foreground,
                            bgColor = androidBgColor,
                            scale = scale,
                            offsetX = offsetX,
                            offsetY = offsetY,
                            frameWidth = frameWidth,
                            frameHeight = frameHeight,
                        )
                    }

                    // Apply outfit if selected
                    var finalBitmap = framed
                    val outfitIndex = _uiState.value.selectedOutfitIndex
                    if (outfitIndex > 0 && outfitIndex < OutfitOverlay.outfitOptions.size) {
                        val outfit = OutfitOverlay.outfitOptions[outfitIndex]
                        finalBitmap = OutfitOverlay.applyOutfit(framed, null, outfit)
                    }

                    // Crop to selected photo size
                    val cropped = PhotoSizeManager.cropToSize(finalBitmap, size)

                    // Create print layout
                    val printLayout = PhotoSizeManager.createPrintLayout(cropped, size)

                    // Update state
                    _uiState.value = _uiState.value.copy(
                        croppedBitmap = cropped,
                        printLayoutBitmap = printLayout,
                    )

                    // Save to gallery
                    withContext(Dispatchers.IO) {
                        ImageUtils.saveToGallery(
                            getApplication(),
                            cropped,
                            "IDPhoto_single_${System.currentTimeMillis()}"
                        )
                        ImageUtils.saveToGallery(
                            getApplication(),
                            printLayout,
                            "IDPhoto_print_${System.currentTimeMillis()}"
                        )
                    }
                }

                _uiState.value = _uiState.value.copy(savedUri = Uri.EMPTY, saveSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Save error: ${e.message}")
            }
        }
    }

    fun savePhoto() {
        viewModelScope.launch {
            try {
                val croppedBitmap = _uiState.value.croppedBitmap ?: _uiState.value.compositeBitmap
                val printLayout = _uiState.value.printLayoutBitmap

                withContext(Dispatchers.IO) {
                    croppedBitmap?.let {
                        ImageUtils.saveToGallery(getApplication(), it, "IDPhoto_single_${System.currentTimeMillis()}")
                    }
                    printLayout?.let {
                        ImageUtils.saveToGallery(getApplication(), it, "IDPhoto_print_${System.currentTimeMillis()}")
                    }
                }

                _uiState.value = _uiState.value.copy(savedUri = Uri.EMPTY, saveSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Save error: ${e.message}")
            }
        }
    }

    fun downloadPrintLayout() {
        viewModelScope.launch {
            try {
                val bitmap = _uiState.value.printLayoutBitmap ?: return@launch
                val uri = withContext(Dispatchers.IO) {
                    ImageUtils.saveToGallery(getApplication(), bitmap, "IDPhoto_print_${System.currentTimeMillis()}")
                }
                _uiState.value = _uiState.value.copy(savedUri = uri, saveSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Error: ${e.message}")
            }
        }
    }

    // ─── Settings ──────────────────────────────────

    fun onWatermarkToggle(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(watermarkEnabled = enabled)
        viewModelScope.launch { settingsStore.setWatermarkEnabled(enabled) }
    }

    fun onPhotoDpiChange(dpi: Int) {
        _uiState.value = _uiState.value.copy(photoDpi = dpi)
        viewModelScope.launch { settingsStore.setPhotoDpi(dpi) }
    }

    fun onOutputFormatChange(format: String) {
        _uiState.value = _uiState.value.copy(outputFormat = format)
        viewModelScope.launch { settingsStore.setOutputFormat(format) }
    }

    // ─── Utility ───────────────────────────────────

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearNoFaceDetected() {
        _uiState.value = _uiState.value.copy(noFaceDetected = false, errorMessage = null, pipelineError = null)
    }

    fun clearSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }

    fun reset() {
        _uiState.value = AppUiState(
            modnetAvailable = modNetProcessor.isModelAvailable(),
            language = _uiState.value.language,
            themeMode = _uiState.value.themeMode,
            photoDpi = _uiState.value.photoDpi,
            outputFormat = _uiState.value.outputFormat,
            watermarkEnabled = _uiState.value.watermarkEnabled,
            paperSize = _uiState.value.paperSize,
            cutLinesEnabled = _uiState.value.cutLinesEnabled,
        )
        editEffectsJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        pipeline.close()
        modNetProcessor.close()
    }
}
