package com.veyro.debloatengine.data.model

import android.graphics.drawable.Drawable
import kotlinx.serialization.Serializable

/** Safety tiers for packages. */
enum class Tier(val displayName: String, val level: Int) {
    SAFE("Safe", 1),
    PROBABLY_SAFE("Advanced", 2),
    RISKY("Risky", 3),
    PROTECTED("Locked", 4)
}

/** Shizuku connection / permission state. */
enum class ShizukuState {
    NOT_INSTALLED,
    STOPPED,
    PERMISSION_DENIED,
    READY
}

/** Storage metrics for the Storage Unchoker gauge. */
data class StorageInfo(
    val totalBytes: Long = 0L,
    val availableBytes: Long = 0L
) {
    val usedBytes: Long get() = (totalBytes - availableBytes).coerceAtLeast(0)
    val usedPercent: Float get() = if (totalBytes > 0) usedBytes.toFloat() / totalBytes * 100f else 0f
    val isThrottling: Boolean get() = usedPercent > 85f
    val totalGb: String get() = String.format("%.1f", totalBytes / GB)
    val freeGb: String get() = String.format("%.1f", availableBytes / GB)

    private companion object { const val GB = 1024f * 1024f * 1024f }
}

/** RAM metrics for the dashboard gauge. */
data class RamInfo(
    val totalBytes: Long = 0L,
    val availableBytes: Long = 0L
) {
    val usedPercent: Float
        get() = if (totalBytes > 0) (totalBytes - availableBytes).toFloat() / totalBytes * 100f else 0f
    val usedGb: String get() = String.format("%.1f", (totalBytes - availableBytes) / GB)
    val totalGb: String get() = String.format("%.0f", totalBytes / GB)

    private companion object { const val GB = 1024f * 1024f * 1024f }
}

/** JSON schema matching Universal Android Debloater rules (assets/debloat_list.json). */
@Serializable
data class DebloatRule(
    val packageName: String,
    val appName: String,
    val tier: String,
    val tags: List<String> = emptyList(),
    val description: String = "",
    val warning: String = ""
)

/** A package row in the debloat lists. */
data class AppItem(
    val packageName: String,
    val appName: String,
    val isInstalled: Boolean,
    val isEnabled: Boolean,
    val tier: Tier,
    val tags: List<String>,
    val description: String,
    val warning: String,
    val isProtected: Boolean,
    val isSystemApp: Boolean,
    val reason: String = "",
    val isGame: Boolean = false,
    val iconDrawable: Drawable? = null
)

/** Result of a shell command executed through Shizuku. */
data class CommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    val isSuccess: Boolean get() = exitCode == 0
    val combined: String get() = (stdout.ifBlank { stderr }).trim()
}

/** Live state of a batch debloat / restore run. */
data class BatchProgress(
    val isRunning: Boolean = false,
    val currentPackage: String = "",
    val index: Int = 0,
    val total: Int = 0,
    val logs: List<String> = emptyList(),
    val finished: Boolean = false
)

/** Remote update manifest fetched from GitHub (update.json). */
@Serializable
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val releaseNotes: String = "",
    val downloadUrl: String,
    val mandatory: Boolean = false
)

enum class DownloadState { IDLE, DOWNLOADING, DOWNLOADED, FAILED }

/**
 * How the engine neutralizes a package. Ordered from safest to deepest.
 *  FREEZE    → app + data stay 100% intact, process can never start (suspend).
 *  DISABLE   → app stays installed with all data, launcher icon hidden.
 *  UNINSTALL → removed for user 0 only, data kept, restorable via install-existing.
 */
enum class DebloatStrategy(
    val label: String,
    val summary: String,
    val detail: String
) {
    FREEZE(
        "🧊 Freeze",
        "App + data kept, never runs",
        "Safest option. The app and all its data stay on your phone untouched — it simply " +
            "can't launch or wake itself up anymore. One tap in Restore brings it back instantly."
    ),
    DISABLE(
        "⏸ Disable",
        "Installed + data kept, icon hidden",
        "The app stays installed with every byte of its data; only the launcher icon and " +
            "background rights are switched off. Fully reversible."
    ),
    UNINSTALL(
        "🗑 Uninstall",
        "Removed for you, data kept, restorable",
        "Classic debloat: removed for user 0 while keeping app data caches. Restorable any " +
            "time via the Restore tab (install-existing)."
    )
}
