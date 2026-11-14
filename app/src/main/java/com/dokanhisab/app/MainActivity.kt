package com.dokanhisab.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import com.dokanhisab.app.ui.theme.LocalSemanticColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dokanhisab.app.data.local.BackupFileInfo
import com.dokanhisab.app.data.local.DatabaseBackupManager
import com.dokanhisab.app.data.local.DokanDatabase
import com.dokanhisab.app.data.local.RestoreResult
import com.dokanhisab.app.data.local.TransactionEntity
import com.dokanhisab.app.data.local.TransactionType
import com.dokanhisab.app.data.preferences.UserPreferencesRepository
import com.dokanhisab.app.data.repository.HisabRepository
import com.dokanhisab.app.domain.HisabCalculator
import com.dokanhisab.app.domain.MonthlyPdfReportGenerator
import com.dokanhisab.app.localization.LocalizationProvider
import com.dokanhisab.app.security.BiometricAuthHelper
import com.dokanhisab.app.security.BiometricCapability
import com.dokanhisab.app.ui.components.AddEditTransactionSheet
import com.dokanhisab.app.ui.components.DashboardShimmerSkeleton
import com.dokanhisab.app.ui.components.ExportLedgerDialog
import com.dokanhisab.app.ui.screens.AssistantScreen
import com.dokanhisab.app.ui.screens.BiometricLockScreen
import com.dokanhisab.app.ui.screens.DashboardScreen
import com.dokanhisab.app.ui.screens.LedgerScreen
import com.dokanhisab.app.ui.screens.SettingsScreen
import com.dokanhisab.app.ui.theme.DokanHisabTheme
import com.dokanhisab.app.ui.viewmodel.AssistantViewModel
import com.dokanhisab.app.ui.viewmodel.ExportFormat
import com.dokanhisab.app.ui.viewmodel.HisabUiState
import com.dokanhisab.app.ui.viewmodel.HisabViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_LEDGER = "ledger"
const val ROUTE_ASSISTANT = "assistant"
const val ROUTE_SETTINGS = "settings"

private data class NavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = DokanDatabase.getInstance(applicationContext)
        val repository = HisabRepository(
            transactionDao = db.transactionDao(),
            categoryDao = db.categoryDao()
        )
        val prefsRepository = UserPreferencesRepository(applicationContext)

        setContent {
            val viewModel: HisabViewModel = viewModel(
                factory = HisabViewModel.Factory(repository, prefsRepository)
            )
            val assistantViewModel: AssistantViewModel = viewModel(
                factory = AssistantViewModel.Factory(
                    context = applicationContext,
                    chatMessageDao = db.chatMessageDao(),
                    preferencesRepository = prefsRepository
                )
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            var isUnlocked by remember { mutableStateOf(false) }
            var suppressNextBackgroundLock by remember { mutableStateOf(false) }
            var isPromptActive by remember { mutableStateOf(false) }
            var lockErrorMessage by remember { mutableStateOf<String?>(null) }
            var biometricCapability by remember {
                mutableStateOf(BiometricAuthHelper.checkCapability(this@MainActivity))
            }

            val isReady = uiState is HisabUiState.Ready
            Crossfade(
                targetState = isReady,
                animationSpec = tween(durationMillis = 240),
                label = "root_state_crossfade"
            ) { ready ->
                if (!ready) {
                    DokanHisabTheme {
                        DashboardShimmerSkeleton(
                            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                        )
                    }
                } else {
                    val state = uiState as? HisabUiState.Ready ?: return@Crossfade
                    val strings = LocalizationProvider.forLanguage(state.preferences.language)
                    val lifecycleOwner = LocalLifecycleOwner.current
                    val latestPrefs by rememberUpdatedState(state.preferences)

                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            when (event) {
                                Lifecycle.Event.ON_RESUME -> {
                                    suppressNextBackgroundLock = false
                                    biometricCapability = BiometricAuthHelper.checkCapability(this@MainActivity)
                                }
                                Lifecycle.Event.ON_STOP -> {
                                    if (latestPrefs.biometricLockEnabled &&
                                        latestPrefs.lockOnBackground &&
                                        !suppressNextBackgroundLock &&
                                        !isPromptActive
                                    ) {
                                        isUnlocked = false
                                        lockErrorMessage = null
                                    }
                                }
                                else -> Unit
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }

                    fun launchUnlockPrompt() {
                        if (isPromptActive) return
                        isPromptActive = true
                        lockErrorMessage = null
                        BiometricAuthHelper.authenticate(
                            activity = this@MainActivity,
                            title = strings.biometricPromptTitle,
                            subtitle = strings.biometricPromptSubtitle,
                            description = state.preferences.shopName.ifBlank { strings.appName },
                            cancelText = strings.biometricCancel,
                            onSuccess = {
                                isPromptActive = false
                                isUnlocked = true
                                lockErrorMessage = null
                            },
                            onError = { code, message ->
                                isPromptActive = false
                                val isUserDismiss = code == BiometricPrompt.ERROR_USER_CANCELED ||
                                    code == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                                    code == BiometricPrompt.ERROR_CANCELED
                                if (!isUserDismiss && message.isNotBlank()) {
                                    lockErrorMessage = message
                                }
                            },
                            onFailed = {
                                lockErrorMessage = strings.biometricAuthFailed
                            }
                        )
                    }

                    val showLockGate = state.preferences.biometricLockEnabled && !isUnlocked

                    LaunchedEffect(showLockGate) {
                        if (showLockGate) {
                            launchUnlockPrompt()
                        }
                    }

                    DokanHisabTheme(
                        palette = state.preferences.palette,
                        themeMode = state.preferences.themeMode,
                        language = state.preferences.language
                    ) {
                        if (showLockGate) {
                            BiometricLockScreen(
                                preferences = state.preferences,
                                strings = strings,
                                errorMessage = lockErrorMessage,
                                onUnlockClick = ::launchUnlockPrompt
                            )
                        } else {
                            DokanHisabApp(
                                state = state,
                                viewModel = viewModel,
                                assistantViewModel = assistantViewModel,
                                activity = this@MainActivity,
                                biometricCapability = biometricCapability,
                                onSuppressNextBackgroundLock = { suppressNextBackgroundLock = true },
                                onSetBiometricPromptActive = { active -> isPromptActive = active },
                                onMarkUnlocked = { isUnlocked = true },
                                onLockNow = {
                                    lockErrorMessage = null
                                    isUnlocked = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DokanHisabApp(
    state: HisabUiState.Ready,
    viewModel: HisabViewModel,
    assistantViewModel: AssistantViewModel,
    activity: FragmentActivity,
    biometricCapability: BiometricCapability,
    onSuppressNextBackgroundLock: () -> Unit,
    onSetBiometricPromptActive: (Boolean) -> Unit,
    onMarkUnlocked: () -> Unit,
    onLockNow: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_DASHBOARD

    val strings = LocalizationProvider.forLanguage(state.preferences.language)

    var showEntrySheet by remember { mutableStateOf(false) }
    var entryInitialType by remember { mutableStateOf(TransactionType.INCOME) }
    var entryInitialTitle by remember { mutableStateOf("") }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportInitialFormat by remember { mutableStateOf(ExportFormat.CSV) }
    var pendingExportContent by remember { mutableStateOf<String?>(null) }
    var pendingPdfYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var pendingPdfMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var folderBackups by remember { mutableStateOf<List<BackupFileInfo>>(emptyList()) }

    val database = remember(context) { DokanDatabase.getInstance(context.applicationContext) }

    val chatMessages by assistantViewModel.messages.collectAsStateWithLifecycle()
    val chatState by assistantViewModel.chatState.collectAsStateWithLifecycle()
    val transcriptionState by assistantViewModel.transcriptionState.collectAsStateWithLifecycle()

    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        val content = pendingExportContent
        pendingExportContent = null
        if (uri != null && content != null) {
            val ok = writeTextToUri(context, uri, content)
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (ok) strings.exportSuccess else strings.exportError
                )
            }
        }
    }

    val saveExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.ms-excel")
    ) { uri: Uri? ->
        val content = pendingExportContent
        pendingExportContent = null
        if (uri != null && content != null) {
            val ok = writeTextToUri(context, uri, content)
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (ok) strings.exportSuccess else strings.exportError
                )
            }
        }
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            MonthlyPdfReportGenerator.generateMonthlyReport(
                                context = context,
                                year = pendingPdfYear,
                                monthZeroBased = pendingPdfMonth,
                                allTransactions = state.allTransactions,
                                preferences = state.preferences,
                                strings = strings,
                                outputStream = out
                            )
                        } ?: false
                    }.getOrDefault(false)
                }
                snackbarHostState.showSnackbar(
                    if (ok) strings.exportSuccess else strings.exportError
                )
            }
        }
    }

    val backupDbLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val ok = DatabaseBackupManager.backupDatabaseToUri(context, database, uri)
                if (ok) {
                    viewModel.setLastBackupTimestamp(System.currentTimeMillis())
                }
                snackbarHostState.showSnackbar(
                    if (ok) strings.backupSavedSuccess else strings.backupFailedError
                )
            }
        }
    }

    fun performRestoreFromUri(uri: Uri) {
        scope.launch {
            when (val res = DatabaseBackupManager.restoreDatabaseFromUri(context, database, uri)) {
                is RestoreResult.Success -> {
                    val countText = HisabCalculator.formatCount(
                        res.restoredTransactions,
                        state.preferences.language,
                        state.preferences.useBanglaDigits
                    )
                    snackbarHostState.showSnackbar(
                        "${strings.restoreSuccessPrefix} ($countText ${strings.entriesCount})"
                    )
                }
                is RestoreResult.Error -> {
                    snackbarHostState.showSnackbar("${strings.restoreFailedError}: ${res.message}")
                }
            }
        }
    }

    val restoreDbLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            performRestoreFromUri(uri)
        }
    }

    val bannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = copyMediaToInternalStorage(
                context = context,
                sourceUri = uri,
                prefix = "shop_banner",
                previousPath = state.preferences.bannerImagePath
            )
            if (savedPath != null) {
                viewModel.setBannerImagePath(savedPath)
                scope.launch { snackbarHostState.showSnackbar(strings.photoUpdated) }
            }
        }
    }

    val profilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = copyMediaToInternalStorage(
                context = context,
                sourceUri = uri,
                prefix = "shop_profile",
                previousPath = state.preferences.profileImagePath
            )
            if (savedPath != null) {
                viewModel.setProfileImagePath(savedPath)
                scope.launch { snackbarHostState.showSnackbar(strings.photoUpdated) }
            }
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(treeUri, flags)
            }
            val label = extractTreeFolderLabel(treeUri)
            viewModel.setSaveDirectory(treeUri.toString(), label)
            scope.launch { snackbarHostState.showSnackbar(strings.saveFolderUpdated) }
        }
    }

    val destinations = listOf(
        NavDestination(
            route = ROUTE_DASHBOARD,
            label = strings.navDashboard,
            selectedIcon = Icons.Rounded.Home,
            unselectedIcon = Icons.Outlined.Home,
            testTag = "nav_dashboard"
        ),
        NavDestination(
            route = ROUTE_LEDGER,
            label = strings.navLedger,
            selectedIcon = Icons.Rounded.TableChart,
            unselectedIcon = Icons.Outlined.TableChart,
            testTag = "nav_ledger"
        ),
        NavDestination(
            route = ROUTE_ASSISTANT,
            label = strings.navAssistant,
            selectedIcon = Icons.Rounded.Forum,
            unselectedIcon = Icons.Outlined.Forum,
            testTag = "nav_assistant"
        ),
        NavDestination(
            route = ROUTE_SETTINGS,
            label = strings.navSettings,
            selectedIcon = Icons.Rounded.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            testTag = "nav_settings"
        )
    )

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openAddSheet(type: TransactionType, initialTitle: String = "") {
        editingTransaction = null
        entryInitialType = type
        entryInitialTitle = initialTitle
        showEntrySheet = true
    }

    fun openEditSheet(tx: TransactionEntity) {
        editingTransaction = tx
        entryInitialType = tx.type
        entryInitialTitle = tx.title
        showEntrySheet = true
    }

    fun handleDelete(tx: TransactionEntity) {
        viewModel.deleteTransaction(tx)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = strings.deletedUndoMessage,
                actionLabel = strings.undo,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoLastDelete()
            }
        }
    }

    fun handleExportSave(format: ExportFormat, year: Int, monthZeroBased: Int) {
        if (format == ExportFormat.PDF) {
            val monthTx = HisabCalculator.filterTransactionsForMonth(
                state.allTransactions,
                year,
                monthZeroBased
            )
            if (monthTx.isEmpty()) {
                scope.launch { snackbarHostState.showSnackbar(strings.pdfEmptyForMonth) }
                return
            }
            val fileName = "DokanHisab_Report_${year}_${String.format(Locale.US, "%02d", monthZeroBased + 1)}.pdf"
            val savedFolderUri = state.preferences.saveDirectoryUri
            if (savedFolderUri.isNotBlank()) {
                scope.launch {
                    val savedDirectly = withContext(Dispatchers.IO) {
                        writePdfToTreeDirectory(
                            context = context,
                            treeUri = Uri.parse(savedFolderUri),
                            fileName = fileName,
                            year = year,
                            monthZeroBased = monthZeroBased,
                            allTransactions = state.allTransactions,
                            preferences = state.preferences,
                            strings = strings
                        )
                    }
                    if (savedDirectly) {
                        snackbarHostState.showSnackbar("${strings.exportSuccess}: $fileName")
                    } else {
                        pendingPdfYear = year
                        pendingPdfMonth = monthZeroBased
                        onSuppressNextBackgroundLock()
                        savePdfLauncher.launch(fileName)
                    }
                }
                return
            }

            pendingPdfYear = year
            pendingPdfMonth = monthZeroBased
            onSuppressNextBackgroundLock()
            savePdfLauncher.launch(fileName)
            return
        }

        val payload = viewModel.buildExportPayload(format)
        if (payload == null) {
            scope.launch { snackbarHostState.showSnackbar(strings.exportEmpty) }
            return
        }
        val dateStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val fileName = "DokanHisab_$dateStamp.${format.extension}"

        val savedFolderUri = state.preferences.saveDirectoryUri
        if (savedFolderUri.isNotBlank()) {
            val savedDirectly = writeToTreeDirectory(
                context = context,
                treeUri = Uri.parse(savedFolderUri),
                fileName = fileName,
                mimeType = format.mimeType,
                content = payload
            )
            if (savedDirectly) {
                scope.launch {
                    snackbarHostState.showSnackbar("${strings.exportSuccess}: $fileName")
                }
                return
            }
        }

        pendingExportContent = payload
        onSuppressNextBackgroundLock()
        when (format) {
            ExportFormat.CSV -> saveCsvLauncher.launch(fileName)
            ExportFormat.EXCEL_XML -> saveExcelLauncher.launch(fileName)
            ExportFormat.PDF -> Unit
        }
    }

    fun handleExportShare(format: ExportFormat, year: Int, monthZeroBased: Int) {
        if (format == ExportFormat.PDF) {
            val monthTx = HisabCalculator.filterTransactionsForMonth(
                state.allTransactions,
                year,
                monthZeroBased
            )
            if (monthTx.isEmpty()) {
                scope.launch { snackbarHostState.showSnackbar(strings.pdfEmptyForMonth) }
                return
            }
            scope.launch {
                onSuppressNextBackgroundLock()
                val ok = shareMonthlyPdfReport(
                    context = context,
                    year = year,
                    monthZeroBased = monthZeroBased,
                    allTransactions = state.allTransactions,
                    preferences = state.preferences,
                    strings = strings
                )
                if (!ok) {
                    snackbarHostState.showSnackbar(strings.exportError)
                }
            }
            return
        }

        val payload = viewModel.buildExportPayload(format)
        if (payload == null) {
            scope.launch { snackbarHostState.showSnackbar(strings.exportEmpty) }
            return
        }
        onSuppressNextBackgroundLock()
        val ok = shareLedgerFile(context, payload, format, strings.exportTitle)
        if (!ok) {
            scope.launch { snackbarHostState.showSnackbar(strings.exportError) }
        }
    }

    fun refreshFolderBackups() {
        val folderUriStr = state.preferences.saveDirectoryUri
        if (folderUriStr.isBlank()) {
            folderBackups = emptyList()
            return
        }
        scope.launch {
            folderBackups = DatabaseBackupManager.listBackupsInDirectory(
                context = context,
                treeUri = Uri.parse(folderUriStr)
            )
        }
    }

    fun handleBackupDatabase() {
        val dateStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val fileName = "DokanHisab_Backup_$dateStamp.db"
        val savedFolderUri = state.preferences.saveDirectoryUri
        if (savedFolderUri.isNotBlank()) {
            scope.launch {
                val ok = DatabaseBackupManager.backupDatabaseToDirectory(
                    context = context,
                    database = database,
                    treeUri = Uri.parse(savedFolderUri),
                    fileName = fileName
                )
                if (ok) {
                    viewModel.setLastBackupTimestamp(System.currentTimeMillis())
                    refreshFolderBackups()
                    snackbarHostState.showSnackbar("${strings.backupSavedSuccess}: $fileName")
                } else {
                    onSuppressNextBackgroundLock()
                    backupDbLauncher.launch(fileName)
                }
            }
        } else {
            onSuppressNextBackgroundLock()
            backupDbLauncher.launch(fileName)
        }
    }

    val semantic = LocalSemanticColors.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    Column {
                        HorizontalDivider(thickness = 1.dp, color = semantic.navBorder)
                        NavigationBar(
                            containerColor = semantic.navBarBg
                        ) {
                            destinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(dest.route) },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = semantic.navIndicator,
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = dest.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = semantic.navBarBg
                    ) {
                        destinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                colors = NavigationRailItemDefaults.colors(
                                    indicatorColor = semantic.navIndicator,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, maxLines = 1) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_DASHBOARD,
                    enterTransition = { fadeIn(animationSpec = tween(180)) },
                    exitTransition = { fadeOut(animationSpec = tween(140)) },
                    popEnterTransition = { fadeIn(animationSpec = tween(180)) },
                    popExitTransition = { fadeOut(animationSpec = tween(140)) },
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_DASHBOARD) {
                        DashboardScreen(
                            state = state,
                            strings = strings,
                            onSelectPeriod = viewModel::setDashboardPeriod,
                            onAddTransaction = { type -> openAddSheet(type) },
                            onEditTransaction = ::openEditSheet,
                            onOpenLedger = { navigateTo(ROUTE_LEDGER) },
                            onOpenMonthlyPdf = {
                                exportInitialFormat = ExportFormat.PDF
                                showExportDialog = true
                            },
                            onToggleLanguage = viewModel::setLanguage
                        )
                    }
                    composable(ROUTE_LEDGER) {
                        LedgerScreen(
                            state = state,
                            strings = strings,
                            onPeriodSelected = viewModel::setLedgerPeriod,
                            onTypeFilterSelected = viewModel::setLedgerTypeFilter,
                            onSearchQueryChange = viewModel::setSearchQuery,
                            onRowClick = ::openEditSheet,
                            onAddClick = { openAddSheet(TransactionType.INCOME) },
                            onExportClick = {
                                exportInitialFormat = ExportFormat.CSV
                                showExportDialog = true
                            }
                        )
                    }
                    composable(ROUTE_ASSISTANT) {
                        AssistantScreen(
                            hisabState = state,
                            messages = chatMessages,
                            chatState = chatState,
                            transcriptionState = transcriptionState,
                            strings = strings,
                            onSelectProvider = assistantViewModel::selectProvider,
                            onSelectModel = assistantViewModel::selectModel,
                            onSelectOpenRouterModel = assistantViewModel::selectOpenRouterModel,
                            onSelectRole = assistantViewModel::selectRole,
                            onSaveCustomInstruction = assistantViewModel::saveCustomSystemInstruction,
                            onSendMessage = { message ->
                                assistantViewModel.sendChatMessage(
                                    userText = message,
                                    preferences = state.preferences,
                                    ledgerSummaryContext = AssistantViewModel.buildLedgerSnapshot(state)
                                )
                            },
                            onClearChat = assistantViewModel::clearChatHistory,
                            onDismissChatError = assistantViewModel::dismissChatError,
                            onStartRecording = assistantViewModel::startRecording,
                            onStopAndTranscribe = {
                                assistantViewModel.stopAndTranscribe(state.preferences)
                            },
                            onResetTranscription = assistantViewModel::cancelRecordingOrResetTranscription,
                            onAddTranscribedToLedger = { transcribedText ->
                                openAddSheet(TransactionType.INCOME, initialTitle = transcribedText)
                            },
                            onOpenSettings = { navigateTo(ROUTE_SETTINGS) },
                            onShowMessage = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        )
                    }
                    composable(ROUTE_SETTINGS) {
                        SettingsScreen(
                            state = state,
                            strings = strings,
                            folderBackups = folderBackups,
                            onLanguageChange = viewModel::setLanguage,
                            onBanglaDigitsChange = viewModel::setUseBanglaDigits,
                            onThemeModeChange = viewModel::setThemeMode,
                            onPaletteChange = viewModel::setPalette,
                            onCustomAppNameSave = { appName ->
                                viewModel.setCustomAppName(appName)
                                scope.launch { snackbarHostState.showSnackbar(strings.customAppNameSaved) }
                            },
                            onShopNameSave = { name ->
                                viewModel.setShopName(name)
                                scope.launch { snackbarHostState.showSnackbar(strings.shopNameSaved) }
                            },
                            onAiProviderChange = viewModel::setAiProvider,
                            onGeminiApiKeySave = { key ->
                                viewModel.setGeminiApiKey(key)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (key.isNotBlank()) strings.apiKeySaved else strings.apiKeyCleared
                                    )
                                }
                            },
                            onOpenRouterApiKeySave = { key ->
                                viewModel.setOpenRouterApiKey(key)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (key.isNotBlank()) strings.openRouterKeySaved else strings.openRouterKeyCleared
                                    )
                                }
                            },
                            onOpenRouterModelSave = viewModel::setOpenRouterModel,
                            onPickBannerPhoto = {
                                onSuppressNextBackgroundLock()
                                bannerPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onRemoveBannerPhoto = {
                                deleteFileQuietly(state.preferences.bannerImagePath)
                                viewModel.setBannerImagePath("")
                            },
                            onPickProfilePhoto = {
                                onSuppressNextBackgroundLock()
                                profilePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onRemoveProfilePhoto = {
                                deleteFileQuietly(state.preferences.profileImagePath)
                                viewModel.setProfileImagePath("")
                            },
                            onSelectSaveFolder = {
                                onSuppressNextBackgroundLock()
                                folderPickerLauncher.launch(null)
                            },
                            onClearSaveFolder = {
                                viewModel.clearSaveDirectory()
                                folderBackups = emptyList()
                                scope.launch { snackbarHostState.showSnackbar(strings.saveFolderCleared) }
                            },
                            onBackupDatabase = ::handleBackupDatabase,
                            onRefreshFolderBackups = ::refreshFolderBackups,
                            onRestoreBackupUri = ::performRestoreFromUri,
                            onBrowseRestoreFile = {
                                onSuppressNextBackgroundLock()
                                restoreDbLauncher.launch(arrayOf("*/*"))
                            },
                            onOpenExport = {
                                exportInitialFormat = ExportFormat.PDF
                                showExportDialog = true
                            },
                            onClearAllData = {
                                viewModel.clearAllTransactions()
                                scope.launch { snackbarHostState.showSnackbar(strings.clearedMessage) }
                            },
                            biometricCapability = biometricCapability,
                            onToggleBiometricLock = { wantEnabled ->
                                val cap = BiometricAuthHelper.checkCapability(activity)
                                if (wantEnabled && cap == BiometricCapability.NONE_ENROLLED) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(strings.biometricEnrollRequiredToast)
                                    }
                                } else if (wantEnabled && cap != BiometricCapability.READY) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(strings.biometricUnavailableBadge)
                                    }
                                } else {
                                    onSetBiometricPromptActive(true)
                                    BiometricAuthHelper.authenticate(
                                        activity = activity,
                                        title = strings.biometricPromptTitle,
                                        subtitle = if (wantEnabled) {
                                            strings.biometricPromptEnableSubtitle
                                        } else {
                                            strings.biometricPromptDisableSubtitle
                                        },
                                        description = state.preferences.shopName.ifBlank { strings.appName },
                                        cancelText = strings.biometricCancel,
                                        onSuccess = {
                                            onSetBiometricPromptActive(false)
                                            onMarkUnlocked()
                                            viewModel.setBiometricLockEnabled(wantEnabled)
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    if (wantEnabled) strings.biometricEnabledToast else strings.biometricDisabledToast
                                                )
                                            }
                                        },
                                        onError = { code, msg ->
                                            onSetBiometricPromptActive(false)
                                            val isUserDismiss = code == BiometricPrompt.ERROR_USER_CANCELED ||
                                                code == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                                                code == BiometricPrompt.ERROR_CANCELED
                                            if (!isUserDismiss && msg.isNotBlank()) {
                                                scope.launch { snackbarHostState.showSnackbar(msg) }
                                            }
                                        },
                                        onFailed = {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(strings.biometricAuthFailed)
                                            }
                                        }
                                    )
                                }
                            },
                            onToggleLockOnBackground = viewModel::setLockOnBackground,
                            onOpenSecuritySettings = {
                                onSuppressNextBackgroundLock()
                                runCatching {
                                    activity.startActivity(BiometricAuthHelper.buildEnrollIntent())
                                }.onFailure {
                                    runCatching {
                                        activity.startActivity(
                                            Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
                                        )
                                    }
                                }
                            },
                            onLockAppNow = onLockNow
                        )
                    }
                }
            }
        }
    }

    if (showEntrySheet) {
        AddEditTransactionSheet(
            initialType = entryInitialType,
            editingTransaction = editingTransaction,
            initialTitle = entryInitialTitle,
            categories = state.categories,
            preferences = state.preferences,
            strings = strings,
            onDismiss = { showEntrySheet = false },
            onSave = { title, amount, type, categoryId, paymentMethod, partyName, note, timestamp ->
                viewModel.saveTransaction(
                    editingTransaction = editingTransaction,
                    title = title,
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    paymentMethod = paymentMethod,
                    partyName = partyName,
                    note = note,
                    timestamp = timestamp
                )
            },
            onDelete = ::handleDelete,
            recentTransactions = state.allTransactions
        )
    }

    if (showExportDialog) {
        ExportLedgerDialog(
            strings = strings,
            preferences = state.preferences,
            initialFormat = exportInitialFormat,
            onDismiss = { showExportDialog = false },
            onSaveToDevice = ::handleExportSave,
            onShareFile = ::handleExportShare
        )
    }
}

private fun copyMediaToInternalStorage(
    context: Context,
    sourceUri: Uri,
    prefix: String,
    previousPath: String
): String? {
    return runCatching {
        val mediaDir = File(context.filesDir, "media").apply { mkdirs() }
        val targetFile = File(mediaDir, "${prefix}_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return null
        if (previousPath.isNotBlank() && previousPath != targetFile.absolutePath) {
            deleteFileQuietly(previousPath)
        }
        targetFile.absolutePath
    }.getOrNull()
}

private fun deleteFileQuietly(path: String) {
    if (path.isNotBlank()) {
        runCatching { File(path).delete() }
    }
}

private fun extractTreeFolderLabel(treeUri: Uri): String {
    val docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
    if (!docId.isNullOrBlank()) {
        return docId.replace("primary:", "Internal Storage/")
    }
    return treeUri.lastPathSegment ?: treeUri.toString()
}

private fun writeToTreeDirectory(
    context: Context,
    treeUri: Uri,
    fileName: String,
    mimeType: String,
    content: String
): Boolean {
    return runCatching {
        val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
        val createdUri = DocumentsContract.createDocument(
            context.contentResolver,
            parentDocUri,
            mimeType,
            fileName
        ) ?: return false
        writeTextToUri(context, createdUri, content)
    }.getOrDefault(false)
}

private fun writeTextToUri(context: Context, uri: Uri, content: String): Boolean {
    return runCatching {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
        } ?: return false
        true
    }.getOrDefault(false)
}

private fun shareLedgerFile(
    context: Context,
    content: String,
    format: ExportFormat,
    chooserTitle: String
): Boolean {
    return runCatching {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val dateStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val file = File(exportDir, "DokanHisab_$dateStamp.${format.extension}")
        file.writeText(content, Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
        true
    }.getOrDefault(false)
}

private fun writePdfToTreeDirectory(
    context: Context,
    treeUri: Uri,
    fileName: String,
    year: Int,
    monthZeroBased: Int,
    allTransactions: List<com.dokanhisab.app.data.local.TransactionWithCategory>,
    preferences: com.dokanhisab.app.data.preferences.UserPreferences,
    strings: com.dokanhisab.app.localization.AppStrings
): Boolean {
    return runCatching {
        val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
        val createdUri = DocumentsContract.createDocument(
            context.contentResolver,
            parentDocUri,
            "application/pdf",
            fileName
        ) ?: return false
        context.contentResolver.openOutputStream(createdUri)?.use { out ->
            MonthlyPdfReportGenerator.generateMonthlyReport(
                context = context,
                year = year,
                monthZeroBased = monthZeroBased,
                allTransactions = allTransactions,
                preferences = preferences,
                strings = strings,
                outputStream = out
            )
        } ?: false
    }.getOrDefault(false)
}

private suspend fun shareMonthlyPdfReport(
    context: Context,
    year: Int,
    monthZeroBased: Int,
    allTransactions: List<com.dokanhisab.app.data.local.TransactionWithCategory>,
    preferences: com.dokanhisab.app.data.preferences.UserPreferences,
    strings: com.dokanhisab.app.localization.AppStrings
): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "DokanHisab_Report_${year}_${String.format(Locale.US, "%02d", monthZeroBased + 1)}.pdf"
        val file = File(exportDir, fileName)
        val generated = file.outputStream().use { out ->
            MonthlyPdfReportGenerator.generateMonthlyReport(
                context = context,
                year = year,
                monthZeroBased = monthZeroBased,
                allTransactions = allTransactions,
                preferences = preferences,
                strings = strings,
                outputStream = out
            )
        }
        if (!generated) return@withContext false

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, strings.monthlyPdfReportTitle)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        withContext(Dispatchers.Main) {
            context.startActivity(Intent.createChooser(shareIntent, strings.monthlyPdfReportTitle))
        }
        true
    }.getOrDefault(false)
}
