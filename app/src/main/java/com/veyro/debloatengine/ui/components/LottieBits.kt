package com.veyro.debloatengine.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty

/**
 * Lottie motion primitives.
 *
 * Two families live side by side:
 *  - UseAnimations stock icons (MIT) → retinted live to any accent color.
 *  - Custom Veyro-branded animations (res/raw/veyro_*.json) → pre-colored
 *    in the brand palette, rendered UNTINTED via [LottieIcon] with tint=false.
 */

/** Lottie icon; set [tint] = false for pre-colored branded animations. */
@Composable
fun LottieIcon(
    resId: Int,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    forever: Boolean = true,
    tint: Boolean = true
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resId))
    val dynamicProperties = if (tint) {
        rememberLottieDynamicProperties(
            rememberLottieDynamicProperty(LottieProperty.STROKE_COLOR, color.toArgb(), "**"),
            rememberLottieDynamicProperty(LottieProperty.COLOR, color.toArgb(), "**")
        )
    } else {
        null
    }
    LottieAnimation(
        composition = composition,
        iterations = if (forever) LottieConstants.IterateForever else 1,
        dynamicProperties = dynamicProperties,
        modifier = modifier.size(size)
    )
}

/** Lottie icon loaded from an assets path (branded pre-colored art). */
@Composable
fun LottieAssetIcon(
    assetPath: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    forever: Boolean = true
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(assetPath))
    LottieAnimation(
        composition = composition,
        iterations = if (forever) LottieConstants.IterateForever else 1,
        modifier = modifier.size(size)
    )
}

/**
 * Engine preloader — the custom branded Veyro animation.
 * Used while scanning packages, applying modes, running batches…
 */
@Composable
fun VeyroPreloader(
    label: String,
    sublabel: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LottieIcon(
            resId = VeyroAnim.veyroPreloader,
            color = accent,
            size = 56.dp,
            tint = false // branded artwork — keep its own palette
        )
        Spacer(Modifier.height(10.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        if (sublabel != null) {
            Spacer(Modifier.height(2.dp))
            Text(sublabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Raw-res ids for the bundled animation set. */
object VeyroAnim {
    // Stock UseAnimations (tinted at runtime)
    val loading = com.veyro.debloatengine.R.raw.anim_loading
    val checkmark = com.veyro.debloatengine.R.raw.anim_checkmark
    val trash = com.veyro.debloatengine.R.raw.anim_trash
    val power = com.veyro.debloatengine.R.raw.anim_power
    val zap = com.veyro.debloatengine.R.raw.anim_zap
    val battery = com.veyro.debloatengine.R.raw.anim_battery
    val cpu = com.veyro.debloatengine.R.raw.anim_cpu
    val lock = com.veyro.debloatengine.R.raw.anim_lock
    val search = com.veyro.debloatengine.R.raw.anim_search
    val download = com.veyro.debloatengine.R.raw.anim_download
    val activity = com.veyro.debloatengine.R.raw.anim_activity
    val shield = com.veyro.debloatengine.R.raw.anim_shield

    // Custom Veyro-branded (render untinted)
    val veyroPreloader = com.veyro.debloatengine.R.raw.veyro_preloader
    val veyroFlush = com.veyro.debloatengine.R.raw.veyro_cache_flush
    val veyroTweaker = com.veyro.debloatengine.R.raw.veyro_animation_tweaker
    val veyroFreeze = com.veyro.debloatengine.R.raw.veyro_freeze
    val veyroDisable = com.veyro.debloatengine.R.raw.veyro_disable
    val veyroUninstall = com.veyro.debloatengine.R.raw.veyro_uninstall
    val veyroGauge = com.veyro.debloatengine.R.raw.veyro_gauge

    // Tools Lab branded icons (assets, pre-colored)
    const val TOOL_COMPILER = "veyro/tools/veyro-tool-speed-compiler.json"
    const val TOOL_BATTERY = "veyro/tools/veyro-tool-battery-analyzer.json"
    const val TOOL_RAM_EXPANSION = "veyro/tools/veyro-tool-ram-expansion-auditor.json"
    const val TOOL_SENSORS = "veyro/tools/veyro-tool-sensor-privacy-kill-switch.json"
    const val TOOL_LOGS = "veyro/tools/veyro-tool-log-buffer-flush.json"
    const val TOOL_OVERLAYS = "veyro/tools/veyro-tool-runtime-resource-overlay-purger.json"
}
