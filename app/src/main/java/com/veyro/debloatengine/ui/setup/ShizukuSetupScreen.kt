@file:Suppress("UnusedMaterial3ScaffoldPaddingParameter")

package com.veyro.debloatengine.ui.setup

import com.veyro.debloatengine.R
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

private val VeyroBg = Color(0xFF11091B)
private val VeyroPanel = Color(0xFF1B1028)
private val VeyroPanelRaised = Color(0xFF241535)
private val VeyroPurple = Color(0xFF9158F6)
private val VeyroLilac = Color(0xFFB897FF)
private val VeyroText = Color(0xFFF7F2FF)
private val VeyroMuted = Color(0xFFB6A9C8)
private val VeyroGreen = Color(0xFF48DC8B)
private val VeyroAmber = Color(0xFFFFD766)

private data class SetupStep(
    val title: String,
    val subtitle: String,
    val label: String,
    val hint: String,
    val detailTitle: String,
    val detailSubtitle: String,
    val instructions: List<String>,
)

private val setupSteps = listOf(
    SetupStep(
        title = "Install Shizuku",
        subtitle = "Start with the companion service Veyro needs to connect.",
        label = "INSTALL",
        hint = "Usually under a minute",
        detailTitle = "Get Shizuku on your phone",
        detailSubtitle = "Install the app, then open it once so its welcome guide is ready.",
        instructions = listOf(
            "Open Google Play and search for Shizuku by Rikka, or use the official GitHub releases.",
            "Install Shizuku, open it once, and allow its intro permissions.",
            "Veyro’s Dashboard → 📖 Setup Guide → Install Shizuku button handles this automatically.",
        ),
    ),
    SetupStep(
        title = "Start the service",
        subtitle = "Use Wireless debugging to start Shizuku—no root access needed.",
        label = "PAIR DEVICE",
        hint = "One-time pairing · about 30 sec",
        detailTitle = "Pair with Wireless debugging",
        detailSubtitle = "Keep Shizuku open and follow these steps on your phone. Settings names vary by Android brand.",
        instructions = listOf(
            "In Shizuku, tap Start via Wireless debugging to open its mini-guide.",
            "If needed: Settings → About phone → Version or Software information → tap Build number 7 times. Enter your screen lock if asked.",
            "Open Developer options (often under Additional settings) and turn Wireless debugging on.",
            "Tap Pair device with pairing code. A 6-digit code appears.",
            "Tap Shizuku’s pairing notification, enter the code, then tap Pair.",
            "Return to Shizuku and tap Start. Check that the service says it is running.",
        ),
    ),
    SetupStep(
        title = "Allow Veyro access",
        subtitle = "Approve one Shizuku prompt so the engine can connect.",
        label = "GRANT ACCESS",
        hint = "One-time permission",
        detailTitle = "Connect Veyro to Shizuku",
        detailSubtitle = "Android shows this permission through Shizuku. You can revoke it later in Shizuku.",
        instructions = listOf(
            "Open Veyro Debloat Engine.",
            "On the dashboard, tap PERMISSION NEEDED — tap to grant.",
            "When Shizuku shows its permission dialog, tap Allow.",
            "Look for the green ENGINE ONLINE — Shizuku active badge.",
        ),
    ),
    SetupStep(
        title = "You’re ready",
        subtitle = "The engine can now use its supported tools without root.",
        label = "ENGINE ONLINE",
        hint = "Setup complete",
        detailTitle = "Use the Veyro engine",
        detailSubtitle = "Open the matching Veyro tabs and choose the action you need.",
        instructions = listOf(
            "Freeze or debloat apps from the Debloat tab.",
            "Use Cache Flush, ad-block DNS, game modes, and Tools Lab.",
            "After a phone reboot, start Shizuku again using Step 2. Re-pair only if needed.",
        ),
    ),
)

/**
 * Drop-in, native Compose walkthrough for Veyro's Setup Guide.
 * The host should keep its own navigation and pass [onClose] and [onFinished].
 * Lottie illustrations are loaded from the host app's assets directory.
 */
@Composable
fun ShizukuSetupScreen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    onFinished: () -> Unit = {},
    onInstallShizuku: (() -> Unit)? = null,
    animationsEnabled: Boolean = true,
) {
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var showingOverview by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val installClick = onInstallShizuku ?: { openShizukuPlayStore(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isResumed = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> isResumed = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val animatorDurationScaleEnabled = rememberAnimatorDurationScaleEnabled()
    val lottieAnimationsAllowed = animationsEnabled && isResumed && animatorDurationScaleEnabled

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1B1027), VeyroBg, Color(0xFF100817)),
                ),
            ),
    ) {
        SetupTopBar(
            onBack = onClose,
            onHelp = { showingOverview = true },
        )

        AnimatedContent(
            targetState = showingOverview,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                val motionAllowed = animationsEnabled && animatorDurationScaleEnabled
                fadeIn(tween(if (motionAllowed) 160 else 0, easing = FastOutSlowInEasing)) togetherWith
                    fadeOut(tween(if (motionAllowed) 100 else 0)) using SizeTransform(clip = false)
            },
            label = "setup overview transition",
        ) { overview ->
            if (overview) {
                OverviewContent(
                    currentStep = currentStep,
                    onSelectStep = {
                        currentStep = it
                        showingOverview = false
                    },
                    onCloseOverview = { showingOverview = false },
                )
            } else {
                StepContent(
                    stepIndex = currentStep,
                    onSeeAllSteps = { showingOverview = true },
                    onInstallShizuku = installClick,
                    animationsAllowed = lottieAnimationsAllowed,
                )
            }
        }

        if (showingOverview) {
            OverviewFooter(
                currentStep = currentStep,
                onBack = { showingOverview = false },
                onContinue = { showingOverview = false },
            )
        } else {
            StepFooter(
                currentStep = currentStep,
                onBack = { if (currentStep > 0) currentStep-- },
                onNext = {
                    if (currentStep < setupSteps.lastIndex) currentStep++ else onFinished()
                },
            )
        }
    }
}

@Composable
private fun SetupTopBar(onBack: () -> Unit, onHelp: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onBack),
            color = VeyroPanelRaised,
            contentColor = VeyroLilac,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("‹", fontSize = 29.sp, lineHeight = 30.sp, fontWeight = FontWeight.Light)
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("VEYRO · SETUP GUIDE", color = VeyroLilac, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.05.sp)
            Text("Shizuku connection", color = VeyroText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Surface(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onHelp),
            color = VeyroPanelRaised,
            contentColor = VeyroMuted,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("?", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StepContent(
    stepIndex: Int,
    onSeeAllSteps: () -> Unit,
    onInstallShizuku: () -> Unit,
    animationsAllowed: Boolean,
) {
    val step = setupSteps[stepIndex]
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 7.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "STEP ${stepIndex + 1}  /  04",
                color = VeyroMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.1.sp,
            )
            TextButton(onClick = onSeeAllSteps, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp, vertical = 4.dp)) {
                Text("See all steps  →", color = VeyroLilac, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 17.dp)) {
            repeat(setupSteps.size) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (index <= stepIndex) VeyroPurple else Color(0xFF342444)),
                )
            }
        }
        Text(step.title, color = VeyroText, fontSize = 27.sp, lineHeight = 31.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.45).sp)
        Text(step.subtitle, color = VeyroMuted, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 6.dp, bottom = 13.dp))
        TutorialIllustration(stepIndex = stepIndex, hint = step.hint, animationsAllowed = animationsAllowed)
        Spacer(Modifier.height(14.dp))
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(
                    modifier = Modifier
                        .size(27.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color(0xFF37204F)),
                    contentAlignment = Alignment.Center,
                ) {
                    StepIcon(stepIndex = stepIndex, modifier = Modifier.size(16.dp))
                }
                Text(step.detailTitle, color = VeyroText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text(step.detailSubtitle, color = VeyroMuted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 7.dp, bottom = 13.dp))
            step.instructions.forEachIndexed { index, instruction ->
                NumberedInstruction(index = index + 1, text = instruction)
                if (index != step.instructions.lastIndex) Spacer(Modifier.height(10.dp))
            }
            when (stepIndex) {
                0 -> {
                    Spacer(Modifier.height(14.dp))
                    Text("Or install from", color = VeyroMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 7.dp)) {
                        OutlinedButton(
                            onClick = onInstallShizuku,
                            shape = RoundedCornerShape(13.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF63458B)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VeyroLilac),
                        ) { Text("Google Play", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = { openShizukuGitHub(context) },
                            shape = RoundedCornerShape(13.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF63458B)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VeyroLilac),
                        ) { Text("GitHub APK", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }
                1 -> {
                    Spacer(Modifier.height(13.dp))
                    NoteCard("After a reboot", "Start Shizuku again using Step 2. Pairing usually only needs to be done once.", tone = NoteTone.Warm)
                }
                2 -> {
                    Spacer(Modifier.height(13.dp))
                    NoteCard("Check the status", "In Veyro, confirm the real badge reads ENGINE ONLINE — Shizuku active.", tone = NoteTone.Green)
                }
                3 -> {
                    Spacer(Modifier.height(16.dp))
                    TroubleshootingSection()
                    Spacer(Modifier.height(11.dp))
                    RootedPhoneCard()
                    Spacer(Modifier.height(10.dp))
                    SummaryCard()
                }
            }
        }
    }
}

@Composable
private fun NumberedInstruction(index: Int, text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(
            modifier = Modifier
                .size(21.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0xFF302040)),
            contentAlignment = Alignment.Center,
        ) {
            Text(index.toString(), color = VeyroLilac, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        }
        Text(text, color = Color(0xFFE9E1F2), fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
    }
}

private val setupIllustrationAssets = listOf(
    "veyro_setup_install.json",
    "veyro_setup_pair.json",
    "veyro_setup_permission.json",
    "veyro_setup_engine.json",
)

private val setupStepIconIds = listOf(
    R.drawable.veyro_ic_install,
    R.drawable.veyro_ic_pair,
    R.drawable.veyro_ic_permission,
    R.drawable.veyro_ic_engine,
)

private const val STATIC_ILLUSTRATION_PROGRESS = 0.6f

@Composable
private fun StepIcon(stepIndex: Int, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = setupStepIconIds[stepIndex]),
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
private fun rememberAnimatorDurationScaleEnabled(): Boolean {
    val context = LocalContext.current
    val enabledState = remember(context) {
        mutableStateOf(readAnimatorDurationScaleEnabled(context))
    }
    DisposableEffect(context, enabledState) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                enabledState.value = readAnimatorDurationScaleEnabled(context)
            }
        }
        val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        runCatching { context.contentResolver.registerContentObserver(uri, false, observer) }
        onDispose { runCatching { context.contentResolver.unregisterContentObserver(observer) } }
    }
    return enabledState.value
}

private fun readAnimatorDurationScaleEnabled(context: android.content.Context): Boolean =
    runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }.getOrDefault(true)

@Composable
private fun TutorialIllustration(stepIndex: Int, hint: String, animationsAllowed: Boolean) {
    val assetPath = setupIllustrationAssets[stepIndex]
    val compositionResult = rememberLottieComposition(LottieCompositionSpec.Asset(assetPath))
    val composition by compositionResult
    val animatedProgress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = animationsAllowed,
        iterations = LottieConstants.IterateForever,
    )
    val progress = if (animationsAllowed) animatedProgress else STATIC_ILLUSTRATION_PROGRESS

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(720f / 420f)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.radialGradient(listOf(Color(0xFF301A48), Color(0xFF21112F), Color(0xFF1A1026))))
            .border(1.dp, Color.White.copy(alpha = .06f), RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // Keep a recognizable, static placeholder visible while the asset loads or if it fails.
            StepIcon(stepIndex = stepIndex, modifier = Modifier.size(84.dp))
        }
        HintPill(
            text = hint,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 12.dp),
        )
        HintPill(
            text = when (stepIndex) {
                0 -> "Open it once"
                1 -> "Pair in notification"
                2 -> "One-time approval"
                else -> "Ready when you are"
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 28.dp),
            accent = if (stepIndex >= 2) VeyroGreen else VeyroLilac,
        )
    }
}

@Composable
private fun HintPill(text: String, modifier: Modifier = Modifier, accent: Color = VeyroLilac) {
    Surface(
        modifier = modifier,
        color = Color(0xEE211431),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .10f)),
        shadowElevation = 5.dp,
    ) {
        Text(text, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp))
    }
}

@Composable
private fun SectionCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF21142F), VeyroPanel)))
            .border(1.dp, Color.White.copy(alpha = .045f), RoundedCornerShape(20.dp))
            .padding(15.dp),
        content = { content() },
    )
}

private enum class NoteTone { Green, Warm }

@Composable
private fun NoteCard(title: String, body: String, tone: NoteTone) {
    val background = if (tone == NoteTone.Green) Color(0xFF112A20) else Color(0xFF302418)
    val accent = if (tone == NoteTone.Green) VeyroGreen else VeyroAmber
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(background).padding(11.dp)) {
        Text(title, color = accent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        Text(body, color = Color(0xFFE0D4E8), fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun TroubleshootingSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(Color(0xFF191125))
            .border(1.dp, Color.White.copy(alpha = .06f), RoundedCornerShape(17.dp)),
    ) {
        Text("Quick troubleshooting", color = VeyroText, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp))
        TroubleshootingRow("“Pairing failed”", "Turn Wireless debugging off, then on again. Try pairing immediately.")
        TroubleshootingRow("Shizuku stops after reboot", "Repeat Step 2 to start the service again.")
        TroubleshootingRow("Veyro says “Shizuku stopped”", "Open Shizuku and tap Start again.", last = true)
    }
}

@Composable
private fun TroubleshootingRow(title: String, body: String, last: Boolean = false) {
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (!last) Modifier.border(width = .5.dp, color = Color.White.copy(alpha = .07f)) else Modifier)
            .padding(horizontal = 13.dp, vertical = 10.dp),
    ) {
        Text(title, color = VeyroText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(body, color = VeyroMuted, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun RootedPhoneCard() {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF21142F)).border(1.dp, VeyroPurple.copy(alpha = .35f), RoundedCornerShape(16.dp)).padding(13.dp)) {
        Text("Have a rooted phone?", color = VeyroLilac, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        Text("Skip Wireless debugging. In Shizuku, choose Start via root, then grant Veyro permission in Step 3.", color = Color(0xFFD0C2DD), fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
private fun SummaryCard() {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF171021)).padding(12.dp)) {
        Text("IN ONE LINE", color = VeyroLilac, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = .8.sp)
        Text("Install Shizuku → enable Wireless debugging → pair the code in Shizuku’s notification → Start → open Veyro → Allow.", color = Color(0xFFD8CBE6), fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun OverviewContent(currentStep: Int, onSelectStep: (Int) -> Unit, onCloseOverview: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 7.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("YOUR SETUP PATH", color = VeyroMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            TextButton(onClick = onCloseOverview, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp, vertical = 4.dp)) {
                Text("Close  ×", color = VeyroLilac, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text("See all steps", color = VeyroText, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-.45).sp)
        Text("A quick map of setup. Tap any step to open its animated walkthrough.", color = VeyroMuted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 5.dp, bottom = 14.dp))
        setupSteps.forEachIndexed { index, step ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (index == currentStep) Color(0xFF28183A) else Color(0xFF20142D))
                    .border(1.dp, if (index == currentStep) VeyroPurple.copy(alpha = .3f) else Color.White.copy(alpha = .06f), RoundedCornerShape(16.dp))
                    .clickable { onSelectStep(index) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(if (index == currentStep) VeyroPurple else Color(0xFF37204D)), contentAlignment = Alignment.Center) {
                    StepIcon(stepIndex = index, modifier = Modifier.size(19.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(step.title, color = VeyroText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(step.hint, color = VeyroMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
                }
                Text("›", color = VeyroLilac, fontSize = 20.sp)
            }
        }
        Spacer(Modifier.height(7.dp))
        TroubleshootingSection()
        Spacer(Modifier.height(10.dp))
        RootedPhoneCard()
        Spacer(Modifier.height(10.dp))
        SummaryCard()
    }
}

@Composable
private fun StepFooter(currentStep: Int, onBack: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .background(VeyroBg.copy(alpha = .96f))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onBack,
            enabled = currentStep > 0,
            modifier = Modifier.width(96.dp).height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD1C3DF), disabledContentColor = Color(0xFF786E83)),
        ) { Text("Back", fontWeight = FontWeight.Bold) }
        Button(
            onClick = onNext,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VeyroPurple, contentColor = Color.White),
        ) { Text(if (currentStep == setupSteps.lastIndex) "Finish setup" else "Next step", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun OverviewFooter(currentStep: Int, onBack: () -> Unit, onContinue: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .background(VeyroBg.copy(alpha = .96f))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(onClick = onBack, modifier = Modifier.width(132.dp).height(48.dp), shape = RoundedCornerShape(16.dp)) {
            Text("Back to step", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Button(onClick = onContinue, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = VeyroPurple)) {
            Text(if (currentStep == 0) "Start tutorial" else "Continue step ${currentStep + 1}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

private fun openShizukuPlayStore(context: android.content.Context) {
    val playUri = Uri.parse("market://details?id=moe.shizuku.privileged.api")
    val webUri = Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, playUri)) }
        .onFailure { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, webUri)) } }
}

private fun openShizukuGitHub(context: android.content.Context) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RikkaApps/Shizuku/releases")))
    }
}
