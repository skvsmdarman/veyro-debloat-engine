package com.veyro.debloatengine.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.veyro.debloatengine.data.model.ShizukuState
import com.veyro.debloatengine.shizuku.ShizukuEngine
import com.veyro.debloatengine.ui.components.GaugeMeter
import com.veyro.debloatengine.ui.components.PressScale
import com.veyro.debloatengine.ui.components.RadarBadge
import com.veyro.debloatengine.ui.theme.DimText
import com.veyro.debloatengine.ui.theme.DeepPurple
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
 * Dashboard — animated RAM / Storage gauges, Shizuku radar badge,
 * Storage Unchoker flush, and the System Animation Tweaker with a
 * live speed-preview card.
 */
@Composable
fun DashboardScreen(
    state: VeyroUiState,
    onFlushCache: () -> Unit,
    onApplyScale: (Float) -> Unit,
    onRequestShizuku: () -> Unit,
    onSetDns: (com.veyro.debloatengine.data.repository.SystemTweaksRepository.DnsProvider) -> Unit,
    onDisableDns: () -> Unit,
    onForceRefreshRate: () -> Unit,
    onRestoreRefreshRate: () -> Unit,
    onForceRate: (Int) -> Unit,
    onOpenTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showShizukuGuide by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // ---- Shizuku status badge ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            PressScale(
                onClick = onRequestShizuku,
                enabled = state.shizukuState != ShizukuState.READY,
                modifier = Modifier.weight(1f)
            ) {
                RadarBadge(
                    active = state.shizukuState == ShizukuState.READY,
                    text = when (state.shizukuState) {
                        ShizukuState.READY -> "ENGINE ONLINE — Shizuku active"
                        ShizukuState.PERMISSION_DENIED -> "PERMISSION NEEDED — tap to grant"
                        ShizukuState.STOPPED -> "SHIZUKU STOPPED — start the service"
                        ShizukuState.NOT_INSTALLED -> "SHIZUKU NOT INSTALLED"
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            // Always available: re-open the setup tutorial, even when online.
            PressScale(onClick = onOpenTutorial) {
                Text(
                    "📖 Tutorial",
                    style = MaterialTheme.typography.labelSmall,
                    color = VeyroPurpleBright,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(VeyroPurple.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }
        if (state.shizukuState != ShizukuState.READY) {
            Spacer(Modifier.height(8.dp))
            ShizukuSetupCard(
                shizukuState = state.shizukuState,
                onOpenGuide = { showShizukuGuide = true },
                onGrant = onRequestShizuku
            )
        }

        Spacer(Modifier.height(18.dp))

        // ---- Performance gauges ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GaugeMeter(
                label = "RAM",
                valueText = "${state.ramInfo.usedPercent.toInt()}%",
                subText = "${state.ramInfo.usedGb}/${state.ramInfo.totalGb}G",
                progress = state.ramInfo.usedPercent / 100f,
                ringColor = if (state.ramInfo.usedPercent > 85f) VeyroRed else VeyroPurpleBright
            )
            GaugeMeter(
                label = "STORAGE",
                valueText = "${state.storageInfo.usedPercent.toInt()}%",
                subText = "${state.storageInfo.freeGb}G free",
                progress = state.storageInfo.usedPercent / 100f,
                ringColor = if (state.storageInfo.isThrottling) VeyroRed else VeyroGreen
            )
        }

        Spacer(Modifier.height(16.dp))

        // ---- Storage Unchoker ----
        AnimatedVisibility(
            visible = state.storageInfo.isThrottling,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(VeyroRed.copy(alpha = 0.12f))
                    .padding(14.dp)
            ) {
                Text(
                    "⚠ Flash throttling: storage is ${state.storageInfo.usedPercent.toInt()}% full. " +
                        "Your UFS/eMMC write speed degrades sharply past 85%. Flush the cache below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VeyroRed
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        EngineCard(
            title = "Storage Unchoker",
            subtitle = "Flash I/O speedup",
            iconRes = com.veyro.debloatengine.ui.components.VeyroAnim.veyroFlush,
            iconColor = VeyroGreen,
            tintIcon = false
        ) {
            Text(
                "One-tap safe system cache flush. Reclaims I/O cache via `pm trim-caches 1000G` " +
                    "— never touches photos, logins or apps.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(10.dp))
            PressScale(onClick = onFlushCache, enabled = state.shizukuState == ShizukuState.READY && !state.isFlushingCache) {
                Button(
                    onClick = onFlushCache,
                    enabled = state.shizukuState == ShizukuState.READY && !state.isFlushingCache,
                    colors = ButtonDefaults.buttonColors(containerColor = VeyroPurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isFlushingCache) {
                        com.veyro.debloatengine.ui.components.LottieIcon(
                            resId = com.veyro.debloatengine.ui.components.VeyroAnim.veyroFlush,
                            color = VeyroWhite,
                            size = 24.dp,
                            tint = false
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (state.isFlushingCache) "Flushing flash cache…" else "⚡ Safe System Cache Flush")
                }
            }
            state.flushMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = if (it.startsWith("Cache flush failed")) VeyroRed else VeyroGreen)
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- OS-Wide AdBlock DNS (Feature 4) ----
        EngineCard(
            title = "OS-Wide AdBlock DNS",
            subtitle = "Socket-level ad & tracker blocking — no VPN app",
            iconRes = com.veyro.debloatengine.ui.components.VeyroAnim.shield,
            iconColor = VeyroGreen
        ) {
            Text(
                "Sets an encrypted, low-latency Private DNS natively — in-app ads, pop-ups and " +
                    "telemetry across ALL apps are blocked without any background VPN draining battery.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.veyro.debloatengine.data.repository.SystemTweaksRepository.DnsProvider.entries.forEachIndexed { i, p ->
                    PressScale(
                        onClick = { onSetDns(p) },
                        enabled = state.shizukuState == ShizukuState.READY,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (state.dnsState.host == p.host) VeyroGreen.copy(alpha = 0.2f) else DeepPurple)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                p.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (state.dnsState.host == p.host) VeyroGreen else VeyroWhite
                            )
                            Text(
                                p.blurb,
                                style = MaterialTheme.typography.labelSmall,
                                color = DimText,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (state.dnsState.enabled) "✓ Active: ${state.dnsState.host}" else "Inactive — tap a provider to block ads system-wide",
                style = MaterialTheme.typography.labelSmall,
                color = if (state.dnsState.enabled) VeyroGreen else DimText
            )
            if (state.dnsState.enabled) {
                Spacer(Modifier.height(6.dp))
                PressScale(onClick = onDisableDns) {
                    Text(
                        "Disable filtering",
                        style = MaterialTheme.typography.labelSmall,
                        color = VeyroRed,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(VeyroRed.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
            state.dnsMessage?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.labelSmall, color = VeyroPurpleBright)
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- Display Smoother (Feature 5) ----
        EngineCard(
            title = "Display Smoother",
            subtitle = "Lock your panel's real refresh rate",
            iconRes = com.veyro.debloatengine.ui.components.VeyroAnim.veyroGauge,
            iconColor = VeyroPurpleBright,
            tintIcon = false
        ) {
            Text(
                "OEMs silently drop YouTube, Chrome and Maps to 60Hz to save power. Lock the " +
                    "display to any rate your panel supports — silky scrolling everywhere, zero jitter.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(10.dp))
            // One chip per supported rate, plus Auto.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val rates = state.refreshState.supportedRates.ifEmpty {
                    listOf(state.refreshState.devicePeakHz.toInt())
                }
                rates.forEach { hz ->
                    val active = state.refreshState.forced &&
                        state.refreshState.currentPeakHz.toInt() == hz
                    PressScale(
                        onClick = { onForceRate(hz) },
                        enabled = state.shizukuState == ShizukuState.READY,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) VeyroPurple else DeepPurple)
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${hz}Hz",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) VeyroWhite else VeyroPurpleBright
                            )
                        }
                    }
                }
                PressScale(
                    onClick = onRestoreRefreshRate,
                    enabled = state.shizukuState == ShizukuState.READY,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DeepPurple)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Auto", style = MaterialTheme.typography.labelLarge, color = DimText)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (state.refreshState.forced)
                    "✓ Locked: min ${state.refreshState.currentMinHz.toInt()}Hz / peak ${state.refreshState.currentPeakHz.toInt()}Hz"
                else
                    "Currently OEM-managed — panel supports ${state.refreshState.supportedRates.joinToString("/").ifBlank { "${state.refreshState.devicePeakHz.toInt()}Hz" }}",
                style = MaterialTheme.typography.labelSmall,
                color = if (state.refreshState.forced) VeyroGreen else DimText
            )
            if (state.shizukuState != ShizukuState.READY) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Connect Shizuku (tutorial at the top) to apply refresh-rate changes.",
                    style = MaterialTheme.typography.labelSmall,
                    color = VeyroAmber
                )
            }
            state.refreshMessage?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.labelSmall, color = VeyroPurpleBright)
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- System Animation Tweaker ----
        EngineCard(
            title = "System Animation Tweaker",
            subtitle = "0.5x Ultra-Fast mode",
            iconRes = com.veyro.debloatengine.ui.components.VeyroAnim.veyroTweaker,
            iconColor = VeyroPurpleBright,
            tintIcon = false
        ) {
            var sliderValue by remember(state.animationScale) { mutableStateOf(state.animationScale) }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PresetChip("0.0x\nOff", 0f, sliderValue) { sliderValue = 0f; onApplyScale(0f) }
                PresetChip("0.5x\nUltra-Fast", 0.5f, sliderValue) { sliderValue = 0.5f; onApplyScale(0.5f) }
                PresetChip("1.0x\nStock", 1.0f, sliderValue) { sliderValue = 1.0f; onApplyScale(1.0f) }
            }

            Spacer(Modifier.height(8.dp))
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onApplyScale(sliderValue) },
                valueRange = 0f..1.5f,
                steps = 9,
                colors = SliderDefaults.colors(
                    thumbColor = VeyroPurpleBright,
                    activeTrackColor = VeyroPurple,
                    inactiveTrackColor = DeepPurple
                )
            )
            Text(
                "Current: ${"%.2f".format(sliderValue)}x applied to window / transition / animator scales",
                style = MaterialTheme.typography.bodySmall,
                color = DimText
            )

            Spacer(Modifier.height(12.dp))
            AnimationPreviewCard(scale = sliderValue)
            state.scaleApplyMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = VeyroPurpleBright)
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    // ---- Guided Shizuku onboarding dialog ----
    if (showShizukuGuide) {
        ShizukuGuideDialog(
            onGrant = {
                showShizukuGuide = false
                onRequestShizuku()
            },
            onDismiss = { showShizukuGuide = false }
        )
    }
}

// ---------------------------------------------------------------------------
// Shizuku setup card + guided dialog
// ---------------------------------------------------------------------------
@Composable
private fun ShizukuSetupCard(
    shizukuState: ShizukuState,
    onOpenGuide: () -> Unit,
    onGrant: () -> Unit
) {
    PressScale(onClick = onOpenGuide) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(VeyroAmber.copy(alpha = 0.12f))
                .padding(14.dp)
        ) {
            Text(
                when (shizukuState) {
                    ShizukuState.NOT_INSTALLED -> "Shizuku is not installed yet"
                    ShizukuState.STOPPED -> "Shizuku is installed but not running"
                    else -> "Shizuku permission not granted yet"
                },
                style = MaterialTheme.typography.titleMedium,
                color = VeyroAmber
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Veyro uses Shizuku to run engine commands without root. Tap for the 3-step setup guide.",
                style = MaterialTheme.typography.bodySmall,
                color = DimText
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PressScale(onClick = onOpenGuide) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(VeyroAmber)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("📖 Setup Guide", style = MaterialTheme.typography.labelLarge, color = Color(0xFF0B0514))
                    }
                }
                PressScale(onClick = onGrant) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(DeepPurple)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            if (shizukuState == ShizukuState.PERMISSION_DENIED) "🔑 Grant Permission" else "Open Shizuku",
                            style = MaterialTheme.typography.labelLarge,
                            color = VeyroWhite
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShizukuGuideDialog(onGrant: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = VeyroWhite,
        title = { Text("Connect the Veyro Engine") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                GuideStep("1", "Install Shizuku", "Free, open-source, made by Rikka. Tap \"Install Shizuku\" below (Play Store) or get it from github.com/RikkaApps/Shizuku/releases.")
                GuideStep("2", "Start the service", "Android 11+: in the Shizuku app choose \"Wireless debugging\" — no PC ever needed. Android 8–10: start it once from a PC with adb.")
                GuideStep("3", "Grant Veyro access", "Come back here and tap \"Grant Permission\". Shizuku will show a one-time dialog — allow it.")
                GuideStep("✓", "Engine online", "The dashboard badge turns green and every engine feature unlocks: cache flush, debloat, modes and game boost.")
                Spacer(Modifier.height(8.dp))
                Text(
                    "No root required. Shizuku only runs shell commands while its service is on.",
                    style = MaterialTheme.typography.labelSmall,
                    color = VeyroGreen
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val playIntent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("market://details?id=moe.shizuku.privileged.api")
                    ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(playIntent)
                    } catch (_: Exception) {
                        try {
                            context.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://github.com/RikkaApps/Shizuku/releases")
                                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) { }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VeyroPurple)
            ) { Text("Install Shizuku", color = VeyroWhite) }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onGrant,
                    colors = ButtonDefaults.buttonColors(containerColor = VeyroGreen)
                ) { Text("Grant Permission", color = Color(0xFF0B0514)) }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = DimText
                    )
                ) { Text("Close") }
            }
        }
    )
}

@Composable
private fun GuideStep(num: String, title: String, body: String) {
    Row(Modifier.padding(vertical = 6.dp)) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(VeyroPurple.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Text(num, style = MaterialTheme.typography.labelLarge, color = VeyroPurpleBright)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = VeyroWhite)
            Text(body, style = MaterialTheme.typography.bodySmall, color = DimText)
        }
    }
}

// ---------------------------------------------------------------------------
// Card container with gradient sheen
// ---------------------------------------------------------------------------
@Composable
private fun EngineCard(
    title: String,
    subtitle: String,
    iconRes: Int? = null,
    iconColor: Color = VeyroPurpleBright,
    tintIcon: Boolean = true,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(listOf(PanelPurple, DeepPurple), start = Offset.Zero, end = Offset.Infinite)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (iconRes != null) {
                com.veyro.debloatengine.ui.components.LottieIcon(
                    resId = iconRes,
                    color = iconColor,
                    size = 26.dp,
                    tint = tintIcon
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium, color = VeyroWhite)
        }
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = VeyroPurpleBright)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun PresetChip(text: String, value: Float, current: Float, onClick: () -> Unit) {
    val selected = kotlin.math.abs(current - value) < 0.01f
    PressScale(onClick = onClick) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) VeyroPurple else DeepPurple)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) VeyroWhite else DimText,
                modifier = Modifier.padding(2.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Live animation-speed preview: a dot sweeping at the chosen speed
// ---------------------------------------------------------------------------
@Composable
private fun AnimationPreviewCard(scale: Float) {
    val x = remember { Animatable(0f) }
    LaunchedEffect(scale) {
        val duration = (700 * scale).toInt().coerceAtLeast(1)
        x.snapTo(0f)
        // Animate 2 cycles so the speed difference is clearly visible.
        repeat(2) {
            x.animateTo(1f, tween(duration, easing = androidx.compose.animation.core.FastOutSlowInEasing))
            x.animateTo(0f, tween(duration, easing = androidx.compose.animation.core.FastOutSlowInEasing))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF120A22))
            .padding(12.dp)
    ) {
        Text("Live preview — same motion at current speed", style = MaterialTheme.typography.labelSmall, color = DimText)
        Spacer(Modifier.height(10.dp))
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DeepPurple)
        ) {
            val trackWidth = maxWidth - 36.dp - 8.dp
            val transition = rememberInfiniteTransition(label = "trackpulse")
            val glow by transition.animateFloat(
                initialValue = 0.6f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                label = "glow"
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp + trackWidth * x.value)
                    .size(28.dp)
                    .scale(glow)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(VeyroWhite, VeyroPurple)))
            )
        }
    }
}
