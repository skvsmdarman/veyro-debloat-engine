@file:Suppress("UnusedMaterial3ScaffoldPaddingParameter")

package com.veyro.debloatengine.ui.setup

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
            "After a phone reboot, Wireless debugging stops; start Shizuku again using Step 2.",
        ),
    ),
)

/**
 * Drop-in, native Compose walkthrough for Veyro's Setup Guide.
 * The host should keep its own navigation and pass [onClose] and [onFinished].
 * Illustrations are small Compose shapes and text; this screen adds no Lottie runtime.
 */
@Composable
fun ShizukuSetupScreen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    onFinished: () -> Unit = {},
    onInstallShizuku: (() -> Unit)? = null,
) {
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var showingOverview by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val installClick = onInstallShizuku ?: { openShizukuPlayStore(context) }

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
                fadeIn(tween(160, easing = FastOutSlowInEasing)) togetherWith
                    fadeOut(tween(100)) using SizeTransform(clip = false)
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
        TutorialIllustration(stepIndex = stepIndex, hint = step.hint)
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
                    Text(listOf("↓", "⌁", "✓", "✓")[stepIndex], color = VeyroLilac, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
                    NoteCard("After a reboot", "Wireless debugging resets. Repeat Step 2 only—usually about 30 seconds.", tone = NoteTone.Warm)
                }
                2 -> {
                    Spacer(Modifier.height(13.dp))
                    NoteCard("Connected", "The green badge confirms Veyro has Shizuku access.", tone = NoteTone.Green)
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

@Composable
private fun TutorialIllustration(stepIndex: Int, hint: String) {
    val context = LocalContext.current
    val motionAllowed = remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        }.getOrDefault(true)
    }
    val transition = rememberInfiniteTransition(label = "setup illustration")
    val floatY by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (motionAllowed) 4f else 0f,
        animationSpec = infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "gentle illustration float",
    )
    val permissionPulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (stepIndex == 2 && motionAllowed) 1.025f else 1f,
        animationSpec = infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "permission prompt pulse",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(188.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.radialGradient(listOf(Color(0xFF301A48), Color(0xFF21112F), Color(0xFF1A1026))))
            .border(1.dp, Color.White.copy(alpha = .06f), RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color(0x337540D4), radius = 67.dp.toPx(), center = Offset(0f, size.height))
            drawCircle(Color(0x338E58F9), radius = 58.dp.toPx(), center = Offset(size.width, 0f))
            drawCircle(Color(0x66B897FF), radius = 2.2.dp.toPx(), center = Offset(size.width * .22f, size.height * .28f))
            drawCircle(Color(0x66B897FF), radius = 2.2.dp.toPx(), center = Offset(size.width * .79f, size.height * .27f))
            drawCircle(Color(0x66B897FF), radius = 2.2.dp.toPx(), center = Offset(size.width * .76f, size.height * .79f))
        }
        Box(
            modifier = Modifier
                .graphicsLayer { translationY = -floatY.dp.toPx() }
                .shadow(16.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = .36f))
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF281B35), Color(0xFF140C1F))))
                .border(1.dp, Color.White.copy(alpha = .15f), RoundedCornerShape(20.dp))
                .padding(10.dp),
        ) {
            IllustrationPhone(stepIndex = stepIndex, permissionPulse = permissionPulse)
        }
        HintPill(
            text = hint,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 35.dp),
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
private fun IllustrationPhone(stepIndex: Int, permissionPulse: Float) {
    Column(modifier = Modifier.width(if (stepIndex == 1) 190.dp else 202.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("9:41", color = Color(0xFFD4C8E1), fontSize = 8.sp)
            Box(Modifier.width(28.dp).height(3.dp).clip(CircleShape).background(Color(0xFF5C4C6E)))
            Text("● 96%", color = Color(0xFFD4C8E1), fontSize = 8.sp)
        }
        Spacer(Modifier.height(8.dp))
        when (stepIndex) {
            0 -> InstallScene()
            1 -> PairScene()
            2 -> PermissionScene(permissionPulse = permissionPulse)
            else -> EngineScene()
        }
    }
}

@Composable
private fun InstallScene() {
    IllustrationScreen {
        Text("Google Play", color = VeyroMuted, fontSize = 8.sp)
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(31.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(Color(0xFF5B91D2), Color(0xFF2D5187)))), contentAlignment = Alignment.Center) {
                Text("▶", color = Color.White, fontSize = 14.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Shizuku", color = VeyroText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("by Rikka", color = VeyroMuted, fontSize = 8.sp)
            }
            Text("INSTALL", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF6636BD)).padding(horizontal = 8.dp, vertical = 5.dp))
        }
        Spacer(Modifier.height(11.dp))
        PlaceholderLine(1f); Spacer(Modifier.height(5.dp)); PlaceholderLine(.74f); Spacer(Modifier.height(5.dp)); PlaceholderLine(.54f)
    }
}

@Composable
private fun PairScene() {
    IllustrationScreen {
        Text("Developer options", color = VeyroMuted, fontSize = 8.sp)
        Spacer(Modifier.height(9.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(Color(0xFF261735)).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Wireless debugging", color = VeyroText, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
            Box(Modifier.width(24.dp).height(13.dp).clip(CircleShape).background(Color(0xFF4C2A77)), contentAlignment = Alignment.CenterEnd) {
                Box(Modifier.padding(end = 2.dp).size(9.dp).clip(CircleShape).background(VeyroLilac))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Pairing code", color = VeyroMuted, fontSize = 8.sp)
        Text("042   761", color = VeyroText, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
    }
}

@Composable
private fun PermissionScene(permissionPulse: Float) {
    IllustrationScreen {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF12291E)).padding(7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(VeyroGreen))
            Text("ENGINE ONLINE", color = VeyroGreen, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(8.dp))
        Text("Shizuku permission", color = VeyroMuted, fontSize = 8.sp)
        Text("Veyro Debloat Engine", color = VeyroMuted, fontSize = 8.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(7.dp))
        PlaceholderLine(1f); Spacer(Modifier.height(5.dp)); PlaceholderLine(.76f)
    }
    Surface(
        modifier = Modifier
            .padding(top = 22.dp)
            .width(148.dp)
            .graphicsLayer { scaleX = permissionPulse; scaleY = permissionPulse }
            .shadow(12.dp, RoundedCornerShape(13.dp)),
        color = Color(0xFFF8F5FB),
        shape = RoundedCornerShape(13.dp),
    ) {
        Column(Modifier.padding(10.dp)) {
            Text("Allow Veyro?", color = Color(0xFF30263A), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
            Text("Let this app use Shizuku service", color = Color(0xFF655A70), fontSize = 7.sp, lineHeight = 10.sp, modifier = Modifier.padding(top = 4.dp, bottom = 7.dp))
            Text("ALLOW", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp)).background(Color(0xFF7948D7)).padding(vertical = 5.dp))
        }
    }
}

@Composable
private fun EngineScene() {
    IllustrationScreen {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF12291E)).padding(7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(VeyroGreen))
            Text("ENGINE ONLINE", color = VeyroGreen, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
        }
        Text("Veyro tools", color = VeyroMuted, fontSize = 8.sp, modifier = Modifier.padding(top = 7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 7.dp)) {
            FeatureMiniCard("❄", "Freeze")
            FeatureMiniCard("⚡", "Cache flush")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 5.dp)) {
            FeatureMiniCard("◉", "DNS")
            FeatureMiniCard("✦", "Tools Lab")
        }
    }
}

@Composable
private fun IllustrationScreen(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1B1027))
            .padding(10.dp),
        content = content,
    )
}

@Composable
private fun PlaceholderLine(fraction: Float) {
    Box(
        Modifier
            .fillMaxWidth(fraction)
            .height(3.dp)
            .clip(CircleShape)
            .background(Color(0xFF443250)),
    )
}

@Composable
private fun RowScope.FeatureMiniCard(icon: String, label: String) {
    Row(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF28183A))
            .padding(horizontal = 6.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(icon, color = VeyroLilac, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xFFE6D9F4), fontSize = 7.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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
        content = content,
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
                    Text("${index + 1}", color = VeyroText, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
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
