package com.veyro.debloatengine.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veyro.debloatengine.data.model.AppItem
import com.veyro.debloatengine.ui.components.PressScale
import com.veyro.debloatengine.ui.components.ShimmerSkeletonList
import com.veyro.debloatengine.ui.components.SweepProgressBar
import com.veyro.debloatengine.ui.theme.DimText
import com.veyro.debloatengine.ui.theme.DeepPurple
import com.veyro.debloatengine.ui.theme.PanelPurple
import com.veyro.debloatengine.ui.theme.VeyroAmber
import com.veyro.debloatengine.ui.theme.VeyroBlue
import com.veyro.debloatengine.ui.theme.VeyroGreen
import com.veyro.debloatengine.ui.theme.VeyroPurple
import com.veyro.debloatengine.ui.theme.VeyroPurpleBright
import com.veyro.debloatengine.ui.theme.VeyroRed
import com.veyro.debloatengine.ui.theme.VeyroWhite
import com.veyro.debloatengine.ui.viewmodel.VeyroUiState
import com.veyro.debloatengine.ui.viewmodel.VeyroViewModel

/** Engine tabs — short labels so all five fit the pill row. */
enum class DebloatTab(val title: String) {
    CPU_KILLERS("CPU"),
    RAM_HOGS("RAM"),
    ALL_APPS("Apps"),
    WAKE_LOCKS("Wake"),
    RESTORE("Restore")
}

/**
 * DebloatScreen — tabbed Lag-Destroyer lists with shimmer skeletons,
 * spring press feedback, animated list filtering, and the live batch
 * progress meter with gradient sweeps.
 */
@Composable
fun DebloatScreen(
    state: VeyroUiState,
    onTabChange: (DebloatTab) -> Unit,
    activeTab: DebloatTab,
    onToggle: (String) -> Unit,
    onSelectAll: (List<AppItem>) -> Unit,
    onClearSelection: () -> Unit,
    onRunBatch: (List<AppItem>) -> Unit,
    onConfirmBatch: () -> Unit,
    onCancelBatch: () -> Unit,
    onRestoreAll: () -> Unit,
    onRestoreSingle: (String) -> Unit,
    onDismissBatch: () -> Unit,
    onAppAction: (AppItem, VeyroViewModel.AppAction) -> Unit,
    onStrategyChange: (com.veyro.debloatengine.data.model.DebloatStrategy) -> Unit,
    onCutWakeLock: (AppItem) -> Unit,
    onRestoreWakeLock: (String) -> Unit,
    onCutAllWakeLocks: (List<AppItem>) -> Unit,
    onClearCache: (List<AppItem>) -> Unit,
    onBatchWithStrategy: (List<AppItem>, com.veyro.debloatengine.data.model.DebloatStrategy) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {
        // ---- Tab pills ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DebloatTab.entries.forEach { tab ->
                TabPill(
                    text = tab.title,
                    selected = tab == activeTab,
                    onClick = { onTabChange(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ---- Content ----
        Box(Modifier.weight(1f)) {
            when (activeTab) {
                DebloatTab.CPU_KILLERS -> PackageListPane(
                    state = state,
                    packages = state.cpuKillers,
                    emptyText = "No tracking daemons found on this device. Your CPU sleeps clean.",
                    onToggle = onToggle,
                    onSelectAll = onSelectAll,
                    onClearSelection = onClearSelection,
                    onRunBatch = onRunBatch,
                    onRestoreSingle = onRestoreSingle,
                    onAppAction = onAppAction,
                    onClearCache = onClearCache,
                    onBatchWithStrategy = onBatchWithStrategy,
                    accent = VeyroRed
                )
                DebloatTab.RAM_HOGS -> PackageListPane(
                    state = state,
                    packages = state.ramHogs,
                    emptyText = "No resident RAM hogs detected. Memory is breathing freely.",
                    onToggle = onToggle,
                    onSelectAll = onSelectAll,
                    onClearSelection = onClearSelection,
                    onRunBatch = onRunBatch,
                    onRestoreSingle = onRestoreSingle,
                    onAppAction = onAppAction,
                    onClearCache = onClearCache,
                    onBatchWithStrategy = onBatchWithStrategy,
                    accent = VeyroPurpleBright
                )
                DebloatTab.ALL_APPS -> PackageListPane(
                    state = state,
                    packages = state.allPackages,
                    emptyText = "No packages found.",
                    onToggle = onToggle,
                    onSelectAll = onSelectAll,
                    onClearSelection = onClearSelection,
                    onRunBatch = onRunBatch,
                    onRestoreSingle = onRestoreSingle,
                    onAppAction = onAppAction,
                    onClearCache = onClearCache,
                    onBatchWithStrategy = onBatchWithStrategy,
                    accent = VeyroGreen
                )
                DebloatTab.WAKE_LOCKS -> WakeLockPane(
                    state = state,
                    onCut = onCutWakeLock,
                    onRestore = onRestoreWakeLock,
                    onCutAll = onCutAllWakeLocks
                )
                DebloatTab.RESTORE -> RestorePane(
                    state = state,
                    onRestoreAll = onRestoreAll,
                    onRestoreSingle = onRestoreSingle
                )
            }

            // ---- Shimmer + Lottie preloader while scanning ----
            androidx.compose.animation.AnimatedVisibility(
                visible = state.isScanning,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    com.veyro.debloatengine.ui.components.VeyroPreloader(
                        label = "Scanning installed packages…",
                        sublabel = "Classifying CPU killers, RAM hogs and safety locks",
                        accent = VeyroPurpleBright
                    )
                    Spacer(Modifier.height(12.dp))
                    ShimmerSkeletonList(rows = 4)
                }
            }
        }

        // ---- Live batch progress ----
        AnimatedVisibility(
            visible = state.batch.isRunning || state.batch.finished,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            BatchProgressPanel(
                progressText = if (state.batch.finished) "Operation complete"
                else "[${state.batch.index}/${state.batch.total}] ${state.batch.currentPackage}",
                progress = if (state.batch.total > 0) state.batch.index.toFloat() / state.batch.total else 0f,
                logs = state.batch.logs,
                finished = state.batch.finished,
                onDismiss = onDismissBatch,
                strategy = state.strategy
            )
        }
    }

    // ---- Review-before-run confirmation dialog ----
    if (state.pendingBatchTargets.isNotEmpty()) {
        BatchConfirmDialog(
            targets = state.pendingBatchTargets,
            strategy = state.pendingBatchStrategy,
            onConfirm = onConfirmBatch,
            onCancel = onCancelBatch
        )
    }
}

// ---------------------------------------------------------------------------
// Strategy selector: how the engine neutralizes packages
// ---------------------------------------------------------------------------
@Composable
private fun WakeLockPane(
    state: VeyroUiState,
    onCut: (AppItem) -> Unit,
    onRestore: (String) -> Unit,
    onCutAll: (List<AppItem>) -> Unit
) {
    val candidates = remember(state.allPackages) {
        com.veyro.debloatengine.data.repository.SystemTweaksRepository.wakeLockCandidates(state.allPackages)
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text("✂️ Wake-Lock Cutter", style = MaterialTheme.typography.titleMedium, color = VeyroWhite)
            Text(
                "Strips WAKE_LOCK + RUN_IN_BACKGROUND via AppOps. The app works normally while " +
                    "you use it — it just can't wake your phone's processor anymore. Reversible 1-tap.",
                style = MaterialTheme.typography.bodySmall,
                color = DimText
            )
            Spacer(Modifier.height(10.dp))
            PressScale(onClick = { onCutAll(candidates) }, enabled = candidates.isNotEmpty()) {
                Button(
                    onClick = { onCutAll(candidates) },
                    enabled = candidates.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = VeyroRed),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("✂️ Cut All Wake-Locks (${candidates.size} apps)") }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${state.wakeLockCut.size} app(s) currently have wake-locks cut",
                style = MaterialTheme.typography.labelSmall,
                color = if (state.wakeLockCut.isEmpty()) DimText else VeyroGreen
            )
        }
        Spacer(Modifier.height(8.dp))

        if (candidates.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No social/shopping wake-lock hogs detected on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DimText
                )
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(candidates, key = { it.packageName }) { item ->
                val cut = item.packageName in state.wakeLockCut
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (cut) VeyroGreen.copy(alpha = 0.10f) else PanelPurple)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DeepPurple),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item.appName.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, color = VeyroPurpleBright)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.appName, style = MaterialTheme.typography.titleMedium, color = VeyroWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            item.packageName,
                            style = MaterialTheme.typography.labelSmall,
                            color = DimText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (cut) "✓ Wake-locks CUT — cannot wake your CPU" else "Holding wake-locks in your pocket",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (cut) VeyroGreen else VeyroAmber
                        )
                    }
                    if (cut) {
                        OutlinedButton(onClick = { onRestore(item.packageName) }) {
                            Text("Restore", color = VeyroGreen)
                        }
                    } else {
                        Button(
                            onClick = { onCut(item) },
                            colors = ButtonDefaults.buttonColors(containerColor = VeyroRed)
                        ) { Text("Cut") }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Tab pill
// ---------------------------------------------------------------------------
@Composable
private fun TabPill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PressScale(onClick = onClick, modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) VeyroPurple else PanelPurple)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) VeyroWhite else DimText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------------------------------------------------------------------
// A list pane of packages
// ---------------------------------------------------------------------------
@Composable
private fun PackageListPane(
    state: VeyroUiState,
    packages: List<AppItem>,
    emptyText: String,
    onToggle: (String) -> Unit,
    onSelectAll: (List<AppItem>) -> Unit,
    onClearSelection: () -> Unit,
    onRunBatch: (List<AppItem>) -> Unit,
    onRestoreSingle: (String) -> Unit,
    onAppAction: (AppItem, VeyroViewModel.AppAction) -> Unit,
    onClearCache: (List<AppItem>) -> Unit,
    onBatchWithStrategy: (List<AppItem>, com.veyro.debloatengine.data.model.DebloatStrategy) -> Unit,
    accent: Color
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${packages.size} targets",
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                modifier = Modifier.weight(1f)
            )
            TextButtonSmall("Select all removable") { onSelectAll(packages) }
            TextButtonSmall("Clear") { onClearSelection() }
        }

        if (packages.isEmpty() && !state.isScanning) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                com.veyro.debloatengine.ui.components.LottieIcon(
                    resId = com.veyro.debloatengine.ui.components.VeyroAnim.search,
                    color = DimText,
                    size = 56.dp,
                    forever = false
                )
                Spacer(Modifier.height(10.dp))
                Text(emptyText, style = MaterialTheme.typography.bodyMedium, color = DimText)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(packages, key = { it.packageName }) { item ->
                androidx.compose.animation.AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    PackageCard(
                        item = item,
                        selected = item.packageName in state.selectedPackages,
                        onToggle = { onToggle(item.packageName) },
                        onRestore = { onRestoreSingle(item.packageName) },
                        onAppAction = onAppAction
                    )
                }
            }
        }

        // ---- Selection action bar ----
        AnimatedVisibility(
            visible = state.selectedPackages.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            // ---- Prominent batch action panel ----
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepPurple)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "⚡ Batch actions — ${state.selectedPackages.size} selected",
                        style = MaterialTheme.typography.labelLarge,
                        color = VeyroWhite,
                        modifier = Modifier.weight(1f)
                    )
                    PressScale(onClick = onClearSelection) {
                        Text(
                            "Clear",
                            style = MaterialTheme.typography.labelSmall,
                            color = DimText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                val targets = packages.filter { it.packageName in state.selectedPackages }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BatchActionButton(
                        label = "Freeze",
                        anim = com.veyro.debloatengine.ui.components.VeyroAnim.veyroFreeze,
                        container = VeyroGreen, content = Color(0xFF0B0514),
                        targets = targets,
                        strategy = com.veyro.debloatengine.data.model.DebloatStrategy.FREEZE,
                        onRun = onBatchWithStrategy,
                        modifier = Modifier.weight(1f)
                    )
                    BatchActionButton(
                        label = "Disable",
                        anim = com.veyro.debloatengine.ui.components.VeyroAnim.veyroDisable,
                        container = VeyroBlue, content = VeyroWhite,
                        targets = targets,
                        strategy = com.veyro.debloatengine.data.model.DebloatStrategy.DISABLE,
                        onRun = onBatchWithStrategy,
                        modifier = Modifier.weight(1f)
                    )
                    BatchActionButton(
                        label = "Remove",
                        anim = com.veyro.debloatengine.ui.components.VeyroAnim.veyroUninstall,
                        container = VeyroRed, content = VeyroWhite,
                        targets = targets,
                        strategy = com.veyro.debloatengine.data.model.DebloatStrategy.UNINSTALL,
                        onRun = onBatchWithStrategy,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = { onClearCache(targets) },
                    colors = ButtonDefaults.buttonColors(containerColor = PanelPurple),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🧹 Stop & Clear Cache (${state.selectedPackages.size}) — safe") }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Every action asks you to confirm first. Freeze & cache-clean are always safe; " +
                        "Remove uninstalls the app for you (data kept, restorable). Nothing is permanent.",
                    style = MaterialTheme.typography.labelSmall,
                    color = DimText
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Package card with spring press feedback, checkbox, and an expandable
// detail sheet with per-app controls (force-stop / freeze / disable / boost)
// ---------------------------------------------------------------------------
@Composable
fun PackageCard(
    item: AppItem,
    selected: Boolean,
    onToggle: () -> Unit,
    onRestore: () -> Unit,
    onAppAction: ((AppItem, VeyroViewModel.AppAction) -> Unit)? = null
) {
    var detailsOpen by remember { mutableStateOf(false) }
    val checkboxScale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 500f),
        label = "checkScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) VeyroPurple.copy(alpha = 0.18f) else PanelPurple)
    ) {
        PressScale(onClick = onToggle, enabled = !item.isProtected) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DeepPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        (if (item.isGame) "🎮" else item.appName.take(1)).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = VeyroPurpleBright
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        item.appName,
                        style = MaterialTheme.typography.titleMedium,
                        color = VeyroWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        item.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = DimText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.reason.isNotBlank()) {
                        Text(
                            item.reason,
                            style = MaterialTheme.typography.labelSmall,
                            color = VeyroAmber,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        if (item.isProtected) item.warning
                        else if (!item.isInstalled) "Removed — restorable"
                        else item.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.isProtected) VeyroRed else DimText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                if (item.isProtected) {
                    // Animated lock (UseAnimations) instead of a static emoji
                    com.veyro.debloatengine.ui.components.LottieIcon(
                        resId = com.veyro.debloatengine.ui.components.VeyroAnim.lock,
                        color = VeyroRed,
                        size = 22.dp,
                        forever = false
                    )
                } else if (!item.isInstalled) {
                    OutlinedButton(onClick = onRestore) {
                        Text("Restore", color = VeyroGreen)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Details toggle
                        PressScale(onClick = { detailsOpen = !detailsOpen }) {
                            Text(
                                if (detailsOpen) "▲" else "ⓘ",
                                style = MaterialTheme.typography.titleMedium,
                                color = VeyroPurpleBright,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                        }
                        Checkbox(
                            checked = selected,
                            onCheckedChange = { onToggle() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = VeyroPurple,
                                checkmarkColor = VeyroWhite
                            ),
                            modifier = Modifier
                                .size(34.dp)
                                .scale(checkboxScale)
                        )
                    }
                }
            }
        }

        // ---- Detail sheet with per-app controls ----
        AnimatedVisibility(
            visible = detailsOpen && !item.isProtected,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                Text(item.description, style = MaterialTheme.typography.bodySmall, color = DimText)
                Text(item.warning, style = MaterialTheme.typography.labelSmall, color = VeyroAmber)
                Spacer(Modifier.height(10.dp))
                if (onAppAction != null) {
                    Text(
                        "PER-APP CONTROLS",
                        style = MaterialTheme.typography.labelSmall,
                        color = VeyroPurpleBright
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionChip("Force stop", VeyroAmber) {
                            onAppAction(item, VeyroViewModel.AppAction.FORCE_STOP)
                        }
                        if (item.isEnabled) {
                            ActionChip("Freeze", VeyroBlue) {
                                onAppAction(item, VeyroViewModel.AppAction.FREEZE)
                            }
                        } else {
                            ActionChip("Unfreeze", VeyroGreen) {
                                onAppAction(item, VeyroViewModel.AppAction.UNFREEZE)
                            }
                        }
                        ActionChip("✂️ Cut Wake-Locks", VeyroBlue) {
                            onAppAction(item, VeyroViewModel.AppAction.CUT_WAKELOCK)
                        }
                        ActionChip("💾 Backup APK", VeyroGreen) {
                            onAppAction(item, VeyroViewModel.AppAction.BACKUP_APK)
                        }
                        ActionChip("🚫 Block BG Data", VeyroAmber) {
                            onAppAction(item, VeyroViewModel.AppAction.BLOCK_BG_DATA)
                        }
                        if (item.isGame) {
                            ActionChip("🚀 Boost", VeyroRed) {
                                onAppAction(item, VeyroViewModel.AppAction.BOOST_AND_LAUNCH)
                            }
                        }
                    }
                    if (item.isGame) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Boost clears RAM, freezes background hogs, kills animations and launches this game focused.",
                            style = MaterialTheme.typography.labelSmall,
                            color = DimText
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Restore pane — driven by the engine's own RemovalLog so frozen, disabled
// AND uninstalled apps all show up and restore correctly.
// ---------------------------------------------------------------------------
@Composable
private fun RestorePane(
    state: VeyroUiState,
    onRestoreAll: () -> Unit,
    onRestoreSingle: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Reversible Restore Engine", style = MaterialTheme.typography.titleMedium, color = VeyroWhite)
        Spacer(Modifier.height(4.dp))
        Text(
            "Everything the engine touched — frozen, disabled or removed — is logged below with " +
                "its method. Restore runs `install-existing` + `pm enable` + `cmd package unsuspend`, " +
                "which undoes any of the three strategies in one tap.",
            style = MaterialTheme.typography.bodySmall,
            color = DimText
        )
        Spacer(Modifier.height(14.dp))

        PressScale(onClick = onRestoreAll, enabled = state.removalEntries.isNotEmpty()) {
            Button(
                onClick = onRestoreAll,
                enabled = state.removalEntries.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = VeyroGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("↺ Restore All (${state.removalEntries.size} logged)", color = Color(0xFF0B0514))
            }
        }

        Spacer(Modifier.height(14.dp))

        if (state.removalEntries.isEmpty()) {
            Text(
                "The engine log is empty — nothing to restore. Every Freeze, Disable or Uninstall " +
                    "you run will appear here automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = DimText
            )
        } else {
            state.removalEntries.reversed().forEach { entry ->
                val methodLabel = when (entry.method) {
                    "freeze" -> "🧊 frozen"
                    "disable" -> "⏸ disabled"
                    else -> "🗑 removed"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(PanelPurple)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            entry.appName,
                            style = MaterialTheme.typography.titleMedium,
                            color = VeyroWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "$methodLabel • ${entry.category}",
                            style = MaterialTheme.typography.labelSmall,
                            color = DimText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(onClick = { onRestoreSingle(entry.packageName) }) {
                        Text("Restore", color = VeyroGreen)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // Fallback: apps not in the log but detected as uninstalled (older removals / other tools)
        val others = state.allPackages.filter { !it.isInstalled && state.removalEntries.none { e -> e.packageName == it.packageName } }
        if (others.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text("Also detected as absent on this device:", style = MaterialTheme.typography.labelSmall, color = DimText)
            others.take(20).forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DeepPurple)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        item.appName,
                        style = MaterialTheme.typography.labelSmall,
                        color = DimText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    PressScale(onClick = { onRestoreSingle(item.packageName) }) {
                        Text("Try restore", style = MaterialTheme.typography.labelSmall, color = VeyroPurpleBright)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Batch progress panel with gradient sweep
// ---------------------------------------------------------------------------
@Composable
private fun BatchProgressPanel(
    progressText: String,
    progress: Float,
    logs: List<String>,
    finished: Boolean,
    onDismiss: () -> Unit,
    strategy: com.veyro.debloatengine.data.model.DebloatStrategy = com.veyro.debloatengine.data.model.DebloatStrategy.FREEZE
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepPurple)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (finished) {
                com.veyro.debloatengine.ui.components.LottieIcon(
                    resId = com.veyro.debloatengine.ui.components.VeyroAnim.checkmark,
                    color = VeyroGreen,
                    size = 26.dp,
                    forever = false
                )
            } else {
                // Batch icon matches the active strategy (custom branded animations)
                com.veyro.debloatengine.ui.components.LottieIcon(
                    resId = when (strategy) {
                        com.veyro.debloatengine.data.model.DebloatStrategy.FREEZE ->
                            com.veyro.debloatengine.ui.components.VeyroAnim.veyroFreeze
                        com.veyro.debloatengine.data.model.DebloatStrategy.DISABLE ->
                            com.veyro.debloatengine.ui.components.VeyroAnim.veyroDisable
                        com.veyro.debloatengine.data.model.DebloatStrategy.UNINSTALL ->
                            com.veyro.debloatengine.ui.components.VeyroAnim.veyroUninstall
                    },
                    color = VeyroWhite,
                    size = 26.dp,
                    tint = false
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                progressText,
                style = MaterialTheme.typography.labelLarge,
                color = VeyroWhite,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (finished) {
                TextButtonSmall("Close") { onDismiss() }
            }
        }
        Spacer(Modifier.height(8.dp))
        SweepProgressBar(
            progress = if (finished) 1f else progress,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .height(150.dp)
                .verticalScroll(rememberScrollState())
        ) {
            logs.takeLast(4).forEach { line ->
                Text(
                    line,
                    style = MaterialTheme.typography.labelSmall,
                    color = DimText
                )
            }
        }
    }
}

@Composable
private fun TextButtonSmall(text: String, onClick: () -> Unit) {
    PressScale(onClick = onClick) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = VeyroPurpleBright,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(VeyroPurple.copy(alpha = 0.12f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Per-app action chip
// ---------------------------------------------------------------------------
@Composable
private fun BatchActionButton(
    label: String,
    anim: Int,
    container: Color,
    content: Color,
    targets: List<AppItem>,
    strategy: com.veyro.debloatengine.data.model.DebloatStrategy,
    onRun: (List<AppItem>, com.veyro.debloatengine.data.model.DebloatStrategy) -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { onRun(targets, strategy) },
        colors = ButtonDefaults.buttonColors(containerColor = container),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 8.dp),
        modifier = modifier
    ) {
        com.veyro.debloatengine.ui.components.LottieIcon(
            resId = anim,
            color = content,
            size = 18.dp,
            tint = false
        )
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = content, maxLines = 1)
    }
}

@Composable
private fun ActionChip(text: String, accent: Color, onClick: () -> Unit) {
    PressScale(onClick = onClick) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.14f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Review-before-run dialog: shows exactly what will happen before the
// engine touches anything.
// ---------------------------------------------------------------------------
@Composable
private fun BatchConfirmDialog(
    targets: List<AppItem>,
    strategy: com.veyro.debloatengine.data.model.DebloatStrategy,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val risky = targets.count { it.tier == com.veyro.debloatengine.data.model.Tier.RISKY || it.isSystemApp }
    val verdict: String
    val verdictColor: Color
    if (strategy == com.veyro.debloatengine.data.model.DebloatStrategy.FREEZE) {
        verdict = "SAFE - app and data stay on the phone; it just can't run. Reversible in one tap."
        verdictColor = VeyroGreen
    } else if (strategy == com.veyro.debloatengine.data.model.DebloatStrategy.DISABLE) {
        verdict = "SAFE - app stays installed with all data, icon hidden. Reversible in one tap."
        verdictColor = VeyroGreen
    } else if (risky > 0) {
        verdict = "CAREFUL - this UNINSTALLS the app for you. Data caches are kept and it is restorable, but re-downloading may be needed."
        verdictColor = VeyroAmber
    } else {
        verdict = "MODERATE - this uninstalls the app for you (data kept, fully restorable from the Restore tab)."
        verdictColor = VeyroAmber
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onCancel,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = VeyroWhite,
        title = { Text(strategy.label + " " + targets.size + " app" + (if (targets.size == 1) "" else "s") + "?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(verdict, style = MaterialTheme.typography.bodySmall, color = verdictColor)
                if (risky > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Warning: " + risky + " of these are system components - removing them may affect a feature " +
                            "(Wi-Fi, camera, OEM tools...). Freeze is the safer choice for these.",
                        style = MaterialTheme.typography.labelSmall,
                        color = VeyroAmber
                    )
                }
                Spacer(Modifier.height(10.dp))
                targets.take(6).forEach { t ->
                    Text(
                        "- " + t.appName + " - " + t.reason.ifBlank { t.description },
                        style = MaterialTheme.typography.labelSmall,
                        color = VeyroWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (targets.size > 6) {
                    Text(
                        "...and " + (targets.size - 6) + " more",
                        style = MaterialTheme.typography.labelSmall,
                        color = DimText
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Everything is logged in the Restore tab - nothing is permanent.",
                    style = MaterialTheme.typography.labelSmall,
                    color = VeyroGreen
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (strategy == com.veyro.debloatengine.data.model.DebloatStrategy.FREEZE) VeyroGreen else VeyroRed
                )
            ) { Text("Yes, do it") }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel) { Text("Cancel", color = DimText) }
        }
    )
}
