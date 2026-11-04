package com.veyro.debloatengine.ui.components

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty

/** Destinations and editable native labels for the Veyro bottom bar. */
enum class VeyroNavDestination(
    val label: String,
    internal val assetPath: String,
    internal val tintTo: Color? = null
) {
    Dashboard("Dashboard", "veyro/navigation/veyro-nav-dashboard.json"),
    Debloat("Debloat", "veyro/navigation/veyro-nav-debloat.json"),
    Modes("Modes", "veyro/navigation/veyro-nav-modes.json"),
    Tools("Tools", "veyro/navigation/veyro-nav-tools.json", Color(0xFFB798FF)),
}

private val VeyroBar = Color(0xFF0B0412)
private val VeyroPill = Color(0xFF261638)
private val VeyroActive = Color(0xFFB798FF)
private val VeyroInactive = Color(0xFFF6F2FF).copy(alpha = 0.72f)

/** Four-destination Veyro navigation bar; selection owned by the host. */
@Composable
fun VeyroBottomBar(
    selected: VeyroNavDestination,
    onSelect: (VeyroNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    animationsEnabled: Boolean = true,
) {
    val systemAnimatorScale = rememberSystemAnimatorScale()
    val motionEnabled = animationsEnabled && systemAnimatorScale > 0f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(VeyroBar)
            .selectableGroup()
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VeyroNavDestination.entries.forEach { destination ->
            VeyroNavButtonCore(
                modifier = Modifier.weight(1f),
                label = destination.label,
                assetPath = destination.assetPath,
                tintTo = destination.tintTo,
                selected = selected == destination,
                animationsEnabled = motionEnabled,
                systemAnimatorScale = systemAnimatorScale,
                onClick = { onSelect(destination) },
            )
        }
    }
}

@Composable
private fun VeyroNavButtonCore(
    modifier: Modifier,
    label: String,
    assetPath: String,
    tintTo: Color?,
    selected: Boolean,
    animationsEnabled: Boolean,
    systemAnimatorScale: Float,
    onClick: () -> Unit,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(assetPath))
    val lottie = rememberLottieAnimatable()
    var playRequest by remember(assetPath) { mutableStateOf(0) }

    val dynamicProperties = tintTo?.let {
        rememberLottieDynamicProperties(
            rememberLottieDynamicProperty(LottieProperty.STROKE_COLOR, it.toArgb(), "**"),
            rememberLottieDynamicProperty(LottieProperty.COLOR, it.toArgb(), "**")
        )
    }

    LaunchedEffect(playRequest, composition, animationsEnabled, systemAnimatorScale) {
        val loadedComposition = composition ?: return@LaunchedEffect
        if (playRequest == 0) return@LaunchedEffect
        if (!animationsEnabled || systemAnimatorScale == 0f) {
            lottie.snapTo(composition = loadedComposition, progress = 1f)
        } else {
            lottie.animate(
                composition = loadedComposition,
                iterations = 1,
                speed = 1f / systemAnimatorScale,
                initialProgress = 0f,
                ignoreSystemAnimationsDisabled = false,
            )
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && animationsEnabled) 0.94f else 1f,
        animationSpec = if (animationsEnabled) {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
        } else {
            snap()
        },
        label = "veyroNavPressScale",
    )
    val pillColor by animateColorAsState(
        targetValue = if (selected) VeyroPill else Color.Transparent,
        animationSpec = if (animationsEnabled) {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
        } else {
            snap()
        },
        label = "veyroNavPillColor",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) VeyroActive else VeyroInactive,
        animationSpec = if (animationsEnabled) {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
        } else {
            snap()
        },
        label = "veyroNavLabelColor",
    )

    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(24.dp))
            .background(pillColor)
            .selectable(
                selected = selected,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    playRequest += 1
                    onClick()
                },
            )
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LottieAnimation(
            composition = composition,
            progress = { lottie.progress },
            dynamicProperties = dynamicProperties,
            modifier = Modifier.size(22.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.width(3.dp))
        BasicText(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = labelColor,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
        )
    }
}

@Composable
private fun rememberSystemAnimatorScale(): Float {
    val resolver = LocalContext.current.contentResolver
    val scaleState = remember(resolver) {
        mutableStateOf(readSystemAnimatorScale(resolver))
    }

    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                scaleState.value = readSystemAnimatorScale(resolver)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            resolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
                false,
                observer,
            )
        }
        onDispose { resolver.unregisterContentObserver(observer) }
    }

    return scaleState.value
}

private fun readSystemAnimatorScale(resolver: ContentResolver): Float {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) return 1f
    val scale = runCatching {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }.getOrDefault(1f)
    return if (scale.isFinite()) scale.coerceAtLeast(0f) else 1f
}
