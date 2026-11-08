package com.veyro.debloatengine

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.veyro.debloatengine.shizuku.ShizukuEngine
import com.veyro.debloatengine.ui.components.PressScale
import com.veyro.debloatengine.ui.screens.DashboardScreen
import com.veyro.debloatengine.ui.screens.DebloatScreen
import com.veyro.debloatengine.ui.screens.DebloatTab
import com.veyro.debloatengine.ui.screens.PerformanceModesScreen
import com.veyro.debloatengine.ui.screens.ToolsScreen
import com.veyro.debloatengine.ui.theme.DeepPurple
import com.veyro.debloatengine.ui.theme.VeyroPurpleBright
import com.veyro.debloatengine.ui.theme.VeyroTheme
import com.veyro.debloatengine.ui.theme.VoidBlack
import com.veyro.debloatengine.ui.update.UpdateBanner
import com.veyro.debloatengine.ui.update.UpdateDialog
import com.veyro.debloatengine.ui.viewmodel.VeyroViewModel

class MainActivity : ComponentActivity() {

    companion object {
        /** Official Nova Devs Telegram channel. */
        const val TELEGRAM_URL = "https://t.me/xcid_Kagenou"
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ShizukuEngine.registerListeners(this)
        requestNotificationPermissionIfNeeded()

        setContent {
            VeyroTheme {
                VeyroRoot(
                    openUpdateDialogOnStart = intent?.getBooleanExtra("show_update_dialog", false) == true ||
                        intent?.getBooleanExtra("install_update", false) == true
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ShizukuEngine.refreshState(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        ShizukuEngine.unregisterListeners()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun VeyroRoot(openUpdateDialogOnStart: Boolean, vm: VeyroViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var activeTab by remember { mutableStateOf(com.veyro.debloatengine.ui.components.VeyroNavDestination.Dashboard) }
    var debloatTab by remember { mutableStateOf(DebloatTab.CPU_KILLERS) }
    // Launch splash: hold the branded preloader for a beat + while the first scan runs.
    var splashHeld by remember { mutableStateOf(true) }
    // Full-screen Shizuku tutorial — reachable from Dashboard & Tools at any time.
    var showShizukuTutorial by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        vm.refreshData()
        if (openUpdateDialogOnStart) vm.showUpdateDialog()
        kotlinx.coroutines.delay(1600)
        splashHeld = false
    }
    // Refresh hardware readouts every time the Tools tab opens.
    LaunchedEffect(activeTab) {
        if (activeTab == com.veyro.debloatengine.ui.components.VeyroNavDestination.Tools) vm.refreshTools()
    }
    val showSplash = splashHeld || (state.isScanning && state.allPackages.isEmpty())

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(VoidBlack, DeepPurple)))
    ) {
        Column(Modifier.fillMaxSize()) {
            UpdateBanner(
                visible = state.showUpdateBanner,
                update = state.updateInfo,
                onUpdateClick = { vm.showUpdateDialog() },
                onDismiss = { vm.dismissUpdateBanner() }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (activeTab) {
                    com.veyro.debloatengine.ui.components.VeyroNavDestination.Dashboard -> DashboardScreen(
                        state = state,
                        onFlushCache = { vm.flushSystemCache() },
                        onApplyScale = { vm.applyAnimationScale(it) },
                        onRequestShizuku = { vm.requestShizukuPermission() },
                        onSetDns = { vm.setPrivateDns(it) },
                        onDisableDns = { vm.disablePrivateDns() },
                        onForceRefreshRate = { vm.forcePeakRefreshRate() },
                        onRestoreRefreshRate = { vm.restoreAutoRefreshRate() },
                        onForceRate = { vm.forceRefreshRate(it) },
                        onOpenTutorial = { showShizukuTutorial = true },
                        modifier = Modifier.fillMaxSize()
                    )
                    com.veyro.debloatengine.ui.components.VeyroNavDestination.Debloat -> DebloatScreen(
                        state = state,
                        activeTab = debloatTab,
                        onTabChange = { debloatTab = it },
                        onToggle = { vm.toggleSelection(it) },
                        onSelectAll = { vm.selectAll(it) },
                        onClearSelection = { vm.clearSelection() },
                        onRunBatch = { vm.requestBatchReview(it) },
                        onConfirmBatch = { vm.confirmPendingBatch(context) },
                        onCancelBatch = { vm.cancelPendingBatch() },
                        onRestoreAll = { vm.restoreAll(context) },
                        onRestoreSingle = { vm.restoreSingle(context, it) },
                        onDismissBatch = { vm.dismissBatchDialog() },
                        onAppAction = { item, action -> vm.runAppAction(context, item, action) },
                        onStrategyChange = { vm.setStrategy(it) },
                        onCutWakeLock = { vm.cutWakeLock(context, it) },
                        onRestoreWakeLock = { vm.restoreWakeLock(context, it) },
                        onCutAllWakeLocks = { vm.cutAllWakeLocks(context, it) },
                        onClearCache = { vm.clearAppCaches(context, it) },
                        onBatchWithStrategy = { targets, strategy -> vm.runBatchWithStrategy(context, targets, strategy) },
                        onApplyPreset = { vm.applyPreset(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                    com.veyro.debloatengine.ui.components.VeyroNavDestination.Modes -> PerformanceModesScreen(
                        state = state,
                        onApplyMode = { vm.applyMode(context, it) },
                        onSelectGame = { vm.selectGame(it) },
                        onBoostLaunch = { vm.boostAndLaunch(context) },
                        onRequestShizuku = { vm.requestShizukuPermission() },
                        onSet720p = { vm.setGame720p(it) },
                        onSetTouchBoost = { vm.setGameTouchBoost(it) },
                        onResetResolution = { vm.resetResolution() },
                        onAddCustomGame = { vm.addCustomGame(it) },
                        onRemoveCustomGame = { vm.removeCustomGame(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                    com.veyro.debloatengine.ui.components.VeyroNavDestination.Tools -> ToolsScreen(
                        state = state,
                        onRunCompiler = { vm.runSpeedCompiler() },
                        onToggleSensors = { vm.toggleSensors(it) },
                        onFlushLogs = { vm.flushLogs() },
                        onRefreshTools = { vm.refreshTools() },
                        onSetRamExpansion = { vm.setRamExpansion(it) },
                        onToggleOverlay = { ov, enable -> vm.toggleOverlay(ov, enable) },
                        onOpenTutorial = { showShizukuTutorial = true },
                        onScanOrphans = { vm.scanOrphanedData() },
                        onDeleteOrphan = { vm.deleteOrphan(it) },
                        onDeleteAllOrphans = { vm.deleteAllOrphans() },
                        onToggleStatusIcon = { vm.toggleStatusIcon(it) },
                        onResetStatusIcons = { vm.resetStatusIcons() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // ---- Watermark + Telegram footer ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Made by ⚡ Nova Devs",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.padding(horizontal = 6.dp))
                androidx.compose.material3.TextButton(
                    onClick = {
                        try {
                            context.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(MainActivity.TELEGRAM_URL)
                                )
                            )
                        } catch (_: Exception) { }
                    }
                ) {
                    Text("📣 Telegram", style = MaterialTheme.typography.labelSmall)
                }
            }

            // ---- Kit navigation bar (Lottie icons, 4 destinations) ----
            com.veyro.debloatengine.ui.components.VeyroBottomBar(
                selected = activeTab,
                onSelect = { activeTab = it }
            )
        }

        // ---- Launch splash (branded Veyro preloader) ----
        androidx.compose.animation.AnimatedVisibility(
            visible = showSplash,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(
                animationSpec = androidx.compose.animation.core.tween(500)
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VoidBlack),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                com.veyro.debloatengine.ui.components.LottieIcon(
                    resId = com.veyro.debloatengine.ui.components.VeyroAnim.veyroPreloader,
                    color = VeyroPurpleBright,
                    size = 130.dp,
                    tint = false
                )
                androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 18.dp))
                Text(
                    "Veyro Debloat Engine",
                    style = MaterialTheme.typography.headlineMedium,
                    color = VeyroPurpleBright
                )
                Text(
                    "Lag Destroyer • Rootless ADB Engine",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "made by ⚡ Nova Devs",
                    style = MaterialTheme.typography.labelSmall,
                    color = VeyroPurpleBright,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        // ---- One-click updater dialog ----
        UpdateDialog(
            update = state.updateInfo,
            downloadState = state.downloadState,
            downloadPercent = state.downloadPercent,
            shizukuReady = state.shizukuState == com.veyro.debloatengine.data.model.ShizukuState.READY,
            onUpdateNow = { vm.startUpdateDownload() },
            onSilentInstall = { vm.silentInstallUpdate() },
            onOpenInstaller = { vm.openSystemInstaller() },
            onDismiss = { vm.dismissUpdateDialog() }
        )

        // ---- Full-screen Shizuku tutorial (4-step guide with Play Store link) ----
        if (showShizukuTutorial) {
            com.veyro.debloatengine.ui.setup.ShizukuSetupScreen(
                onClose = { showShizukuTutorial = false },
                onFinished = {
                    showShizukuTutorial = false
                    ShizukuEngine.requestPermission()
                }
            )
        }
    }
}
