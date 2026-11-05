package com.veyro.debloatengine.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.veyro.debloatengine.data.model.AppItem
import com.veyro.debloatengine.data.model.ShizukuState
import com.veyro.debloatengine.data.repository.PerfMode
import com.veyro.debloatengine.ui.components.PressScale
import com.veyro.debloatengine.ui.components.SweepProgressBar
import com.veyro.debloatengine.ui.theme.DeepPurple
import com.veyro.debloatengine.ui.theme.DimText
import com.veyro.debloatengine.ui.theme.PanelPurple
import com.veyro.debloatengine.ui.theme.VeyroAmber
import com.veyro.debloatengine.ui.theme.VeyroGreen
import com.veyro.debloatengine.ui.theme.VeyroPurple
import com.veyro.debloatengine.ui.theme.VeyroPurpleBright
import com.veyro.debloatengine.ui.theme.VeyroRed
import com.veyro.debloatengine.ui.theme.VeyroWhite
import com.veyro.debloatengine.ui.viewmodel.VeyroUiState
import com.veyro.debloatengine.ui.viewmodel.VeyroViewModel

/**
 * Engine Modes — Eco / Performance / Gaming, each with its own animation
 * preference and background policy, plus the Gaming "Boost & Launch" focus
 * engine that clears RAM and freezes hogs before starting the chosen game.
 */
@Composable
fun PerformanceModesScreen(
    state: VeyroUiState,
    onApplyMode: (PerfMode) -> Unit,
    onSelectGame: (String?) -> Unit,
    onBoostLaunch: () -> Unit,
    onRequestShizuku: () -> Unit,
    onSet720p: (Boolean) -> Unit,
    onSetTouchBoost: (Boolean) -> Unit,
    onResetResolution: () -> Unit,
    onAddCustomGame: (String) -> Unit,
    onRemoveCustomGame: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shizukuReady = state.shizukuState == ShizukuState.READY
    var showAddGameDialog by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(false)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            com.veyro.debloatengine.ui.components.LottieIcon(
                resId = com.veyro.debloatengine.ui.components.VeyroAnim.veyroGauge,
                color = VeyroPurpleBright,
                size = 34.dp,
                tint = false
            )
            Spacer(Modifier.width(10.dp))
            Text("Engine Modes", style = MaterialTheme.typography.headlineMedium, color = VeyroWhite)
        }
        Text(
            "One tap re-animations + RAM policy + battery policy for the whole device. " +
                "Default: Eco (battery first).",
            style = MaterialTheme.typography.bodySmall,
            color = DimText
        )
        Spacer(Modifier.height(14.dp))

        // ---- Mode cards ----
        PerfMode.entries.forEach { mode ->
            ModeCard(
                mode = mode,
                selected = state.currentMode == mode,
                enabled = shizukuReady && !state.isApplyingMode,
                shizukuReady = shizukuReady,
                onApply = { onApplyMode(mode) }
            )
            Spacer(Modifier.height(10.dp))
        }

        if (!shizukuReady) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(VeyroAmber.copy(alpha = 0.12f))
                    .padding(12.dp)
            ) {
                Text(
                    "Connect Shizuku first (Dashboard → Setup) so the engine can execute the mode commands.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VeyroAmber
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // ---- Gaming focus: pick a game and boost ----
        ModeSectionCard(
            title = "🎮 Game Focus Engine",
            subtitle = "Clear RAM → freeze hogs → animations off → launch"
        ) {
            Text(
                "Pick a heavy game. When you hit Boost & Launch, every resident RAM hog is " +
                    "force-stopped, cached background processes are killed, animations are " +
                    "switched off, and your game gets the whole device to itself. Your other " +
                    "apps stay installed — only RAM is cleared, and they reload when you return.",
                style = MaterialTheme.typography.bodySmall,
                color = DimText
            )

            Spacer(Modifier.height(12.dp))

            if (state.installedGames.isEmpty()) {
                Text(
                    "No games detected on this device — you can still boost any app from " +
                        "Debloat → All Apps → tap ⓘ.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VeyroAmber
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(state.installedGames, key = { it.packageName }) { game ->
                        GameChip(
                            game = game,
                            selected = state.selectedGame == game.packageName,
                            onClick = {
                                onSelectGame(
                                    if (state.selectedGame == game.packageName) null else game.packageName
                                )
                            }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Not all games carry the system GAME tag — missing one? Add it below.",
                    style = MaterialTheme.typography.labelSmall,
                    color = DimText
                )
                PressScale(onClick = { showAddGameDialog = true }) {
                    Text(
                        "＋ Add apps to game list",
                        style = MaterialTheme.typography.labelSmall,
                        color = VeyroPurpleBright,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(VeyroPurple.copy(alpha = 0.14f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                // Swipe affordance: explicit hint + animated arrow so the
                // horizontal game strip is discoverable.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val hint = rememberInfiniteTransition(label = "swipehint")
                    val nudge by hint.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
                        label = "nudge"
                    )
                    Text("‹", style = MaterialTheme.typography.labelLarge, color = DimText)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Swipe the games sideways to browse all",
                        style = MaterialTheme.typography.labelSmall,
                        color = VeyroPurpleBright
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "›",
                        style = MaterialTheme.typography.labelLarge,
                        color = VeyroPurpleBright,
                        modifier = Modifier.padding(start = (nudge * 10).dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
            }

            Spacer(Modifier.height(12.dp))

            val selectedGameName = state.allPackages
                .find { it.packageName == state.selectedGame }?.appName

            Spacer(Modifier.height(14.dp))
            Text("GAME OVERDRIVE", style = MaterialTheme.typography.labelLarge, color = VeyroPurpleBright)
            Spacer(Modifier.height(6.dp))

            // 720p FPS Booster
            OverdriveToggle(
                title = "720p FPS Booster",
                blurb = "Drops rendering to ~720p (2/3 scale) — 40%+ FPS in heavy 3D games " +
                    "like Genshin/BGMI/COD. Reset anytime below.",
                checked = state.game720p,
                onChange = onSet720p
            )
            // Competitive touch
            OverdriveToggle(
                title = "Competitive Touch",
                blurb = "Pointer speed +7 and zero tap-blocking threshold — minimum input delay.",
                checked = state.gameTouchBoost,
                onChange = onSetTouchBoost
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Display: ${if (state.resolution.overridden) "${state.resolution.current} (overridden)" else "${state.resolution.native.ifBlank { "?" }} (native)"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state.resolution.overridden) VeyroAmber else DimText,
                    modifier = Modifier.weight(1f)
                )
                if (state.resolution.overridden) {
                    PressScale(onClick = onResetResolution) {
                        Text(
                            "Reset to native",
                            style = MaterialTheme.typography.labelSmall,
                            color = VeyroGreen,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(VeyroGreen.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            PressScale(onClick = onBoostLaunch, enabled = shizukuReady && state.selectedGame != null && !state.isApplyingMode) {
                Button(
                    onClick = onBoostLaunch,
                    enabled = shizukuReady && state.selectedGame != null && !state.isApplyingMode,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.selectedGame != null) VeyroGreen else DeepPurple
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (selectedGameName != null) "🚀 Boost & Launch $selectedGameName"
                        else "🚀 Boost & Launch — pick a game first",
                        color = if (state.selectedGame != null) Color(0xFF0B0514) else DimText
                    )
                }
            }

            // Live mode log
            AnimatedVisibility(
                visible = state.isApplyingMode || state.modeLog.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (state.isApplyingMode) {
                        com.veyro.debloatengine.ui.components.LottieIcon(
                            resId = com.veyro.debloatengine.ui.components.VeyroAnim.power,
                            color = VeyroPurpleBright,
                            size = 40.dp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Rewriting device policy…",
                            style = MaterialTheme.typography.labelLarge,
                            color = VeyroPurpleBright
                        )
                        Spacer(Modifier.height(8.dp))
                        SweepProgressBar(progress = 0.6f, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                    }
                    state.modeLog.forEach { line ->
                        Text(
                            "• $line",
                            style = MaterialTheme.typography.labelSmall,
                            color = VeyroGreen
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showAddGameDialog) {
        AddGameDialog(
            state = state,
            onAdd = onAddCustomGame,
            onRemove = onRemoveCustomGame,
            onDismiss = { showAddGameDialog = false }
        )
    }
}

// ---------------------------------------------------------------------------
// Manual game list editor — searchable, persisted via the ViewModel.
// ---------------------------------------------------------------------------
@Composable
private fun AddGameDialog(
    state: VeyroUiState,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    val candidates = state.allPackages
        .filter { it.isInstalled && !it.isProtected }
        .filter {
            query.isBlank() ||
                it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
        .sortedBy { it.appName.lowercase() }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = VeyroWhite,
        title = { Text("Add games to the Focus list") },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Search installed apps…", color = DimText) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .height(320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    candidates.forEach { app ->
                        val isCustom = app.packageName in state.customGames
                        val isDetected = app.isGame
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    app.appName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = VeyroWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    if (isDetected) "auto-detected game" else "added manually",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DimText
                                )
                            }
                            PressScale(
                                onClick = {
                                    if (isCustom) onRemove(app.packageName) else onAdd(app.packageName)
                                }
                            ) {
                                Text(
                                    if (isCustom) "Remove" else "Add",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isCustom) VeyroRed else VeyroGreen,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            (if (isCustom) VeyroRed else VeyroGreen).copy(alpha = 0.12f)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = VeyroPurple)) {
                Text("Done")
            }
        }
    )
}

// ---------------------------------------------------------------------------
// Mode card
// ---------------------------------------------------------------------------
@Composable
private fun ModeCard(
    mode: PerfMode,
    selected: Boolean,
    enabled: Boolean,
    shizukuReady: Boolean,
    onApply: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 380f),
        label = "modeScale"
    )
    val accent = when (mode) {
        PerfMode.ECO -> VeyroGreen
        PerfMode.PERFORMANCE -> VeyroPurpleBright
        PerfMode.GAMING -> VeyroRed
    }

    PressScale(onClick = { if (enabled) onApply() }, enabled = enabled) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (selected) Brush.linearGradient(listOf(accent.copy(alpha = 0.30f), PanelPurple))
                    else Brush.linearGradient(listOf(PanelPurple, DeepPurple))
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.veyro.debloatengine.ui.components.LottieIcon(
                    resId = when (mode) {
                        PerfMode.ECO -> com.veyro.debloatengine.ui.components.VeyroAnim.battery
                        PerfMode.PERFORMANCE -> com.veyro.debloatengine.ui.components.VeyroAnim.zap
                        PerfMode.GAMING -> com.veyro.debloatengine.ui.components.VeyroAnim.activity
                    },
                    color = accent,
                    size = 26.dp
                )
                Spacer(Modifier.width(10.dp))
                Text(mode.title, style = MaterialTheme.typography.titleMedium, color = accent, modifier = Modifier.weight(1f))
                if (selected) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accent.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("ACTIVE", style = MaterialTheme.typography.labelSmall, color = accent)
                    }
                }
            }
            Text(mode.tagline, style = MaterialTheme.typography.labelSmall, color = DimText)
            Spacer(Modifier.height(8.dp))
            Text(mode.description, style = MaterialTheme.typography.bodySmall, color = VeyroWhite)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ModePill(mode.animationLabel, accent)
                ModePill(if (mode.batterySaver) "Battery saver ON" else "Full CPU boost", accent)
                ModePill(if (mode.clearRam) "RAM trimmed" else "RAM untouched", accent)
            }
        }
    }
}

@Composable
private fun ModePill(text: String, accent: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DeepPurple)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = accent, maxLines = 1)
    }
}

@Composable
private fun GameChip(game: AppItem, selected: Boolean, onClick: () -> Unit) {
    PressScale(onClick = onClick) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (selected) VeyroPurple else PanelPurple)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.veyro.debloatengine.ui.components.LottieIcon(
                resId = com.veyro.debloatengine.ui.components.VeyroAnim.activity,
                color = if (selected) VeyroWhite else DimText,
                size = 18.dp
            )
            Spacer(Modifier.width(6.dp))
            Text(
                game.appName,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) VeyroWhite else DimText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OverdriveToggle(
    title: String,
    blurb: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    PressScale(onClick = { onChange(!checked) }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (checked) VeyroPurple.copy(alpha = 0.25f) else DeepPurple)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (checked) VeyroGreen else PanelPurple),
                contentAlignment = Alignment.Center
            ) {
                if (checked) Text("✓", style = MaterialTheme.typography.labelSmall, color = Color(0xFF0B0514))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = VeyroWhite)
                Text(blurb, style = MaterialTheme.typography.labelSmall, color = DimText)
            }
        }
    }
}

@Composable
private fun ModeSectionCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(PanelPurple, DeepPurple)))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            com.veyro.debloatengine.ui.components.LottieIcon(
                resId = com.veyro.debloatengine.ui.components.VeyroAnim.activity,
                color = VeyroPurpleBright,
                size = 24.dp
            )
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = VeyroWhite)
        }
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = VeyroPurpleBright)
        Spacer(Modifier.height(10.dp))
        content()
    }
}
