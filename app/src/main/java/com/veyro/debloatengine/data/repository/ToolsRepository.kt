package com.veyro.debloatengine.data.repository

import android.content.Context
import com.veyro.debloatengine.data.model.AppItem
import com.veyro.debloatengine.data.model.CommandResult
import com.veyro.debloatengine.shizuku.ShizukuEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Advanced hardware & system tools:
 *  - Speed Compiler (AOT speed-profile), Battery analyzer, Thermal status,
 *    RAM Expansion (swap) auditor + adjust, Sensor kill-switch,
 *    Logcat flush/shrink, RRO overlay purger, resolution downscaler,
 *    touch latency, per-app cache clear, APK backup vault, BG-data block.
 */
object ToolsRepository {

    // ------------------------------------------------------------------
    // Speed Compiler (AOT)
    // ------------------------------------------------------------------
    suspend fun compileSpeedProfile(): CommandResult =
        ShizukuEngine.exec("cmd package compile -m speed-profile -a")

    // ------------------------------------------------------------------
    // Battery analyzer (dumpsys battery)
    // ------------------------------------------------------------------
    data class BatteryInfo(
        val rawLines: List<String> = emptyList(),
        val level: Int = -1,
        val temperatureC: Float = 0f,
        val voltageMv: Int = 0,
        val currentMa: Float = 0f,
        val cycleCount: Int = -1,
        val health: String = ""
    ) {
        val chargingWatts: Float get() = voltageMv * kotlin.math.abs(currentMa) / 1_000_000f
    }

    suspend fun readBattery(): BatteryInfo = withContext(Dispatchers.IO) {
        val out = ShizukuEngine.exec("dumpsys battery")
        fun field(name: String): String =
            out.combined.lines().firstOrNull { it.contains(name, ignoreCase = true) }
                ?.substringAfter(':')?.trim() ?: ""

        val level = field("level").toIntOrNull() ?: -1
        val temp = (field("temperature").toIntOrNull() ?: 0) / 10f
        val voltage = field("voltage").toIntOrNull() ?: 0
        val currentRaw = field("current now").toIntOrNull()
            ?: field("current_now").toIntOrNull()
            ?: field("Charge counter").toIntOrNull() ?: 0
        // Many devices report µA; convert to mA when magnitude is large.
        val currentMa = if (kotlin.math.abs(currentRaw) > 100000) currentRaw / 1000f else currentRaw.toFloat()
        val cycles = field("cycle").toIntOrNull()
            ?: field("cycle_count").toIntOrNull()
            ?: field("Charge counter").let { -1 }
        BatteryInfo(
            rawLines = out.combined.lines().filter { it.isNotBlank() }.take(24),
            level = level,
            temperatureC = temp,
            voltageMv = voltage,
            currentMa = currentMa,
            cycleCount = cycles,
            health = field("health").ifBlank { "Unknown" }
        )
    }

    // ------------------------------------------------------------------
    // Thermal throttling detector
    // ------------------------------------------------------------------
    data class ThermalInfo(val statusLabel: String, val rawLine: String) {
        val severity: Int
            get() = when {
                statusLabel.contains("CRITICAL", true) -> 4
                statusLabel.contains("SEVERE", true) -> 3
                statusLabel.contains("MODERATE", true) -> 2
                statusLabel.contains("LIGHT", true) -> 1
                else -> 0
            }
    }

    suspend fun readThermal(): ThermalInfo = withContext(Dispatchers.IO) {
        val out = ShizukuEngine.exec(
            "dumpsys thermalservice | grep -iE 'mStatus|ThermalStatus|status:' | head -4"
        )
        val line = out.combined.lineSequence().firstOrNull { it.isNotBlank() } ?: ""
        val label = listOf("CRITICAL", "SEVERE", "MODERATE", "LIGHT", "NONE")
            .firstOrNull { out.combined.contains(it, ignoreCase = true) } ?: "NONE"
        ThermalInfo(label, line)
    }

    // ------------------------------------------------------------------
    // RAM Expansion (swap / zram) auditor
    // ------------------------------------------------------------------
    data class SwapInfo(
        val swapTotalMb: Int = 0,
        val swapFreeMb: Int = 0,
        val raw: List<String> = emptyList()
    ) {
        val usedMb: Int get() = (swapTotalMb - swapFreeMb).coerceAtLeast(0)
        val active: Boolean get() = swapTotalMb > 0
        val heavyUse: Boolean get() = active && usedMb > swapTotalMb * 0.6f
    }

    suspend fun readSwap(): SwapInfo = withContext(Dispatchers.IO) {
        val out = ShizukuEngine.exec("cat /proc/meminfo | grep -iE 'SwapTotal|SwapFree|Zram'")
        fun mb(name: String): Int {
            val v = out.combined.lines().firstOrNull { it.contains(name, ignoreCase = true) }
                ?.substringAfter(':')?.trim()?.split(' ')?.firstOrNull()?.toLongOrNull() ?: 0L
            return (v / 1024L).toInt()
        }
        SwapInfo(
            swapTotalMb = mb("SwapTotal"),
            swapFreeMb = mb("SwapFree"),
            raw = out.combined.lines().filter { it.isNotBlank() }
        )
    }

    /**
     * Best-effort RAM Expansion adjust. OEM keys differ; we write the two
     * most common secure/global keys. A reboot is required for the change
     * to take effect on most skins.
     */
    suspend fun setRamExpansionSizeGb(gb: Int): CommandResult {
        val cmds = listOf(
            "settings put secure memory_extension $gb",
            "settings put secure ram_expand_size_list $gb",
            "settings put global ram_expand_size $gb",
            "settings put secure swappable_enabled ${if (gb > 0) 1 else 0}"
        )
        return ShizukuEngine.exec(cmds.joinToString(" && "))
    }

    // ------------------------------------------------------------------
    // Sensor privacy kill-switch
    // ------------------------------------------------------------------
    suspend fun readSensorsOff(): Boolean =
        ShizukuEngine.exec("settings get global sensors_off").combined.trim() == "1"

    suspend fun setSensorsOff(off: Boolean): CommandResult =
        ShizukuEngine.exec("settings put global sensors_off ${if (off) 1 else 0}")

    // ------------------------------------------------------------------
    // Logcat flush + shrink
    // ------------------------------------------------------------------
    suspend fun flushLogs(): CommandResult =
        ShizukuEngine.exec("logcat -c && logcat -G 64K")

    // ------------------------------------------------------------------
    // RRO overlay purger
    // ------------------------------------------------------------------
    data class OverlayInfo(
        val packageName: String,
        val enabled: Boolean,
        val isTarget: String
    )

    suspend fun listOverlays(): List<OverlayInfo> = withContext(Dispatchers.IO) {
        val out = ShizukuEngine.exec("cmd overlay list")
        out.combined.lines().mapNotNull { raw ->
            val line = raw.trim()
            if (line.startsWith("[")) {
                val enabled = line.startsWith("[x]")
                val pkg = line.substringAfter(']').trim().substringBefore(' ').trim()
                val target = line.substringAfter("->").trim()
                if (pkg.isNotBlank()) OverlayInfo(pkg, enabled, target) else null
            } else null
        }
    }

    /** Carrier / theme / region overlays are the intrusive ones. */
    fun isIntrusiveOverlay(pkg: String): Boolean =
        listOf("carrier", "theme", "region", "res-autonotify", "oplus-res", "miui.theme")
            .any { pkg.contains(it, ignoreCase = true) }

    suspend fun setOverlayEnabled(pkg: String, enable: Boolean): CommandResult =
        ShizukuEngine.exec("cmd overlay ${if (enable) "enable" else "disable"} --user 0 $pkg")

    // ------------------------------------------------------------------
    // Resolution downscaler (FPS booster) — density-aware
    // ------------------------------------------------------------------
    data class ResolutionInfo(val native: String, val current: String) {
        val overridden: Boolean get() = current != native
    }

    suspend fun readResolution(): ResolutionInfo = withContext(Dispatchers.IO) {
        val out = ShizukuEngine.exec("wm size")
        val native = Regex("Physical size:\\s*(\\S+)").find(out.combined)?.groupValues?.get(1) ?: ""
        val override = Regex("Override size:\\s*(\\S+)").find(out.combined)?.groupValues?.get(1)
        ResolutionInfo(native, override ?: native)
    }

    /** Current physical density (dpi) from `wm density`, override if present. */
    suspend fun readDensity(): Int = withContext(Dispatchers.IO) {
        val out = ShizukuEngine.exec("wm density")
        val override = Regex("Override density:\\s*(\\d+)").find(out.combined)?.groupValues?.get(1)
        val physical = Regex("Physical density:\\s*(\\d+)").find(out.combined)?.groupValues?.get(1)
        override?.toIntOrNull() ?: physical?.toIntOrNull() ?: 420
    }

    suspend fun setResolution(w: Int, h: Int): CommandResult =
        ShizukuEngine.exec("wm size ${w}x$h")

    suspend fun resetResolution(): CommandResult =
        ShizukuEngine.exec("wm size reset && wm density reset")

    /** 2/3-scale 720-class resolution for the native aspect ratio. */
    fun downscaleTarget(native: String): Pair<Int, Int>? {
        val m = Regex("(\\d+)x(\\d+)").find(native) ?: return null
        val (w, h) = m.destructured
        val wi = w.toIntOrNull() ?: return null
        val hi = h.toIntOrNull() ?: return null
        return Pair((wi * 2 / 3) - ((wi * 2 / 3) % 10), (hi * 2 / 3) - ((hi * 2 / 3) % 10))
    }

    /**
     * FPS-boost downscale WITH density scaling.
     *
     * Changing only `wm size` makes the whole UI render giant (same dp on
     * fewer px). Scaling density by the same factor keeps every element at
     * its normal physical size while the GPU renders ~44% fewer pixels —
     * that's where the FPS actually comes from.
     */
    suspend fun applyScaledDownscale(native: String, nativeDensity: Int): CommandResult {
        val target = downscaleTarget(native) ?: return CommandResult(-1, "", "Could not parse native resolution")
        val (w, h) = target
        val nativeW = Regex("(\\d+)x(\\d+)").find(native)?.groupValues?.get(1)?.toIntOrNull()
            ?: return CommandResult(-1, "", "Could not parse native width")
        val scaledDensity = (nativeDensity * w / nativeW).coerceAtLeast(120)
        return ShizukuEngine.exec("wm size ${w}x$h && wm density $scaledDensity")
    }

    // ------------------------------------------------------------------
    // Competitive touch latency
    // ------------------------------------------------------------------
    suspend fun setCompetitiveTouch(on: Boolean): CommandResult =
        if (on) {
            ShizukuEngine.exec("settings put system pointer_speed 7 && settings put secure tap_duration_threshold 0")
        } else {
            ShizukuEngine.exec("settings put system pointer_speed 0 && settings delete secure tap_duration_threshold")
        }

    // ------------------------------------------------------------------
    // Per-app cache clear + APK backup vault + BG-data block
    // ------------------------------------------------------------------
    /**
     * Per-app cache cleanup. `pm clear --cache-only` hangs indefinitely on
     * some ColorOS/HyperOS builds, so we use the reliable recipe instead:
     * stop the app dead (releases its cache files), then trim the system
     * cache pool — which frees every app's cached data instantly.
     */
    suspend fun clearAppCache(pkg: String): CommandResult {
        ShizukuEngine.exec("am force-stop $pkg")
        val res = ShizukuEngine.exec("pm trim-caches 999G")
        return if (res.exitCode == 0) {
            CommandResult(0, "Stopped $pkg and trimmed system caches", "")
        } else res
    }

    /**
     * Extracts the installed APK(s) of a package into /sdcard/Veyro/Backups/
     * via Shizuku shell — 100% offline standalone backup.
     */
    suspend fun backupApk(item: AppItem): CommandResult = withContext(Dispatchers.IO) {
        val pkg = item.packageName
        val pathRes = ShizukuEngine.exec("pm path $pkg")
        val paths = pathRes.combined.lines()
            .map { it.removePrefix("package:").trim() }
            .filter { it.isNotBlank() }
        if (paths.isEmpty()) {
            return@withContext CommandResult(-1, "", "No APK path found for $pkg")
        }
        val safeName = item.appName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val mkdir = ShizukuEngine.exec("mkdir -p /sdcard/Veyro/Backups")
        val cpCmds = paths.mapIndexed { i, p ->
            val suffix = if (paths.size > 1) "_$i.apk" else ".apk"
            "cp \"$p\" \"/sdcard/Veyro/Backups/${safeName}$suffix\""
        }
        val cp = ShizukuEngine.exec((listOf("true") + cpCmds).joinToString(" && "))
        val combined = (if (mkdir.exitCode == 0) "" else "mkdir: ${mkdir.stderr}\n") +
            cp.combined + "\nSaved to /sdcard/Veyro/Backups/"
        CommandResult(cp.exitCode, combined, cp.stderr)
    }

    /** Zero-VPN background data block (AppOps data saver). */
    suspend fun blockBackgroundData(pkg: String, block: Boolean): CommandResult =
        ShizukuEngine.exec(
            "cmd appops set $pkg DATA_SAVER ${if (block) "allow" else "ignore"} && " +
                "cmd appops set $pkg RUN_IN_BACKGROUND ${if (block) "ignore" else "allow"}"
        )
}
