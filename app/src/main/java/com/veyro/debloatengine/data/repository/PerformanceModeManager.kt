package com.veyro.debloatengine.data.repository

import android.content.Context
import com.veyro.debloatengine.data.model.AppItem
import com.veyro.debloatengine.shizuku.ShizukuEngine

/**
 * Engine performance modes.
 *
 * Each mode bundles an animation preference with a background-policy so the
 * whole device personality changes with one tap:
 *  - ECO         → battery first (battery saver ON, gentle 0.5x animations)
 *  - PERFORMANCE → multitasking (battery saver OFF, stock 1.0x animations)
 *  - GAMING      → full game focus (animations OFF, aggressive RAM clearing)
 */
enum class PerfMode(
    val title: String,
    val tagline: String,
    val animationScale: Float,
    val batterySaver: Boolean,
    val clearRam: Boolean,
    val freezeRamHogs: Boolean,
    val description: String
) {
    ECO(
        "Eco Mode",
        "Battery first",
        0.5f,
        batterySaver = true,
        clearRam = true,
        freezeRamHogs = false,
        description = "System battery saver ON, background RAM trimmed, 0.5x animations " +
            "cut GPU work. Best for everyday scrolling and long screen-on sessions."
    ),
    PERFORMANCE(
        "Performance Mode",
        "Multitasking",
        1.0f,
        batterySaver = false,
        clearRam = true,
        freezeRamHogs = false,
        description = "Battery saver OFF so CPUs can boost, cached processes trimmed to " +
            "keep free RAM for many apps at once, stock 1.0x animations."
    ),
    GAMING(
        "Gaming Mode",
        "Game focus",
        0.0f,
        batterySaver = false,
        clearRam = true,
        freezeRamHogs = true,
        description = "Every background RAM hog is force-stopped, all cached processes " +
            "killed and animations switched OFF — then your game launches with the " +
            "entire device focused on it."
    );

    val animationLabel: String get() = when (this) {
        GAMING -> "Animations OFF"
        else -> "${animationScale}x animations"
    }
}

/**
 * Applies modes via Shizuku and persists the user's choice.
 * Default selection is ECO (battery first).
 */
object PerformanceModeManager {

    private const val PREFS = "veyro_modes"
    private const val KEY_MODE = "current_mode"

    fun current(context: Context): PerfMode {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_MODE, null)
        return stored?.let { runCatching { PerfMode.valueOf(it) }.getOrNull() } ?: PerfMode.ECO
    }

    fun setCurrent(context: Context, mode: PerfMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_MODE, mode.name).apply()
    }

    /**
     * Applies a mode through Shizuku. Returns a human-readable action log.
     * When [launchPackage] is set (Gaming boost), the app is launched last.
     */
    suspend fun apply(
        context: Context,
        mode: PerfMode,
        ramHogPackages: List<AppItem>,
        launchPackage: String? = null
    ): List<String> {
        val log = mutableListOf<String>()
        setCurrent(context, mode)

        // 1. Animation preference
        val scaleRes = ShizukuEngine.setAnimationScale(mode.animationScale)
        log += if (scaleRes.isSuccess) "Animations → ${mode.animationLabel}" else "Animation scale failed: ${scaleRes.stderr}"

        // 2. Battery saver (Eco on / others off)
        val bsRes = ShizukuEngine.setBatterySaver(mode.batterySaver)
        log += if (bsRes.isSuccess) {
            if (mode.batterySaver) "System battery saver → ON" else "System battery saver → OFF"
        } else "Battery saver toggle failed: ${bsRes.stderr}"

        // 3. Clear cached/background RAM
        if (mode.clearRam) {
            val killRes = ShizukuEngine.killBackgroundProcesses()
            log += if (killRes.isSuccess) "Background cached processes cleared" else "RAM clear failed: ${killRes.stderr}"
        }

        // 4. Gaming: force-stop every resident RAM hog
        if (mode.freezeRamHogs) {
            var stopped = 0
            ramHogPackages.filter { it.isInstalled && !it.isProtected }.forEach { hog ->
                val res = ShizukuEngine.forceStop(hog.packageName)
                if (res.isSuccess || res.exitCode == 0) stopped++
            }
            log += "Force-stopped $stopped resident RAM hog(s)"
        }

        // 5. Launch the focused game
        if (launchPackage != null) {
            val launchRes = ShizukuEngine.launchApp(launchPackage)
            log += if (launchRes.exitCode == 0) "Launched focus app" else "Launch failed: ${launchRes.stderr}"
        }

        return log
    }
}
