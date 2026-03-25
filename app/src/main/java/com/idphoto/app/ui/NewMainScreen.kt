package com.idphoto.app.ui

import android.Manifest
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FaceRetouchingOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.idphoto.app.ui.components.LanguageSheet
import com.idphoto.app.ui.screens.*
import com.idphoto.app.ui.theme.ThemeMode

/**
 * New MainScreen — Navigation Compose based with all routes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMainScreen(viewModel: AppViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.onImageSelected(it)
            navController.navigate(NavRoutes.PROCESSING)
        }
    }

    // Camera permission
    var pendingCameraAction by remember { mutableStateOf(false) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingCameraAction) {
            pendingCameraAction = false
            navController.navigate(NavRoutes.CAMERA)
        }
    }

    // Language bottom sheet
    val sheetState = rememberModalBottomSheetState()

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Save success feedback
    val strings = getStrings(uiState.language)
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar(
                message = strings.saveSuccessMessage,
                duration = SnackbarDuration.Short,
            )
            viewModel.clearSaveSuccess()
        }
    }

    // Provide localized strings
    CompositionLocalProvider(
        LocalStrings provides getStrings(uiState.language),
        LocalLanguage provides uiState.language,
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = NavRoutes.SPLASH,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // ── Splash ──
                    composable(NavRoutes.SPLASH) {
                        SplashScreen(
                            onNavigateToHome = {
                                navController.navigate(NavRoutes.HOME) {
                                    popUpTo(NavRoutes.SPLASH) { inclusive = true }
                                }
                            },
                        )
                    }

                    // ── Home ──
                    composable(NavRoutes.HOME) {
                        HomeScreen(
                            onNavigateToCamera = {
                                pendingCameraAction = true
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            onNavigateToGallery = {
                                imagePickerLauncher.launch("image/*")
                            },
                            onNavigateToSizes = {
                                navController.navigate(NavRoutes.SIZES)
                            },
                            onNavigateToSettings = {
                                navController.navigate(NavRoutes.SETTINGS)
                            },
                            onNavigateToSizeWithIndex = { index ->
                                viewModel.onSizeSelected(index)
                                pendingCameraAction = true
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            onShowLanguagePicker = {
                                viewModel.showLanguageSheet()
                            },
                        )
                    }

                    // ── Size Select ──
                    composable(NavRoutes.SIZES) {
                        SizeSelectScreen(
                            onBack = { navController.popBackStack() },
                            onSizeSelected = { size ->
                                viewModel.onSizeSelected(size)
                                pendingCameraAction = true
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                        )
                    }

                    // ── Camera ──
                    composable(NavRoutes.CAMERA) {
                        CameraScreen(
                            selectedSizeName = uiState.selectedSize.name,
                            onPhotoCaptured = { bitmap ->
                                viewModel.onPhotoCaptured(bitmap)
                                navController.navigate(NavRoutes.PROCESSING) {
                                    popUpTo(NavRoutes.CAMERA) { inclusive = true }
                                }
                            },
                            onDismiss = { navController.popBackStack() },
                            onChangeSizeClick = {
                                navController.navigate(NavRoutes.SIZES)
                            },
                            onGalleryClick = {
                                imagePickerLauncher.launch("image/*")
                            },
                        )
                    }

                    // ── Processing ──
                    composable(NavRoutes.PROCESSING) {
                        val strings = LocalStrings.current

                        // Only navigate after observing a true→false transition of isProcessing
                        var hasSeenProcessing by remember { mutableStateOf(false) }

                        LaunchedEffect(uiState.isProcessing) {
                            if (uiState.isProcessing) {
                                hasSeenProcessing = true
                            }
                            if (hasSeenProcessing && !uiState.isProcessing && uiState.processedBitmap != null) {
                                navController.navigate(NavRoutes.EDIT) {
                                    popUpTo(NavRoutes.PROCESSING) { inclusive = true }
                                }
                            }
                        }

                        // No face detected dialog
                        if (uiState.noFaceDetected) {
                            AlertDialog(
                                onDismissRequest = {
                                    viewModel.clearNoFaceDetected()
                                    navController.popBackStack(NavRoutes.HOME, false)
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.FaceRetouchingOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(48.dp),
                                    )
                                },
                                title = {
                                    Text(strings.noFaceDetected)
                                },
                                text = {
                                    Text(strings.noFaceMessage)
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.clearNoFaceDetected()
                                        // Go back to camera to retake
                                        navController.navigate(NavRoutes.CAMERA) {
                                            popUpTo(NavRoutes.HOME) { inclusive = false }
                                        }
                                    }) {
                                        Text(strings.retry)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = {
                                        viewModel.clearNoFaceDetected()
                                        navController.popBackStack(NavRoutes.HOME, false)
                                    }) {
                                        Text(strings.cancel)
                                    }
                                },
                            )
                        }

                        ProcessingScreen(
                            steps = uiState.pipelineSteps,
                            currentMessage = uiState.pipelineMessage,
                            errorMessage = uiState.pipelineError,
                            onRetry = { viewModel.retryPipeline() },
                            onCancel = {
                                navController.popBackStack(NavRoutes.HOME, false)
                            },
                        )
                    }

                    // ── Edit ──
                    composable(NavRoutes.EDIT) {
                        EditScreen(
                            photo = uiState.compositeBitmap ?: uiState.processedBitmap,
                            selectedBgColor = uiState.selectedBgColor,
                            selectedBgIndex = uiState.selectedBgIndex,
                            activeEditTool = uiState.activeEditTool,
                            brightnessLevel = uiState.brightnessLevel,
                            pipelineRunId = uiState.pipelineRunId,
                            selectedSize = uiState.selectedSize,
                            onBack = {
                                viewModel.closeToolPanel()
                                viewModel.reset()
                                navController.popBackStack(NavRoutes.HOME, false)
                            },
                            onSave = { scale, offsetX, offsetY, frameW, frameH, saveOptions ->
                                viewModel.savePhotoWithTransform(scale, offsetX, offsetY, frameW, frameH, saveOptions)
                            },
                            onBgColorSelected = { color ->
                                viewModel.onBgColorSelected(color)
                            },
                            onBgOptionSelected = { index ->
                                viewModel.onBgOptionSelected(index)
                            },
                            onToolClick = { tool ->
                                viewModel.onEditToolClick(tool)
                            },
                            onBrightnessLevelChanged = { level ->
                                viewModel.onBrightnessLevelChanged(level)
                            },
                            onCloseToolPanel = {
                                viewModel.closeToolPanel()
                            },
                            onNavigateToPrint = {
                                viewModel.closeToolPanel()
                                viewModel.preparePrintLayout()
                                navController.navigate(NavRoutes.PRINT)
                            },
                        )
                    }

                    // ── Print ──
                    composable(NavRoutes.PRINT) {
                        PrintScreen(
                            photo = uiState.croppedBitmap ?: uiState.compositeBitmap,
                            quantity = uiState.printQuantity,
                            paperSize = uiState.paperSize,
                            cutLinesEnabled = uiState.cutLinesEnabled,
                            onBack = { navController.popBackStack() },
                            onQuantityChange = { viewModel.onPrintQuantityChange(it) },
                            onPaperSizeClick = {
                                // Cycle through paper sizes
                                val sizes = listOf("4×6 inch", "5×7 inch", "A4", "A5")
                                val current = sizes.indexOf(uiState.paperSize)
                                val next = (current + 1) % sizes.size
                                viewModel.onPaperSizeChange(sizes[next])
                            },
                            onCutLinesToggle = { viewModel.onCutLinesToggle(it) },
                            onDownload = { viewModel.downloadPrintLayout() },
                            onPrint = { viewModel.savePhoto() },
                        )
                    }

                    // ── Settings ──
                    composable(NavRoutes.SETTINGS) {
                        SettingsScreen(
                            themeMode = uiState.themeMode,
                            photoDpi = uiState.photoDpi,
                            outputFormat = uiState.outputFormat,
                            watermarkEnabled = uiState.watermarkEnabled,
                            onBack = { navController.popBackStack() },
                            onThemeModeChange = { viewModel.setThemeMode(it) },
                            onLanguageClick = { viewModel.showLanguageSheet() },
                            onPhotoDpiChange = { viewModel.onPhotoDpiChange(it) },
                            onOutputFormatChange = { viewModel.onOutputFormatChange(it) },
                            onWatermarkToggle = { viewModel.onWatermarkToggle(it) },
                        )
                    }
                }

                // Language Sheet overlay
                if (uiState.showLanguageSheet) {
                    LanguageSheet(
                        sheetState = sheetState,
                        currentLanguage = uiState.language,
                        onLanguageSelected = { lang -> viewModel.setLanguage(lang) },
                        onDismiss = { viewModel.hideLanguageSheet() },
                    )
                }
            }
        }
    }
}
