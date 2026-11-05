package com.veyro.debloatengine.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.veyro.debloatengine.data.repository.ToolsRepository
import com.veyro.debloatengine.ui.components.LottieIcon
import com.veyro.debloatengine.ui.components.PressScale
import com.veyro.debloatengine.ui.components.VeyroAnim
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
 * Tools — hardware & system lab:
 * Speed Compiler (AOT), battery analyzer, thermal status, RAM Expansion
 * auditor, sensor kill-switch, logcat flush, RRO overlay purger.
 */
@Composable
fun ToolsScreen(
    state: VeyroUiState,
    onRunCompiler: () -> Unit,
    onToggleSensors: (Boolean) -> Unit,
    onFlushLogs: () -> Unit,
    onRefreshTools: () -> Unit,
    onSetRamExpansion: (Int) -> Unit,
    onToggleOverlay: (ToolsRepository.OverlayInfo, Boolean) -> Unit,
    onOpenTutorial: () -> Unit,
    onScanOrphans: () -> Unit,
    onDeleteOrphan: (String) -> Unit,
    onDeleteAllOrphans: () -> Unit,
    onToggleStatusIcon: (String) -> Unit,
    onResetStatusIcons: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tools Lab", style = MaterialTheme.typography.headlineMedium, color = VeyroWhite, modifier = Modifier.weight(1f))
            PressScale(onClick = onRefreshTools) {
                Text(
                    "↻ Refresh",
                    style = MaterialTheme.typography.labelSmall,
                    color = VeyroPurpleBright,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(VeyroPurple.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }
        Text(
            "Hardware-level diagnostics and OS surgery — every action reversible.",
            style = MaterialTheme.typography.bodySmall,
            color = DimText
        )
        Spacer(Modifier.height(10.dp))

        // ---- Shizuku tutorial card (always available, even when online) ----
        PressScale(onClick = onOpenTutorial) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF261638), DeepPurple))
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📘", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Shizuku Setup Tutorial",
                        style = MaterialTheme.typography.titleMedium,
                        color = VeyroWhite
                    )
                    Text(
                        if (state.shizukuState == com.veyro.debloatengine.data.model.ShizukuState.READY)
                            "Engine online ✓ — tap anytime to re-read the guide"
                        else
                            "Not connected — the 4-step guide takes ~1 minute. Includes the Play Store link.",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.shizukuState == com.veyro.debloatengine.data.model.ShizukuState.READY) VeyroGreen else VeyroAmber
                    )
                }
                Text("›", style = MaterialTheme.typography.headlineMedium, color = VeyroPurpleBright)
            }
        }
        Spacer(Modifier.height(10.dp))

        state.toolsNotice?.let { notice ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(VeyroAmber.copy(alpha = 0.12f))
                    .padding(10.dp)
            ) {
                Text(notice, style = MaterialTheme.typography.labelSmall, color = VeyroAmber)
            }
            Spacer(Modifier.height(10.dp))
        }

        // ---- Thermal status banner ----
        ThermalBanner(state.thermal)

        // ---- Speed Compiler ----
        ToolCard(
            title = "Speed Compiler",
            subtitle = "AOT profile compilation — 30-50% faster cold starts",
            iconAsset = com.veyro.debloatengine.ui.components.VeyroAnim.TOOL_COMPILER
        ) {
            Text(
                "Recompiles every app's hot paths into native CPU code. Run after big app updates " +
                    "or when scrolling stutters appear.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(10.dp))
            PressScale(onClick = onRunCompiler, enabled = !state.isCompiling) {
                Button(
                    onClick = onRunCompiler,
                    enabled = !state.isCompiling,
                    colors = ButtonDefaults.buttonColors(containerColor = VeyroPurple),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (state.isCompiling) "Compiling…" else "🚀 Compile speed-profile (all apps)") }
            }
        }

        // ---- Battery analyzer ----
        ToolCard(
            title = "Battery Analyzer",
            subtitle = "Cycle count • real charging watts • temperature",
            iconAsset = com.veyro.debloatengine.ui.components.VeyroAnim.TOOL_BATTERY
        ) {
            val b = state.battery
            if (b.level < 0) {
                Text("Tap refresh to read battery registers.", style = MaterialTheme.typography.bodySmall, color = DimText)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("Level", "${b.level}%")
                    StatPill("Temp", "${b.temperatureC}°C", hot = b.temperatureC > 40f)
                    StatPill("Now", "${"%.0f".format(b.chargingWatts)} W")
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("Voltage", "${b.voltageMv} mV")
                    StatPill("Current", "${"%.0f".format(kotlin.math.abs(b.currentMa))} mA")
                    StatPill(
                        "Cycles",
                        if (b.cycleCount >= 0) "${b.cycleCount}" else "n/a (OEM hides it)"
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Health: ${b.health}" + if (b.temperatureC > 43f) " — ⚠ hot, charging slowed" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (b.temperatureC > 43f) VeyroRed else DimText
                )
            }
        }

        // ---- RAM Expansion auditor ----
        ToolCard(
            title = "RAM Expansion Auditor",
            subtitle = "OEM \"Virtual RAM\" swaps onto slow flash",
            iconAsset = com.veyro.debloatengine.ui.components.VeyroAnim.TOOL_RAM_EXPANSION
        ) {
            val s = state.swap
            Text(
                if (s.active)
                    "Swap: ${s.usedMb} MB of ${s.swapTotalMb} MB used" +
                        if (s.heavyUse) " — ⚠ heavy swapping is causing I/O micro-stutters" else ""
                else "No swap/zram active — pure RAM, no flash swapping.",
                style = MaterialTheme.typography.bodySmall,
                color = if (s.heavyUse) VeyroRed else DimText
            )
            s.raw.forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = DimText, maxLines = 1) }
            Spacer(Modifier.height(8.dp))
            var slider by remember { mutableStateOf(0f) }
            Text(
                "Set RAM Expansion: ${slider.toInt()} GB (best-effort, OEM-dependent — reboot to apply)",
                style = MaterialTheme.typography.labelSmall,
                color = DimText
            )
            Slider(
                value = slider,
                onValueChange = { slider = it },
                steps = 7,
                valueRange = 0f..8f,
                colors = SliderDefaults.colors(thumbColor = VeyroPurpleBright, activeTrackColor = VeyroPurple)
            )
            PressScale(onClick = { onSetRamExpansion(slider.toInt()) }) {
                Button(
                    onClick = { onSetRamExpansion(slider.toInt()) },
                    colors = ButtonDefaults.buttonColors(containerColor = VeyroPurple),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Apply ${slider.toInt()} GB (reboot needed)") }
            }
        }

        // ---- Sensor kill-switch ----
        ToolCard(
            title = "Sensor Privacy Kill-Switch",
            subtitle = "Global cutoff for motion sensors",
            iconAsset = com.veyro.debloatengine.ui.components.VeyroAnim.TOOL_SENSORS
        ) {
            Text(
                "Disables accelerometer, gyro, light and step sensors system-wide — stops movement " +
                    "tracking and sensor battery drain. Turn on when the phone is idle on a desk; " +
                    "auto-rotate will pause while active.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PressScale(onClick = { onToggleSensors(true) }, modifier = Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (state.sensorsOff) VeyroRed else DeepPurple)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("Sensors OFF", color = VeyroWhite, style = MaterialTheme.typography.labelLarge) }
                }
                PressScale(onClick = { onToggleSensors(false) }, modifier = Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!state.sensorsOff) VeyroGreen else DeepPurple)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("Sensors ON", color = Color(0xFF0B0514), style = MaterialTheme.typography.labelLarge) }
                }
            }
        }

        // ---- Logcat flush ----
        ToolCard(
            title = "Log Buffer Flush",
            subtitle = "Clear debug spam + shrink 16MB buffers to 64KB",
            iconAsset = com.veyro.debloatengine.ui.components.VeyroAnim.TOOL_LOGS
        ) {
            Text(
                "OEM diagnostic services constantly write megabytes of logs to RAM buffers, burning " +
                    "CPU cycles even with the screen off. Flush clears them and shrinks the buffer.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(10.dp))
            PressScale(onClick = onFlushLogs) {
                Button(
                    onClick = onFlushLogs,
                    colors = ButtonDefaults.buttonColors(containerColor = VeyroPurple),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🧹 Clear logs + shrink buffer to 64K") }
            }
        }

        // ---- RRO overlay purger ----
        ToolCard(
            title = "Runtime Resource Overlay Purger",
            subtitle = "Disables carrier/theme injected overlays",
            iconAsset = com.veyro.debloatengine.ui.components.VeyroAnim.TOOL_OVERLAYS
        ) {
            val intrusive = state.overlays.filter { it.enabled && ToolsRepository.isIntrusiveOverlay(it.packageName) }
            Text(
                "OEMs bundle hidden carrier & theme overlays that inject brand styles and boot " +
                    "splashes. Purge them or toggle any overlay below.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            if (state.overlays.isEmpty()) {
                Text("Pull to refresh to list overlays.", style = MaterialTheme.typography.labelSmall, color = DimText)
            } else {
                if (intrusive.isNotEmpty()) {
                    Text(
                        "${intrusive.size} intrusive overlay(s) detected",
                        style = MaterialTheme.typography.labelSmall,
                        color = VeyroAmber
                    )
                }
                state.overlays.filter { it.enabled }.take(8).forEach { ov ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                ov.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = VeyroWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "→ ${ov.isTarget}" + if (ToolsRepository.isIntrusiveOverlay(ov.packageName)) " • intrusive" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (ToolsRepository.isIntrusiveOverlay(ov.packageName)) VeyroAmber else DimText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        PressScale(onClick = { onToggleOverlay(ov, false) }) {
                            Text(
                                "Disable",
                                style = MaterialTheme.typography.labelSmall,
                                color = VeyroRed,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(VeyroRed.copy(alpha = 0.12f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                state.overlays.filter { !it.enabled }.take(4).forEach { ov ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            ov.packageName,
                            style = MaterialTheme.typography.labelSmall,
                            color = DimText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        PressScale(onClick = { onToggleOverlay(ov, true) }) {
                            Text(
                                "Re-enable",
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
            }
        }

        // ---- Orphaned data cleaner (Scoped Storage unblocker) ----
        ToolCard(
            title = "Orphaned Data Cleaner",
            subtitle = "Frees junk regular cleaners can't even see",
            iconAsset = com.veyro.debloatengine.ui.components.VeyroAnim.TOOL_LOGS
        ) {
            Text(
                "When you uninstall a game or heavy app, gigabytes stay trapped in " +
                    "Android/data and Android/obb — locked by Scoped Storage. Shell access " +
                    "bypasses those locks: scan, see the size, delete with one tap.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(10.dp))
            PressScale(onClick = onScanOrphans, enabled = !state.isScanningOrphans) {
                Button(
                    onClick = onScanOrphans,
                    enabled = !state.isScanningOrphans,
                    colors = ButtonDefaults.buttonColors(containerColor = VeyroPurple),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (state.isScanningOrphans) "Scanning Android/data…" else "🔍 Scan for orphaned junk") }
            }
            state.orphanMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (state.orphans.isEmpty()) VeyroGreen else VeyroAmber
                )
            }
            if (state.orphans.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                PressScale(onClick = onDeleteAllOrphans) {
                    Button(
                        onClick = onDeleteAllOrphans,
                        colors = ButtonDefaults.buttonColors(containerColor = VeyroRed),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("🗑 Delete ALL orphans") }
                }
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier.height(210.dp).verticalScroll(rememberScrollState())
                ) {
                    state.orphans.forEach { orphan ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    orphan.packageName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = VeyroWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${orphan.kind} • " + if (orphan.sizeMb >= 1024)
                                        "%.1f GB".format(orphan.sizeMb / 1024f)
                                    else "${orphan.sizeMb} MB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (orphan.sizeMb > 500) VeyroRed else DimText
                                )
                            }
                            PressScale(onClick = { onDeleteOrphan(orphan.path) }) {
                                Text(
                                    "Delete",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VeyroRed,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(VeyroRed.copy(alpha = 0.12f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---- Status bar cleaner ----
        ToolCard(
            title = "Status Bar Cleaner",
            subtitle = "Hide permanent clutter icons — Pixel-style minimalism",
            iconAsset = com.veyro.debloatengine.ui.components.VeyroAnim.TOOL_SENSORS
        ) {
            Text(
                "Hides useless always-on icons (VoLTE, VoWiFi, NFC…) via Android's internal " +
                    "icon_blacklist — without affecting any functionality. Instant, reversible.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                com.veyro.debloatengine.data.repository.SystemTweaksRepository.STATUS_BAR_ICONS.forEach { (key, label) ->
                    val hidden = key in state.hiddenStatusIcons
                    PressScale(onClick = { onToggleStatusIcon(key) }) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hidden) Color(0xFF0B0514) else DimText,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (hidden) VeyroRed else DeepPurple)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (state.hiddenStatusIcons.isEmpty()) "Nothing hidden — tap an icon to hide it"
                else "Hidden: ${state.hiddenStatusIcons.joinToString(", ")} — tap again to un-hide",
                style = MaterialTheme.typography.labelSmall,
                color = if (state.hiddenStatusIcons.isEmpty()) DimText else VeyroGreen
            )
            if (state.hiddenStatusIcons.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                PressScale(onClick = onResetStatusIcons) {
                    Text(
                        "Reset — show every icon again",
                        style = MaterialTheme.typography.labelSmall,
                        color = VeyroPurpleBright,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(VeyroPurple.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        state.toolsLog.takeLast(4).forEach {
            Text("• $it", style = MaterialTheme.typography.labelSmall, color = VeyroGreen)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ThermalBanner(thermal: ToolsRepository.ThermalInfo) {
    if (thermal.severity == 0) return
    val color = when (thermal.severity) {
        1 -> VeyroAmber
        2 -> VeyroAmber
        else -> VeyroRed
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(12.dp)
    ) {
        Text(
            "🌡 Thermal: ${thermal.statusLabel.uppercase()} — CPU/GPU clock throttling active",
            style = MaterialTheme.typography.labelLarge,
            color = color
        )
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun ToolCard(
    title: String,
    subtitle: String,
    iconAsset: String? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(PanelPurple, DeepPurple)))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (iconAsset != null) {
                com.veyro.debloatengine.ui.components.LottieAssetIcon(
                    assetPath = iconAsset,
                    size = 28.dp
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium, color = VeyroWhite)
        }
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = VeyroPurpleBright)
        Spacer(Modifier.height(10.dp))
        content()
    }
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun StatPill(label: String, value: String, hot: Boolean = false) {
    Column(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DeepPurple)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = DimText)
        Text(value, style = MaterialTheme.typography.labelLarge, color = if (hot) VeyroRed else VeyroWhite)
    }
}
